package com.cireex.combatsync.combat;

import com.cireex.combatsync.CireeXCombatSync;
import com.cireex.combatsync.api.events.CombatHitEvent;
import com.cireex.combatsync.telemetry.CombatTelemetry;
import com.cireex.combatsync.tournament.TournamentMode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * CombatEngine - Main combat orchestrator.
 */
public class CombatEngine {

    private final CireeXCombatSync plugin;
    private final HitDetectionEngine hitDetectionEngine;
    private final KnockbackEngine knockbackEngine;
    private final VelocityApplier velocityApplier;
    private final ComboTracker comboTracker;
    private final SprintResetHandler sprintResetHandler;

    public CombatEngine(CireeXCombatSync plugin,
            HitDetectionEngine hitDetectionEngine,
            KnockbackEngine knockbackEngine,
            VelocityApplier velocityApplier,
            ComboTracker comboTracker,
            SprintResetHandler sprintResetHandler) {
        this.plugin = plugin;
        this.hitDetectionEngine = hitDetectionEngine;
        this.knockbackEngine = knockbackEngine;
        this.velocityApplier = velocityApplier;
        this.comboTracker = comboTracker;
        this.sprintResetHandler = sprintResetHandler;
    }

    public boolean processHit(Player attacker, Player victim, boolean isBlocking) {
        return processHitWithArena(attacker, victim, isBlocking, null);
    }

    public boolean processHitWithArena(Player attacker, Player victim, boolean isBlocking, String arenaId) {
        HitDetectionEngine.HitResult hitResult = hitDetectionEngine.validateHit(attacker, victim);

        if (!hitResult.isValid()) {
            plugin.debug("Hit rejected by detection engine: " + hitResult.getReason());
            return false;
        }

        boolean sprintReset = sprintResetHandler.isLegitSprintReset(attacker);
        CombatHitEvent hitEvent = new CombatHitEvent(attacker, victim, sprintReset);
        Bukkit.getPluginManager().callEvent(hitEvent);

        KBProfile profile = resolveProfile(victim, arenaId);
        Vector velocity = knockbackEngine.calculateKnockback(victim, attacker, profile, arenaId);

        // low-confidence hits still register, but KB is scaled down; floor at 0.55 so
        // every hit feels like a hit and doesn't produce zero-momentum taps
        double hitConfidence = hitResult.getConfidence();
        if (hitConfidence < 0.90) {
            double kbMultiplier = Math.max(0.55, hitConfidence);
            velocity.multiply(kbMultiplier);
            plugin.debug("KB degraded by confidence: " + String.format("%.2f", hitConfidence) +
                    " (mult: " + String.format("%.2f", kbMultiplier) + ")");
        }

        if (isBlocking) {
            velocity = knockbackEngine.applyBlockHitReduction(velocity);
            plugin.debug("Block-hit reduction applied");
        }

        // only delay the first hit of a combo — allows "catching" (hit-select feel)
        int delayTicks = 0;
        int currentCombo = comboTracker.getCombo(attacker);
        if (currentCombo == 0 && profile.getHitSelectDelay() > 0) {
            delayTicks = profile.getHitSelectDelay();
            plugin.debug("Hit-select delay applied: " + delayTicks + " ticks");
        }

        boolean applied = velocityApplier.applyVelocity(victim, velocity, profile, delayTicks);

        if (applied) {
            comboTracker.recordHitReceived(victim);
            comboTracker.recordHitDealt(attacker);
            recordTelemetry(attacker, victim, velocity, hitResult.getConfidence(), profile);
            logTournamentHit(victim, attacker, velocity, profile);
            plugin.debug("Combat processed: " + attacker.getName() + " -> " + victim.getName() +
                    " (Confidence: " + String.format("%.2f", hitResult.getConfidence()) + ")");
        }

        return applied;
    }

    private KBProfile resolveProfile(Player victim, String arenaId) {
        ArenaKBResolver arenaResolver = plugin.getArenaKBResolver();

        if (arenaId != null && arenaResolver.hasOverride(arenaId)) {
            ArenaKBResolver.ArenaOverride override = arenaResolver.getOverride(arenaId);
            if (override.getForceProfile() != null) {
                KBProfile forced = plugin.getProfileManager().getProfile(override.getForceProfile());
                if (forced != null) {
                    return forced;
                }
            }
        }

        // persistence was removed, so this is "vanilla" unless set this session
        KBProfile assigned = plugin.getProfileManager().getProfile(victim);
        if (!assigned.getName().equals("vanilla")) {
            return assigned;
        }

        // world-based auto-detection fallback
        KBProfile worldProfile = plugin.getProfileManager().getWorldProfile(victim.getWorld().getName());
        if (worldProfile != null) {
            return worldProfile;
        }

        return plugin.getProfileManager().getDefaultProfile();
    }

    private void recordTelemetry(Player attacker, Player victim, Vector velocity,
            double confidence, KBProfile profile) {
        CombatTelemetry telemetry = plugin.getCombatTelemetry();
        if (telemetry == null)
            return;

        double horizontal = Math.sqrt(velocity.getX() * velocity.getX() + velocity.getZ() * velocity.getZ());
        int ping = plugin.getLagCompensationEngine().getPlayerPing(attacker);

        telemetry.recordHit(horizontal, velocity.getY(), confidence, ping, profile.getName());
    }

    private void logTournamentHit(Player victim, Player attacker, Vector velocity, KBProfile profile) {
        TournamentMode tm = plugin.getTournamentMode();
        if (tm == null || !tm.isActive())
            return;

        double horizontal = Math.sqrt(velocity.getX() * velocity.getX() + velocity.getZ() * velocity.getZ());
        tm.logKBCalc(victim, attacker, horizontal, velocity.getY(), profile.getName());
    }

    /** Legacy overload — takes an explicit profile instead of resolving one. */
    public boolean processHit(Player attacker, Player victim, boolean isBlocking, KBProfile profile) {
        HitDetectionEngine.HitResult hitResult = hitDetectionEngine.validateHit(attacker, victim);
        if (!hitResult.isValid()) {
            return false;
        }

        boolean sprintReset = sprintResetHandler.isLegitSprintReset(attacker);
        CombatHitEvent hitEvent = new CombatHitEvent(attacker, victim, sprintReset);
        Bukkit.getPluginManager().callEvent(hitEvent);

        Vector velocity = knockbackEngine.calculateKnockback(victim, attacker, profile, null);

        double hitConfidence = hitResult.getConfidence();
        if (hitConfidence < 0.90) {
            double kbMultiplier = Math.max(0.55, hitConfidence);
            velocity.multiply(kbMultiplier);
        }

        if (isBlocking) {
            velocity = knockbackEngine.applyBlockHitReduction(velocity);
        }

        boolean applied = velocityApplier.applyVelocity(victim, velocity, profile);

        if (applied) {
            comboTracker.recordHitReceived(victim);
            comboTracker.recordHitDealt(attacker);
            recordTelemetry(attacker, victim, velocity, hitResult.getConfidence(), profile);
            logTournamentHit(victim, attacker, velocity, profile);
        }

        return applied;
    }

    public boolean applyKnockback(Player victim, Player attacker) {
        KBProfile profile = plugin.getProfileManager().getProfile(victim);
        Vector velocity = knockbackEngine.calculateKnockback(victim, attacker);
        return velocityApplier.applyVelocity(victim, velocity, profile);
    }

    public boolean applyKnockback(Player victim, Player attacker, KBProfile profile) {
        Vector velocity = knockbackEngine.calculateKnockback(victim, attacker, profile, null);
        return velocityApplier.applyVelocity(victim, velocity, profile);
    }

    public boolean applyRawVelocity(Player player, Vector velocity) {
        KBProfile profile = plugin.getProfileManager().getProfile(player);
        return velocityApplier.applyVelocity(player, velocity, profile);
    }

    public boolean isInCombat(Player player) {
        return comboTracker.getCombo(player) > 0 ||
                comboTracker.getRecentHitsReceived(player) > 0;
    }

    public void recordComboEnd(Player player, int comboLength) {
        CombatTelemetry telemetry = plugin.getCombatTelemetry();
        if (telemetry != null) {
            telemetry.recordCombo(comboLength);
        }
    }

    public void recordDeath(Player player, boolean wasEdgeDeath) {
        CombatTelemetry telemetry = plugin.getCombatTelemetry();
        if (telemetry != null) {
            telemetry.recordDeath(wasEdgeDeath);
        }
    }

    public HitDetectionEngine getHitDetectionEngine() {
        return hitDetectionEngine;
    }

    public KnockbackEngine getKnockbackEngine() {
        return knockbackEngine;
    }

    public VelocityApplier getVelocityApplier() {
        return velocityApplier;
    }

    public ComboTracker getComboTracker() {
        return comboTracker;
    }
}
