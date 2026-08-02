# Changelog

All notable changes to Strength SMP Troll Items are documented here.

## [2.1.0]

### Changed

- Valid positive damage events are no longer discarded solely because the
  target's `noDamageTicks` value is positive, making Resizing Sword activation
  consistent with Paper's registered damage ticks.
- An asynchronous Modrinth update checker now alerts permitted administrators
  once per server restart and can be disabled with
  `update-checker.enabled: false`.
- Hungry Berry conversions now use Minecraft's native food and consumable
  components with a six-tick eating time by default.
- Players must keep holding right-click; releasing early or changing the active
  item cancels consumption through the native item-use lifecycle.
- Converted items remain edible at full hunger, use configurable nutrition and
  saturation, and suppress their original right-click action.
- Previously marked stacks receive current native components before use.
- Five platform/version-classified artifacts are replaced by one universal jar
  compiled against Paper 26.1.1 and compatibility-checked against all five
  supported Paper/Purpur APIs.

## [1.0.0]

### Changed

- Troll-item hit effects now activate on valid damage ticks even when armor,
  enchantments, or Resistance reduce final health damage to zero.
- Creative, Spectator, explicitly invulnerable, and damage-immunity-window
  targets do not activate hit effects; cancelled protection-plugin events and
  zero raw damage remain ignored.
- Server support changed from the Bukkit-family matrix to the approved
  Paper/Purpur-only release matrix; Spigot and CraftBukkit are unsupported.
- Builds now produce five exact-target jars: Paper 26.1.1, Paper 26.1.2,
  Paper 26.2, Purpur 26.1.2, and Purpur 26.2; no universal jar is produced.
- ProtocolLib is no longer required; private Ravagers use native Paper entity
  visibility and remain visually hidden from non-participants.
- Private Ravager sounds may still be heard by nearby non-participants because
  packet-level sound filtering was intentionally removed with ProtocolLib.

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
