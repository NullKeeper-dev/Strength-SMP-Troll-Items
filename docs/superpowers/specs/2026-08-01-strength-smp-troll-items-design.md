# Strength SMP Troll Items — Design Specification

**Date:** 2026-08-01
**Status:** Approved
**Initial planned release:** 0.1.0

## 1. Purpose

Strength SMP Troll Items is a Java server plugin that recreates three troll
items inspired by LeekLeekMC's video, [Strength SMP VS Admin
Items](https://youtu.be/OSGbr1bcNb8), and credits the creator's
[YouTube channel](https://www.youtube.com/@leekleekmc).

The plugin provides administrator commands for issuing the items and a Bukkit
YAML configuration for changing item presentation, messages, and balance
values. The design favors native server behavior, persistent item/entity state,
and broad Bukkit-family compatibility.

## 2. Supported Environment

- Author: [NullKeeper-dev](https://github.com/NullKeeper-dev).
- Project page: [Modrinth — Strength SMP Troll
  Items](https://modrinth.com/project/strength-smp-troll-items).
- License: GPL-3.0-only.
- Minecraft: 26.1, 26.1.1, 26.1.2, and 26.2.
- Servers: Paper, Purpur, Spigot, and CraftBukkit.
- Java: 25.
- Build system: Gradle Wrapper.
- Required runtime dependency: ProtocolLib.
- Plugin descriptor: `plugin.yml`, with its version expanded from the Gradle
  project version during resource processing.
- Distribution: one common jar when the supported APIs prove binary-compatible;
  separate 26.1 and 26.2 jars only if verification finds an API break.

The implementation must use Bukkit-compatible APIs for gameplay behavior.
Paper-only APIs or server internals are out of scope. ProtocolLib is used only
where packet isolation cannot be guaranteed through Bukkit events alone.

## 3. Scope

### Included

- Three command-issued custom items.
- Persistent identification through namespaced Persistent Data Container keys.
- YAML configuration and atomic runtime reload.
- Persistent scale values and private Ravager assignments.
- Packet and event isolation for private Ravagers.
- MockBukkit tests for isolated logic and a documented multiplayer test matrix.
- README, changelog, license, Gradle build, and release-ready jar output.

### Excluded

- Crafting recipes.
- A GUI or menu.
- A database.
- A limit on accumulated Ravagers; every qualifying crossbow hit adds a new
  group.
- Automatic replacement of issued items when names or lore are reconfigured.
- Cross-world teleportation of Ravagers.

## 4. Commands and Permissions

### Give command

```text
/trollitems give <player> <resizing_sword|spooky_crossbow|hungry_berry> [amount]
```

- Permission: `trollitems.give`, granted to operators by default.
- `amount` defaults to `1` and must be an integer from `1` through `64`.
- Stackable berries are combined where inventory rules allow.
- Unstackable swords and crossbows are delivered as separate items.
- Inventory overflow is dropped safely at the target player's location, and
  the command sender is told how many items overflowed.
- Player, item identifier, and amount are validated before inventory changes.

### Reload command

```text
/trollitems reload
```

- Permission: `trollitems.reload`, granted to operators by default.
- A successful reload swaps in one new immutable configuration snapshot.
- A failed reload keeps the previous snapshot active and reports a concise
  error to the sender while logging detailed context to the server console.

Running `/trollitems` without a valid subcommand displays only the commands the
sender has permission to use. Commands work from players and the console.

## 5. Custom Item Identity

Each item carries a stable plugin namespaced key whose value is one of the
three item identifiers. Gameplay checks use this key, never display names or
lore, so renaming an item does not remove its behavior and similarly named
vanilla items do not gain plugin behavior.

Configuration reloads affect gameplay values immediately. Item appearance
changes apply to newly issued items only; existing items keep their current
name and lore until reissued.

### Default presentation

| Identifier | Material | Name | Durability | Lore |
| --- | --- | --- | --- | --- |
| `resizing_sword` | Wooden Sword | Bold yellow `Resizing Sword` | Unbreakable | Gray `Attack to grow target` and `Sneak + Attack to shrink target` |
| `spooky_crossbow` | Crossbow | Bold purple `Spooky Crossbow` | Vanilla default | Gray `Shoot a player to summon their fears.` |
| `hungry_berry` | Glow Berries | Bold orange `Hungry Berry` | Not applicable | Gray `Do not eat.` and `Hit a player to make their held item edible.` |

## 6. Resizing Sword

### Trigger

An uncancelled melee damage event whose direct attacker is a player holding a
marked Resizing Sword in the main hand and whose target is any living entity.
The effect is applied once per successful damage event after protection plugins
have had an opportunity to cancel it. A hit that deals no final damage has no
effect.

### Effect

- A standing attacker increases the target's scale by `0.05` by default.
- A sneaking attacker decreases the target's scale by `0.05` by default.
- The result is clamped to Minecraft's vanilla minimum and maximum scale
  attribute bounds.
- The new value is written to the target's scale attribute and persistent
  plugin metadata.
- The attacker receives the configured yellow message:
  `<target>'s size is now <size>`.
- Size is formatted without unnecessary trailing zeroes while retaining enough
  precision for the configured step.

For players, the stored scale is reapplied after login and respawn. For other
living entities, it remains with the saved entity through chunk unloads and
server restarts. A dead non-player entity is not recreated.

If a server implementation exposes no scale attribute for a particular living
entity, that hit changes nothing and sends the configured unsupported-target
message rather than throwing an error.

## 7. Spooky Crossbow

### Trigger and spawning

When the marked crossbow fires, its projectile receives persistent origin data
containing the shooter's UUID. Each player who takes uncancelled damage from
that tagged projectile triggers one spawn group. A projectile can trigger no
more than one group for the same victim.

Each group contains five Ravagers by default. Repeated hits always create five
additional Ravagers; existing groups are not replaced. Spawn attempts select
valid nearby positions within the configured radius. A cancelled or invalid
individual spawn is logged, while the remaining Ravagers are still attempted.
The shooter is told if fewer than the configured count could be spawned.

### Ravager state and AI

Every spawned Ravager stores:

- A plugin marker.
- Its shooter UUID.
- Its assigned target UUID.

The Ravager receives permanent Speed II by default. The plugin does not modify
the vanilla follow-range attribute. A protected retargeting task restores the
assigned player as the AI target only while that player is online, alive, in
the same world, and within the Ravager's current vanilla detection range.
Otherwise, the Ravager remains alive and idle.

The assignment survives target death, respawn, logout, chunk unload, and full
server restart. If the target changes worlds, the Ravager stays in its current
world and resumes pursuit only when the normal targeting conditions become true
again. Ravagers are never automatically despawned or respawned and remain until
killed or removed with an explicit server administration action.

The assigned player is the only player the Ravager may target or damage.
Ravagers retain ordinary pathfinding, block interaction, environmental damage,
and incidental interaction with non-player entities. The shooter and other
players may not be damaged by that Ravager.

### Participant privacy

The participants for one Ravager are its shooter and assigned target. Only
those participants may:

- Receive its spawn, movement, metadata, animation, status, or sound packets.
- Damage it or interact with it.
- Observe its death.

ProtocolLib filters entity-bound outbound packets for every non-participant and
cancels incoming interaction packets that reference the private entity.
Bukkit damage and interaction listeners repeat the authorization check as
defense in depth. Private Ravagers have player collision disabled so invisible
entities cannot push other players. This also disables push collision for the
two participants but does not prevent participants from attacking the Ravager
or the Ravager from attacking its assigned target.

Visibility is recalculated on player join, player world change, Ravager spawn,
and Ravager chunk load. Different Ravager groups may have different shooters,
even when they share the same target.

## 8. Hungry Berry

### Trigger

An uncancelled melee damage event whose attacker is a player holding a marked
Hungry Berry in the main hand and whose target is a player.

### Conversion

- Only the target's main-hand stack at the instant of the hit is converted.
- An empty main hand causes no conversion and sends the configured message to
  the attacker.
- The implementation creates a copy of the stack, preserves its material,
  amount, durability, name, lore, enchantments, custom model data, and other
  metadata, adds a persistent edible marker, then replaces the hand slot with
  the copy.
- The complete stack is converted, and split or transferred items retain the
  edible marker.
- The attacking Hungry Berry is not consumed.

### Eating

Right-clicking with a converted item cancels its normal right-click action and
immediately consumes exactly one item. It has no eating delay, is allowed at
full hunger, restores zero hunger and zero saturation by default, and applies
no food effects. This explicit interaction handling guarantees that normally
usable items such as bows, shields, or placeable blocks are consumed instead
of performing their original right-click behavior.

The conversion is permanent. Moving a converted stack to another slot or the
offhand does not remove its edible behavior; the main-hand restriction applies
only when choosing which stack the Hungry Berry converts.

## 9. Configuration

`config.yml` exposes every user-facing message and presentation field plus all
balance values, including:

- Custom item names and lore.
- Resizing step and size message formatting.
- Ravagers per hit, speed level, spawn radius, and retarget interval.
- Converted-item nutrition, saturation, and consume delay, defaulting to zero.
- Command, success, failure, empty-hand, unsupported-target, and partial-spawn
  messages.

Numeric values have explicit accepted ranges. Item identifiers and permission
nodes remain stable and are not configurable. Unknown keys are ignored with a
warning so future versions can remain backward compatible; missing keys use
documented defaults. Invalid types or out-of-range values reject the entire new
snapshot rather than partially applying it.

## 10. Persistence and Lifecycle

- Issued and converted item state lives on the item stack.
- Desired scale lives on the living entity, with player join/respawn recovery.
- Ravager ownership and targeting live on each real Ravager entity.
- No separate database or mutable registry file is required.
- Runtime indexes contain only loaded Ravagers and are rebuilt from entity
  metadata on startup and chunk load.
- Ravager death removes it from runtime indexes; no replacement is spawned.
- Plugin disable unregisters packet listeners and cancels scheduled tasks
  without deleting persistent gameplay state.

This model lets Minecraft's normal player, entity, chunk, and world saves carry
the durable state while keeping YAML limited to configuration.

## 11. Error Isolation and Compatibility

- Command executors catch unexpected exceptions at their boundary, return a
  generic user-safe failure, and log the full stack trace with command context.
- Scheduled retargeting catches errors per Ravager so one bad entity cannot
  terminate the task.
- Event listeners validate entity, item, UUID, world, and configuration data
  before use.
- Optional values are never assumed present, and malformed persistent metadata
  is ignored with a contextual warning.
- ProtocolLib is a hard dependency. If it is unavailable or incompatible, the
  server's dependency loader prevents this plugin from enabling; the plugin
  must not fall back to leaking private entities.
- Gameplay listeners respect cancelled damage events, preserving protection
  plugin decisions.

ProtocolLib packet listener ordering is a compatibility risk when another
plugin also rewrites entity or sound packets. WorldGuard or similar protection
plugins may cancel the triggering damage or Ravager spawn event. These are
intentional integration points and must be documented in the README.

## 12. Testing and Acceptance

### Automated tests

MockBukkit tests cover Bukkit-bound commands, listeners, inventory behavior,
and persistent metadata. Plain unit tests cover pure configuration,
calculation, and authorization services. Together they cover:

- Item creation and stable identity.
- Command parsing, amount boundaries, permissions, and inventory overflow.
- Configuration defaults, validation, and atomic reload failure.
- Grow/shrink calculation and vanilla-bound clamping.
- Successful-hit and cancelled-hit gating.
- Projectile tagging and one-group-per-projectile-victim behavior.
- Ravager participant authorization, metadata recovery, and target eligibility.
- Hungry Berry main-hand selection, immutable stack conversion, and one-item
  instant consumption.

Protocol packet contents, native AI, scale rendering, world interaction, and
cross-server behavior require manual verification because MockBukkit cannot
faithfully simulate them.

### Manual verification matrix

Run the final jar with a compatible ProtocolLib build on Minecraft 26.1,
26.1.1, 26.1.2, and 26.2. Verify at least one Paper and one Purpur server for
the full feature suite, then repeat compatibility smoke tests on Spigot and
CraftBukkit for each distinct binary target.

Use three players to verify:

1. Permission denial, valid giving, invalid input, and inventory overflow.
2. Resizing growth, sneaking shrink, vanilla limits, mob persistence, player
   death, relog, and restart recovery.
3. Five additional Ravagers per crossbow hit, different shooters, private
   visuals/audio/interactions, target-only player damage, normal world and mob
   interaction, offline idle behavior, respawn/relog/restart recovery, and
   permanent removal after death.
4. Conversion of common, enchanted, damaged, placeable, and normally usable
   item stacks; main-hand-only selection; instant consumption; zero hunger;
   metadata persistence; and offhand behavior after conversion.
5. Successful reload and rejected invalid YAML without losing the last valid
   runtime configuration.

## 13. Documentation and Release Requirements

The README follows the repository's required format and includes:

- Feature highlights and screenshot placeholders until hosted images exist.
- Modrinth as the plugin download source.
- ProtocolLib installation and its official download link.
- Java 25 and supported server versions.
- Commands, permissions, configuration examples, and the unbounded Ravager
  performance warning.
- Manual verification limitations.
- Clear inspiration credit linking the original video and LeekLeekMC channel.

The initial release uses semantic version `0.1.0`, maintains one matching
pending changelog section, compiles every necessary binary target, and produces
only normal distributable jars without source jars.
