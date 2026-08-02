<div align="center">
  <img src="INSERT IMAGE LINK HERE" alt="Strength SMP Troll Items icon" width="128">
  <h1>Strength SMP Troll Items</h1>
  <p>Three configurable admin-style troll items for Bukkit-family servers.</p>
  <p>
    <img src="https://img.shields.io/badge/platform-Bukkit%20%7C%20Spigot%20%7C%20Paper%20%7C%20Purpur-222222" alt="Bukkit, Spigot, Paper, and Purpur">
    <img src="https://img.shields.io/badge/Minecraft-26.1--26.2-62B47A" alt="Minecraft 26.1 through 26.2">
    <img src="https://img.shields.io/badge/license-GPL--3.0--only-blue" alt="GPL-3.0-only">
  </p>
</div>

| Resizing Sword | Spooky Crossbow | Hungry Berry |
| --- | --- | --- |
| Grow any living entity by `0.05`, or sneak-hit to shrink it. | Every successful player hit summons five additional private Ravagers. | Permanently converts the target's held stack into instant right-click food. |

## See it in action

### Resize anything alive

Scale changes persist through relogs, respawns, chunk reloads, and restarts.

![Resizing Sword demonstration](INSERT IMAGE LINK HERE)

### Summon private Ravagers

Only the shooter and target can see or interact with each summoned Ravager.

![Spooky Crossbow demonstration](INSERT IMAGE LINK HERE)

### Eat any item

Converted stacks keep their material and metadata, but right-clicking instantly consumes one.

![Hungry Berry demonstration](INSERT IMAGE LINK HERE)

## Inspiration

Inspired by [Strength SMP VS Admin Items](https://youtu.be/OSGbr1bcNb8?si=KAaJoD2ywrMnSUmY), originally created by [LeekLeekMC](https://www.youtube.com/@leekleekmc).

## Install

1. Install a compatible [Paper](https://papermc.io/software/paper/downloads), [Purpur](https://purpurmc.org/downloads), or [Spigot/CraftBukkit](https://www.spigotmc.org/wiki/buildtools/) server.
2. Download [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/) using its current [development build](https://github.com/dmulloy2/ProtocolLib/releases), then place it in `plugins/`.
3. Download this plugin from **INSERT MODRINTH LINK HERE** and place its jar in `plugins/`.
4. Start the server, then adjust `plugins/StrengthSmpTrollItems/config.yml` if desired.

> **Supported versions:** The common jar targets CraftBukkit, Spigot, Paper, and Purpur on Minecraft 26.1, 26.1.1, 26.1.2, and 26.2 and requires Java 25. ProtocolLib must explicitly support the exact server build; all packet paths still require live verification before release.

## Use

1. Give an operator or staff role the appropriate permission.
2. Run `/trollitems give <player> <resizing_sword|spooky_crossbow|hungry_berry> [amount]`.
3. Use `/trollitems reload` after changing `config.yml`.

| Command | Permission | Default |
| --- | --- | --- |
| `/trollitems give ...` | `trollitems.give` | Operators |
| `/trollitems reload` | `trollitems.reload` | Operators |

Key balance settings are `resize.step`, `ravagers.per-hit`, `ravagers.speed-level`, `ravagers.spawn-radius`, `ravagers.retarget-interval-ticks`, `edible.nutrition`, `edible.saturation`, and `edible.consume-delay-ticks`. Item names, lore, and every player-facing message are configurable too.

> **Tip:** Ravagers remain until killed. Every successful tagged projectile hit adds five more by default, so repeated use can create a large permanent mob population.

### Compatibility

`ProtocolRavagerIsolation` registers high-priority ProtocolLib sound and interaction filters, so packet-rewriting plugins can have listener-order conflicts. `RavagerProtectionListener`, the item damage listeners, and custom Ravager spawning also overlap with WorldGuard-style protection plugins; cancelled damage or spawn events are intentionally respected.

<details>
<summary><strong>Build from source</strong></summary>

Install JDK 25, then run:

```powershell
.\gradlew.bat clean build
```

The build writes five jars to `build/libs/`:

- `strength-smp-troll-items-<version>-paper-26.1.1.jar`
- `strength-smp-troll-items-<version>-paper-26.1.2.jar`
- `strength-smp-troll-items-<version>-paper-26.2.jar`
- `strength-smp-troll-items-<version>-purpur-26.1.2.jar`
- `strength-smp-troll-items-<version>-purpur-26.2.jar`

No universal or sources jar is produced.

</details>

## Manual verification before release

This environment verifies isolated behavior with MockBukkit and compiles against the common Spigot API, but cannot replace a three-player live server test. Test on 26.1, 26.1.1, 26.1.2, and 26.2 across Paper, Purpur, Spigot, and CraftBukkit with a ProtocolLib build that claims support for each version:

1. Confirm grow/shrink rendering, `0.05` increments, vanilla bounds, every living entity type, relog, death, chunk unload, and restart persistence.
2. With shooter, target, and outsider clients, verify private Ravager spawn/movement/death/sound packets, no outsider collision/damage/interaction, participant attacks, target-only Ravager damage, five more Ravagers per later hit, normal block/world interaction, logout idling, and restart recovery.
3. Convert named, enchanted, damaged, stackable, usable, placeable, main-hand, and later-offhand items; confirm right-click cancels normal use, instantly consumes exactly one at full hunger, restores zero hunger by default, and remains edible after transfer/relog.
4. Verify `/trollitems reload`, permission denial, invalid input, full-inventory overflow, and partial Ravager spawn messaging.

---

GPL-3.0-only · 2026 · [NullKeeper-dev](https://github.com/NullKeeper-dev)
