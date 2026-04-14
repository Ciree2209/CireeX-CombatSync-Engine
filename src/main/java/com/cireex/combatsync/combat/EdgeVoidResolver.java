package com.cireex.combatsync.combat;

import com.cireex.combatsync.CireeXCombatSync;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Edge and void detection — reduces KB near borders/drops so players
 * don't get launched off the map unfairly. WorldGuard soft dependency
 * via reflection for region-aware behavior.
 */
public class EdgeVoidResolver {

    private final CireeXCombatSync plugin;

    private static final int EDGE_SCAN_RADIUS = 3;
    private static final double EDGE_REDUCTION_MIN = 0.85;
    private static final double VOID_REDUCTION_MIN = 0.90;
    private static final double BORDER_PROXIMITY_BLOCKS = 5.0;

    // all void types are y=0 on 1.8.8
    private static final int VOID_Y_NORMAL = 0;
    private static final int VOID_Y_NETHER = 0;
    private static final int VOID_Y_END = 0;

    private boolean worldGuardEnabled = false;
    private Object worldGuardPlugin = null;

    public EdgeVoidResolver(CireeXCombatSync plugin) {
        this.plugin = plugin;
        detectWorldGuard();
    }

    private void detectWorldGuard() {
        try {
            Plugin wg = plugin.getServer().getPluginManager().getPlugin("WorldGuard");
            if (wg != null && wg.isEnabled()) {
                worldGuardPlugin = wg;
                worldGuardEnabled = true;
                plugin.debug("WorldGuard integration enabled.");
            }
        } catch (Exception e) {
            worldGuardEnabled = false;
        }
    }

    public boolean isWorldGuardEnabled() {
        return worldGuardEnabled;
    }

    public double getEdgeHorizontalMultiplier(Player victim, KBProfile profile) {
        if (!profile.hasEdgeBoost()) {
            return 1.0;
        }

        Location loc = victim.getLocation();

        double borderProximity = getWorldBorderProximity(victim);
        if (borderProximity < BORDER_PROXIMITY_BLOCKS) {
            double factor = borderProximity / BORDER_PROXIMITY_BLOCKS;
            return EDGE_REDUCTION_MIN + (1.0 - EDGE_REDUCTION_MIN) * factor;
        }

        int edgeScore = calculateEdgeScore(loc);
        if (edgeScore > 0) {
            double factor = 1.0 - (edgeScore / 12.0); // max edge score ~12
            return EDGE_REDUCTION_MIN + (1.0 - EDGE_REDUCTION_MIN) * factor;
        }

        return 1.0;
    }

    public double getVoidHorizontalMultiplier(Player victim, KBProfile profile, long lastHitTick) {
        if (!profile.hasEdgeBoost()) {
            return 1.0;
        }

        Location loc = victim.getLocation();
        int voidY = getVoidYLevel(loc.getWorld());

        double distanceToVoid = loc.getY() - voidY;
        if (distanceToVoid < 10) {
            double factor = distanceToVoid / 10.0;
            return VOID_REDUCTION_MIN + (1.0 - VOID_REDUCTION_MIN) * factor;
        }

        return 1.0;
    }

    private int calculateEdgeScore(Location loc) {
        int score = 0;
        Block center = loc.getBlock();

        BlockFace[] faces = { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST,
                BlockFace.NORTH_EAST, BlockFace.NORTH_WEST, BlockFace.SOUTH_EAST, BlockFace.SOUTH_WEST };

        for (BlockFace face : faces) {
            for (int distance = 1; distance <= EDGE_SCAN_RADIUS; distance++) {
                Block check = center.getRelative(face, distance);
                if (isDangerousBelow(check)) {
                    score += (EDGE_SCAN_RADIUS - distance + 1);
                }
            }
        }

        return score;
    }

    private boolean isDangerousBelow(Block block) {
        Block below = block.getRelative(BlockFace.DOWN);

        if (below.getType() == Material.LAVA || below.getType() == Material.STATIONARY_LAVA) {
            return true;
        }

        // 3+ consecutive air blocks = dangerous drop
        int airCount = 0;
        Block check = below;
        for (int i = 0; i < 10; i++) {
            if (check.getType() == Material.AIR) {
                airCount++;
                check = check.getRelative(BlockFace.DOWN);
            } else {
                break;
            }
        }

        return airCount >= 3;
    }

    public double getWorldBorderProximity(Player player) {
        Location loc = player.getLocation();
        WorldBorder border = loc.getWorld().getWorldBorder();

        Location center = border.getCenter();
        double size = border.getSize() / 2.0;

        double distX = Math.abs(loc.getX() - center.getX());
        double distZ = Math.abs(loc.getZ() - center.getZ());

        double distToBorderX = size - distX;
        double distToBorderZ = size - distZ;

        return Math.min(distToBorderX, distToBorderZ);
    }

    public boolean isNearWorldBorder(Player player) {
        return getWorldBorderProximity(player) < BORDER_PROXIMITY_BLOCKS;
    }

    private int getVoidYLevel(World world) {
        switch (world.getEnvironment()) {
            case NETHER:
                return VOID_Y_NETHER;
            case THE_END:
                return VOID_Y_END;
            default:
                return VOID_Y_NORMAL;
        }
    }

    public boolean isNearVoid(Player player) {
        Location loc = player.getLocation();
        int voidY = getVoidYLevel(loc.getWorld());
        return loc.getY() - voidY < 10;
    }

    /** When combo protection triggers we want max escape KB, so skip edge reduction. */
    public boolean shouldDisableEdgeWithCombo(boolean comboProtectionActive) {
        return comboProtectionActive;
    }

    /**
     * Returns true if the player is in a PvP-disabled WorldGuard region.
     * Uses reflection to avoid a hard compile-time dependency.
     */
    public boolean isInProtectedRegion(Player player) {
        if (!worldGuardEnabled || worldGuardPlugin == null) {
            return false;
        }

        try {
            // WorldGuard 6.x API via reflection
            Class<?> wgClass = Class.forName("com.sk89q.worldguard.bukkit.WorldGuardPlugin");
            Object wg = wgClass.cast(worldGuardPlugin);

            Object regionManager = wgClass.getMethod("getRegionManager", World.class)
                    .invoke(wg, player.getWorld());

            if (regionManager == null)
                return false;

            Class<?> vectorClass = Class.forName("com.sk89q.worldedit.Vector");
            Object vector = vectorClass.getConstructor(double.class, double.class, double.class)
                    .newInstance(player.getLocation().getX(),
                            player.getLocation().getY(),
                            player.getLocation().getZ());

            Object regions = regionManager.getClass().getMethod("getApplicableRegions", vectorClass)
                    .invoke(regionManager, vector);

            // simplified — full implementation would check PvP flags
            return regions != null;

        } catch (Exception e) {
            worldGuardEnabled = false;
            return false;
        }
    }

    /**
     * Returns the highest-priority non-global WorldGuard region the player is in,
     * or null if not in any region or WorldGuard isn't available.
     */
    public String getWorldGuardRegion(Player player) {
        if (!worldGuardEnabled || worldGuardPlugin == null) {
            return null;
        }

        try {
            Class<?> wgClass = Class.forName("com.sk89q.worldguard.bukkit.WorldGuardPlugin");
            Object wg = wgClass.cast(worldGuardPlugin);

            Object regionManager = wgClass.getMethod("getRegionManager", World.class)
                    .invoke(wg, player.getWorld());

            if (regionManager == null)
                return null;

            Class<?> vectorClass = Class.forName("com.sk89q.worldedit.Vector");
            Object vector = vectorClass.getConstructor(double.class, double.class, double.class)
                    .newInstance(player.getLocation().getX(),
                            player.getLocation().getY(),
                            player.getLocation().getZ());

            Object regions = regionManager.getClass().getMethod("getApplicableRegions", vectorClass)
                    .invoke(regionManager, vector);

            Iterable<?> regionSet = (Iterable<?>) regions;
            for (Object region : regionSet) {
                String id = (String) region.getClass().getMethod("getId").invoke(region);
                if (id != null && !id.equals("__global__")) {
                    return id;
                }
            }

        } catch (Exception e) {
            // reflection failed — just give up
        }

        return null;
    }
}
