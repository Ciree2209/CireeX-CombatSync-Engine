package com.cireex.combatsync.combat;

import com.cireex.combatsync.CireeXCombatSync;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ComboTracker - Tracks hit combos per player for combo-lock prevention.
 * 
 * OPTIMIZED FOR 2000+ PLAYERS:
 * - ConcurrentHashMap for thread safety
 * - Periodic cleanup of inactive players
 * 
 * Combo-lock prevention rule:
 * - If player receives ≥3 hits within 10 ticks
 * - Apply escape boost to prevent infinite zero-KB juggles
 */
public class ComboTracker {

    private final CireeXCombatSync plugin;

    private final Map<UUID, Deque<Long>> receivedHits = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<Long>> dealtHits = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> currentCombo = new ConcurrentHashMap<>();
    // air combo data for elasticity physics
    private final Map<UUID, Long> lastGroundTime = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> airComboCount = new ConcurrentHashMap<>();
    private final Map<UUID, Double> lastVelocityY = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastActivity = new ConcurrentHashMap<>();
    private BukkitTask cleanupTask;

    public ComboTracker(CireeXCombatSync plugin) {
        this.plugin = plugin;
        startCleanupTask();
    }

    private void startCleanupTask() {
        cleanupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();
            long timeout = 60000;
            int removed = 0;

            Iterator<Map.Entry<UUID, Long>> it = lastActivity.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Long> entry = it.next();
                if (now - entry.getValue() > timeout) {
                    UUID uuid = entry.getKey();
                    it.remove();
                    receivedHits.remove(uuid);
                    dealtHits.remove(uuid);
                    currentCombo.remove(uuid);
                    removed++;
                }
            }

            if (removed > 0) {
                plugin.debug("ComboTracker cleaned up " + removed + " inactive players.");
            }
        }, 20L * 60 * 2, 20L * 60 * 2); // Every 2 minutes
    }

    public void recordHitReceived(Player victim) {
        UUID uuid = victim.getUniqueId();
        long currentTick = getCurrentTick();

        lastActivity.put(uuid, System.currentTimeMillis());

        Deque<Long> hits = receivedHits.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        hits.addFirst(currentTick);

        int tickWindow = plugin.getConfigReloader().getComboLockTickWindow();
        cleanOldHits(hits, currentTick, tickWindow);
        currentCombo.put(uuid, 0);
    }

    public void recordHitDealt(Player attacker) {
        UUID uuid = attacker.getUniqueId();
        long currentTick = getCurrentTick();

        lastActivity.put(uuid, System.currentTimeMillis());

        Deque<Long> hits = dealtHits.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        hits.addFirst(currentTick);
        currentCombo.merge(uuid, 1, Integer::sum);
        cleanOldHits(hits, currentTick, 40); // 2-second combo window
    }

    public void recordAirHit(Player victim) {
        UUID uuid = victim.getUniqueId();
        if (isVictimAirborne(victim)) {
            airComboCount.merge(uuid, 1, Integer::sum);
        }
    }

    public void recordGroundTouch(Player victim) {
        UUID uuid = victim.getUniqueId();
        lastGroundTime.put(uuid, System.currentTimeMillis());
        airComboCount.put(uuid, 0);
    }

    public boolean isVictimAirborne(Player victim) {
        UUID uuid = victim.getUniqueId();
        Long lastGround = lastGroundTime.get(uuid);

        // airborne if no ground touch in the last 200ms (~4 ticks)
        if (lastGround == null) {
            return !plugin.getGroundStateResolver().isOnGround(victim);
        }

        long timeSinceGround = System.currentTimeMillis() - lastGround;
        return timeSinceGround > 200 && !plugin.getGroundStateResolver().isOnGround(victim);
    }

    public int getAirComboCount(Player victim) {
        return airComboCount.getOrDefault(victim.getUniqueId(), 0);
    }

    public void recordVelocityY(Player victim, double velocityY) {
        lastVelocityY.put(victim.getUniqueId(), velocityY);
    }

    public double getLastVelocityY(Player victim) {
        return lastVelocityY.getOrDefault(victim.getUniqueId(), 0.0);
    }

    public int getRecentHitsReceived(Player player) {
        UUID uuid = player.getUniqueId();
        Deque<Long> hits = receivedHits.get(uuid);

        if (hits == null || hits.isEmpty()) {
            return 0;
        }

        long currentTick = getCurrentTick();
        int tickWindow = plugin.getConfigReloader().getComboLockTickWindow();

        int count = 0;
        for (Long hitTick : hits) {
            if (currentTick - hitTick <= tickWindow) {
                count++;
            }
        }

        return count;
    }

    public boolean shouldApplyComboEscape(Player victim) {
        if (!plugin.getConfigReloader().isComboLockPreventionEnabled()) {
            return false;
        }

        int recentHits = getRecentHitsReceived(victim);
        int threshold = plugin.getConfigReloader().getComboLockHitThreshold();

        return recentHits >= threshold;
    }

    public int getCombo(Player player) {
        return currentCombo.getOrDefault(player.getUniqueId(), 0);
    }

    public double getEscapeHorizontalBoost() {
        return plugin.getConfigReloader().getComboLockHorizontalBoost();
    }

    public double getEscapeVerticalBoost() {
        return plugin.getConfigReloader().getComboLockVerticalBoost();
    }

    private void cleanOldHits(Deque<Long> hits, long currentTick, int maxTicks) {
        while (!hits.isEmpty() && currentTick - hits.peekLast() > maxTicks) {
            hits.removeLast();
        }
    }

    private long getCurrentTick() {
        return System.currentTimeMillis() / 50;
    }

    public void resetCombo(Player player) {
        UUID uuid = player.getUniqueId();
        receivedHits.remove(uuid);
        dealtHits.remove(uuid);
        currentCombo.remove(uuid);
        airComboCount.remove(uuid);
        lastGroundTime.remove(uuid);
        lastVelocityY.remove(uuid);
        lastActivity.remove(uuid);
    }

    public void cleanup() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
        receivedHits.clear();
        dealtHits.clear();
        currentCombo.clear();
        lastActivity.clear();
    }
}
