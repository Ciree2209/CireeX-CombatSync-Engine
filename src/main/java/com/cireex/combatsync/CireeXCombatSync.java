package com.cireex.combatsync;

import com.cireex.combatsync.api.CombatAPI;
import com.cireex.combatsync.api.CombatAPIImpl;
import com.cireex.combatsync.combat.*;
import com.cireex.combatsync.commands.KBDebugCommand;
import com.cireex.combatsync.commands.KnockbackCommand;
import com.cireex.combatsync.lag.LagCompensationEngine;
import com.cireex.combatsync.listeners.CombatListener;
import com.cireex.combatsync.telemetry.CombatTelemetry;
import com.cireex.combatsync.tournament.TournamentMode;
import com.cireex.combatsync.util.PluginConflictDetector;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * @author CireeX
 * @version 1.0.0
 */
public class CireeXCombatSync extends JavaPlugin {

    private static CireeXCombatSync instance;

    private ConfigReloader configReloader;
    private ProfileManager profileManager;
    private RandomizationEngine randomizationEngine;
    private GroundStateResolver groundStateResolver;
    private EnvironmentResolver environmentResolver;
    private SprintResetHandler sprintResetHandler;
    private ComboTracker comboTracker;
    private HitDetectionEngine hitDetectionEngine;
    private KnockbackEngine knockbackEngine;
    private VelocityApplier velocityApplier;
    private CombatEngine combatEngine;
    private LagCompensationEngine lagCompensationEngine;

    private ArenaKBResolver arenaKBResolver;
    private HitConfidenceScorer hitConfidenceScorer;
    private VelocitySmoother velocitySmoother;
    private CombatTelemetry combatTelemetry;
    private TournamentMode tournamentMode;
    private PluginConflictDetector conflictDetector;

    private CombatAPIImpl combatAPI;

    @Override
    public void onEnable() {
        instance = this;
        long startTime = System.currentTimeMillis();

        log(Level.INFO, "");
        log(Level.INFO, "╔══════════════════════════════════════════════╗");
        log(Level.INFO, "║    CireeXCombatSync™ - Combat Engine         ║");
        log(Level.INFO, "║    Tournament-grade 1.8.8 Combat System      ║");
        log(Level.INFO, "║    Version: " + getDescription().getVersion() + "                            ║");
        log(Level.INFO, "╚══════════════════════════════════════════════╝");
        log(Level.INFO, "");

        saveDefaultConfig();
        saveResource("profiles.yml", false);
        saveResource("arenas.yml", false);

        conflictDetector = new PluginConflictDetector(this);
        conflictDetector.detect();

        initializeModules();
        registerListeners();
        registerCommands();
        registerAPI();

        long loadTime = System.currentTimeMillis() - startTime;
        log(Level.INFO, "");
        log(Level.INFO, "✓ Combat modules initialized");
        log(Level.INFO, "✓ Loaded " + profileManager.getProfileCount() + " knockback profiles");
        log(Level.INFO, "✓ Loaded " + arenaKBResolver.getArenaIds().size() + " arena overrides");
        log(Level.INFO, "✓ Telemetry system active");
        log(Level.INFO, "✓ Tournament mode ready");
        log(Level.INFO, "");
        log(Level.INFO, "CireeXCombatSync enabled in " + loadTime + "ms!");
        log(Level.INFO, "");
    }

    @Override
    public void onDisable() {
        log(Level.INFO, "CireeXCombatSync disabled.");

        if (tournamentMode != null && tournamentMode.isActive()) {
            tournamentMode.deactivate();
        }

        if (comboTracker != null)
            comboTracker.cleanup();
        if (sprintResetHandler != null)
            sprintResetHandler.cleanup();
        if (lagCompensationEngine != null)
            lagCompensationEngine.cleanup();
        if (velocitySmoother != null)
            velocitySmoother.clear();

        instance = null;
    }

    private void initializeModules() {
        log(Level.INFO, "Initializing combat modules...");

        configReloader = new ConfigReloader(this);
        profileManager = new ProfileManager(this);
        arenaKBResolver = new ArenaKBResolver(this);
        randomizationEngine = new RandomizationEngine(this);
        groundStateResolver = new GroundStateResolver(this);
        environmentResolver = new EnvironmentResolver(this);
        sprintResetHandler = new SprintResetHandler(this);
        comboTracker = new ComboTracker(this);
        lagCompensationEngine = new LagCompensationEngine(this);
        hitConfidenceScorer = new HitConfidenceScorer(this);
        velocitySmoother = new VelocitySmoother();
        combatTelemetry = new CombatTelemetry(this);
        tournamentMode = new TournamentMode(this);

        hitDetectionEngine = new HitDetectionEngine(this, groundStateResolver, environmentResolver);
        hitDetectionEngine.setLagCompensation(lagCompensationEngine);

        knockbackEngine = new KnockbackEngine(this,
                profileManager,
                randomizationEngine,
                groundStateResolver,
                environmentResolver,
                sprintResetHandler,
                comboTracker);
        knockbackEngine.setLagCompensation(lagCompensationEngine);

        velocityApplier = new VelocityApplier(this);

        combatEngine = new CombatEngine(this,
                hitDetectionEngine,
                knockbackEngine,
                velocityApplier,
                comboTracker,
                sprintResetHandler);

        log(Level.INFO, "All combat modules initialized.");
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new CombatListener(this, combatEngine), this);
        Bukkit.getPluginManager().registerEvents(sprintResetHandler, this);
    }

    private void registerCommands() {
        KnockbackCommand kbCommand = new KnockbackCommand(this);
        getCommand("kb").setExecutor(kbCommand);
        getCommand("kb").setTabCompleter(kbCommand);

        KBDebugCommand kbDebugCommand = new KBDebugCommand(this);
        getCommand("kbdebug").setExecutor(kbDebugCommand);
    }

    private void registerAPI() {
        combatAPI = new CombatAPIImpl(this, profileManager, knockbackEngine, comboTracker);
        Bukkit.getServicesManager().register(
                CombatAPI.class,
                combatAPI,
                this,
                ServicePriority.High);
    }

    public void reload() {
        reloadConfig();
        configReloader.reload();
        profileManager.reload();
        arenaKBResolver.reload();
        randomizationEngine.reload();
        log(Level.INFO, "Configuration reloaded. " + profileManager.getProfileCount() + " profiles, " +
                arenaKBResolver.getArenaIds().size() + " arena overrides loaded.");
    }

    public void log(Level level, String message) {
        getLogger().log(level, message);
    }

    public void debug(String message) {
        if (getConfig().getBoolean("debug", false)) {
            getLogger().info("[DEBUG] " + message);
        }
    }

    public static CireeXCombatSync getInstance() {
        return instance;
    }

    public ConfigReloader getConfigReloader() {
        return configReloader;
    }

    public ProfileManager getProfileManager() {
        return profileManager;
    }

    public RandomizationEngine getRandomizationEngine() {
        return randomizationEngine;
    }

    public GroundStateResolver getGroundStateResolver() {
        return groundStateResolver;
    }

    public EnvironmentResolver getEnvironmentResolver() {
        return environmentResolver;
    }

    public SprintResetHandler getSprintResetHandler() {
        return sprintResetHandler;
    }

    public ComboTracker getComboTracker() {
        return comboTracker;
    }

    public HitDetectionEngine getHitDetectionEngine() {
        return hitDetectionEngine;
    }

    public KnockbackEngine getKnockbackEngine() {
        return knockbackEngine;
    }

    public VelocityApplier getVelocityApplier() {
        return velocityApplier;
    }

    public CombatEngine getCombatEngine() {
        return combatEngine;
    }

    public LagCompensationEngine getLagCompensationEngine() {
        return lagCompensationEngine;
    }

    public CombatAPIImpl getCombatAPI() {
        return combatAPI;
    }

    public ArenaKBResolver getArenaKBResolver() {
        return arenaKBResolver;
    }

    public HitConfidenceScorer getHitConfidenceScorer() {
        return hitConfidenceScorer;
    }

    public VelocitySmoother getVelocitySmoother() {
        return velocitySmoother;
    }

    public CombatTelemetry getCombatTelemetry() {
        return combatTelemetry;
    }

    public TournamentMode getTournamentMode() {
        return tournamentMode;
    }

    public PluginConflictDetector getConflictDetector() {
        return conflictDetector;
    }
}
