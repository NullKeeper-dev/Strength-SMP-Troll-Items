# Paper/Purpur, Native Ravagers, and Damage-Tick Revision

**Date:** 2026-08-02
**Status:** Approved
**Amends:** `2026-08-01-strength-smp-troll-items-design.md`

## Decision

Strength SMP Troll Items will support five official targets: Paper 26.1.1,
Paper 26.1.2, Paper 26.2, Purpur 26.1.2, and Purpur 26.2. It will produce one
separately compiled and labelled jar for each target. Paper 26.1, Purpur 26.1,
and Purpur 26.1.1 are skipped because those projects publish no server builds
for those combinations. Spigot and CraftBukkit are not supported or tested,
although inherited `org.bukkit` types and `plugin.yml` remain necessary parts
of the Paper plugin API.

ProtocolLib will be removed completely. Private Ravagers will be hidden by
default before they become trackable, then shown through Paper's native entity
visibility API only to their shooter and assigned target. Server-side listeners
will continue blocking unauthorized damage and interaction, and player
collision will remain disabled. Sound leakage to nearby non-participants is an
accepted limitation.

All three troll-item hit effects use the same damage-tick eligibility rules:

- The relevant damage event must not be cancelled.
- The attack must create a new valid damage tick; repeated attacks during the
  target's damage-immunity window do not qualify.
- Creative and Spectator players and invulnerable entities do not qualify.
- Final health damage is irrelevant. Armor, enchantments, or Resistance may
  reduce it to zero and the troll effect still activates.
- Existing attacker, held-item, target-type, and projectile-origin rules remain
  unchanged.

## Rejected Alternatives

A health-delta check was rejected because it would incorrectly suppress hits
whose final damage is reduced to zero. Direct CraftBukkit/NMS damage hooks were
rejected because they would be fragile across the supported targets. Keeping
ProtocolLib as either a hard or optional
dependency was rejected because native visual privacy is sufficient and sound
leakage is acceptable.

## Verification

Automated tests will cover the shared damage-tick policy, cancelled events,
Creative/Spectator and invulnerable targets, immunity-window repeats, and zero
final damage. Each of the five jars must compile against its target's API. A
live five-target Paper/Purpur matrix must verify damage timing and three-player
Ravager visibility because mocks cannot prove native tracking or world behavior.

Removing Spigot and CraftBukkit support is a breaking compatibility change. The
approved project version is `1.0.0`.
