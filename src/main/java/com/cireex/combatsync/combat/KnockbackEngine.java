package com.cireex.combatsync.combat;

import com.cireex.combatsync.CireeXCombatSync;
import com.cireex.combatsync.api.events.KnockbackApplyEvent;
import com.cireex.combatsync.lag.LagCompensationEngine;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/**
 * KnockbackEngine - Core knockback calculation engine.
 * 
 * Velocity Application Flow (MANDATORY ORDER):
 * 1. Resolve grounded state
 * 2. Read vanilla velocity
 * 3. Apply friction
 * 4. Apply sprint reset
 * 5. Apply enchant modifiers
 * 6. Apply profile multipliers
 * 7. Apply arena overrides
 * 8. Apply combo protection
 * 9. Apply edge/void normalization
 * 10. Apply latency modifier
 * 11. Apply micro-random (if not tournament)
 * 12. Clamp & normalize
 */
public class KnockbackEngine {

    private final CireeXCombatSync plugin;
    private final ProfileManager profileManager;
    private final RandomizationEngine randomizationEngine;
    private final GroundStateResolver groundResolver;
    private final EnvironmentResolver environmentResolver;
    private final SprintResetHandler sprintResetHandler;
    private final ComboTracker comboTracker;
    private final EdgeVoidResolver edgeVoidResolver;

    private LagCompensationEngine lagCompensation;

    public KnockbackEngine(CireeXCombatSync plugin,
            ProfileManager profileManager,
            RandomizationEngine randomizationEngine,
            GroundStateResolver groundResolver,
            EnvironmentResolver environmentResolver,
            SprintResetHandler sprintResetHandler,
            ComboTracker comboTracker) {
        this.plugin = plugin;
        this.profileManager = profileManager;
        this.randomizationEngine = randomizationEngine;
        this.groundResolver = groundResolver;
        this.environmentResolver = environmentResolver;
        this.sprintResetHandler = sprintResetHandler;
        this.comboTracker = comboTracker;
        this.edgeVoidResolver = new EdgeVoidResolver(plugin);
    }

    public void setLagCompensation(LagCompensationEngine lagCompensation) {
        this.lagCompensation = lagCompensation;
    }

    public Vector calculateKnockback(Player victim, Player attacker) {
        KBProfile profile = profileManager.getProfile(victim);
        return calculateKnockback(victim, attacker, profile, null);
    }

    public Vector calculateKnockback(Player victim, Player attacker, KBProfile profile) {
        return calculateKnockback(victim, attacker, profile, null);
    }

    public Vector calculateKnockback(Player victim, Player attacker, KBProfile profile, String arenaId) {
        ConfigReloader config = plugin.getConfigReloader();

        boolean grounded = groundResolver.isOnGround(victim);
        boolean sprinting = attacker.isSprinting();

        // direction matters for combo escape boost direction — don't simplify this
        Vector attackerToVictim = victim.getLocation().toVector()
                .subtract(attacker.getLocation().toVector());

        // prevent division by zero when players are standing on top of each other
        if (attackerToVictim.lengthSquared() < 0.001) {
            attackerToVictim = attacker.getLocation().getDirection();
        }
        attackerToVictim.normalize();

        double horizontal = profile.getHorizontal();
        double vertical = profile.getVertical();
        double friction = grounded ? profile.getFriction() : profile.getAirFriction();

        Vector velocity = new Vector();
        velocity.setX(attackerToVictim.getX() * horizontal);
        velocity.setZ(attackerToVictim.getZ() * horizontal);
        velocity.setY(vertical);

        velocity.setX(velocity.getX() / friction);
        velocity.setZ(velocity.getZ() / friction);

        if (sprintResetHandler.isLegitSprintReset(attacker)) {
            double mult = sprintResetHandler.getSprintResetMultiplier();
            velocity.setX(velocity.getX() * mult);
            velocity.setZ(velocity.getZ() * mult);
            plugin.debug("Sprint reset applied: " + mult + "x");
        }

        // enchant multipliers (Knockback I/II)
        int knockbackLevel = getKnockbackEnchantLevel(attacker);
        if (knockbackLevel > 0) {
            double enchantMult = 1.0 + (knockbackLevel * 0.1);
            velocity.setX(velocity.getX() * enchantMult);
            velocity.setZ(velocity.getZ() * enchantMult);
            velocity.setY(velocity.getY() * (1.0 + knockbackLevel * 0.05));
        }

        // mirrors vanilla sprint KB bonus
        if (sprinting) {
            double sprintMult = profile.getSprintMultiplier();
            velocity.setX(velocity.getX() * sprintMult);
            velocity.setZ(velocity.getZ() * sprintMult);
        }

        double envHorizontalMult = environmentResolver.getHorizontalMultiplier(victim);
        double envVerticalMult = environmentResolver.getVerticalMultiplier(victim);
        velocity.setX(velocity.getX() * envHorizontalMult);
        velocity.setZ(velocity.getZ() * envHorizontalMult);
        velocity.setY(velocity.getY() * envVerticalMult);

        // elasticity — reduces vertical KB per consecutive air hit to prevent infinite juggles
        int airCombo = comboTracker.getAirComboCount(victim);
        if (comboTracker.isVictimAirborne(victim) && airCombo >= 2) {
            double elasticity = profile.getElasticity();
            double comboDecay = profile.getComboDecayMultiplier();

            double comboFactor = 1.0 - (airCombo * 0.06 * comboDecay);
            comboFactor = Math.max(0.72, Math.min(1.0, comboFactor));

            velocity.setY(velocity.getY() * comboFactor * elasticity);
            plugin.debug("Elasticity applied: " + elasticity + " (combo: " + airCombo + ")");
        }

        // reduce vertical slightly near void for clutch-friendly feel
        if (profile.hasEdgeDampening() && edgeVoidResolver.isNearVoid(victim)) {
            velocity.setY(velocity.getY() * 0.82);
            plugin.debug("Edge dampening applied");
        }

        velocity = applyArenaOverrides(velocity, arenaId, profile);

        boolean comboProtectionApplied = false;
        ArenaKBResolver.ArenaOverride arenaOverride = getArenaOverride(arenaId);
        boolean arenaAllowsCombo = arenaOverride == null || arenaOverride.hasComboProtection();

        if (profile.hasComboProtection() && arenaAllowsCombo && comboTracker.shouldApplyComboEscape(victim)) {
            double hBoost = comboTracker.getEscapeHorizontalBoost();
            double vBoost = comboTracker.getEscapeVerticalBoost();

            // boost away from attacker (was incorrectly using a fixed direction before)
            velocity.setX(velocity.getX() + attackerToVictim.getX() * hBoost);
            velocity.setZ(velocity.getZ() + attackerToVictim.getZ() * hBoost);
            velocity.setY(velocity.getY() + vBoost);

            comboProtectionApplied = true;
            plugin.debug("Combo escape boost applied (direction-based)");
        }

        boolean arenaAllowsEdge = arenaOverride == null || arenaOverride.hasEdgeLogic();
        if (!comboProtectionApplied && arenaAllowsEdge &&
                !edgeVoidResolver.shouldDisableEdgeWithCombo(comboProtectionApplied)) {
            double edgeMult = edgeVoidResolver.getEdgeHorizontalMultiplier(victim, profile);
            velocity.setX(velocity.getX() * edgeMult);
            velocity.setZ(velocity.getZ() * edgeMult);

            boolean arenaAllowsVoid = arenaOverride == null || arenaOverride.hasVoidLogic();
            if (arenaAllowsVoid) {
                long lastHitTick = System.currentTimeMillis() / 50;
                double voidMult = edgeVoidResolver.getVoidHorizontalMultiplier(victim, profile, lastHitTick);
                velocity.setX(velocity.getX() * voidMult);
                velocity.setZ(velocity.getZ() * voidMult);
            }
        }

        if (lagCompensation != null) {
            double latencyMod = lagCompensation.getLatencyModifier(attacker);
            velocity.setX(velocity.getX() * latencyMod);
            velocity.setZ(velocity.getZ() * latencyMod);
        }

        // randomization is disabled in tournament mode for deterministic replays
        boolean arenaAllowsRandom = arenaOverride == null || arenaOverride.hasRandomization();
        boolean tournamentActive = plugin.getTournamentMode() != null &&
                plugin.getTournamentMode().isRandomnessDisabled();

        if (profile.isRandomized() && randomizationEngine.isEnabled() &&
                arenaAllowsRandom && !tournamentActive) {
            double hRand = randomizationEngine.getHorizontalMultiplier(victim);
            double vRand = randomizationEngine.getVerticalMultiplier(victim);
            velocity.setX(velocity.getX() * hRand);
            velocity.setZ(velocity.getZ() * hRand);
            velocity.setY(velocity.getY() * vRand);
        }

        // anticheat-safe clamp
        velocity = clampVelocity(velocity, config, arenaOverride);

        KnockbackApplyEvent event = new KnockbackApplyEvent(victim, velocity, profile);
        Bukkit.getPluginManager().callEvent(event);
        velocity = event.getVelocity();

        // clamp again — event listeners may have set unsafe values
        velocity = clampVelocity(velocity, config, arenaOverride);

        return velocity;
    }

    private Vector applyArenaOverrides(Vector velocity, String arenaId, KBProfile profile) {
        ArenaKBResolver.ArenaOverride override = getArenaOverride(arenaId);
        if (override == null || !override.isEnabled()) {
            return velocity;
        }

        velocity.setX(velocity.getX() * override.getHorizontalMultiplier());
        velocity.setZ(velocity.getZ() * override.getHorizontalMultiplier());
        velocity.setY(velocity.getY() * override.getVerticalMultiplier());

        if (override.getFrictionMultiplier() != 1.0) {
            velocity.setX(velocity.getX() / override.getFrictionMultiplier());
            velocity.setZ(velocity.getZ() / override.getFrictionMultiplier());
        }

        plugin.debug("Arena overrides applied: " + arenaId);
        return velocity;
    }

    private ArenaKBResolver.ArenaOverride getArenaOverride(String arenaId) {
        if (arenaId == null)
            return null;
        return plugin.getArenaKBResolver().getOverride(arenaId);
    }

    private Vector clampVelocity(Vector velocity, ConfigReloader config,
            ArenaKBResolver.ArenaOverride arenaOverride) {
        double maxVertical = config.getMaxVertical();
        double minHorizontal = config.getMinHorizontal();
        double maxHorizontal = config.getMaxHorizontal();

        // arena caps take priority over global limits
        if (arenaOverride != null && arenaOverride.hasVerticalCap()) {
            maxVertical = Math.min(maxVertical, arenaOverride.getVerticalCap());
        }
        if (arenaOverride != null && arenaOverride.hasHorizontalCap()) {
            maxHorizontal = Math.min(maxHorizontal, arenaOverride.getHorizontalCap());
        }

        // never exceed 0.42 Y — anticheats flag this
        velocity.setY(Math.min(velocity.getY(), maxVertical));

        velocity.setX(clampHorizontal(velocity.getX(), minHorizontal, maxHorizontal));
        velocity.setZ(clampHorizontal(velocity.getZ(), minHorizontal, maxHorizontal));

        return velocity;
    }

    private double clampHorizontal(double value, double min, double max) {
        double sign = value >= 0 ? 1 : -1;
        double abs = Math.abs(value);

        if (abs < min * 0.5) {
            return value; // Allow small values
        }

        abs = Math.min(abs, max);
        return abs * sign;
    }

    private int getKnockbackEnchantLevel(Player attacker) {
        ItemStack item = attacker.getItemInHand();
        if (item == null)
            return 0;
        return item.getEnchantmentLevel(Enchantment.KNOCKBACK);
    }

    public Vector applyBlockHitReduction(Vector velocity) {
        if (!plugin.getConfigReloader().isBlockHitEnabled()) {
            return velocity;
        }

        double hReduction = plugin.getConfigReloader().getBlockHitHorizontalReduction();
        double vReduction = plugin.getConfigReloader().getBlockHitVerticalReduction();

        velocity.setX(velocity.getX() - (velocity.getX() > 0 ? hReduction : -hReduction));
        velocity.setZ(velocity.getZ() - (velocity.getZ() > 0 ? hReduction : -hReduction));
        velocity.setY(velocity.getY() - vReduction);

        return velocity;
    }

    public boolean isBlocking(Player player) {
        return player.isBlocking();
    }

    public EdgeVoidResolver getEdgeVoidResolver() {
        return edgeVoidResolver;
    }
}
