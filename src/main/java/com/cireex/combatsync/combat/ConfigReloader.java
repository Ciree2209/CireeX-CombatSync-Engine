package com.cireex.combatsync.combat;

import com.cireex.combatsync.CireeXCombatSync;

/** Caches config values and supports hot-reload without restart. */
public class ConfigReloader {

    private final CireeXCombatSync plugin;

    private double maxVertical;
    private double minHorizontal;
    private double maxHorizontal;

    private double rayLength;
    private double rayStep;
    private double angleTolerance;
    private double eyeHeight;
    private double hitDelayTicks;
    private double aabbExpand;

    private boolean blockHitEnabled;
    private double blockHitVerticalReduction;
    private double blockHitHorizontalReduction;

    private boolean sprintResetEnabled;
    private int sprintResetMaxTickWindow;
    private double sprintResetMultiplier;

    private boolean comboLockPreventionEnabled;
    private int comboLockHitThreshold;
    private int comboLockTickWindow;
    private double comboLockHorizontalBoost;
    private double comboLockVerticalBoost;

    private boolean randomizationEnabled;
    private double randomHorizontalMin;
    private double randomHorizontalMax;
    private double randomVerticalMin;
    private double randomVerticalMax;
    private boolean useGaussian;

    private boolean velocityIntegrityEnabled;
    private boolean velocityIntegrityDebug;

    private double waterHorizontalMult;
    private double waterVerticalMult;
    private double lavaHorizontalMult;
    private double lavaVerticalMult;
    private double webHorizontalMult;
    private double webVerticalMult;
    private double jumpingVerticalLeniency;
    private double fallingReachReduction;
    private double slabStairsAngleReduction;

    private boolean debug;

    public ConfigReloader(CireeXCombatSync plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        maxVertical = plugin.getConfig().getDouble("clamps.maxVertical", 0.42);
        minHorizontal = plugin.getConfig().getDouble("clamps.minHorizontal", 0.28);
        maxHorizontal = plugin.getConfig().getDouble("clamps.maxHorizontal", 0.43);

        rayLength = plugin.getConfig().getDouble("hitDetection.rayLength", 3.01);
        rayStep = plugin.getConfig().getDouble("hitDetection.rayStep", 0.1);
        angleTolerance = plugin.getConfig().getDouble("hitDetection.angleTolerance", 2.6);
        eyeHeight = plugin.getConfig().getDouble("hitDetection.eyeHeight", 1.62);
        hitDelayTicks = plugin.getConfig().getDouble("hitDetection.hitDelayTicks", 1.6);
        aabbExpand = plugin.getConfig().getDouble("hitDetection.aabbExpand", 0.08);

        blockHitEnabled = plugin.getConfig().getBoolean("blockHit.enabled", true);
        blockHitVerticalReduction = plugin.getConfig().getDouble("blockHit.verticalReduction", 0.015);
        blockHitHorizontalReduction = plugin.getConfig().getDouble("blockHit.horizontalReduction", 0.01);

        sprintResetEnabled = plugin.getConfig().getBoolean("sprintReset.enabled", true);
        sprintResetMaxTickWindow = plugin.getConfig().getInt("sprintReset.maxTickWindow", 2);
        sprintResetMultiplier = plugin.getConfig().getDouble("sprintReset.multiplier", 1.04);

        comboLockPreventionEnabled = plugin.getConfig().getBoolean("comboLockPrevention.enabled", true);
        comboLockHitThreshold = plugin.getConfig().getInt("comboLockPrevention.hitThreshold", 3);
        comboLockTickWindow = plugin.getConfig().getInt("comboLockPrevention.tickWindow", 10);
        comboLockHorizontalBoost = plugin.getConfig().getDouble("comboLockPrevention.horizontalBoost", 0.03);
        comboLockVerticalBoost = plugin.getConfig().getDouble("comboLockPrevention.verticalBoost", 0.02);

        randomizationEnabled = plugin.getConfig().getBoolean("randomization.enabled", true);
        randomHorizontalMin = plugin.getConfig().getDouble("randomization.horizontalMin", 0.996);
        randomHorizontalMax = plugin.getConfig().getDouble("randomization.horizontalMax", 1.004);
        randomVerticalMin = plugin.getConfig().getDouble("randomization.verticalMin", 0.997);
        randomVerticalMax = plugin.getConfig().getDouble("randomization.verticalMax", 1.003);
        useGaussian = plugin.getConfig().getBoolean("randomization.useGaussian", true);

        velocityIntegrityEnabled = plugin.getConfig().getBoolean("velocityIntegrity.enabled", true);
        velocityIntegrityDebug = plugin.getConfig().getBoolean("velocityIntegrity.debugWarnings", false);

        waterHorizontalMult = plugin.getConfig().getDouble("environment.water.horizontalMultiplier", 0.6);
        waterVerticalMult = plugin.getConfig().getDouble("environment.water.verticalMultiplier", 0.5);
        lavaHorizontalMult = plugin.getConfig().getDouble("environment.lava.horizontalMultiplier", 0.5);
        lavaVerticalMult = plugin.getConfig().getDouble("environment.lava.verticalMultiplier", 0.4);
        webHorizontalMult = plugin.getConfig().getDouble("environment.web.horizontalMultiplier", 0.3);
        webVerticalMult = plugin.getConfig().getDouble("environment.web.verticalMultiplier", 0.25);
        jumpingVerticalLeniency = plugin.getConfig().getDouble("environment.jumping.verticalLeniency", 0.03);
        fallingReachReduction = plugin.getConfig().getDouble("environment.falling.reachReduction", 0.01);
        slabStairsAngleReduction = plugin.getConfig().getDouble("environment.slabStairs.angleToleranceReduction", 0.3);

        debug = plugin.getConfig().getBoolean("debug", false);
    }

    public double getMaxVertical() {
        return maxVertical;
    }

    public double getMinHorizontal() {
        return minHorizontal;
    }

    public double getMaxHorizontal() {
        return maxHorizontal;
    }

    public double getRayLength() {
        return rayLength;
    }

    public double getRayStep() {
        return rayStep;
    }

    public double getAngleTolerance() {
        return angleTolerance;
    }

    public double getEyeHeight() {
        return eyeHeight;
    }

    public double getHitDelayTicks() {
        return hitDelayTicks;
    }

    public double getAabbExpand() {
        return aabbExpand;
    }

    public boolean isBlockHitEnabled() {
        return blockHitEnabled;
    }

    public double getBlockHitVerticalReduction() {
        return blockHitVerticalReduction;
    }

    public double getBlockHitHorizontalReduction() {
        return blockHitHorizontalReduction;
    }

    public boolean isSprintResetEnabled() {
        return sprintResetEnabled;
    }

    public int getSprintResetMaxTickWindow() {
        return sprintResetMaxTickWindow;
    }

    public double getSprintResetMultiplier() {
        return sprintResetMultiplier;
    }

    public boolean isComboLockPreventionEnabled() {
        return comboLockPreventionEnabled;
    }

    public int getComboLockHitThreshold() {
        return comboLockHitThreshold;
    }

    public int getComboLockTickWindow() {
        return comboLockTickWindow;
    }

    public double getComboLockHorizontalBoost() {
        return comboLockHorizontalBoost;
    }

    public double getComboLockVerticalBoost() {
        return comboLockVerticalBoost;
    }

    public boolean isRandomizationEnabled() {
        return randomizationEnabled;
    }

    public double getRandomHorizontalMin() {
        return randomHorizontalMin;
    }

    public double getRandomHorizontalMax() {
        return randomHorizontalMax;
    }

    public double getRandomVerticalMin() {
        return randomVerticalMin;
    }

    public double getRandomVerticalMax() {
        return randomVerticalMax;
    }

    public boolean useGaussian() {
        return useGaussian;
    }

    public boolean isVelocityIntegrityEnabled() {
        return velocityIntegrityEnabled;
    }

    public boolean isVelocityIntegrityDebug() {
        return velocityIntegrityDebug;
    }

    public double getWaterHorizontalMult() {
        return waterHorizontalMult;
    }

    public double getWaterVerticalMult() {
        return waterVerticalMult;
    }

    public double getLavaHorizontalMult() {
        return lavaHorizontalMult;
    }

    public double getLavaVerticalMult() {
        return lavaVerticalMult;
    }

    public double getWebHorizontalMult() {
        return webHorizontalMult;
    }

    public double getWebVerticalMult() {
        return webVerticalMult;
    }

    public double getJumpingVerticalLeniency() {
        return jumpingVerticalLeniency;
    }

    public double getFallingReachReduction() {
        return fallingReachReduction;
    }

    public double getSlabStairsAngleReduction() {
        return slabStairsAngleReduction;
    }

    public boolean isDebug() {
        return debug;
    }
}
