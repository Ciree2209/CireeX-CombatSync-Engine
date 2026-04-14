package com.cireex.combatsync.api.events;

import com.cireex.combatsync.combat.KBProfile;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired when a player's KB profile changes. */
public class ProfileSwitchEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final KBProfile oldProfile;
    private final KBProfile newProfile;

    public ProfileSwitchEvent(Player player, KBProfile oldProfile, KBProfile newProfile) {
        this.player = player;
        this.oldProfile = oldProfile;
        this.newProfile = newProfile;
    }

    public Player getPlayer() { return player; }
    public KBProfile getOldProfile() { return oldProfile; }
    public KBProfile getNewProfile() { return newProfile; }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
