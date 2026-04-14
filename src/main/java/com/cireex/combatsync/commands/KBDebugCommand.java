package com.cireex.combatsync.commands;

import com.cireex.combatsync.CireeXCombatSync;
import com.cireex.combatsync.combat.KBProfile;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /kbdebug — admin overlay showing live KB state for your own player.
 * Permission: combatsync.admin
 */
public class KBDebugCommand implements CommandExecutor {

    private final CireeXCombatSync plugin;

    public KBDebugCommand(CireeXCombatSync plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("combatsync.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        showDebugOverlay(player, player);
        return true;
    }

    private void showDebugOverlay(Player viewer, Player target) {
        KBProfile profile = plugin.getProfileManager().getProfile(target);

        viewer.sendMessage("");
        viewer.sendMessage(ChatColor.GOLD + "━━━━━ " + ChatColor.YELLOW + "KB Debug: " +
                target.getName() + ChatColor.GOLD + " ━━━━━");

        viewer.sendMessage(ChatColor.GRAY + "Profile: " + ChatColor.WHITE + profile.getName());
        viewer.sendMessage(ChatColor.GRAY + "Description: " + ChatColor.WHITE + profile.getDescription());
        viewer.sendMessage(ChatColor.GRAY + "Profile Lock: " + ChatColor.AQUA + profile.getHash() +
                ChatColor.DARK_GRAY + " (integrity verification)");

        viewer.sendMessage("");
        viewer.sendMessage(ChatColor.YELLOW + "▸ Core Values:");
        viewer.sendMessage(ChatColor.GRAY + "  Horizontal: " + ChatColor.WHITE +
                String.format("%.3f", profile.getHorizontal()));
        viewer.sendMessage(ChatColor.GRAY + "  Vertical: " + ChatColor.WHITE +
                String.format("%.3f", profile.getVertical()));
        viewer.sendMessage(ChatColor.GRAY + "  Friction: " + ChatColor.WHITE +
                String.format("%.2f", profile.getFriction()) +
                ChatColor.DARK_GRAY + " (stop: " + String.format("%.3f", profile.getStopMultiplier()) + ")");

        viewer.sendMessage("");
        viewer.sendMessage(ChatColor.YELLOW + "▸ Elasticity Physics:");
        viewer.sendMessage(ChatColor.GRAY + "  Elasticity: " + ChatColor.WHITE +
                String.format("%.2f", profile.getElasticity()));
        viewer.sendMessage(ChatColor.GRAY + "  Combo Decay: " + ChatColor.WHITE +
                String.format("%.2f", profile.getComboDecayMultiplier()));
        viewer.sendMessage(ChatColor.GRAY + "  Edge Dampening: " +
                (profile.hasEdgeDampening() ? ChatColor.GREEN + "✓ Enabled" : ChatColor.RED + "✗ Disabled"));

        int airCombo = plugin.getComboTracker().getAirComboCount(target);
        boolean airborne = plugin.getComboTracker().isVictimAirborne(target);
        int ping = getPing(target);

        viewer.sendMessage("");
        viewer.sendMessage(ChatColor.YELLOW + "▸ Current State:");
        viewer.sendMessage(ChatColor.GRAY + "  Airborne: " +
                (airborne ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"));
        viewer.sendMessage(ChatColor.GRAY + "  Air Combo: " + ChatColor.WHITE + airCombo + " hits");
        viewer.sendMessage(ChatColor.GRAY + "  Ping: " + ChatColor.WHITE + ping + "ms " +
                ChatColor.DARK_GRAY + "(smooth: " + getSmoothFactorLabel(ping) + ")");

        if (plugin.getHitDetectionEngine() != null) {
            double lastConf = plugin.getHitDetectionEngine().getLastConfidence(target);
            viewer.sendMessage(ChatColor.GRAY + "  Last Hit Confidence: " +
                    getConfidenceColor(lastConf) + String.format("%.2f%%", lastConf * 100));

            double reachConf = plugin.getHitDetectionEngine().getLastReachConfidence(target);
            double angleConf = plugin.getHitDetectionEngine().getLastAngleConfidence(target);
            double aabbConf = plugin.getHitDetectionEngine().getLastAABBConfidence(target);

            viewer.sendMessage(ChatColor.GRAY + "    Reach: " + ChatColor.WHITE + String.format("%.2f", reachConf) +
                    ChatColor.GRAY + " | Angle: " + ChatColor.WHITE + String.format("%.2f", angleConf) +
                    ChatColor.GRAY + " | AABB: " + ChatColor.WHITE + String.format("%.2f", aabbConf));
        }

        viewer.sendMessage("");
        viewer.sendMessage(ChatColor.YELLOW + "▸ Features:");
        viewer.sendMessage(ChatColor.GRAY + "  Combo Protection: " +
                (profile.hasComboProtection() ? ChatColor.GREEN + "✓" : ChatColor.RED + "✗"));
        viewer.sendMessage(ChatColor.GRAY + "  Randomization: " +
                (profile.isRandomized() ? ChatColor.GREEN + "✓" : ChatColor.RED + "✗"));
        viewer.sendMessage(ChatColor.GRAY + "  Edge Boost: " +
                (profile.hasEdgeBoost() ? ChatColor.GREEN + "✓" : ChatColor.RED + "✗"));
        viewer.sendMessage(ChatColor.GRAY + "  Deterministic: " +
                (profile.isDeterministic() ? ChatColor.GREEN + "✓ (Tournament)" : ChatColor.GRAY + "✗"));

        viewer.sendMessage(ChatColor.GOLD + "━━━━━━━━━━━━━━━━━━━━");
        viewer.sendMessage("");
    }

    private int getPing(Player player) {
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            return (int) handle.getClass().getField("ping").get(handle);
        } catch (Exception e) {
            return -1;
        }
    }

    private String getSmoothFactorLabel(int ping) {
        if (ping < 120) {
            return "0.82 crisp";
        } else {
            return "0.90 stable";
        }
    }

    private ChatColor getConfidenceColor(double confidence) {
        if (confidence >= 0.9)
            return ChatColor.GREEN;
        if (confidence >= 0.7)
            return ChatColor.YELLOW;
        if (confidence >= 0.5)
            return ChatColor.GOLD;
        return ChatColor.RED;
    }
}
