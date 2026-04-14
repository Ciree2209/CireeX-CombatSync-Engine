package com.cireex.combatsync.api.events;

import com.cireex.combatsync.combat.KBProfile;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.util.Vector;

/**
 * Fired before knockback is applied. NOT cancellable — only modification
 * is allowed. Modify values carefully to avoid anticheat flags.
 */
public class KnockbackApplyEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final Player victim;
    private Vector velocity;
    private final KBProfile profile;

    public KnockbackApplyEvent(Player victim, Vector velocity, KBProfile profile) {
        this.victim = victim;
        this.velocity = velocity.clone();
        this.profile = profile;
    }

    public Player getVictim() {
        return victim;
    }

    public Vector getVelocity() {
        return velocity.clone();
    }

    public void setVelocity(Vector velocity) {
        this.velocity = velocity.clone();
    }

    public KBProfile getProfile() {
        return profile;
    }

    public void multiplyHorizontal(double multiplier) {
        velocity.setX(velocity.getX() * multiplier);
        velocity.setZ(velocity.getZ() * multiplier);
    }

    public void multiplyVertical(double multiplier) {
        velocity.setY(velocity.getY() * multiplier);
    }

    public void addVelocity(double x, double y, double z) {
        velocity.add(new Vector(x, y, z));
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
