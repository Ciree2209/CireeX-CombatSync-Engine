package com.cireex.combatsync.combat;

import com.cireex.combatsync.CireeXCombatSync;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/** KB multipliers for water, lava, webs, jumping, and partial-block states. */
public class EnvironmentResolver {

    private final CireeXCombatSync plugin;

    public EnvironmentResolver(CireeXCombatSync plugin) {
        this.plugin = plugin;
    }

    public boolean isInWater(Player player) {
        Material type = player.getLocation().getBlock().getType();
        return type == Material.WATER || type == Material.STATIONARY_WATER;
    }

    public boolean isInLava(Player player) {
        Material type = player.getLocation().getBlock().getType();
        return type == Material.LAVA || type == Material.STATIONARY_LAVA;
    }

    public boolean isInWeb(Player player) {
        return player.getLocation().getBlock().getType() == Material.WEB;
    }

    public double getHorizontalMultiplier(Player player) {
        ConfigReloader config = plugin.getConfigReloader();

        if (isInWater(player)) {
            return config.getWaterHorizontalMult();
        }
        if (isInLava(player)) {
            return config.getLavaHorizontalMult();
        }
        if (isInWeb(player)) {
            return config.getWebHorizontalMult();
        }

        return 1.0;
    }

    public double getVerticalMultiplier(Player player) {
        ConfigReloader config = plugin.getConfigReloader();

        if (isInWater(player)) {
            return config.getWaterVerticalMult();
        }
        if (isInLava(player)) {
            return config.getLavaVerticalMult();
        }
        if (isInWeb(player)) {
            return config.getWebVerticalMult();
        }

        return 1.0;
    }

    public double getJumpingLeniency(Player player, GroundStateResolver groundResolver) {
        if (groundResolver.isJumping(player)) {
            return plugin.getConfigReloader().getJumpingVerticalLeniency();
        }
        return 0.0;
    }

    public double getFallingReachReduction(Player attacker, GroundStateResolver groundResolver) {
        if (groundResolver.isFalling(attacker)) {
            return plugin.getConfigReloader().getFallingReachReduction();
        }
        return 0.0;
    }

    public double getAngleToleranceReduction(Player victim, GroundStateResolver groundResolver) {
        if (groundResolver.isOnPartialBlock(victim)) {
            return plugin.getConfigReloader().getSlabStairsAngleReduction();
        }
        return 0.0;
    }

    // ghost hit prevention
    public boolean hasBlockBetween(Location from, Location to) {
        double distance = from.distance(to);
        if (distance < 0.5)
            return false;

        double stepSize = 0.1;
        org.bukkit.util.Vector direction = to.toVector().subtract(from.toVector()).normalize();

        for (double d = 0; d < distance; d += stepSize) {
            Location point = from.clone().add(direction.clone().multiply(d));
            Block block = point.getBlock();

            if (isSolidForRaytrace(block)) {
                return true;
            }
        }

        return false;
    }

    private boolean isSolidForRaytrace(Block block) {
        Material type = block.getType();

        if (type == Material.AIR)
            return false;
        if (type == Material.WATER || type == Material.STATIONARY_WATER)
            return false;
        if (type == Material.LAVA || type == Material.STATIONARY_LAVA)
            return false;

        // Transparent blocks that don't block hits
        if (type == Material.GLASS || type == Material.THIN_GLASS)
            return false;
        if (type == Material.IRON_FENCE)
            return false; // Iron bars
        if (type == Material.WEB)
            return false;
        if (type == Material.CARPET)
            return false;
        if (type == Material.SIGN_POST || type == Material.WALL_SIGN)
            return false;

        // Partial blocks that might block - check more carefully
        String name = type.name();
        if (name.contains("FENCE") && !name.contains("GATE")) {
            // Fences have gaps
            return false;
        }

        return type.isSolid();
    }

    public boolean isSafeLocation(Location loc) {
        Block block = loc.getBlock();
        Block above = block.getRelative(0, 1, 0);
        return !block.getType().isSolid() && !above.getType().isSolid();
    }
}
