package com.cireex.combatsync.combat;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prevents sudden velocity spikes during TPS dips via linear interpolation.
 * Only kicks in when delta exceeds the profile's smoothing threshold.
 */
public class VelocitySmoother {

    // higher factor for high-ping players = smoother but less snappy feel
    private static final double SMOOTH_FACTOR_LOW_PING = 0.82; // <120ms
    private static final double SMOOTH_FACTOR_HIGH_PING = 0.90; // >=120ms
    private static final int PING_THRESHOLD = 120;

    private final Map<UUID, Vector> previousVelocities = new ConcurrentHashMap<>();

    private boolean enabled = true;

    public Vector smooth(Player player, Vector newVelocity, KBProfile profile) {
        if (!enabled) {
            return newVelocity;
        }

        UUID uuid = player.getUniqueId();
        Vector previous = previousVelocities.get(uuid);

        previousVelocities.put(uuid, newVelocity.clone());

        if (previous == null) {
            return newVelocity;
        }

        double threshold = profile.getSmoothingThreshold();

        double deltaX = Math.abs(newVelocity.getX() - previous.getX());
        double deltaY = Math.abs(newVelocity.getY() - previous.getY());
        double deltaZ = Math.abs(newVelocity.getZ() - previous.getZ());

        boolean hasSpike = deltaX > threshold ||
                deltaY > threshold ||
                deltaZ > threshold;

        if (!hasSpike) {
            return newVelocity;
        }

        return lerp(previous, newVelocity, getSmoothFactor(player));
    }

    private double getSmoothFactor(Player player) {
        int ping = getPing(player);
        return ping >= PING_THRESHOLD ? SMOOTH_FACTOR_HIGH_PING : SMOOTH_FACTOR_LOW_PING;
    }

    private int getPing(Player player) {
        try {
            // 1.8.8 NMS reflection
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            return (int) handle.getClass().getField("ping").get(handle);
        } catch (Exception e) {
            return 100;
        }
    }

    private Vector lerp(Vector from, Vector to, double t) {
        return new Vector(
                from.getX() + (to.getX() - from.getX()) * t,
                from.getY() + (to.getY() - from.getY()) * t,
                from.getZ() + (to.getZ() - from.getZ()) * t);
    }

    public void removePlayer(Player player) {
        previousVelocities.remove(player.getUniqueId());
    }

    public void clear() {
        previousVelocities.clear();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
