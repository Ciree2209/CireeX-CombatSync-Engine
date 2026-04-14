# CireeX Combat Sync

A high-performance combat engine for Minecraft 1.8.8, designed to provide consistent knockback mechanics and precise hit detection.

## Features

### Knockback Engine
- **Profile System**: Define multiple knockback profiles with custom horizontal, vertical, and friction values.
- **Arena Overrides**: Apply specific knockback profiles to different game arenas or world regions.
- **Dynamic Physics**: Friction and velocity application layers designed to match standard 1.8.8 mechanics.

### Hit Detection & Lag Compensation
- **Hybrid Validation**: Uses AABB bounding box checks combined with raytracing for accurate hit registration.
- **Position Snapshots**: Tracks historical entity positions to compensate for network latency.
- **Confidence Scoring**: Analyzes reach and angles to adjust knockback based on hit reliability.

### Combat Mechanics
- **Sprint Reset Detection**: Real-time detection of W-taps and S-taps for mechanical consistency.
- **Combo Tracking**: Tracks consecutive hits for use in custom mechanics or telemetry.
- **Velocity Smoothing**: Minimizes rubber-banding during high-velocity events.

### Tournament Mode
- **Deterministic Play**: Disables all movement randomization for pure skill-based matches.
- **KB Logging**: Logs every knockback calculation with SHA-256 hashes for post-match verification.
- **Match Summaries**: Automatically exports match data (hits, duration, winner) to external reports.

### Diagnostics & Telemetry
- **Combat Telemetry**: Tracks hit registration success rates, average combos, and ping-based performance.
- **Debug Utility**: Use `/kbdebug` to visualize velocity vectors and hit detection angles in real-time.
- **Conflict Detector**: Automatically identifies other plugins that might interfere with combat mechanics.

## Commands

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/kb reload` | Reloads all configuration files and profiles. | `combatsync.reload` |
| `/kb list` | Lists all loaded knockback profiles. | `combatsync.admin` |
| `/kb info [player]` | Shows current profile and combat state for a player. | `combatsync.admin` |
| `/kb setarena <id> <profile>` | Assigns a profile to a specific arena. | `combatsync.admin` |
| `/kb test [player]` | Manually applies the current KB profile for testing. | `combatsync.debug` |
| `/kb stats` | Displays real-time combat telemetry and statistics. | `combatsync.admin` |
| `/kb tournament <start|stop>` | Manages deterministic tournament mode. | `combatsync.admin` |
| `/kbdebug` | Toggles visual debug mapping for hit detection. | `combatsync.debug` |

## API Integration

Developers can hook into the engine using the `CombatAPI` registered via the Bukkit Services Manager.

```java
CombatAPI api = Bukkit.getServicesManager().getRegistration(CombatAPI.class).getProvider();

// Example: Get a player's current combo
int combo = api.getCombo(player);

// Example: Get current KB profile
KBProfile profile = api.getProfile(player);
```

## Requirements
- **Server Version**: Bukkit/Spigot/Paper 1.8.8
- **Java**: 8 or higher

---
Developed for the competitive PvP community.
