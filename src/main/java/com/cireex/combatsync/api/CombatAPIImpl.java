package com.cireex.combatsync.api;

import com.cireex.combatsync.CireeXCombatSync;
import com.cireex.combatsync.api.events.ProfileSwitchEvent;
import com.cireex.combatsync.combat.ComboTracker;
import com.cireex.combatsync.combat.KBProfile;
import com.cireex.combatsync.combat.KnockbackEngine;
import com.cireex.combatsync.combat.ProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Set;

/** CombatAPI implementation. */
public class CombatAPIImpl implements CombatAPI {

    private final CireeXCombatSync plugin;
    private final ProfileManager profileManager;
    private final KnockbackEngine knockbackEngine;
    private final ComboTracker comboTracker;

    public CombatAPIImpl(CireeXCombatSync plugin,
            ProfileManager profileManager,
            KnockbackEngine knockbackEngine,
            ComboTracker comboTracker) {
        this.plugin = plugin;
        this.profileManager = profileManager;
        this.knockbackEngine = knockbackEngine;
        this.comboTracker = comboTracker;
    }

    @Override
    public KBProfile getProfile(Player player) {
        return profileManager.getProfile(player);
    }

    @Override
    public boolean setProfile(Player player, String profileName) {
        if (!profileManager.hasProfile(profileName)) {
            return false;
        }

        KBProfile oldProfile = profileManager.getProfile(player);
        profileManager.setPlayerProfile(player, profileName);
        KBProfile newProfile = profileManager.getProfile(player);

        Bukkit.getPluginManager().callEvent(new ProfileSwitchEvent(player, oldProfile, newProfile));

        return true;
    }

    @Override
    public boolean applyKnockback(Player victim, Player attacker) {
        KBProfile profile = profileManager.getProfile(victim);
        Vector velocity = knockbackEngine.calculateKnockback(victim, attacker);
        return plugin.getVelocityApplier().applyVelocity(victim, velocity, profile);
    }

    @Override
    public boolean applyKnockback(Player victim, Player attacker, String profileName) {
        KBProfile profile = profileManager.getProfile(profileName);
        if (profile == null) {
            profile = profileManager.getDefaultProfile();
        }

        Vector velocity = knockbackEngine.calculateKnockback(victim, attacker, profile, null);
        return plugin.getVelocityApplier().applyVelocity(victim, velocity, profile);
    }

    @Override
    public int getCombo(Player player) {
        return comboTracker.getCombo(player);
    }

    @Override
    public boolean isInCombat(Player player) {
        return plugin.getCombatEngine().isInCombat(player);
    }

    @Override
    public void resetCombatState(Player player) {
        comboTracker.resetCombo(player);
    }

    @Override
    public void setArenaProfile(String arenaId, String profileName) {
        profileManager.setArenaProfile(arenaId, profileName);
    }

    @Override
    public void setQueueProfile(String queueId, String profileName) {
        profileManager.setQueueProfile(queueId, profileName);
    }

    @Override
    public void reloadConfig() {
        plugin.reload();
    }

    @Override
    public Set<String> getProfileNames() {
        return profileManager.getProfileNames();
    }

    @Override
    public boolean hasProfile(String profileName) {
        return profileManager.hasProfile(profileName);
    }

    @Override
    public void setPlayerArena(Player player, String arenaId) {
        profileManager.setPlayerArena(player, arenaId);
    }

    @Override
    public String getPlayerArena(Player player) {
        return profileManager.getPlayerArena(player);
    }

    @Override
    public void removePlayerArena(Player player) {
        profileManager.removePlayerArena(player);
    }
}
