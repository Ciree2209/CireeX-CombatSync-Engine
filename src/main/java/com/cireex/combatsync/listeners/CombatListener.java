package com.cireex.combatsync.listeners;

import com.cireex.combatsync.CireeXCombatSync;
import com.cireex.combatsync.combat.CombatEngine;
import com.cireex.combatsync.combat.KBProfile;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

/** Melee, arrow, and fishing rod hit routing into CombatEngine/VelocityApplier. */
public class CombatListener implements Listener {

    private final CireeXCombatSync plugin;
    private final CombatEngine combatEngine;

    // rod deals less KB than melee — feels right for the hook mechanic
    private static final double ROD_KB_MULTIPLIER = 0.4;
    private static final double ARROW_KB_MULTIPLIER = 0.8;

    public CombatListener(CireeXCombatSync plugin, CombatEngine combatEngine) {
        this.plugin = plugin;
        this.combatEngine = combatEngine;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();

        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();

            if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
                boolean isBlocking = victim.isBlocking();
                String arenaId = plugin.getProfileManager().getPlayerArena(victim);
                combatEngine.processHitWithArena(attacker, victim, isBlocking, arenaId);
            }

        } else if (event.getDamager() instanceof Projectile) {
            Projectile projectile = (Projectile) event.getDamager();

            if (projectile.getShooter() instanceof Player) {
                Player attacker = (Player) projectile.getShooter();
                handleProjectileHit(attacker, victim, projectile);
            }
        }
    }

    private void handleProjectileHit(Player attacker, Player victim, Projectile projectile) {
        KBProfile profile = plugin.getProfileManager().getProfile(victim);
        String arenaId = plugin.getProfileManager().getPlayerArena(victim);

        Vector velocity = plugin.getKnockbackEngine().calculateKnockback(victim, attacker, profile, arenaId);

        // arrows don't get the full melee multiplier
        double multiplier = ARROW_KB_MULTIPLIER;

        velocity.setX(velocity.getX() * multiplier);
        velocity.setZ(velocity.getZ() * multiplier);
        velocity.setY(velocity.getY() * multiplier);

        plugin.getVelocityApplier().applyVelocity(victim, velocity, profile);

        plugin.debug("Projectile KB applied: " + attacker.getName() + " -> " + victim.getName());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_ENTITY) {
            return;
        }

        if (!(event.getCaught() instanceof Player)) {
            return;
        }

        Player attacker = event.getPlayer();
        Player victim = (Player) event.getCaught();

        if (attacker.equals(victim)) {
            return;
        }

        handleRodHit(attacker, victim);
    }

    private void handleRodHit(Player attacker, Player victim) {
        // pulls victim toward attacker
        Vector pullDirection = attacker.getLocation().toVector()
                .subtract(victim.getLocation().toVector());

        if (pullDirection.lengthSquared() < 0.001) {
            return;
        }

        pullDirection.normalize();

        double horizontal = 0.3 * ROD_KB_MULTIPLIER;
        double vertical = 0.2;

        Vector velocity = new Vector();
        velocity.setX(pullDirection.getX() * horizontal);
        velocity.setZ(pullDirection.getZ() * horizontal);
        velocity.setY(vertical);

        KBProfile profile = plugin.getProfileManager().getProfile(victim);
        plugin.getVelocityApplier().applyVelocity(victim, velocity, profile);

        plugin.getComboTracker().recordHitReceived(victim);
        plugin.getComboTracker().recordHitDealt(attacker);

        plugin.debug("Rod KB applied: " + attacker.getName() + " -> " + victim.getName());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getLagCompensationEngine().initializePlayer(event.getPlayer());
        plugin.debug("Player joined: " + event.getPlayer().getName());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        int combo = plugin.getComboTracker().getCombo(player);
        if (combo > 1) {
            plugin.getCombatEngine().recordComboEnd(player, combo);
        }

        plugin.getComboTracker().resetCombo(player);
        plugin.getSprintResetHandler().removePlayer(player);
        plugin.getRandomizationEngine().removePlayer(player);
        plugin.getVelocityApplier().removePlayer(player);
        plugin.getLagCompensationEngine().removePlayer(player);
        plugin.getVelocitySmoother().removePlayer(player);
        plugin.getProfileManager().removePlayerArena(player);

        plugin.debug("Player quit, data cleaned: " + player.getName());
    }
}
