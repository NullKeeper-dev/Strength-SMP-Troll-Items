<div align="center">
  <img src="INSERT IMAGE LINK HERE" alt="Strength SMP Troll Items icon" width="128">
  <h1>Strength SMP Troll Items</h1>
  <p>Three configurable admin-style troll items for Paper and Purpur.</p>
  <p>
    <img src="https://img.shields.io/badge/platform-Paper%20%7C%20Purpur-222222" alt="Paper and Purpur">
    <img src="https://img.shields.io/badge/Minecraft-26.1.1%20%7C%2026.1.2%20%7C%2026.2-62B47A" alt="Minecraft 26.1.1, 26.1.2, and 26.2">
    <img src="https://img.shields.io/badge/license-GPL--3.0--only-blue" alt="GPL-3.0-only">
  </p>
</div>

| Resizing Sword | Spooky Crossbow | Hungry Berry |
| --- | --- | --- |
| Grow any living entity by `0.05`, or sneak-hit to shrink it. | Every eligible player hit summons five additional persistent Ravagers. | Permanently converts the target's complete held stack into instant right-click food. |

## See it in action

### Resize anything alive

Scale changes persist through relogs, respawns, chunk reloads, and restarts.

![Resizing Sword demonstration](INSERT IMAGE LINK HERE)

### Summon private Ravagers

Only the shooter and target can see, interact with, or damage each Ravager. The
Ravager can damage only its assigned target and otherwise behaves normally in
the world until killed.

![Spooky Crossbow demonstration](INSERT IMAGE LINK HERE)

### Eat any item

Converted stacks keep their material and metadata. Right-click instantly
consumes one item with zero nutrition or saturation by default.

![Hungry Berry demonstration](INSERT IMAGE LINK HERE)

## Inspiration

Inspired by [Strength SMP VS Admin Items](https://youtu.be/OSGbr1bcNb8?si=KAaJoD2ywrMnSUmY), originally created by [LeekLeekMC](https://www.youtube.com/@leekleekmc).

## Install

1. Install Java 25 and a matching [Paper](https://papermc.io/downloads/paper) or [Purpur](https://purpurmc.org/download/purpur) server.
2. Download the plugin from **INSERT MODRINTH LINK HERE** and choose the jar matching both the server platform and exact Minecraft version.
3. Place that jar in the server's `plugins/` folder and start the server.
4. Adjust `plugins/StrengthSmpTrollItems/config.yml` if desired, then run `/trollitems reload`.

| Platform | Minecraft | Jar suffix |
| --- | --- | --- |
| Paper | 26.1.1 | `paper-26.1.1` |
| Paper | 26.1.2 | `paper-26.1.2` |
| Paper | 26.2 | `paper-26.2` |
| Purpur | 26.1.2 | `purpur-26.1.2` |
| Purpur | 26.2 | `purpur-26.2` |

Paper 26.1, Purpur 26.1, and Purpur 26.1.1 are unavailable upstream and are
not release targets. Spigot and CraftBukkit are not supported. There is no
universal jar and there are no runtime dependencies.

## Use

1. Give an operator or staff role the appropriate permission.
2. Run `/trollitems give <player> <resizing_sword|spooky_crossbow|hungry_berry> [amount]`.
3. Use the issued item from the main hand.

| Command | Permission | Default |
| --- | --- | --- |
| `/trollitems give ...` | `trollitems.give` | Operators |
| `/trollitems reload` | `trollitems.reload` | Operators |

Key balance settings are `resize.step`, `ravagers.per-hit`,
`ravagers.speed-level`, `ravagers.spawn-radius`,
`ravagers.retarget-interval-ticks`, `edible.nutrition`,
`edible.saturation`, and `edible.consume-delay-ticks`. Item names, lore, and
player-facing messages are configurable too.

Troll effects activate on a valid damage tick even when armor, enchantments,
or Resistance reduce final health damage to zero. Cancelled events, zero raw
damage, Creative or Spectator targets, invulnerable targets, and repeat swings
during the damage-immunity window do not activate them.

> **Tip:** Ravagers remain until killed. Every eligible tagged-projectile hit
> adds five more by default, so repeated use can create a large permanent mob
> population.

### Compatibility

Nearby outsiders may hear private Ravagers even though they cannot see or
interact with them. Plugins that also call Paper's `showEntity` or `hideEntity`
methods may override visibility depending on listener or call order.
WorldGuard-style protection plugins remain authoritative: if they cancel a
damage or spawn event, the troll effect or spawn is not forced through.

<details>
<summary><strong>Build from source</strong></summary>

Install JDK 25, then run:

```powershell
.\gradlew.bat clean build verifyDistributables
```

The build writes these five jars to `build/libs/`:

- `strength-smp-troll-items-<version>-paper-26.1.1.jar`
- `strength-smp-troll-items-<version>-paper-26.1.2.jar`
- `strength-smp-troll-items-<version>-paper-26.2.jar`
- `strength-smp-troll-items-<version>-purpur-26.1.2.jar`
- `strength-smp-troll-items-<version>-purpur-26.2.jar`

No universal or sources jar is produced.

</details>

## Manual verification before release

Automated tests cover isolated logic, but private entity tracking and native
world behavior need three real clients. Fully test Paper 26.1.1 and Purpur
26.2, then repeat startup and feature smoke checks on the other three targets:

1. Confirm only shooter and target render each Ravager; outsiders cannot collide, attack, or interact; sound leakage is acceptable.
2. Confirm five more Ravagers per eligible hit, restart persistence, normal world behavior, target-only player damage, and permanent removal when killed.
3. Confirm sword and berry activate against armored, Resistance-protected Survival players even when final damage is zero.
4. Confirm Creative, Spectator, invulnerable, protection-cancelled, zero-raw-damage, and immunity-window hits do not activate.
5. Confirm scale persistence and complete-stack edible conversion with instant right-click consumption.

---

GPL-3.0-only · 2026 · [NullKeeper-dev](https://github.com/NullKeeper-dev)
