package com.cireex.combatsync.util;

/** Math utilities for knockback calculations. */
public final class MathUtil {

    private MathUtil() {}

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    public static boolean approxEquals(double a, double b, double epsilon) {
        return Math.abs(a - b) < epsilon;
    }

    public static double square(double x) {
        return x * x;
    }

    public static double horizontalDistance(double x1, double z1, double x2, double z2) {
        double dx = x2 - x1;
        double dz = z2 - z1;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static double normalize(double value, double min, double max) {
        if (max <= min)
            return 0;
        return (value - min) / (max - min);
    }

    public static double map(double value, double fromMin, double fromMax, double toMin, double toMax) {
        double normalized = normalize(value, fromMin, fromMax);
        return lerp(toMin, toMax, normalized);
    }
}
