package com.cireex.combatsync.combat;

import com.cireex.combatsync.CireeXCombatSync;
import com.cireex.combatsync.lag.LagCompensationEngine;
import com.cireex.combatsync.lag.PositionSnapshot;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Hybrid AABB + raytrace hit detection with lag compensation and confidence scoring. */
public class HitDetectionEngine {

    @SuppressWarnings("unused") // Reserved for future debug logging
    private final CireeXCombatSync plugin;
    @SuppressWarnings("unused") // Reserved for ground-aware hit detection
    private final GroundStateResolver groundResolver;
    @SuppressWarnings("unused") // Reserved for environment-aware hit detection
    private final EnvironmentResolver environmentResolver;
    private final HitConfidenceScorer confidenceScorer;

    private LagCompensationEngine lagCompensation;

    private static final double MAX_REACH = 3.01;
    private static final double PERFECT_REACH = 2.8; // 100% confidence below this

    // angle tolerance scales with distance to avoid off-angle hits at range (was 38.0)
    private static final double BASE_ANGLE = 14.0;
    private static final double MAX_ANGLE = 34.0;

    private static final double AABB_NEAR_MISS = 0.15; // near-misses within 0.15 blocks get 0.7 confidence

    private final Map<UUID, Double> lastConfidence = new ConcurrentHashMap<>();
    private final Map<UUID, Double> lastReachConf = new ConcurrentHashMap<>();
    private final Map<UUID, Double> lastAngleConf = new ConcurrentHashMap<>();
    private final Map<UUID, Double> lastAABBConf = new ConcurrentHashMap<>();

    public HitDetectionEngine(CireeXCombatSync plugin,
            GroundStateResolver groundResolver,
            EnvironmentResolver environmentResolver) {
        this.plugin = plugin;
        this.groundResolver = groundResolver;
        this.environmentResolver = environmentResolver;
        this.confidenceScorer = new HitConfidenceScorer(plugin);
    }

    public void setLagCompensation(LagCompensationEngine lagCompensation) {
        this.lagCompensation = lagCompensation;
    }

    public HitResult validateHit(Player attacker, Player victim) {
        Location attackerLoc = attacker.getEyeLocation();
        Vector attackerDir = attackerLoc.getDirection();

        PositionSnapshot snapshot = null;
        Location victimLoc = victim.getLocation();

        if (lagCompensation != null) {
            snapshot = lagCompensation.getCompensatedSnapshot(attacker, victim);
            if (snapshot != null) {
                victimLoc = snapshot.getPosition().toLocation(victim.getWorld());
            }
        }

        double distance = attackerLoc.distance(victimLoc.clone().add(0, 0.9, 0));

        // reach confidence (35% weight)
        double reachConfidence;
        if (distance <= PERFECT_REACH) {
            reachConfidence = 1.0;
        } else if (distance > MAX_REACH) {
            return new HitResult(false, 0, "Reach exceeded: " + String.format("%.2f", distance));
        } else {
            reachConfidence = 1.0 - ((distance - PERFECT_REACH) / (MAX_REACH - PERFECT_REACH));
        }
        reachConfidence = clamp(reachConfidence, 0, 1);

        // angle confidence (40% weight) — tolerance widens with distance
        Vector toVictim = victimLoc.clone().add(0, 0.9, 0).toVector()
                .subtract(attackerLoc.toVector()).normalize();
        double angle = Math.toDegrees(Math.acos(
                Math.max(-1, Math.min(1, attackerDir.dot(toVictim)))));

        double distanceFactor = clamp(distance / MAX_REACH, 0, 1);
        double allowedAngle = lerp(BASE_ANGLE, MAX_ANGLE, distanceFactor);

        double angleConfidence = 1.0 - (angle / allowedAngle);
        angleConfidence = clamp(angleConfidence, 0, 1);

        // AABB confidence (25% weight)
        boolean rayHit = checkRaytrace(attackerLoc.toVector(), attackerDir, victimLoc.toVector());
        double distToAABB = getDistanceToAABB(attackerLoc.toVector(), victimLoc.toVector());

        double aabbConfidence;
        if (rayHit) {
            aabbConfidence = 1.0;
        } else if (distToAABB < AABB_NEAR_MISS) {
            aabbConfidence = 0.7;
        } else {
            aabbConfidence = 0.0;
        }

        double confidence = (reachConfidence * 0.35) +
                (angleConfidence * 0.40) +
                (aabbConfidence * 0.25);
        confidence = clamp(confidence, 0, 1);

        UUID attackerUUID = attacker.getUniqueId();
        lastConfidence.put(attackerUUID, confidence);
        lastReachConf.put(attackerUUID, reachConfidence);
        lastAngleConf.put(attackerUUID, angleConfidence);
        lastAABBConf.put(attackerUUID, aabbConfidence);

        // all hits accepted — low confidence just degrades KB amount instead of rejecting
        return new HitResult(true, confidence, "Valid hit (conf: " + String.format("%.2f", confidence) + ")");
    }

    public boolean isValidHit(Player attacker, Player victim) {
        return validateHit(attacker, victim).isValid();
    }

    private double getDistanceToAABB(Vector attackerPos, Vector victimPos) {
        // victim hitbox: 0.6 wide, 1.8 tall
        double minX = victimPos.getX() - 0.3;
        double maxX = victimPos.getX() + 0.3;
        double minY = victimPos.getY();
        double maxY = victimPos.getY() + 1.8;
        double minZ = victimPos.getZ() - 0.3;
        double maxZ = victimPos.getZ() + 0.3;

        // Closest point on AABB
        double closestX = Math.max(minX, Math.min(maxX, attackerPos.getX()));
        double closestY = Math.max(minY, Math.min(maxY, attackerPos.getY()));
        double closestZ = Math.max(minZ, Math.min(maxZ, attackerPos.getZ()));

        return Math.sqrt(
                Math.pow(attackerPos.getX() - closestX, 2) +
                        Math.pow(attackerPos.getY() - closestY, 2) +
                        Math.pow(attackerPos.getZ() - closestZ, 2));
    }

    private boolean checkRaytrace(Vector origin, Vector direction, Vector targetPos) {
        double[] bb = new double[] {
                targetPos.getX() - 0.3, targetPos.getY(), targetPos.getZ() - 0.3,
                targetPos.getX() + 0.3, targetPos.getY() + 1.8, targetPos.getZ() + 0.3
        };

        double tMin = 0.0;
        double tMax = MAX_REACH;

        for (int i = 0; i < 3; i++) {
            double originVal = i == 0 ? origin.getX() : (i == 1 ? origin.getY() : origin.getZ());
            double dirVal = i == 0 ? direction.getX() : (i == 1 ? direction.getY() : direction.getZ());
            double minVal = bb[i];
            double maxVal = bb[i + 3];

            if (Math.abs(dirVal) < 1e-6) {
                if (originVal < minVal || originVal > maxVal)
                    return false;
            } else {
                double t1 = (minVal - originVal) / dirVal;
                double t2 = (maxVal - originVal) / dirVal;
                if (t1 > t2) {
                    double temp = t1;
                    t1 = t2;
                    t2 = temp;
                }
                tMin = Math.max(tMin, t1);
                tMax = Math.min(tMax, t2);
                if (tMin > tMax)
                    return false;
            }
        }

        return true;
    }

    private double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public double getLastConfidence(Player attacker) {
        return lastConfidence.getOrDefault(attacker.getUniqueId(), 1.0);
    }

    public double getLastReachConfidence(Player attacker) {
        return lastReachConf.getOrDefault(attacker.getUniqueId(), 1.0);
    }

    public double getLastAngleConfidence(Player attacker) {
        return lastAngleConf.getOrDefault(attacker.getUniqueId(), 1.0);
    }

    public double getLastAABBConfidence(Player attacker) {
        return lastAABBConf.getOrDefault(attacker.getUniqueId(), 1.0);
    }

    public HitConfidenceScorer getConfidenceScorer() {
        return confidenceScorer;
    }

    public static class HitResult {
        private final boolean valid;
        private final double confidence;
        private final String reason;

        public HitResult(boolean valid, double confidence, String reason) {
            this.valid = valid;
            this.confidence = confidence;
            this.reason = reason;
        }

        public boolean isValid() {
            return valid;
        }

        public double getConfidence() {
            return confidence;
        }

        public String getReason() {
            return reason;
        }
    }
}
