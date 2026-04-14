package com.cireex.combatsync.combat;

/** Immutable knockback profile — all KB parameters for a single config entry. */
public class KBProfile {

    private final String name;
    private final String description;

    private final double horizontal;
    private final double vertical;
    private final double friction;
    private final double airFriction;
    private final double extraVerticalLimit;
    private final double sprintMultiplier;
    private final double bedwarsReduction;
    private final boolean edgeBoost;
    private final double elasticity;
    private final double comboDecayMultiplier;
    private final boolean edgeDampening;
    private final double smoothingThreshold; // spike detection threshold
    private final boolean randomness;
    private final boolean comboProtection;
    private final int hitSelectDelay;
    private final boolean deterministic; // disables RNG for tournament replays
    private final String hash; // SHA-256 of key params for tournament verification

    private KBProfile(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.horizontal = builder.horizontal;
        this.vertical = builder.vertical;
        this.friction = builder.friction;
        this.airFriction = builder.airFriction;
        this.extraVerticalLimit = builder.extraVerticalLimit;
        this.sprintMultiplier = builder.sprintMultiplier;
        this.bedwarsReduction = builder.bedwarsReduction;
        this.edgeBoost = builder.edgeBoost;
        this.elasticity = builder.elasticity;
        this.comboDecayMultiplier = builder.comboDecayMultiplier;
        this.edgeDampening = builder.edgeDampening;
        this.smoothingThreshold = builder.smoothingThreshold;
        this.randomness = builder.randomness;
        this.comboProtection = builder.comboProtection;
        this.hitSelectDelay = builder.hitSelectDelay;
        this.deterministic = builder.deterministic;
        this.hash = computeHash();
    }

    public String getName() {
        return name;
    }

    public String getDescription() { return description; }
    public double getHorizontal() { return horizontal; }
    public double getVertical() { return vertical; }
    public double getFriction() { return friction; }
    public double getAirFriction() { return airFriction; }
    public double getExtraVerticalLimit() { return extraVerticalLimit; }
    public double getSprintMultiplier() { return sprintMultiplier; }
    public double getBedwarsReduction() { return bedwarsReduction; }
    public boolean hasEdgeBoost() { return edgeBoost; }
    public boolean isRandomized() { return randomness; }
    public boolean hasComboProtection() { return comboProtection; }
    public int getHitSelectDelay() { return hitSelectDelay; }
    public double getElasticity() { return elasticity; }
    public double getComboDecayMultiplier() { return comboDecayMultiplier; }
    public boolean hasEdgeDampening() { return edgeDampening; }

    /** inverse of friction — how much velocity is actually retained */
    public double getStopMultiplier() {
        return 1.0 / friction;
    }

    public double getSmoothingThreshold() {
        return smoothingThreshold;
    }

    public boolean isDeterministic() {
        return deterministic;
    }

    public String getHash() {
        return hash;
    }

    /**
     * Compute SHA-256 hash of profile for tournament verification.
     */
    private String computeHash() {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            StringBuilder data = new StringBuilder();
            data.append(name).append(":");
            data.append(horizontal).append(":");
            data.append(vertical).append(":");
            data.append(friction).append(":");
            data.append(airFriction).append(":");
            data.append(elasticity).append(":");
            data.append(comboDecayMultiplier).append(":");
            data.append(randomness).append(":");
            data.append(deterministic);

            byte[] hash = md.digest(data.toString().getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().substring(0, 12); // First 12 chars
        } catch (Exception e) {
            return "ERROR";
        }
    }

    @Override
    public String toString() {
        return "KBProfile{" +
                "name='" + name + '\'' +
                ", h=" + horizontal +
                ", v=" + vertical +
                ", f=" + friction +
                ", e=" + elasticity +
                '}';
    }

    public static class Builder {
        private String name = "unnamed";
        private String description = "";
        private double horizontal = 0.4;
        private double vertical = 0.4;
        private double friction = 2.0;
        private double airFriction = 0.91;
        private double extraVerticalLimit = 0.42;
        private double sprintMultiplier = 1.0;
        private double bedwarsReduction = 1.0;
        private boolean edgeBoost = false;
        private double elasticity = 0.92;
        private double comboDecayMultiplier = 1.0;
        private boolean edgeDampening = false;
        private double smoothingThreshold = 0.15;
        private boolean randomness = false;
        private boolean comboProtection = false;
        private int hitSelectDelay = 0;
        private boolean deterministic = false;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder horizontal(double horizontal) {
            this.horizontal = horizontal;
            return this;
        }

        public Builder vertical(double vertical) {
            this.vertical = vertical;
            return this;
        }

        public Builder friction(double friction) {
            this.friction = friction;
            return this;
        }

        public Builder airFriction(double airFriction) {
            this.airFriction = airFriction;
            return this;
        }

        public Builder extraVerticalLimit(double extraVerticalLimit) {
            this.extraVerticalLimit = extraVerticalLimit;
            return this;
        }

        public Builder sprintMultiplier(double sprintMultiplier) {
            this.sprintMultiplier = sprintMultiplier;
            return this;
        }

        public Builder bedwarsReduction(double bedwarsReduction) {
            this.bedwarsReduction = bedwarsReduction;
            return this;
        }

        public Builder edgeBoost(boolean edgeBoost) {
            this.edgeBoost = edgeBoost;
            return this;
        }

        public Builder randomness(boolean randomness) {
            this.randomness = randomness;
            return this;
        }

        public Builder comboProtection(boolean comboProtection) {
            this.comboProtection = comboProtection;
            return this;
        }

        public Builder hitSelectDelay(int hitSelectDelay) {
            this.hitSelectDelay = hitSelectDelay;
            return this;
        }

        public Builder elasticity(double elasticity) {
            this.elasticity = elasticity;
            return this;
        }

        public Builder comboDecayMultiplier(double comboDecayMultiplier) {
            this.comboDecayMultiplier = comboDecayMultiplier;
            return this;
        }

        public Builder edgeDampening(boolean edgeDampening) {
            this.edgeDampening = edgeDampening;
            return this;
        }

        public Builder smoothingThreshold(double smoothingThreshold) {
            this.smoothingThreshold = smoothingThreshold;
            return this;
        }

        public Builder deterministic(boolean deterministic) {
            this.deterministic = deterministic;
            return this;
        }

        public KBProfile build() {
            return new KBProfile(this);
        }
    }
}
