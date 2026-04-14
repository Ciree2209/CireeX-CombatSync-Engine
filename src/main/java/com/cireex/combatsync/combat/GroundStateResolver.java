package com.cireex.combatsync.combat;

import com.cireex.combatsync.CireeXCombatSync;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

/** More accurate than Player.isOnGround() — handles slabs, stairs, fences, edges. */
@SuppressWarnings("deprecation") // Using 1.8.8 API methods
public class GroundStateResolver {

    public GroundStateResolver(CireeXCombatSync plugin) {
        // Plugin reference available for future use
    }

    public boolean isOnGround(Player player) {
        if (player.isOnGround()) {
            return true;
        }

        Location loc = player.getLocation();
        Block below = loc.getBlock().getRelative(BlockFace.DOWN);

        if (isGroundBlock(below)) {
            double blockTopY = below.getY() + getBlockHeight(below);
            if (loc.getY() - blockTopY < 0.1) {
                return true;
            }
        }

        // also check adjacent blocks when near a block edge
        double x = loc.getX();
        double z = loc.getZ();
        double fractX = x - Math.floor(x);
        double fractZ = z - Math.floor(z);

        if (fractX < 0.3 || fractX > 0.7 || fractZ < 0.3 || fractZ > 0.7) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0)
                        continue;
                    Block adjacent = below.getRelative(dx, 0, dz);
                    if (isGroundBlock(adjacent)) {
                        double blockTopY = adjacent.getY() + getBlockHeight(adjacent);
                        if (loc.getY() - blockTopY < 0.1) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private boolean isGroundBlock(Block block) {
        Material type = block.getType();

        if (type == Material.AIR)
            return false;
        if (type == Material.WATER || type == Material.STATIONARY_WATER)
            return false;
        if (type == Material.LAVA || type == Material.STATIONARY_LAVA)
            return false;

        return type.isSolid() ||
                isSlab(type) ||
                isStairs(type) ||
                isFence(type) ||
                type == Material.CARPET ||
                type == Material.SNOW;
    }

    private double getBlockHeight(Block block) {
        Material type = block.getType();

        if (isSlab(type)) {
            byte data = block.getData();
            if ((data & 0x8) != 0) {
                return 1.0; // Top slab
            }
            return 0.5; // Bottom slab
        }

        if (isStairs(type)) {
            return 1.0; // complex shape, treat as full for simplicity
        }

        if (isFence(type)) {
            return 1.5; // fence collision is 1.5 blocks tall
        }

        if (type == Material.SOUL_SAND) {
            return 0.875;
        }

        if (type == Material.CARPET) {
            return 0.0625; // 1/16 block
        }

        if (type == Material.SNOW) {
            byte data = block.getData();
            return (data + 1) * 0.125;
        }

        return 1.0;
    }

    private boolean isSlab(Material type) {
        String name = type.name();
        return name.contains("STEP") || name.contains("SLAB");
    }

    private boolean isStairs(Material type) {
        return type.name().contains("STAIRS");
    }

    private boolean isFence(Material type) {
        String name = type.name();
        return name.contains("FENCE") && !name.contains("GATE");
    }

    public boolean isJumping(Player player) {
        return player.getVelocity().getY() > 0.1 && !isOnGround(player);
    }

    public boolean isFalling(Player player) {
        return player.getVelocity().getY() < -0.1 && !isOnGround(player);
    }

    public boolean isOnPartialBlock(Player player) {
        Block below = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
        Material type = below.getType();
        return isSlab(type) || isStairs(type);
    }
}
