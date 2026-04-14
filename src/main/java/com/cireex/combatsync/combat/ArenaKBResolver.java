package com.cireex.combatsync.combat;

import com.cireex.combatsync.CireeXCombatSync;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Per-arena KB micro-tuning. Loaded from arenas.yml.
 *
 * Config format:
 *   arenas:
 *     bw_bridge:
 *       horizontalMultiplier: 0.97
 *       verticalMultiplier: 1.0
 *       edgeLogic: true
 *       voidLogic: true
 *     practice_boxing:
 *       verticalCap: 0.39
 *       comboProtection: false
 */
public class ArenaKBResolver {

    private final CireeXCombatSync plugin;
    private final Map<String, ArenaOverride> arenaOverrides = new HashMap<>();

    public ArenaKBResolver(CireeXCombatSync plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        arenaOverrides.clear();

        File configFile = new File(plugin.getDataFolder(), "arenas.yml");
        if (!configFile.exists()) {
            plugin.saveResource("arenas.yml", false);
        }

        if (!configFile.exists()) {
            createDefaultArenasConfig();
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        ConfigurationSection arenasSection = config.getConfigurationSection("arenas");

        if (arenasSection != null) {
            for (String arenaId : arenasSection.getKeys(false)) {
                ConfigurationSection arena = arenasSection.getConfigurationSection(arenaId);
                if (arena != null) {
                    ArenaOverride override = loadArenaOverride(arena);
                    arenaOverrides.put(arenaId.toLowerCase(), override);
                    plugin.debug("Loaded arena override: " + arenaId);
                }
            }
        }

        plugin.log(java.util.logging.Level.INFO, "Loaded " + arenaOverrides.size() + " arena overrides.");
    }

    private void createDefaultArenasConfig() {
        try {
            File configFile = new File(plugin.getDataFolder(), "arenas.yml");
            configFile.createNewFile();
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

            config.set("# Arena-specific KB overrides", null);
            config.set("# These multiply/override the base profile values", null);
            config.createSection("arenas");

            ConfigurationSection arenas = config.getConfigurationSection("arenas");

            ConfigurationSection bridge = arenas.createSection("bw_bridge_example");
            bridge.set("horizontalMultiplier", 0.97);
            bridge.set("verticalMultiplier", 1.0);
            bridge.set("edgeLogic", true);
            bridge.set("voidLogic", true);
            bridge.set("enabled", false);

            ConfigurationSection sumo = arenas.createSection("sumo_platform_example");
            sumo.set("horizontalMultiplier", 1.0);
            sumo.set("verticalMultiplier", 1.0);
            sumo.set("edgeLogic", false);
            sumo.set("voidLogic", false);
            sumo.set("comboProtection", false);
            sumo.set("enabled", false);

            config.save(configFile);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create default arenas.yml: " + e.getMessage());
        }
    }

    private ArenaOverride loadArenaOverride(ConfigurationSection section) {
        return new ArenaOverride(
                section.getBoolean("enabled", true),
                section.getDouble("horizontalMultiplier", 1.0),
                section.getDouble("verticalMultiplier", 1.0),
                section.getDouble("frictionMultiplier", 1.0),
                section.getDouble("horizontalCap", -1),
                section.getDouble("verticalCap", -1),
                section.getBoolean("edgeLogic", true),
                section.getBoolean("voidLogic", true),
                section.getBoolean("comboProtection", true),
                section.getBoolean("randomization", true),
                section.getString("forceProfile", null));
    }

    public ArenaOverride getOverride(String arenaId) {
        if (arenaId == null)
            return null;
        return arenaOverrides.get(arenaId.toLowerCase());
    }

    public boolean hasOverride(String arenaId) {
        if (arenaId == null)
            return false;
        ArenaOverride override = arenaOverrides.get(arenaId.toLowerCase());
        return override != null && override.isEnabled();
    }

    public Set<String> getArenaIds() {
        return arenaOverrides.keySet();
    }

    public void setArenaProfile(String arenaId, String profileName) {
        arenaId = arenaId.toLowerCase();

        ArenaOverride existing = arenaOverrides.get(arenaId);
        if (existing != null) {
            arenaOverrides.put(arenaId, new ArenaOverride(
                    true,
                    existing.getHorizontalMultiplier(),
                    existing.getVerticalMultiplier(),
                    existing.getFrictionMultiplier(),
                    existing.getHorizontalCap(),
                    existing.getVerticalCap(),
                    existing.hasEdgeLogic(),
                    existing.hasVoidLogic(),
                    existing.hasComboProtection(),
                    existing.hasRandomization(),
                    profileName));
        } else {
            arenaOverrides.put(arenaId, new ArenaOverride(
                    true, 1.0, 1.0, 1.0, -1, -1,
                    true, true, true, true, profileName));
        }

        plugin.debug("Arena '" + arenaId + "' set to profile: " + profileName);
    }

    public void resetArena(String arenaId) {
        arenaId = arenaId.toLowerCase();
        arenaOverrides.remove(arenaId);
        plugin.debug("Arena '" + arenaId + "' reset to default.");
    }

    public void createArena(String arenaId) {
        arenaId = arenaId.toLowerCase();
        if (!arenaOverrides.containsKey(arenaId)) {
            arenaOverrides.put(arenaId, new ArenaOverride(
                    true, 1.0, 1.0, 1.0, -1, -1,
                    true, true, true, true, null));
        }
    }

    public static class ArenaOverride {
        private final boolean enabled;
        private final double horizontalMultiplier;
        private final double verticalMultiplier;
        private final double frictionMultiplier;
        private final double horizontalCap;
        private final double verticalCap;
        private final boolean edgeLogic;
        private final boolean voidLogic;
        private final boolean comboProtection;
        private final boolean randomization;
        private final String forceProfile;

        public ArenaOverride(boolean enabled, double horizontalMultiplier, double verticalMultiplier,
                double frictionMultiplier, double horizontalCap, double verticalCap,
                boolean edgeLogic, boolean voidLogic, boolean comboProtection,
                boolean randomization, String forceProfile) {
            this.enabled = enabled;
            this.horizontalMultiplier = horizontalMultiplier;
            this.verticalMultiplier = verticalMultiplier;
            this.frictionMultiplier = frictionMultiplier;
            this.horizontalCap = horizontalCap;
            this.verticalCap = verticalCap;
            this.edgeLogic = edgeLogic;
            this.voidLogic = voidLogic;
            this.comboProtection = comboProtection;
            this.randomization = randomization;
            this.forceProfile = forceProfile;
        }

        public boolean isEnabled() { return enabled; }
        public double getHorizontalMultiplier() { return horizontalMultiplier; }
        public double getVerticalMultiplier() { return verticalMultiplier; }
        public double getFrictionMultiplier() { return frictionMultiplier; }
        public double getHorizontalCap() { return horizontalCap; }
        public double getVerticalCap() { return verticalCap; }
        public boolean hasEdgeLogic() { return edgeLogic; }
        public boolean hasVoidLogic() { return voidLogic; }
        public boolean hasComboProtection() { return comboProtection; }
        public boolean hasRandomization() { return randomization; }
        public String getForceProfile() { return forceProfile; }
        public boolean hasHorizontalCap() { return horizontalCap > 0; }
        public boolean hasVerticalCap() { return verticalCap > 0; }
    }
}
