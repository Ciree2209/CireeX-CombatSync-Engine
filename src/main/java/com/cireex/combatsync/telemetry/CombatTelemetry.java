package com.cireex.combatsync.telemetry;

import com.cireex.combatsync.CireeXCombatSync;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Lightweight combat analytics — samples 1 in 20 hits for performance.
 * Tracks average KB, combo distribution, edge deaths, and ping/success correlation.
 */
public class CombatTelemetry {

    @SuppressWarnings("unused") // reserved for config-based sampling rate
    private final CireeXCombatSync plugin;

    private static final int SAMPLE_RATE = 20;
    private final AtomicInteger hitCounter = new AtomicInteger(0);

    private final RollingAverage avgHorizontalKB = new RollingAverage(1000);
    private final RollingAverage avgVerticalKB = new RollingAverage(1000);
    private final RollingAverage avgComboLength = new RollingAverage(500);
    private final RollingAverage avgHitConfidence = new RollingAverage(1000);

    private final AtomicLong edgeDeaths = new AtomicLong(0);
    private final AtomicLong normalDeaths = new AtomicLong(0);

    private final Map<String, PingBucketStats> pingBuckets = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> profileUsage = new ConcurrentHashMap<>();
    private final AtomicLong totalProfileHits = new AtomicLong(0);

    private final int[] confidenceDistribution = new int[10]; // 0.0-0.1 per bucket

    public CombatTelemetry(CireeXCombatSync plugin) {
        this.plugin = plugin;
        initPingBuckets();
    }

    private void initPingBuckets() {
        pingBuckets.put("0-30", new PingBucketStats());
        pingBuckets.put("31-60", new PingBucketStats());
        pingBuckets.put("61-100", new PingBucketStats());
        pingBuckets.put("101-150", new PingBucketStats());
        pingBuckets.put("151-200", new PingBucketStats());
        pingBuckets.put("200+", new PingBucketStats());
    }

    /** Samples 1 in SAMPLE_RATE hits — cheap enough to leave on in prod. */
    public void recordHit(double horizontalKB, double verticalKB,
            double hitConfidence, int ping, String profileName) {
        int count = hitCounter.incrementAndGet();

        if (count % SAMPLE_RATE != 0) {
            return;
        }

        avgHorizontalKB.add(horizontalKB);
        avgVerticalKB.add(verticalKB);
        avgHitConfidence.add(hitConfidence);

        String bucket = getPingBucket(ping);
        PingBucketStats stats = pingBuckets.get(bucket);
        if (stats != null) {
            stats.recordHit(hitConfidence >= 0.7);
        }

        profileUsage.computeIfAbsent(profileName, k -> new AtomicLong(0)).incrementAndGet();
        totalProfileHits.incrementAndGet();

        int bucket_idx = Math.min(9, (int) (hitConfidence * 10));
        synchronized (confidenceDistribution) {
            confidenceDistribution[bucket_idx]++;
        }
    }

    public void recordCombo(int comboLength) {
        if (comboLength > 1) {
            avgComboLength.add(comboLength);
        }
    }

    public void recordDeath(boolean wasEdgeDeath) {
        if (wasEdgeDeath) {
            edgeDeaths.incrementAndGet();
        } else {
            normalDeaths.incrementAndGet();
        }
    }

    public double getAverageHorizontalKB() { return avgHorizontalKB.getAverage(); }
    public double getAverageVerticalKB() { return avgVerticalKB.getAverage(); }
    public double getAverageComboLength() { return avgComboLength.getAverage(); }
    public double getAverageHitConfidence() { return avgHitConfidence.getAverage(); }
    public long getEdgeDeaths() { return edgeDeaths.get(); }
    public long getNormalDeaths() { return normalDeaths.get(); }

    public double getEdgeDeathPercentage() {
        long total = edgeDeaths.get() + normalDeaths.get();
        if (total == 0)
            return 0;
        return (edgeDeaths.get() * 100.0) / total;
    }

    public Map<String, Double> getProfileUsagePercentages() {
        Map<String, Double> result = new HashMap<>();
        long total = totalProfileHits.get();
        if (total == 0)
            return result;

        for (Map.Entry<String, AtomicLong> entry : profileUsage.entrySet()) {
            result.put(entry.getKey(), (entry.getValue().get() * 100.0) / total);
        }
        return result;
    }

    public Map<String, Double> getPingBucketSuccessRates() {
        Map<String, Double> result = new HashMap<>();
        for (Map.Entry<String, PingBucketStats> entry : pingBuckets.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getSuccessRate());
        }
        return result;
    }

    public int[] getConfidenceDistribution() {
        synchronized (confidenceDistribution) {
            return Arrays.copyOf(confidenceDistribution, confidenceDistribution.length);
        }
    }

    public long getTotalHitsRecorded() { return hitCounter.get(); }
    public long getTotalHitsSampled() { return hitCounter.get() / SAMPLE_RATE; }

    private String getPingBucket(int ping) {
        if (ping <= 30) return "0-30";
        if (ping <= 60) return "31-60";
        if (ping <= 100) return "61-100";
        if (ping <= 150) return "101-150";
        if (ping <= 200) return "151-200";
        return "200+";
    }

    public void reset() {
        hitCounter.set(0);
        avgHorizontalKB.reset();
        avgVerticalKB.reset();
        avgComboLength.reset();
        avgHitConfidence.reset();
        edgeDeaths.set(0);
        normalDeaths.set(0);
        for (PingBucketStats stats : pingBuckets.values()) {
            stats.reset();
        }
        profileUsage.clear();
        totalProfileHits.set(0);
        synchronized (confidenceDistribution) {
            Arrays.fill(confidenceDistribution, 0);
        }
    }

    private static class RollingAverage {
        private final double[] values;
        private int index = 0;
        private int count = 0;

        RollingAverage(int size) {
            this.values = new double[size];
        }

        synchronized void add(double value) {
            values[index] = value;
            index = (index + 1) % values.length;
            if (count < values.length)
                count++;
        }

        synchronized double getAverage() {
            if (count == 0)
                return 0;
            double sum = 0;
            for (int i = 0; i < count; i++) {
                sum += values[i];
            }
            return sum / count;
        }

        synchronized void reset() {
            Arrays.fill(values, 0);
            index = 0;
            count = 0;
        }
    }

    private static class PingBucketStats {
        private final AtomicLong hits = new AtomicLong(0);
        private final AtomicLong successfulHits = new AtomicLong(0);

        void recordHit(boolean successful) {
            hits.incrementAndGet();
            if (successful) {
                successfulHits.incrementAndGet();
            }
        }

        double getSuccessRate() {
            long total = hits.get();
            if (total == 0)
                return 0;
            return (successfulHits.get() * 100.0) / total;
        }

        void reset() {
            hits.set(0);
            successfulHits.set(0);
        }
    }
}
