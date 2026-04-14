package com.cireex.combatsync.tournament;

import com.cireex.combatsync.CireeXCombatSync;
import org.bukkit.entity.Player;

import java.io.*;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Deterministic tournament mode — disables randomness, locks profiles,
 * hashes every KB calc for post-match verification, and exports a full
 * match log. Intended for LAN events, cash tournaments, and ranked ladders.
 */
public class TournamentMode {

    private final CireeXCombatSync plugin;

    private final AtomicBoolean active = new AtomicBoolean(false);
    private String tournamentId;
    private String lockedProfile;
    private long startTime;

    private final Map<UUID, MatchData> activeMatches = new ConcurrentHashMap<>();
    private final List<MatchSummary> completedMatches = Collections.synchronizedList(new ArrayList<>());

    // every KB calc is hashed for replay verification
    private final List<KBCalcRecord> kbLog = Collections.synchronizedList(new ArrayList<>());
    private static final int MAX_KB_LOG_SIZE = 10000;

    public TournamentMode(CireeXCombatSync plugin) {
        this.plugin = plugin;
    }

    public boolean activate(String tournamentId, String lockedProfile) {
        if (active.get()) {
            return false;
        }

        if (!plugin.getProfileManager().hasProfile(lockedProfile)) {
            return false;
        }

        this.tournamentId = tournamentId;
        this.lockedProfile = lockedProfile;
        this.startTime = System.currentTimeMillis();
        this.active.set(true);

        kbLog.clear();
        completedMatches.clear();

        plugin.log(java.util.logging.Level.INFO,
                "Tournament mode ACTIVATED: " + tournamentId + " (Profile: " + lockedProfile + ")");

        return true;
    }

    public void deactivate() {
        if (!active.get())
            return;

        active.set(false);
        plugin.log(java.util.logging.Level.INFO, "Tournament mode DEACTIVATED: " + tournamentId);

        if (!completedMatches.isEmpty() || !kbLog.isEmpty()) {
            exportSummary();
        }
    }

    public boolean isActive() {
        return active.get();
    }

    public String getLockedProfile() {
        return lockedProfile;
    }

    public boolean isRandomnessDisabled() {
        return active.get();
    }

    public void startMatch(Player player1, Player player2, String matchId) {
        MatchData match = new MatchData(matchId, player1.getUniqueId(), player2.getUniqueId());
        activeMatches.put(player1.getUniqueId(), match);
        activeMatches.put(player2.getUniqueId(), match);
    }

    public void endMatch(Player winner, Player loser) {
        MatchData match = activeMatches.remove(winner.getUniqueId());
        activeMatches.remove(loser.getUniqueId());

        if (match != null) {
            match.setWinner(winner.getUniqueId());
            match.setEndTime(System.currentTimeMillis());
            completedMatches.add(match.toSummary(winner.getName(), loser.getName()));
        }
    }

    public void recordMatchHit(Player attacker, Player victim, double damage) {
        MatchData match = activeMatches.get(attacker.getUniqueId());
        if (match != null) {
            match.recordHit(attacker.getUniqueId(), damage);
        }
    }

    public void logKBCalc(Player victim, Player attacker,
            double horizontal, double vertical,
            String profile) {
        if (!active.get())
            return;

        // ring-buffer the log — oldest entry goes first
        if (kbLog.size() >= MAX_KB_LOG_SIZE) {
            kbLog.remove(0);
        }

        String hash = computeKBHash(
                victim.getUniqueId(), attacker.getUniqueId(),
                horizontal, vertical, System.currentTimeMillis());

        kbLog.add(new KBCalcRecord(
                System.currentTimeMillis(),
                victim.getName(), attacker.getName(),
                horizontal, vertical, profile, hash));
    }

    private String computeKBHash(UUID victim, UUID attacker,
            double h, double v, long time) {
        try {
            String data = victim.toString() + attacker.toString() +
                    String.format("%.6f", h) + String.format("%.6f", v) + time;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data.getBytes());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) { // first 8 bytes = 16 hex chars
                sb.append(String.format("%02x", hash[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return "error";
        }
    }

    public File exportSummary() {
        try {
            File exportDir = new File(plugin.getDataFolder(), "tournaments");
            exportDir.mkdirs();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String filename = tournamentId + "_" + sdf.format(new Date()) + ".txt";
            File exportFile = new File(exportDir, filename);

            try (PrintWriter writer = new PrintWriter(new FileWriter(exportFile))) {
                writer.println("=== CIREEXCOMBATSYNC TOURNAMENT REPORT ===");
                writer.println();
                writer.println("Tournament ID: " + tournamentId);
                writer.println("Profile: " + lockedProfile);
                writer.println("Start Time: " + new Date(startTime));
                writer.println("End Time: " + new Date());
                writer.println("Duration: " + ((System.currentTimeMillis() - startTime) / 1000) + " seconds");
                writer.println();

                writer.println("=== MATCH SUMMARIES ===");
                writer.println("Total Matches: " + completedMatches.size());
                writer.println();

                for (MatchSummary match : completedMatches) {
                    writer.println("Match: " + match.matchId);
                    writer.println("  Winner: " + match.winner);
                    writer.println("  Loser: " + match.loser);
                    writer.println("  Duration: " + match.durationSeconds + "s");
                    writer.println("  Total Hits: " + match.totalHits);
                    writer.println();
                }

                writer.println("=== KB CALCULATION LOG (Last " + kbLog.size() + " records) ===");
                writer.println();
                for (KBCalcRecord record : kbLog) {
                    writer.println(String.format("[%s] %s -> %s | H:%.4f V:%.4f | %s | Hash:%s",
                            new SimpleDateFormat("HH:mm:ss.SSS").format(new Date(record.timestamp)),
                            record.attacker, record.victim,
                            record.horizontal, record.vertical,
                            record.profile, record.hash));
                }

                writer.println();
                writer.println("=== END OF REPORT ===");
            }

            plugin.log(java.util.logging.Level.INFO, "Tournament summary exported to: " + exportFile.getName());
            return exportFile;

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to export tournament summary: " + e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unused") // fields used for match tracking and summary generation
    private static class MatchData {
        final String matchId;
        final UUID player1;
        final UUID player2;
        final long startTime;
        final AtomicInteger player1Hits = new AtomicInteger(0);
        final AtomicInteger player2Hits = new AtomicInteger(0);
        UUID winner;
        long endTime;

        MatchData(String matchId, UUID player1, UUID player2) {
            this.matchId = matchId;
            this.player1 = player1;
            this.player2 = player2;
            this.startTime = System.currentTimeMillis();
        }

        void recordHit(UUID attacker, double damage) {
            if (attacker.equals(player1)) {
                player1Hits.incrementAndGet();
            } else {
                player2Hits.incrementAndGet();
            }
        }

        void setWinner(UUID winner) { this.winner = winner; }
        void setEndTime(long endTime) { this.endTime = endTime; }

        MatchSummary toSummary(String winnerName, String loserName) {
            return new MatchSummary(
                    matchId, winnerName, loserName,
                    (endTime - startTime) / 1000,
                    player1Hits.get() + player2Hits.get());
        }
    }

    private static class MatchSummary {
        final String matchId;
        final String winner;
        final String loser;
        final long durationSeconds;
        final int totalHits;

        MatchSummary(String matchId, String winner, String loser,
                long durationSeconds, int totalHits) {
            this.matchId = matchId;
            this.winner = winner;
            this.loser = loser;
            this.durationSeconds = durationSeconds;
            this.totalHits = totalHits;
        }
    }

    private static class KBCalcRecord {
        final long timestamp;
        final String victim;
        final String attacker;
        final double horizontal;
        final double vertical;
        final String profile;
        final String hash;

        KBCalcRecord(long timestamp, String victim, String attacker,
                double horizontal, double vertical, String profile, String hash) {
            this.timestamp = timestamp;
            this.victim = victim;
            this.attacker = attacker;
            this.horizontal = horizontal;
            this.vertical = vertical;
            this.profile = profile;
            this.hash = hash;
        }
    }
}
