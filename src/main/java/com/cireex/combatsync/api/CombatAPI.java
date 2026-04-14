package com.cireex.combatsync.api;

import com.cireex.combatsync.combat.KBProfile;
import org.bukkit.entity.Player;

/**
 * CombatAPI - Public API for external plugin integration.
 * 
 * Register via Bukkit Services:
 * Bukkit.getServicesManager().getRegistration(CombatAPI.class).getProvider()
 */
public interface CombatAPI {

    /**
     * Get the current KB profile for a player.
     */
    KBProfile getProfile(Player player);

    /**
     * Set a player's KB profile.
     * 
     * @param player      The player
     * @param profileName Name of the profile to assign
     * @return true if profile was found and set
     */
    boolean setProfile(Player player, String profileName);

    /**
     * Apply knockback to a victim from an attacker.
     * Uses the victim's current profile.
     * 
     * @return true if knockback was applied
     */
    boolean applyKnockback(Player victim, Player attacker);

    /**
     * Apply knockback with a specific profile.
     * 
     * @return true if knockback was applied
     */
    boolean applyKnockback(Player victim, Player attacker, String profileName);

    /**
     * Get the current combo count for a player.
     */
    int getCombo(Player player);

    /**
     * Check if a player is currently in combat.
     */
    boolean isInCombat(Player player);

    /**
     * Reset a player's combat state (combo, hits received, etc).
     */
    void resetCombatState(Player player);

    /**
     * Set the KB profile for an arena.
     */
    void setArenaProfile(String arenaId, String profileName);

    /**
     * Set the KB profile for a queue type.
     */
    void setQueueProfile(String queueId, String profileName);

    /**
     * Reload all configuration and profiles.
     */
    void reloadConfig();

    /**
     * Get all available profile names.
     */
    java.util.Set<String> getProfileNames();

    /**
     * Check if a profile exists.
     */
    boolean hasProfile(String profileName);

    /**
     * Set the arena a player is currently in.
     * Used for arena-specific KB overrides.
     */
    void setPlayerArena(Player player, String arenaId);

    /**
     * Get the arena a player is currently in.
     */
    String getPlayerArena(Player player);

    /**
     * Remove a player's arena context.
     */
    void removePlayerArena(Player player);
}
