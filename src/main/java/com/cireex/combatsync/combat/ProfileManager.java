package com.cireex.combatsync.combat;

import com.cireex.combatsync.CireeXCombatSync;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Loads KB profiles from YAML and tracks per-player/arena/world assignments. */
public class ProfileManager {

    private final CireeXCombatSync plugin;

    private final Map<String, KBProfile> profiles = new HashMap<>();
    private final Map<UUID, String> playerProfiles = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerArenas = new ConcurrentHashMap<>();
    private final Map<String, String> arenaProfiles = new HashMap<>();
    private final Map<String, String> queueProfiles = new HashMap<>();
    private final Map<String, String> worldProfiles = new HashMap<>();
    private String defaultProfileName;
    private KBProfile defaultProfile;

    public ProfileManager(CireeXCombatSync plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        profiles.clear();
        arenaProfiles.clear();
        queueProfiles.clear();
        worldProfiles.clear();

        File profileFile = new File(plugin.getDataFolder(), "profiles.yml");
        if (!profileFile.exists()) {
            plugin.saveResource("profiles.yml", false);
        }

        FileConfiguration profileConfig = YamlConfiguration.loadConfiguration(profileFile);
        defaultProfileName = plugin.getConfig().getString("defaultProfile", "vanilla");

        ConfigurationSection profilesSection = profileConfig.getConfigurationSection("profiles");
        if (profilesSection != null) {
            for (String profileName : profilesSection.getKeys(false)) {
                ConfigurationSection section = profilesSection.getConfigurationSection(profileName);
                if (section != null) {
                    KBProfile profile = loadProfile(profileName, section);
                    profiles.put(profileName, profile);
                    plugin.debug("Loaded profile: " + profile);
                }
            }
        }

        ConfigurationSection arenasSection = profileConfig.getConfigurationSection("arenas");
        if (arenasSection != null) {
            for (String arenaId : arenasSection.getKeys(false)) {
                String profileName = arenasSection.getString(arenaId);
                if (profileName != null && profiles.containsKey(profileName)) {
                    arenaProfiles.put(arenaId, profileName);
                }
            }
        }

        ConfigurationSection queuesSection = profileConfig.getConfigurationSection("queues");
        if (queuesSection != null) {
            for (String queueId : queuesSection.getKeys(false)) {
                String profileName = queuesSection.getString(queueId);
                if (profileName != null && profiles.containsKey(profileName)) {
                    queueProfiles.put(queueId, profileName);
                }
            }
        }

        ConfigurationSection worldsSection = profileConfig.getConfigurationSection("worlds");
        if (worldsSection != null) {
            for (String worldName : worldsSection.getKeys(false)) {
                String profileName = worldsSection.getString(worldName);
                if (profileName != null && profiles.containsKey(profileName)) {
                    worldProfiles.put(worldName, profileName);
                }
            }
        }

        defaultProfile = profiles.get(defaultProfileName);
        if (defaultProfile == null) {
            // configured default not found — fall back to hardcoded vanilla
            defaultProfile = createVanillaProfile();
            profiles.put("vanilla", defaultProfile);
        }
    }

    private KBProfile loadProfile(String name, ConfigurationSection section) {
        return new KBProfile.Builder()
                .name(name)
                .description(section.getString("description", ""))
                .horizontal(section.getDouble("horizontal", 0.4))
                .vertical(section.getDouble("vertical", 0.4))
                .friction(section.getDouble("friction", 2.0))
                .airFriction(section.getDouble("airFriction", 0.91))
                .extraVerticalLimit(section.getDouble("extraVerticalLimit", 0.42))
                .sprintMultiplier(section.getDouble("sprintMultiplier", 1.0))
                .bedwarsReduction(section.getDouble("bedwarsReduction", 1.0))
                .edgeBoost(section.getBoolean("edgeBoost", false))
                .elasticity(section.getDouble("elasticity", 0.92))
                .comboDecayMultiplier(section.getDouble("comboDecayMultiplier", 1.0))
                .edgeDampening(section.getBoolean("edgeDampening", false))
                .smoothingThreshold(section.getDouble("smoothingThreshold", 0.15))
                .randomness(section.getBoolean("randomness", false))
                .comboProtection(section.getBoolean("comboProtection", false))
                .deterministic(section.getBoolean("deterministic", false))
                .hitSelectDelay(section.getInt("hitSelectDelay", 0))
                .build();
    }

    private KBProfile createVanillaProfile() {
        return new KBProfile.Builder()
                .name("vanilla")
                .description("Default vanilla Minecraft knockback")
                .horizontal(0.4)
                .vertical(0.4)
                .friction(2.0)
                .airFriction(0.91)
                .randomness(false)
                .comboProtection(false)
                .build();
    }

    public KBProfile getProfile(String name) {
        return profiles.getOrDefault(name, defaultProfile);
    }

    public KBProfile getProfile(Player player) {
        String profileName = playerProfiles.get(player.getUniqueId());
        if (profileName != null) {
            return profiles.getOrDefault(profileName, defaultProfile);
        }
        return defaultProfile;
    }

    public KBProfile getArenaProfile(String arenaId) {
        String profileName = arenaProfiles.get(arenaId);
        if (profileName != null) {
            return profiles.getOrDefault(profileName, defaultProfile);
        }
        return defaultProfile;
    }

    public KBProfile getQueueProfile(String queueId) {
        String profileName = queueProfiles.get(queueId);
        if (profileName != null) {
            return profiles.getOrDefault(profileName, defaultProfile);
        }
        return defaultProfile;
    }

    public KBProfile getWorldProfile(String worldName) {
        String profileName = worldProfiles.get(worldName);
        if (profileName != null) {
            return profiles.getOrDefault(profileName, defaultProfile);
        }
        return null;
    }

    public KBProfile getDefaultProfile() {
        return defaultProfile;
    }

    public void setPlayerProfile(Player player, String profileName) {
        if (profiles.containsKey(profileName)) {
            playerProfiles.put(player.getUniqueId(), profileName);
        }
    }

    public void clearPlayerProfile(Player player) {
        playerProfiles.remove(player.getUniqueId());
    }

    public void setArenaProfile(String arenaId, String profileName) {
        if (profiles.containsKey(profileName)) {
            arenaProfiles.put(arenaId, profileName);
        }
    }

    public void setQueueProfile(String queueId, String profileName) {
        if (profiles.containsKey(profileName)) {
            queueProfiles.put(queueId, profileName);
        }
    }

    public java.util.Set<String> getProfileNames() {
        return profiles.keySet();
    }

    public int getProfileCount() {
        return profiles.size();
    }

    public boolean hasProfile(String name) {
        return profiles.containsKey(name);
    }

    public String getPlayerProfileName(Player player) {
        return playerProfiles.getOrDefault(player.getUniqueId(), defaultProfileName);
    }

    public void setPlayerArena(Player player, String arenaId) {
        if (arenaId != null) {
            playerArenas.put(player.getUniqueId(), arenaId);
        } else {
            playerArenas.remove(player.getUniqueId());
        }
    }

    public String getPlayerArena(Player player) {
        return playerArenas.get(player.getUniqueId());
    }

    public void removePlayerArena(Player player) {
        playerArenas.remove(player.getUniqueId());
    }

    public boolean isPlayerInArena(Player player) {
        return playerArenas.containsKey(player.getUniqueId());
    }

}
