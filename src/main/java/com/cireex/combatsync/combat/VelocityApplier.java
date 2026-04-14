package com.cireex.combatsync.combat;

import com.cireex.combatsync.CireeXCombatSync;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * VelocityApplier - applies KB velocity with anticheat-safe rules.
 *
 * Rules: never cancel EntityVelocityEvent, never teleport, never exceed 0.42 Y,
 * single write per hit, no packet stacking.
 */
public class VelocityApplier {

    private final CireeXCombatSync plugin;

    private final Map<UUID, Long> lastApplyTick = new ConcurrentHashMap<>();
    private final Map<UUID, Vector> lastAppliedVelocity = new ConcurrentHashMap<>();

    public VelocityApplier(CireeXCombatSync plugin) {
        this.plugin = plugin;
    }

    public boolean applyVelocity(Player player, Vector velocity, KBProfile profile) {
        return applyVelocity(player, velocity, profile, 0);
    }

    public boolean applyVelocity(Player player, Vector velocity, KBProfile profile, int delayTicks) {
        if (delayTicks > 0) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    executeApply(player, velocity, profile);
                }
            }, delayTicks);
            return true;
        }

        return executeApply(player, velocity, profile);
    }

    private boolean executeApply(Player player, Vector velocity, KBProfile profile) {
        ConfigReloader config = plugin.getConfigReloader();

        // reject duplicate applies within the same tick
        if (config.isVelocityIntegrityEnabled()) {
            if (!checkIntegrity(player)) {
                if (config.isVelocityIntegrityDebug()) {
                    plugin.getLogger().warning("[VelocityIntegrity] Rejected duplicate velocity for " +
                            player.getName() + " in same tick");
                }
                return false;
            }
        }

        // smooth against TPS-dip spikes
        VelocitySmoother smoother = plugin.getVelocitySmoother();
        if (smoother != null && smoother.isEnabled()) {
            velocity = smoother.smooth(player, velocity, profile);
        }

        velocity = safetyClamp(velocity, config);
        player.setVelocity(velocity);
        recordApplication(player, velocity);

        plugin.debug("Applied velocity to " + player.getName() + ": " + formatVector(velocity));
        return true;
    }

    private boolean checkIntegrity(Player player) {
        UUID uuid = player.getUniqueId();
        long currentTick = getCurrentTick();

        Long lastTick = lastApplyTick.get(uuid);
        if (lastTick != null && lastTick == currentTick) {
            return false;
        }

        return true;
    }

    private void recordApplication(Player player, Vector velocity) {
        UUID uuid = player.getUniqueId();
        long currentTick = getCurrentTick();

        lastApplyTick.put(uuid, currentTick);
        lastAppliedVelocity.put(uuid, velocity.clone());
    }

    private Vector safetyClamp(Vector velocity, ConfigReloader config) {
        double maxY = config.getMaxVertical();
        double maxH = config.getMaxHorizontal();

        if (velocity.getY() > maxY) {
            velocity.setY(maxY);
        }

        if (Math.abs(velocity.getX()) > maxH) {
            velocity.setX(velocity.getX() > 0 ? maxH : -maxH);
        }
        if (Math.abs(velocity.getZ()) > maxH) {
            velocity.setZ(velocity.getZ() > 0 ? maxH : -maxH);
        }

        return velocity;
    }

    public Vector getLastAppliedVelocity(Player player) {
        Vector last = lastAppliedVelocity.get(player.getUniqueId());
        return last != null ? last.clone() : null;
    }

    public long getLastApplyTick(Player player) {
        return lastApplyTick.getOrDefault(player.getUniqueId(), 0L);
    }

    public void removePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        lastApplyTick.remove(uuid);
        lastAppliedVelocity.remove(uuid);
    }

    private long getCurrentTick() {
        return System.currentTimeMillis() / 50;
    }

    private String formatVector(Vector v) {
        return String.format("(%.4f, %.4f, %.4f)", v.getX(), v.getY(), v.getZ());
    }

    /** Bypasses integrity check — use only for scripted effects, not combat hits. */
    public void forceApplyVelocity(Player player, Vector velocity) {
        ConfigReloader config = plugin.getConfigReloader();
        velocity = safetyClamp(velocity, config);
        player.setVelocity(velocity);
        recordApplication(player, velocity);
    }
}
