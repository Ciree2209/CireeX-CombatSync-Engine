package com.cireex.combatsync.combat;

import com.cireex.combatsync.CireeXCombatSync;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Micro-randomization for KB values — prevents robotic patterns and
 * anticheat fingerprinting. Never exceeds ±1%. Gaussian by default,
 * seeded per player UUID so each player gets a different RNG stream.
 */
public class RandomizationEngine {

    private final CireeXCombatSync plugin;

    // seeded per player so hits don't cluster at the same value
    private final ConcurrentHashMap<UUID, Random> playerRandoms = new ConcurrentHashMap<>();

    private double horizontalMin;
    private double horizontalMax;
    private double verticalMin;
    private double verticalMax;
    private boolean useGaussian;
    private boolean enabled;

    public RandomizationEngine(CireeXCombatSync plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        ConfigReloader config = plugin.getConfigReloader();
        this.enabled = config.isRandomizationEnabled();
        this.horizontalMin = config.getRandomHorizontalMin();
        this.horizontalMax = config.getRandomHorizontalMax();
        this.verticalMin = config.getRandomVerticalMin();
        this.verticalMax = config.getRandomVerticalMax();
        this.useGaussian = config.useGaussian();
    }

    private Random getPlayerRandom(Player player) {
        return playerRandoms.computeIfAbsent(
                player.getUniqueId(),
                uuid -> new Random(uuid.getLeastSignificantBits()));
    }

    public double getHorizontalMultiplier(Player player) {
        if (!enabled)
            return 1.0;
        return random(player, horizontalMin, horizontalMax);
    }

    public double getVerticalMultiplier(Player player) {
        if (!enabled)
            return 1.0;
        return random(player, verticalMin, verticalMax);
    }

    public double random(Player player, double min, double max) {
        if (!enabled)
            return 1.0;

        Random rand = getPlayerRandom(player);
        double value;

        if (useGaussian) {
            // 68% within 1 stddev, centered at midpoint
            double mid = (min + max) / 2.0;
            double range = (max - min) / 2.0;
            double gaussian = rand.nextGaussian() * (range / 3.0); // 3 stddev = full range
            value = mid + gaussian;
            value = Math.max(min, Math.min(max, value));
        } else {
            value = min + rand.nextDouble() * (max - min);
        }

        return value;
    }

    /** Non-player-seeded variant for one-off calculations. */
    public static double random(double min, double max) {
        return min + Math.random() * (max - min);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void removePlayer(Player player) {
        playerRandoms.remove(player.getUniqueId());
    }
}
