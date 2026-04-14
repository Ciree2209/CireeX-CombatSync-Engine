package com.cireex.combatsync.lag;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Immutable snapshot of a player's position at a point in time.
 * Used for lag compensation hit evaluation against historical positions.
 */
@SuppressWarnings("deprecation") // Using 1.8.8 API methods
public final class PositionSnapshot {

    private final long time; // System.currentTimeMillis()
    private final Vector position;
    private final double minX, minY, minZ;
    private final double maxX, maxY, maxZ;
    private final boolean onGround;
    private final boolean sprinting;

    public PositionSnapshot(Player player) {
        this.time = System.currentTimeMillis();
        this.position = player.getLocation().toVector();
        this.onGround = player.isOnGround();
        this.sprinting = player.isSprinting();

        // player hitbox: 0.6 wide, 1.8 tall
        double x = player.getLocation().getX();
        double y = player.getLocation().getY();
        double z = player.getLocation().getZ();

        this.minX = x - 0.3;
        this.maxX = x + 0.3;
        this.minY = y;
        this.maxY = y + 1.8;
        this.minZ = z - 0.3;
        this.maxZ = z + 0.3;
    }

    public long getTime() {
        return time;
    }

    public Vector getPosition() {
        return position.clone();
    }

    public boolean isOnGround() {
        return onGround;
    }

    public boolean isSprinting() {
        return sprinting;
    }

    public double[] getBoundingBox(double expand) {
        return new double[] {
                minX - expand, minY - expand, minZ - expand,
                maxX + expand, maxY + expand, maxZ + expand
        };
    }

    public boolean containsPoint(Vector point, double expand) {
        double[] bb = getBoundingBox(expand);
        return point.getX() >= bb[0] && point.getX() <= bb[3] &&
                point.getY() >= bb[1] && point.getY() <= bb[4] &&
                point.getZ() >= bb[2] && point.getZ() <= bb[5];
    }

    /** Slab method AABB intersection — fast and accurate. */
    public boolean rayIntersects(Vector origin, Vector direction, double range, double expand) {
        double[] bb = getBoundingBox(expand);

        double tMin = 0.0;
        double tMax = range;

        // X axis
        if (Math.abs(direction.getX()) < 1e-6) {
            if (origin.getX() < bb[0] || origin.getX() > bb[3])
                return false;
        } else {
            double t1 = (bb[0] - origin.getX()) / direction.getX();
            double t2 = (bb[3] - origin.getX()) / direction.getX();
            if (t1 > t2) { double temp = t1; t1 = t2; t2 = temp; }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) return false;
        }

        // Y axis
        if (Math.abs(direction.getY()) < 1e-6) {
            if (origin.getY() < bb[1] || origin.getY() > bb[4])
                return false;
        } else {
            double t1 = (bb[1] - origin.getY()) / direction.getY();
            double t2 = (bb[4] - origin.getY()) / direction.getY();
            if (t1 > t2) { double temp = t1; t1 = t2; t2 = temp; }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) return false;
        }

        // Z axis
        if (Math.abs(direction.getZ()) < 1e-6) {
            if (origin.getZ() < bb[2] || origin.getZ() > bb[5])
                return false;
        } else {
            double t1 = (bb[2] - origin.getZ()) / direction.getZ();
            double t2 = (bb[5] - origin.getZ()) / direction.getZ();
            if (t1 > t2) { double temp = t1; t1 = t2; t2 = temp; }
            tMin = Math.max(tMin, t1);
            tMax = Math.min(tMax, t2);
            if (tMin > tMax) return false;
        }

        return true;
    }

    public Vector getCenter() {
        return new Vector(
                (minX + maxX) / 2.0,
                (minY + maxY) / 2.0,
                (minZ + maxZ) / 2.0);
    }
}
