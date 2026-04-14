package com.cireex.combatsync.util;

import org.bukkit.Location;
import org.bukkit.util.Vector;

/** Vector utilities for combat calculations. */
public final class VectorUtil {

    private VectorUtil() {}

    public static Vector getDirection(Location from, Location to) {
        return to.toVector().subtract(from.toVector()).normalize();
    }

    public static Vector getHorizontalDirection(Location from, Location to) {
        Vector dir = getDirection(from, to);
        dir.setY(0);
        return dir.length() > 0 ? dir.normalize() : dir;
    }

    public static double angleBetween(Vector a, Vector b) {
        double dot = a.dot(b);
        double lenA = a.length();
        double lenB = b.length();

        if (lenA == 0 || lenB == 0)
            return 0;

        double cos = dot / (lenA * lenB);
        cos = MathUtil.clamp(cos, -1.0, 1.0);
        return Math.toDegrees(Math.acos(cos));
    }

    public static boolean isZero(Vector v, double epsilon) {
        return Math.abs(v.getX()) < epsilon &&
                Math.abs(v.getY()) < epsilon &&
                Math.abs(v.getZ()) < epsilon;
    }

    public static double horizontalLength(Vector v) {
        return Math.sqrt(v.getX() * v.getX() + v.getZ() * v.getZ());
    }

    public static Vector scaleHorizontal(Vector v, double scale) {
        return new Vector(v.getX() * scale, v.getY(), v.getZ() * scale);
    }

    public static Vector limit(Vector v, double max) {
        double length = v.length();
        if (length > max && length > 0) {
            return v.clone().multiply(max / length);
        }
        return v.clone();
    }

    public static Vector limitHorizontal(Vector v, double max) {
        double hLength = horizontalLength(v);
        if (hLength > max && hLength > 0) {
            double scale = max / hLength;
            return new Vector(v.getX() * scale, v.getY(), v.getZ() * scale);
        }
        return v.clone();
    }

    public static Vector rotateAroundY(Vector v, double degrees) {
        double rad = Math.toRadians(degrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double x = v.getX() * cos - v.getZ() * sin;
        double z = v.getX() * sin + v.getZ() * cos;
        return new Vector(x, v.getY(), z);
    }

    public static Vector createKnockback(Vector direction, double horizontal, double vertical) {
        Vector kb = direction.clone().normalize();
        kb.multiply(horizontal);
        kb.setY(vertical);
        return kb;
    }
}
