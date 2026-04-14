package com.cireex.combatsync.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

 //Fired when a valid combat hit is detected.
public class CombatHitEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final Player attacker;
    private final Player victim;
    private final boolean sprintReset;

    public CombatHitEvent(Player attacker, Player victim, boolean sprintReset) {
        this.attacker = attacker;
        this.victim = victim;
        this.sprintReset = sprintReset;
    }

    public Player getAttacker() { return attacker; }
    public Player getVictim() { return victim; }
    public boolean isSprintReset() { return sprintReset; }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
