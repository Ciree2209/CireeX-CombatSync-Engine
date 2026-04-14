package com.cireex.combatsync.lag;

import com.cireex.combatsync.CireeXCombatSync;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lag-compensated hit detection — evaluates hits against past positions
 * (historical reconciliation). We do NOT move players forward.
 *
 * Optimized for 2000+ players: only tracks players in active combat,
 * idles are ignored until they get hit/hit someone.
 */
public class LagCompensationEngine {

    private static final int MAX_COMPENSATION_MS = 150;
    private static final long SNAPSHOT_INTERVAL_TICKS = 1L;
    private static final long COMBAT_TIMEOUT_MS = 10000; // 10 seconds after last combat

    private final CireeXCombatSync plugin;
    private final Map<UUID, SnapshotBuffer> playerBuffers = new ConcurrentHashMap<>();
    private final Set<UUID> activeCombatPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> lastCombatTime = new ConcurrentHashMap<>();

    private BukkitTask snapshotTask;
    private BukkitTask cleanupTask;

    // reflection cache for 1.8.8 ping access
    private Method getHandleMethod;
    private Field pingField;

    public LagCompensationEngine(CireeXCombatSync plugin) {
        this.plugin = plugin;
        initReflection();
        startSnapshotTask();
        startCleanupTask();
    }

    private void initReflection() {
        try {
            String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
            getHandleMethod = craftPlayerClass.getMethod("getHandle");
            Class<?> entityPlayerClass = Class.forName("net.minecraft.server." + version + ".EntityPlayer");
            pingField = entityPlayerClass.getField("ping");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to initialize ping reflection: " + e.getMessage());
        }
    }

    public int getPlayerPing(Player player) {
        try {
            if (getHandleMethod != null && pingField != null) {
                Object entityPlayer = getHandleMethod.invoke(player);
                return pingField.getInt(entityPlayer);
            }
        } catch (Exception e) {
            // fall through
        }
        return 50;
    }

    private void startSnapshotTask() {
        // one snapshot per tick per active combat player — no chunking
        // ensures 1-tick resolution for everyone fighting; idle players are skipped
        snapshotTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (activeCombatPlayers.isEmpty()) {
                return;
            }

            for (UUID uuid : activeCombatPlayers) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    recordSnapshotInternal(player);
                }
            }
        }, SNAPSHOT_INTERVAL_TICKS, SNAPSHOT_INTERVAL_TICKS);
    }

    private void startCleanupTask() {
        cleanupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();
            int removed = 0;

            Iterator<Map.Entry<UUID, Long>> it = lastCombatTime.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Long> entry = it.next();
                if (now - entry.getValue() > COMBAT_TIMEOUT_MS) {
                    UUID uuid = entry.getKey();
                    it.remove();
                    activeCombatPlayers.remove(uuid);
                    playerBuffers.remove(uuid);
                    removed++;
                }
            }

            if (removed > 0) {
                plugin.debug("Cleaned up " + removed + " inactive player buffers.");
            }
        }, 20L * 60 * 5, 20L * 60 * 5); // every 5 minutes
    }

    /** Only players in active combat get snapshot tracking. */
    public void markInCombat(Player player) {
        UUID uuid = player.getUniqueId();
        activeCombatPlayers.add(uuid);
        lastCombatTime.put(uuid, System.currentTimeMillis());
    }

    private void recordSnapshotInternal(Player player) {
        SnapshotBuffer buffer = playerBuffers.computeIfAbsent(
                player.getUniqueId(),
                k -> new SnapshotBuffer());
        buffer.addSnapshot(new PositionSnapshot(player));
    }

    public void recordSnapshot(Player player) {
        markInCombat(player);
        recordSnapshotInternal(player);
    }

    public void initializePlayer(Player player) {
        // don't pre-allocate — wait until they enter combat
        plugin.debug("Player initialized for lag comp: " + player.getName());
    }

    public SnapshotBuffer getBuffer(Player player) {
        return playerBuffers.computeIfAbsent(
                player.getUniqueId(),
                k -> new SnapshotBuffer());
    }

    /** Half-RTT (one-way latency), clamped to 150ms max. */
    public long getCompensationDelay(Player attacker) {
        int ping = Math.min(getPlayerPing(attacker), 200);
        long delay = ping / 2;
        return Math.min(delay, MAX_COMPENSATION_MS);
    }

    public PositionSnapshot getCompensatedSnapshot(Player attacker, Player victim) {
        markInCombat(attacker);
        markInCombat(victim);

        long delay = getCompensationDelay(attacker);
        long targetTime = System.currentTimeMillis() - delay;

        SnapshotBuffer buffer = getBuffer(victim);
        return buffer.getSnapshotAt(targetTime);
    }

    public PingTier getPingTier(Player player) {
        int ping = Math.min(getPlayerPing(player), 200);

        if (ping <= 60)
            return PingTier.LOW;
        if (ping <= 120)
            return PingTier.MEDIUM;
        return PingTier.HIGH;
    }

    /** Slightly normalizes friction to counter packet delay drag. */
    public double getLatencyModifier(Player attacker) {
        PingTier tier = getPingTier(attacker);

        switch (tier) {
            case LOW:
                return 1.0;
            case MEDIUM:
                return 0.97;
            case HIGH:
                return 0.94;
            default:
                return 1.0;
        }
    }

    public void removePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        playerBuffers.remove(uuid);
        activeCombatPlayers.remove(uuid);
        lastCombatTime.remove(uuid);
    }

    public int getActivePlayerCount() {
        return activeCombatPlayers.size();
    }

    public int getBufferCount() {
        return playerBuffers.size();
    }

    public void cleanup() {
        if (snapshotTask != null) {
            snapshotTask.cancel();
        }
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
        playerBuffers.clear();
        activeCombatPlayers.clear();
        lastCombatTime.clear();
    }

    public enum PingTier {
        LOW,    // 0-60ms
        MEDIUM, // 61-120ms
        HIGH    // 121-200ms
    }
}
