package com.cireex.combatsync.combat;

import com.cireex.combatsync.CireeXCombatSync;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSprintEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects W-tap sprint resets and applies a one-time KB boost.
 * Sprint toggled OFF → ON within 2 ticks = legit reset.
 * Never stacks — one boost per 5 ticks max.
 */
public class SprintResetHandler implements Listener {

    private final CireeXCombatSync plugin;

    private final Map<UUID, Long> lastSprintOff = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastSprintOn = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> legitSprintReset = new ConcurrentHashMap<>();

    // prevent boost stacking
    private final Map<UUID, Long> lastBoostTick = new ConcurrentHashMap<>();

    public SprintResetHandler(CireeXCombatSync plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSprintToggle(PlayerToggleSprintEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        long currentTick = getCurrentTick();

        if (event.isSprinting()) {
            lastSprintOn.put(uuid, currentTick);

            Long offTime = lastSprintOff.get(uuid);
            if (offTime != null) {
                int maxWindow = plugin.getConfigReloader().getSprintResetMaxTickWindow();
                if (currentTick - offTime <= maxWindow) {
                    legitSprintReset.put(uuid, true);
                    plugin.debug("Sprint reset detected for " + player.getName());
                }
            }
        } else {
            lastSprintOff.put(uuid, currentTick);
            legitSprintReset.put(uuid, false);
        }
    }

    /** Consumes the reset flag — one-time use per detection. */
    public boolean isLegitSprintReset(Player player) {
        if (!plugin.getConfigReloader().isSprintResetEnabled()) {
            return false;
        }

        UUID uuid = player.getUniqueId();
        Boolean reset = legitSprintReset.get(uuid);

        if (reset != null && reset) {
            long currentTick = getCurrentTick();
            Long lastBoost = lastBoostTick.get(uuid);

            // only one boost per 5 ticks
            if (lastBoost != null && currentTick - lastBoost < 5) {
                return false;
            }

            legitSprintReset.put(uuid, false);
            lastBoostTick.put(uuid, currentTick);
            return true;
        }

        return false;
    }

    public double getSprintResetMultiplier() {
        return plugin.getConfigReloader().getSprintResetMultiplier();
    }

    public long getLastSprintOnTime(Player player) {
        return lastSprintOn.getOrDefault(player.getUniqueId(), 0L);
    }

    private long getCurrentTick() {
        return System.currentTimeMillis() / 50;
    }

    public void removePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        lastSprintOff.remove(uuid);
        lastSprintOn.remove(uuid);
        legitSprintReset.remove(uuid);
        lastBoostTick.remove(uuid);
    }

    public void cleanup() {
        lastSprintOff.clear();
        lastSprintOn.clear();
        legitSprintReset.clear();
        lastBoostTick.clear();
    }
}
