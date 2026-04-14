# ⚔️ CireeX Combat Sync v1.0 (Enterprise)

[![Version](https://img.shields.io/badge/Version-1.0.0--Stable-blue.svg?style=for-the-badge)](https://github.com/CireeX/CombatSync)
[![Format](https://img.shields.io/badge/Format-Spigot/Paper-yellow.svg?style=for-the-badge)](https://papermc.io)
[![API](https://img.shields.io/badge/API-1.8.8--R0.1-green.svg?style=for-the-badge)](https://helpch.at/docs/1.8.8/)
[![Anticheat](https://img.shields.io/badge/Anticheat-Ready-red.svg?style=for-the-badge)](https://github.com/CireeX/CombatSync)
[![License](https://img.shields.io/badge/License-MIT-green.svg?style=for-the-badge)](LICENSE)

**CireeX Combat Sync** is a premium, tournament-grade combat synchronization engine engineered for high-concurrency 1.8.8 PvP networks. Built to match and exceed the mechanical "feel" of top-tier networks,it utilizes a sophisticated velocity pipeline and historical reconciliation to eliminate the "latency gap" in competitive play.

---

## 💎 Why CireeX Combat Sync?

In high-stakes PvP (Practice, Ranked BedWars), the difference between a combo and a loss is often measured in milliseconds. Standard Minecraft combat calculation is reactive and often inconsistent under varying network conditions. 

- **Tournament-Grade Feel**: Precise reproduction of the beloved 1.8.8 "Smooth" knockback, eliminating jumpy or erratic velocity pulses.
- **Latency-Equalization**: Advanced lag compensation ensures that a player with 10ms ping and a player with 150ms ping experience fair, consistent hit registration and knockback.
- **Anticheat-Invisible Architecture**: Designed from the ground up to never cancel events or teleport players, ensuring 100% compatibility with Verus, Vulcan, Grim, and other top-tier anticheats.

---

## 🔥 The Precision Engine Breakdown

The CireeX engine orchestrates a total of **15+ independent combat services** to deliver a flawless pvp experience.

### 🎯 Hybrid Hit Detection
*Eliminating "Ghost-Hits" and reach inconsistencies.*
- **AABB + Raytrace Reconciliation**: Cross-references bounding box intersections with pixel-perfect raycasts to ensure every click counts.
- **Historical Reconstruction**: Back-clocks entity positions to the exact millisecond an attacker clicked, accounting for network delay.
- **Confidence Scoring**: Analyzes hit angles and distance to prevent "Impossible Hits" while maintaining aggressive reach leniency.

### 📐 The Velocity Pipeline (11-Step Calculation)
*How we calculate the perfect knockback, every time.*
1. **Grounded State Resolution**: Detects if the victim is on ground, slab, or stairs.
2. **Vanilla Reference Fetch**: Reads the base protocol velocity for the hit.
3. **Friction Application**: Applies profile-specific horizontal/vertical friction.
4. **Sprint Reset Detection**: Real-time detection of W/S/Z-taps with legitimate velocity boosts.
5. **Enchantment Escalation**: Calculates Knockback I/II and Sharpness modifiers.
6. **Profile Multipliers**: Applies the custom presets (e.g. Practice vs. BedWars).
7. **Combo Protection**: Dynamic scale reduction for victims in infinite "zero-kb" loops.
8. **Edge/Void Normalization**: Smooths knockback vector when near the void (Perfect for BedWars).
9. **Latency Modification**: Adjusts friction based on the victim's current jitter/ping.
10. **Micro-Randomization**: Adds <±1% Gaussian jitter to prevent anticheat pattern matching.
11. **Clamp & Integrity Layer**: Final safety check to ensure values never exceed anticheat limits.

### 🛡️ Enterprise Stability Systems
*Ensuring your server stays up while the combat stays smooth.*
- **Plugin Conflict Detector**: Automatically scans and disables conflicting combat systems in other plugins.
- **Combat Telemetry**: Real-time tracking of hit-registration efficiency and velocity application accuracy.
- **Tournament Mode**: A high-priority execution state for arenas that prioritizes combat packets above all other server tasks.
- **Velocity Smoother**: Prevents "rubber-banding" during high knockback events (e.g. TNT Jumping).

---

## 📊 Performance & Presets

The engine comes with **7 Production-Ready Profiles** optimized for different game modes:

| Profile | Focus | Feel Description |
| :--- | :--- | :--- |
| **Vanilla+** | Generic | The base Minecraft feel, but without the "lag-spikes." |
| **Practice (MMC)** | Duels | Low vertical, tight horizontal. Perfect for combos. |
| **Ranked BedWars** | Strategic | Enhanced edge-protection to allow for void clutches. |
| **BedWars Clutch** | Survival | High vertical air-time to allow for block-placement. |
| **Duels Consistent** | Comp | Zero randomization for pure mechanical skill testing. |
| **High KB** | Fun | Exaggerated velocity for unique game modes (e.g. Knockback-FFA). |
| **Low KB** | Tank | Reduced knockback for Factions/Gapple-style combat. |

---

## 🛠️ Commands & Control

The CLI provides total control over the server's mechanical identity.

| Category | Primary Commands |
| :--- | :--- |
| **Management** | `/kb reload`, `/kb list`, `/kb info` |
| **Player Tuning** | `/kb profile <player> <profile>` |
| **Diagnostics** | `/kb test`, `/kbdebug` |

### Key Command Highlights:
- `/kb reload`: Zero-downtime hot-reload of all `profiles.yml` and `config.yml` values.
- `/kbdebug`: Real-time visualization of velocity vectors and hit detection angles.
- `/kb profile`: Dynamically switch a player's combat engine (Great for VIP/Donator perks).

---

## 🔌 Developer API

CireeX Combat Sync offers a comprehensive API for deeper integration into your game-mode logic.

```java
// Access the CombatAPI via Bukkit Services
CombatAPI api = Bukkit.getServicesManager().getRegistration(CombatAPI.class).getProvider();

// Hook into the Physics Pipeline
@EventHandler
public void onKnockback(KnockbackApplyEvent event) {
    // Modify velocity before it's sent to the netty queue
    if (isInSpecialRegion(event.getPlayer())) {
        event.multiplyHorizontal(1.2);
    }
}

// Track Combo States
int currentCombo = api.getCombo(player);
```

---

## 🔒 Anticheat Compliance (Zero False Flags)

The engine is engineered for **100% compliance** with Verus, GrimAC, Vulcan, and Spartan.

- **Hard Clamps**: Max Y velocity is hard-clamped at **0.42** (the Minecraft standard).
- **Zero Cancellation**: We never cancel a velocity event; we only modify the payload, ensuring the anticheat's "expected position" remains in sync.
- **Protocol Fidelity**: All velocity is applied via standard packets (No `teleport()` or `packet-injection` hacks).
- **Gaussian Jitter**: Randomization follows a natural bell curve, preventing manual pattern detection or heuristic flags.

---

## 📦 Getting Started

1. **Deploy**: Drop `CireeXCombatSync.jar` into your `/plugins` folder.
2. **Setup**: The engine will auto-detect your protocol and apply the "Vanilla+" preset.
3. **Configure**: Tune your `profiles.yml` to match your network's specific playstyle.

---

## 💻 Technical Architecture

Built on a **High-Performance Combat Pipeline**, CireeX Combat Sync separates data calculation from application, ensuring that network lag or thread congestion cannot cause "skipped hits."

> [!IMPORTANT]
> This engine is optimized specifically for **1.8.8**. It utilizes NMS access and Protocol-level optimizations that are not compatible with generic 1.12+ Spigot versions.

---

Developed with ❤️ for the PvP Community.
*Part of the CireeX High-Performance Suite.*
