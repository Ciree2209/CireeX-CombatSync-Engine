package com.cireex.combatsync.commands;

import com.cireex.combatsync.CireeXCombatSync;
import com.cireex.combatsync.combat.ArenaKBResolver;
import com.cireex.combatsync.combat.KBProfile;
import com.cireex.combatsync.telemetry.CombatTelemetry;
import com.cireex.combatsync.tournament.TournamentMode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * KnockbackCommand - Main command for knockback configuration.
 * 
 * Commands:
 * - /kb reload - Hot reload configuration
 * - /kb profile <player> <profile> - Set player profile
 * - /kb info [player] - Show current configuration/profile
 * - /kb test [player] - Debug knockback values
 * - /kb list - List all profiles
 * - /kb stats - Show combat telemetry
 * - /kb tournament start <id> <profile> - Start tournament mode
 * - /kb tournament stop - Stop tournament mode
 * - /kb tournament export - Export tournament data
 */
public class KnockbackCommand implements CommandExecutor, TabCompleter {

    private final CireeXCombatSync plugin;

    private static final String PREFIX = ChatColor.DARK_GRAY + "[" +
            ChatColor.GOLD + "CombatSync" +
            ChatColor.DARK_GRAY + "] ";

    public KnockbackCommand(CireeXCombatSync plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                return handleReload(sender);
            case "setarena":
                return handleSetArena(sender, args);
            case "reset":
                return handleReset(sender, args);
            case "info":
                return handleInfo(sender, args);
            case "test":
                return handleTest(sender, args);
            case "list":
                return handleList(sender);
            case "stats":
                return handleStats(sender);
            case "tournament":
                return handleTournament(sender, args);
            case "arenas":
                return handleArenas(sender);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("combatsync.reload")) {
            sender.sendMessage(PREFIX + ChatColor.RED + "No permission.");
            return true;
        }

        long start = System.currentTimeMillis();
        plugin.reload();
        long time = System.currentTimeMillis() - start;

        sender.sendMessage(PREFIX + ChatColor.GREEN + "Configuration reloaded in " + time + "ms!");
        sender.sendMessage(PREFIX + ChatColor.GRAY + "Profiles: " +
                ChatColor.WHITE + plugin.getProfileManager().getProfileCount() +
                ChatColor.GRAY + " | Arenas: " +
                ChatColor.WHITE + plugin.getArenaKBResolver().getArenaIds().size());
        return true;
    }

    private boolean handleSetArena(CommandSender sender, String[] args) {
        if (!sender.hasPermission("combatsync.admin")) {
            sender.sendMessage(PREFIX + ChatColor.RED + "No permission.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Usage: /kb setarena <arena> <profile>");
            return true;
        }

        String arenaId = args[1].toLowerCase();
        String profileName = args[2].toLowerCase();

        if (!plugin.getProfileManager().hasProfile(profileName)) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Profile not found: " + profileName);
            sender.sendMessage(PREFIX + ChatColor.GRAY + "Available: " +
                    String.join(", ", plugin.getProfileManager().getProfileNames()));
            return true;
        }

        plugin.getArenaKBResolver().setArenaProfile(arenaId, profileName);

        sender.sendMessage(
                PREFIX + ChatColor.GREEN + "Arena '" + arenaId + "' set to profile: " + ChatColor.WHITE + profileName);
        sender.sendMessage(PREFIX + ChatColor.GRAY + "Players in this arena will now use this profile.");
        return true;
    }

    private boolean handleReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("combatsync.admin")) {
            sender.sendMessage(PREFIX + ChatColor.RED + "No permission.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Usage: /kb reset <arena>");
            sender.sendMessage(PREFIX + ChatColor.GRAY + "Resets an arena to use the default vanilla profile.");
            return true;
        }

        String arenaId = args[1].toLowerCase();

        if (!plugin.getArenaKBResolver().hasOverride(arenaId)) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Arena not found: " + arenaId);
            sender.sendMessage(PREFIX + ChatColor.GRAY + "Use /kb arenas to see configured arenas.");
            return true;
        }

        plugin.getArenaKBResolver().resetArena(arenaId);

        sender.sendMessage(PREFIX + ChatColor.GREEN + "Arena '" + arenaId + "' reset to default vanilla KB.");
        sender.sendMessage(PREFIX + ChatColor.GRAY + "All custom overrides removed.");
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("combatsync.admin")) {
            sender.sendMessage(PREFIX + ChatColor.RED + "No permission.");
            return true;
        }

        Player target = null;
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
        } else if (sender instanceof Player) {
            target = (Player) sender;
        }

        TournamentMode tm = plugin.getTournamentMode();

        sender.sendMessage("");
        sender.sendMessage(PREFIX + ChatColor.GOLD + "CireeXCombatSync™ Info");
        sender.sendMessage(ChatColor.GRAY + "├─ " + ChatColor.WHITE + "Version: " +
                plugin.getDescription().getVersion());
        sender.sendMessage(ChatColor.GRAY + "├─ " + ChatColor.WHITE + "Profiles: " +
                plugin.getProfileManager().getProfileCount());
        sender.sendMessage(ChatColor.GRAY + "├─ " + ChatColor.WHITE + "Arena Overrides: " +
                plugin.getArenaKBResolver().getArenaIds().size());
        sender.sendMessage(ChatColor.GRAY + "├─ " + ChatColor.WHITE + "Tournament Mode: " +
                (tm.isActive() ? ChatColor.GREEN + "ACTIVE" : ChatColor.GRAY + "Inactive"));
        sender.sendMessage(ChatColor.GRAY + "└─ " + ChatColor.WHITE + "Debug: " +
                plugin.getConfig().getBoolean("debug"));

        if (target != null) {
            KBProfile profile = plugin.getProfileManager().getProfile(target);
            int ping = plugin.getLagCompensationEngine().getPlayerPing(target);

            sender.sendMessage("");
            sender.sendMessage(PREFIX + ChatColor.GOLD + "Player: " + target.getName());
            sender.sendMessage(ChatColor.GRAY + "├─ " + ChatColor.WHITE + "Profile: " + profile.getName());
            sender.sendMessage(ChatColor.GRAY + "├─ " + ChatColor.WHITE + "H: " + profile.getHorizontal() +
                    " | V: " + profile.getVertical() + " | F: " + profile.getFriction());
            sender.sendMessage(ChatColor.GRAY + "├─ " + ChatColor.WHITE + "Ping: " + ping + "ms");
            sender.sendMessage(ChatColor.GRAY + "└─ " + ChatColor.WHITE + "Combo: " +
                    plugin.getComboTracker().getCombo(target));
        }

        sender.sendMessage("");
        return true;
    }

    private boolean handleTest(CommandSender sender, String[] args) {
        if (!sender.hasPermission("combatsync.debug")) {
            sender.sendMessage(PREFIX + ChatColor.RED + "No permission.");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Must be a player.");
            return true;
        }

        Player attacker = (Player) sender;
        Player victim = attacker;

        if (args.length >= 2) {
            victim = Bukkit.getPlayer(args[1]);
            if (victim == null) {
                sender.sendMessage(PREFIX + ChatColor.RED + "Player not found: " + args[1]);
                return true;
            }
        }

        KBProfile profile = plugin.getProfileManager().getProfile(victim);
        Vector velocity = plugin.getKnockbackEngine().calculateKnockback(victim, attacker, profile);

        sender.sendMessage("");
        sender.sendMessage(PREFIX + ChatColor.GOLD + "KB Test Result");
        sender.sendMessage(ChatColor.GRAY + "├─ " + ChatColor.WHITE + "Profile: " + profile.getName());
        sender.sendMessage(ChatColor.GRAY + "├─ " + ChatColor.WHITE + "Velocity:");
        sender.sendMessage(ChatColor.GRAY + "│  ├─ X: " + ChatColor.AQUA + String.format("%.4f", velocity.getX()));
        sender.sendMessage(ChatColor.GRAY + "│  ├─ Y: " + ChatColor.AQUA + String.format("%.4f", velocity.getY()));
        sender.sendMessage(ChatColor.GRAY + "│  └─ Z: " + ChatColor.AQUA + String.format("%.4f", velocity.getZ()));
        sender.sendMessage(ChatColor.GRAY + "└─ " + ChatColor.GREEN + "Applied!");

        plugin.getVelocityApplier().applyVelocity(victim, velocity, profile);
        return true;
    }

    private boolean handleList(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(PREFIX + ChatColor.GOLD + "Available Profiles:");

        for (String name : plugin.getProfileManager().getProfileNames()) {
            KBProfile profile = plugin.getProfileManager().getProfile(name);
            sender.sendMessage(ChatColor.GRAY + "├─ " + ChatColor.WHITE + name +
                    ChatColor.DARK_GRAY + " (H:" + profile.getHorizontal() +
                    " V:" + profile.getVertical() +
                    " F:" + profile.getFriction() + ")");
        }

        sender.sendMessage("");
        return true;
    }

    private boolean handleStats(CommandSender sender) {
        if (!sender.hasPermission("combatsync.admin")) {
            sender.sendMessage(PREFIX + ChatColor.RED + "No permission.");
            return true;
        }

        CombatTelemetry telemetry = plugin.getCombatTelemetry();

        sender.sendMessage("");
        sender.sendMessage(PREFIX + ChatColor.GOLD + "Combat Telemetry");
        sender.sendMessage(ChatColor.GRAY + "├─ " + ChatColor.WHITE + "Total Hits: " +
                telemetry.getTotalHitsRecorded() +
                ChatColor.DARK_GRAY + " (Sampled: " + telemetry.getTotalHitsSampled() + ")");
        sender.sendMessage(ChatColor.GRAY + "├─ " + ChatColor.WHITE + "Avg Horizontal KB: " +
                String.format("%.4f", telemetry.getAverageHorizontalKB()));
        sender.sendMessage(ChatColor.GRAY + "├─ " + ChatColor.WHITE + "Avg Vertical KB: " +
                String.format("%.4f", telemetry.getAverageVerticalKB()));
        sender.sendMessage(ChatColor.GRAY + "├─ " + ChatColor.WHITE + "Avg Combo Length: " +
                String.format("%.1f", telemetry.getAverageComboLength()));
        sender.sendMessage(ChatColor.GRAY + "├─ " + ChatColor.WHITE + "Avg Hit Confidence: " +
                String.format("%.1f%%", telemetry.getAverageHitConfidence() * 100));
        sender.sendMessage(ChatColor.GRAY + "├─ " + ChatColor.WHITE + "Edge Deaths: " +
                String.format("%.1f%%", telemetry.getEdgeDeathPercentage()));

        sender.sendMessage(ChatColor.GRAY + "├─ " + ChatColor.WHITE + "Profile Usage:");
        Map<String, Double> usage = telemetry.getProfileUsagePercentages();
        for (Map.Entry<String, Double> entry : usage.entrySet()) {
            sender.sendMessage(ChatColor.GRAY + "│  ├─ " + entry.getKey() + ": " +
                    String.format("%.1f%%", entry.getValue()));
        }

        sender.sendMessage(ChatColor.GRAY + "└─ " + ChatColor.WHITE + "Ping Bucket Success:");
        Map<String, Double> pingStats = telemetry.getPingBucketSuccessRates();
        for (Map.Entry<String, Double> entry : pingStats.entrySet()) {
            sender.sendMessage(ChatColor.GRAY + "   ├─ " + entry.getKey() + "ms: " +
                    String.format("%.1f%%", entry.getValue()));
        }

        sender.sendMessage("");
        return true;
    }

    private boolean handleTournament(CommandSender sender, String[] args) {
        if (!sender.hasPermission("combatsync.admin")) {
            sender.sendMessage(PREFIX + ChatColor.RED + "No permission.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Usage: /kb tournament <start|stop|export>");
            return true;
        }

        TournamentMode tm = plugin.getTournamentMode();
        String action = args[1].toLowerCase();

        switch (action) {
            case "start":
                if (args.length < 4) {
                    sender.sendMessage(PREFIX + ChatColor.RED + "Usage: /kb tournament start <id> <profile>");
                    return true;
                }
                if (tm.activate(args[2], args[3])) {
                    sender.sendMessage(PREFIX + ChatColor.GREEN + "Tournament mode " +
                            ChatColor.BOLD + "ACTIVATED");
                    sender.sendMessage(PREFIX + ChatColor.GRAY + "ID: " + args[2] +
                            " | Profile: " + args[3]);
                    sender.sendMessage(PREFIX + ChatColor.YELLOW + "⚠ Randomization DISABLED");
                    sender.sendMessage(PREFIX + ChatColor.YELLOW + "⚠ All KB calcs are being logged");
                } else {
                    sender.sendMessage(PREFIX + ChatColor.RED + "Failed to start tournament mode.");
                }
                break;

            case "stop":
                if (!tm.isActive()) {
                    sender.sendMessage(PREFIX + ChatColor.RED + "Tournament mode is not active.");
                    return true;
                }
                tm.deactivate();
                sender.sendMessage(PREFIX + ChatColor.GREEN + "Tournament mode " +
                        ChatColor.BOLD + "DEACTIVATED");
                sender.sendMessage(PREFIX + ChatColor.GRAY + "Summary exported to /tournaments folder.");
                break;

            case "export":
                if (!tm.isActive()) {
                    sender.sendMessage(PREFIX + ChatColor.RED + "Tournament mode is not active.");
                    return true;
                }
                File export = tm.exportSummary();
                if (export != null) {
                    sender.sendMessage(PREFIX + ChatColor.GREEN + "Exported: " + export.getName());
                } else {
                    sender.sendMessage(PREFIX + ChatColor.RED + "Export failed.");
                }
                break;

            default:
                sender.sendMessage(PREFIX + ChatColor.RED + "Unknown action: " + action);
        }

        return true;
    }

    private boolean handleArenas(CommandSender sender) {
        if (!sender.hasPermission("combatsync.admin")) {
            sender.sendMessage(PREFIX + ChatColor.RED + "No permission.");
            return true;
        }

        Set<String> arenas = plugin.getArenaKBResolver().getArenaIds();

        sender.sendMessage("");
        sender.sendMessage(PREFIX + ChatColor.GOLD + "Arena Overrides (" + arenas.size() + ")");

        if (arenas.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No arena overrides configured.");
        } else {
            for (String arena : arenas) {
                ArenaKBResolver.ArenaOverride override = plugin.getArenaKBResolver().getOverride(arena);
                String status = override.isEnabled() ? ChatColor.GREEN + "✓" : ChatColor.RED + "✗";
                sender.sendMessage(ChatColor.GRAY + "├─ " + status + " " + ChatColor.WHITE + arena);
            }
        }

        sender.sendMessage("");
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(PREFIX + ChatColor.GOLD + "CireeXCombatSync™ Commands");
        sender.sendMessage(ChatColor.GRAY + "├─ " + ChatColor.WHITE + "/kb reload " +
                ChatColor.DARK_GRAY + "- Reload configuration");
        sender.sendMessage(ChatColor.GRAY + "├─ " + ChatColor.WHITE + "/kb setarena <arena> <profile> " +
                ChatColor.DARK_GRAY + "- Set arena profile");
        sender.sendMessage(ChatColor.GRAY + "├─ " + ChatColor.WHITE + "/kb reset <arena> " +
                ChatColor.DARK_GRAY + "- Reset arena to default");
        sender.sendMessage(ChatColor.GRAY + "├─ " + ChatColor.WHITE + "/kb arenas " +
                ChatColor.DARK_GRAY + "- List arena overrides");
        sender.sendMessage(ChatColor.GRAY + "├─ " + ChatColor.WHITE + "/kb list " +
                ChatColor.DARK_GRAY + "- List profiles");
        sender.sendMessage(ChatColor.GRAY + "├─ " + ChatColor.WHITE + "/kb info [player] " +
                ChatColor.DARK_GRAY + "- Show info");
        sender.sendMessage(ChatColor.GRAY + "├─ " + ChatColor.WHITE + "/kb test [player] " +
                ChatColor.DARK_GRAY + "- Test knockback");
        sender.sendMessage(ChatColor.GRAY + "├─ " + ChatColor.WHITE + "/kb stats " +
                ChatColor.DARK_GRAY + "- Combat telemetry");
        sender.sendMessage(ChatColor.GRAY + "└─ " + ChatColor.WHITE + "/kb tournament <start|stop|export>");
        sender.sendMessage("");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("reload", "setarena", "reset", "info", "test", "list",
                    "stats", "tournament", "arenas"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "setarena":
                case "reset":
                    completions.addAll(plugin.getArenaKBResolver().getArenaIds());
                    break;
                case "info":
                case "test":
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        completions.add(player.getName());
                    }
                    break;
                case "tournament":
                    completions.addAll(Arrays.asList("start", "stop", "export"));
                    break;
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("setarena")) {
                completions.addAll(plugin.getProfileManager().getProfileNames());
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("tournament") && args[1].equalsIgnoreCase("start")) {
                completions.addAll(plugin.getProfileManager().getProfileNames());
            }
        }

        String input = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(input))
                .collect(Collectors.toList());
    }
}
