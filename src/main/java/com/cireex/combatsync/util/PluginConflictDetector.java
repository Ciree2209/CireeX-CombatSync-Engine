package com.cireex.combatsync.util;

import com.cireex.combatsync.CireeXCombatSync;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Detects other velocity-modifying plugins at startup and warns admins.
 * Doesn't block functionality — just gives visibility.
 */
public class PluginConflictDetector {

    private final CireeXCombatSync plugin;

    private static final String[] KNOWN_CONFLICTS = {
            "KnockbackMaster",
            "KnockbackPlus",
            "BetterKnockback",
            "CombatPlus",
            "KBEditor",
            "KnockbackEdit",
            "VelocityEdit",
            "KBSync",
            "PracticeKB",
            "CustomKB",
            "PvPKnockback"
    };

    private static final String[] ANTICHEAT_PLUGINS = {
            "Vulcan",
            "Grim",
            "Spartan",
            "Verus",
            "Matrix",
            "AAC",
            "NoCheatPlus",
            "AntiAura"
    };

    private final List<String> detectedConflicts = new ArrayList<>();
    private final List<String> detectedAnticheats = new ArrayList<>();

    public PluginConflictDetector(CireeXCombatSync plugin) {
        this.plugin = plugin;
    }

    public void detect() {
        detectedConflicts.clear();
        detectedAnticheats.clear();

        for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
            String name = p.getName();

            for (String conflict : KNOWN_CONFLICTS) {
                if (name.equalsIgnoreCase(conflict) ||
                        name.toLowerCase().contains(conflict.toLowerCase())) {
                    detectedConflicts.add(name);
                    break;
                }
            }

            for (String ac : ANTICHEAT_PLUGINS) {
                if (name.equalsIgnoreCase(ac)) {
                    detectedAnticheats.add(name);
                    break;
                }
            }
        }

        if (!detectedConflicts.isEmpty()) {
            plugin.log(Level.WARNING, "╔══════════════════════════════════════════╗");
            plugin.log(Level.WARNING, "║ ⚠ POTENTIAL PLUGIN CONFLICTS DETECTED ⚠ ║");
            plugin.log(Level.WARNING, "╠══════════════════════════════════════════╣");
            for (String conflict : detectedConflicts) {
                plugin.log(Level.WARNING, "║ - " + conflict);
            }
            plugin.log(Level.WARNING, "╠══════════════════════════════════════════╣");
            plugin.log(Level.WARNING, "║ These plugins may interfere with KB.     ║");
            plugin.log(Level.WARNING, "║ Consider disabling or configuring them.  ║");
            plugin.log(Level.WARNING, "╚══════════════════════════════════════════╝");
        }

        if (!detectedAnticheats.isEmpty()) {
            plugin.log(Level.INFO, "Detected anticheats: " + String.join(", ", detectedAnticheats));
            plugin.log(Level.INFO, "CireeXCombatSync is designed to be compatible with all major anticheats.");
        }
    }

    public List<String> getDetectedConflicts() {
        return new ArrayList<>(detectedConflicts);
    }

    public List<String> getDetectedAnticheats() {
        return new ArrayList<>(detectedAnticheats);
    }

    public boolean hasConflicts() {
        return !detectedConflicts.isEmpty();
    }
}
