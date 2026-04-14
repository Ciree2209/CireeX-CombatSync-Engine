package com.cireex.combatsync.combat;

import com.cireex.combatsync.CireeXCombatSync;
import com.cireex.combatsync.lag.PositionSnapshot;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Computes a 0.0–1.0 confidence score for each hit.
 * Used to degrade KB on borderline hits rather than reject them outright,
 * and to feed telemetry/anticheat correlation.
 *
 * Factors: reach distance (30%), angle margin (30%), snapshot freshness (25%),
 * target movement delta (15%).
 */
public class HitConfidenceScorer {

    private static final double DISTANCE_WEIGHT = 0.30;
    private static final double ANGLE_WEIGHT = 0.30;
    private static final double FRESHNESS_WEIGHT = 0.25;
    private static final double MOVEMENT_WEIGHT = 0.15;

    private static final double MAX_DISTANCE = 3.01;
    private static final double MAX_ANGLE = 2.6;
    private static final long MAX_SNAPSHOT_AGE_MS = 150;
    private static final double MAX_MOVEMENT_DELTA = 2.0;

    public HitConfidenceScorer(CireeXCombatSync plugin) {
        // plugin available for future use
    }

    public double calculateConfidence(Player attacker, Player victim,
            double distance, double angle,
            PositionSnapshot snapshot) {
        double distanceScore = clamp(1.0 - (distance / MAX_DISTANCE), 0, 1);
        double angleScore = clamp(1.0 - (angle / MAX_ANGLE), 0, 1);

        double freshnessScore = 1.0;
        if (snapshot != null) {
            long age = System.currentTimeMillis() - snapshot.getTime();
            freshnessScore = clamp(1.0 - ((double) age / MAX_SNAPSHOT_AGE_MS), 0, 1);
        }

        Vector victimVelocity = victim.getVelocity();
        double movementMagnitude = Math.sqrt(
                victimVelocity.getX() * victimVelocity.getX() +
                        victimVelocity.getZ() * victimVelocity.getZ());
        double movementScore = clamp(1.0 - (movementMagnitude / MAX_MOVEMENT_DELTA), 0, 1);

        double confidence = (distanceScore * DISTANCE_WEIGHT) +
                (angleScore * ANGLE_WEIGHT) +
                (freshnessScore * FRESHNESS_WEIGHT) +
                (movementScore * MOVEMENT_WEIGHT);

        return clamp(confidence, 0, 1);
    }

    public String getConfidenceLabel(double confidence) {
        if (confidence >= 0.9)
            return "Excellent";
        if (confidence >= 0.8)
            return "Very Clean";
        if (confidence >= 0.7)
            return "Clean";
        if (confidence >= 0.6)
            return "Good";
        if (confidence >= 0.5)
            return "Acceptable";
        if (confidence >= 0.4)
            return "Marginal";
        return "Borderline";
    }

    /** All hits ≥0.3 are accepted — low confidence degrades KB, not the hit itself. */
    public boolean shouldAcceptHit(double confidence) {
        return confidence >= 0.3;
    }

    public boolean isHighQualityHit(double confidence) {
        return confidence >= 0.7;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
