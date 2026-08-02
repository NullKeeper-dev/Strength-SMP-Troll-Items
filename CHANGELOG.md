# Changelog

All notable changes to Strength SMP Troll Items are documented here.

## [0.1.0] - 2026-08-01

### Added

- Configurable Resizing Sword with persistent `0.05` grow and sneak-shrink behavior for every living entity.
- Configurable Spooky Crossbow that adds five persistent Speed II Ravagers per successful player hit.
- Shooter/target Ravager assignments, target recovery, participant-only visibility, ProtocolLib sound and interaction filtering, and Bukkit damage defenses.
- Hungry Berry conversion that permanently marks a complete held stack as edible and instantly consumes one item on right-click with zero nutrition by default.
- `/trollitems give` and `/trollitems reload` commands with operator-default permissions, validation, overflow handling, and safe reload rollback.
- Java 25 Gradle build, Bukkit/Spigot 26.1 API baseline, ProtocolLib hard dependency, Paper-backed MockBukkit coverage, and a 26.2 API compatibility compile.

### Safety

- Ravager spawn radius is capped at 64 blocks to prevent unsafe config-driven scans.
- Private Ravager packet decoding fails closed, and scheduled work isolates and logs runtime errors.
- Private Ravagers reject non-player AI targets, leave runtime indexes on chunk unload, and still spawn from a tagged projectile if its shooter disconnects before impact.
- Extreme integer settings are rejected before conversion, and malformed persistent entity/projectile metadata produces contextual warnings.
