# Strength SMP Troll Items Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a configurable Java 25 Bukkit-family plugin containing the Resizing Sword, Spooky Crossbow, and Hungry Berry specified in the approved design.

**Architecture:** A small `JavaPlugin` composition root wires immutable YAML configuration, Persistent Data Container-backed item/entity services, focused Bukkit listeners, and a ProtocolLib isolation adapter. Pure calculations and authorization rules remain separate from Bukkit-facing code so MockBukkit and JUnit can verify them without a live server.

**Tech Stack:** Java 25, Gradle 9.5.1 Kotlin DSL, Paper API 26.1.2 build 74, ProtocolLib 5.4.0 compile API with 5.5 development runtime, MockBukkit 4.114.0, and JUnit 6.1.0.

## Global Constraints

- Support Minecraft 26.1, 26.1.1, 26.1.2, and 26.2 with one common jar unless verification proves a binary split necessary.
- Support Paper, Purpur, Spigot, and CraftBukkit without Paper-only APIs or direct server internals.
- Require Java 25 and ProtocolLib; fail closed if ProtocolLib is unavailable or incompatible.
- Use `plugin.yml`; expand `${version}` from `gradle.properties` during `processResources`.
- Use namespaced Persistent Data Container keys for durable item and entity identity.
- Use Bukkit YAML for immutable, atomically replaceable configuration snapshots.
- Respect cancelled damage and spawn events, validate all command/config inputs, and isolate command/scheduler failures.
- Commit directly to `main`; use the repository's short imperative commit style.
- Keep the planned initial release at `0.1.0`, with one `CHANGELOG.md` entry headed `## [0.1.0]`.
- Generate only the normal distributable jar; never configure a sources jar.
- Keep the user's untracked `Prompt.txt` intact.
- ProtocolLib currently documents 26.1.x development support but not confirmed 26.2 support; compile 26.2-compatible code and leave live 26.2 privacy acceptance pending a compatible runtime build.

## File Map

### Build and resources

- `settings.gradle.kts`: root project name and plugin repositories.
- `build.gradle.kts`: Java toolchain, dependencies, test platform, resource expansion, and jar metadata.
- `gradle.properties`: the sole project version and Gradle defaults.
- `gradle/wrapper/*`, `gradlew`, `gradlew.bat`: reproducible Gradle 9.5.1 wrapper.
- `.gitignore`: preserve `AGENTS.md` and ignore Gradle, build, IDE, and server-run outputs.
- `src/main/resources/plugin.yml`: entrypoint, hard dependency, commands, and permissions.
- `src/main/resources/config.yml`: complete documented defaults.

### Production code

- `dev/nullkeeper/strengthsmptrollitems/StrengthSmpTrollItemsPlugin.java`: lifecycle and dependency wiring only.
- `dev/nullkeeper/strengthsmptrollitems/RuntimeComponents.java`: start/close composition seam used by the plugin and integration tests.
- `config/PluginConfig.java`: immutable config records.
- `config/ConfigLoader.java`: validated YAML-to-record conversion.
- `config/ConfigService.java`: atomic current snapshot and safe reload result.
- `items/TrollItemType.java`: stable item IDs and base materials needed by configuration and item services.
- `items/PersistentKeys.java`: all namespaced keys.
- `items/TrollItemService.java`: item creation, identity, edible marking, and projectile metadata.
- `command/GiveItemService.java`: immutable give-result calculation and inventory delivery.
- `command/TrollItemsCommand.java`: `/trollitems give` and `/trollitems reload` boundary.
- `resize/ScaleMath.java`: pure resize and clamp calculation.
- `resize/ScaleService.java`: attribute/PDC application and restoration.
- `resize/ResizingSwordListener.java`: successful melee trigger.
- `resize/ScalePersistenceListener.java`: join, respawn, and chunk-load restoration.
- `hungry/EdibleItemService.java`: immutable stack conversion and one-item consumption.
- `hungry/HungryBerryListener.java`: main-hand conversion trigger.
- `hungry/EdibleInteractionListener.java`: instant right-click consumption.
- `ravager/RavagerAssignment.java`: shooter/target value object.
- `ravager/RavagerAccessPolicy.java`: participant and damage authorization.
- `ravager/RavagerMetadataStore.java`: PDC serialization.
- `ravager/PrivateRavagerRegistry.java`: copy-on-write index of loaded private Ravagers.
- `ravager/ProjectileHitTracker.java`: one trigger per projectile/victim.
- `ravager/RavagerSpawner.java`: safe positions and real Ravager construction.
- `ravager/SpookyCrossbowListener.java`: projectile tagging and spawn trigger.
- `ravager/RavagerTargetController.java`: protected periodic target restoration.
- `ravager/RavagerVisibilityService.java`: Bukkit show/hide reconciliation.
- `ravager/ProtocolRavagerIsolation.java`: ProtocolLib sound and interaction filtering.
- `ravager/RavagerProtectionListener.java`: target-only player damage and outsider interaction cancellation.
- `ravager/RavagerLifecycleListener.java`: startup/chunk/join/world/death registry and visibility recovery.

### Tests and documentation

- Matching tests under `src/test/java/dev/nullkeeper/strengthsmptrollitems/**` for every pure service and Bukkit-facing behavior MockBukkit supports.
- `README.md`: required project presentation, install/use/build instructions, warnings, and inspiration credits.
- `CHANGELOG.md`: one planned `0.1.0` entry.
- `LICENSE`: GPL-3.0-only text.

---

### Task 1: Reproducible Build and Configuration Foundation

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `gradlew`
- Create: `gradlew.bat`
- Modify: `.gitignore`
- Create: `src/main/resources/plugin.yml`
- Create: `src/main/resources/config.yml`
- Create: `src/main/java/dev/nullkeeper/strengthsmptrollitems/config/PluginConfig.java`
- Create: `src/main/java/dev/nullkeeper/strengthsmptrollitems/config/ConfigLoader.java`
- Create: `src/main/java/dev/nullkeeper/strengthsmptrollitems/config/ConfigService.java`
- Create: `src/main/java/dev/nullkeeper/strengthsmptrollitems/items/TrollItemType.java`
- Test: `src/test/java/dev/nullkeeper/strengthsmptrollitems/config/ConfigLoaderTest.java`

**Interfaces:**
- Produces: `PluginConfig`, `ConfigLoader.load(ConfigurationSection)`, `ConfigService.current()`, `ConfigService.reload(ConfigurationSection)`, and `ConfigService.reloadFromDisk()`.
- Both reload methods return `ReloadResult(boolean successful, String message)` and never discard the last valid snapshot on failure. The command calls `reloadFromDisk()`; tests pass explicit sections to `reload(ConfigurationSection)`.

- [x] **Step 1: Create the Gradle skeleton and wrapper**

Use `version=0.1.0`, Java toolchain/release 25, Paper API `26.1.2.build.74-stable`, ProtocolLib `5.4.0`, MockBukkit `4.114.0`, and JUnit BOM `6.1.0`. Configure `processResources` with:

```kotlin
filesMatching("plugin.yml") {
    expand("version" to project.version)
}
```

Generate the wrapper with `gradle wrapper --gradle-version 9.5.1`, and do not configure `withSourcesJar()`.

- [x] **Step 2: Write failing configuration tests**

Cover defaults, `resize-step: 0.05`, Ravager count/speed/radius/retarget interval, edible zero nutrition/saturation/delay, message loading, missing-key defaults, and rejection of invalid numeric ranges. Include this atomicity assertion:

```java
PluginConfig before = service.current();
ReloadResult result = service.reload(invalidYaml);
assertFalse(result.successful());
assertSame(before, service.current());
```

- [x] **Step 3: Run the focused test and confirm red**

Run: `./gradlew test --tests "*.ConfigLoaderTest"`

Expected: compilation fails because the configuration classes do not exist.

- [x] **Step 4: Implement immutable validated configuration**

Define nested records in `PluginConfig`:

```java
public record PluginConfig(
        Map<TrollItemType, ItemPresentation> items,
        ResizeSettings resize,
        RavagerSettings ravagers,
        EdibleSettings edible,
        Messages messages) {}
```

Return `Map.copyOf` and `List.copyOf` at construction boundaries. Accept Ravager count `1..64`, speed level `1..255`, positive finite spawn radius, retarget ticks `1..1200`, nonnegative finite resize step, nutrition `0..20`, nonnegative finite saturation, and consume ticks `0..72000`. Reject the entire snapshot on invalid types/ranges.

- [x] **Step 5: Run tests and inspect resource expansion**

Run: `./gradlew clean test processResources`

Expected: configuration tests pass and `build/resources/main/plugin.yml` contains `version: '0.1.0'` plus `depend: [ProtocolLib]`.

- [x] **Step 6: Commit the foundation**

```text
git add .gitignore settings.gradle.kts build.gradle.kts gradle.properties gradle gradlew gradlew.bat src
git commit -m "Add plugin build and configuration foundation"
```

### Task 2: Stable Item Identity and Admin Commands

**Files:**
- Create: `src/main/java/dev/nullkeeper/strengthsmptrollitems/items/PersistentKeys.java`
- Create: `src/main/java/dev/nullkeeper/strengthsmptrollitems/items/TrollItemService.java`
- Create: `src/main/java/dev/nullkeeper/strengthsmptrollitems/command/GiveItemService.java`
- Create: `src/main/java/dev/nullkeeper/strengthsmptrollitems/command/TrollItemsCommand.java`
- Test: `src/test/java/dev/nullkeeper/strengthsmptrollitems/items/TrollItemServiceTest.java`
- Test: `src/test/java/dev/nullkeeper/strengthsmptrollitems/command/TrollItemsCommandTest.java`

**Interfaces:**
- Consumes: `PluginConfig`, `ConfigService`, and `TrollItemType` from Task 1.
- Produces: `TrollItemService.create(TrollItemType, PluginConfig)`, `isType(ItemStack, TrollItemType)`, `markEdible(ItemStack)`, `isEdible(ItemStack)`, `tagProjectile(Projectile, UUID)`, and `projectileShooter(Projectile)`.
- Produces: `GiveItemService.give(Player, TrollItemType, int, PluginConfig)` returning `GiveResult(int delivered, int dropped)`.

- [x] **Step 1: Write failing MockBukkit item tests**

Verify material, bold configured name/color, gray lore, unbreakable Wooden Sword, ordinary Crossbow durability, Glow Berry stacking, stable PDC identity after rename, false identity for a lookalike vanilla item, and preservation of identity through clone/serialization.

- [x] **Step 2: Run the item tests and confirm red**

Run: `./gradlew test --tests "*.TrollItemServiceTest"`

Expected: compilation fails because item types/services do not exist.

- [x] **Step 3: Implement item identity and factory methods**

Use stable IDs `resizing_sword`, `spooky_crossbow`, and `hungry_berry`. Parse configured legacy ampersand colors, including hex orange, into Bukkit-supported display strings. Store the item ID under `strengthsmptrollitems:item_type`.

```java
ItemStack created = new ItemStack(type.material());
ItemMeta meta = created.getItemMeta();
meta.getPersistentDataContainer().set(keys.itemType(), PersistentDataType.STRING, type.id());
created.setItemMeta(meta);
return created;
```

- [x] **Step 4: Write failing command tests**

Test permission denial, console use, default amount, invalid player/item/amount, amount bounds `1..64`, unstackable delivery, berry stacking, inventory overflow, reload permission, successful reload, and failed reload retaining the previous snapshot.

- [x] **Step 5: Run command tests and confirm red**

Run: `./gradlew test --tests "*.TrollItemsCommandTest"`

Expected: compilation fails because command classes do not exist.

- [x] **Step 6: Implement command and delivery boundaries**

Catch unexpected exceptions at the top of `onCommand`, send the configured safe error, and log the command/subcommand plus stack trace. Validate all inputs before creating items. Deliver overflow with `World.dropItemNaturally` and report the exact dropped count.

```java
try {
    return switch (args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT)) {
        case "give" -> executeGive(sender, args);
        case "reload" -> executeReload(sender, args);
        default -> sendUsage(sender);
    };
} catch (RuntimeException exception) {
    logger.log(Level.SEVERE, "Failed /trollitems command", exception);
    sender.sendMessage(configs.current().messages().internalError());
    return true;
}
```

- [x] **Step 7: Run focused and full tests**

Run: `./gradlew test --tests "*.TrollItemServiceTest" --tests "*.TrollItemsCommandTest"`

Expected: all item and command tests pass.

- [x] **Step 8: Commit items and commands**

```text
git add src/main src/test
git commit -m "Add troll item commands and identity"
```

### Task 3: Resizing Sword and Persistent Scale

**Files:**
- Create: `src/main/java/dev/nullkeeper/strengthsmptrollitems/resize/ScaleMath.java`
- Create: `src/main/java/dev/nullkeeper/strengthsmptrollitems/resize/ScaleService.java`
- Create: `src/main/java/dev/nullkeeper/strengthsmptrollitems/resize/ResizingSwordListener.java`
- Create: `src/main/java/dev/nullkeeper/strengthsmptrollitems/resize/ScalePersistenceListener.java`
- Test: `src/test/java/dev/nullkeeper/strengthsmptrollitems/resize/ScaleMathTest.java`
- Test: `src/test/java/dev/nullkeeper/strengthsmptrollitems/resize/ResizingSwordListenerTest.java`

**Interfaces:**
- Consumes: `TrollItemService.isType` and `PluginConfig.resize()`.
- Produces: `ScaleMath.change(double current, double step, boolean shrink, double minimum, double maximum)`.
- Produces: `ScaleService.apply(LivingEntity, boolean, double)` returning `ScaleResult(boolean applied, double value)` and `ScaleService.restore(LivingEntity)`.

- [x] **Step 1: Write failing pure scale tests**

Assert `1.0 -> 1.05`, `1.0 -> 0.95` while sneaking, custom step use, exact minimum/maximum clamping, finite-value validation, and stable decimal formatting without unnecessary trailing zeroes.

- [x] **Step 2: Run scale tests and confirm red**

Run: `./gradlew test --tests "*.ScaleMathTest"`

Expected: compilation fails because `ScaleMath` does not exist.

- [x] **Step 3: Implement scale calculation and PDC service**

Read `Attribute.SCALE`, calculate through `ScaleMath`, set the base value, and store the result as a PDC `DOUBLE`. Return an unsupported result when the attribute is absent. Reapply stored player values on join and one tick after respawn; scan living entities during chunk load.

```java
double delta = shrink ? -step : step;
double changed = Math.clamp(current + delta, minimum, maximum);
attribute.setBaseValue(changed);
entity.getPersistentDataContainer().set(keys.scale(), PersistentDataType.DOUBLE, changed);
```

- [x] **Step 4: Write failing listener tests**

Use MockBukkit entities to prove main-hand marked sword gating, all-living-entity support, normal grow, sneaking shrink, cancelled/no-final-damage no-op, unsupported-target messaging, and configured yellow attacker output.

- [x] **Step 5: Implement the event listeners**

Register at `EventPriority.MONITOR` with `ignoreCancelled = true`. Apply only to direct player melee damage with positive final damage. Never catch and swallow an exception; log target type and attacker UUID when recovery is possible.

```java
if (!(event.getDamager() instanceof Player attacker)
        || !(event.getEntity() instanceof LivingEntity target)
        || event.getFinalDamage() <= 0.0
        || !items.isType(attacker.getInventory().getItemInMainHand(), TrollItemType.RESIZING_SWORD)) {
    return;
}
scaleService.apply(target, attacker.isSneaking(), configs.current().resize().step());
```

- [x] **Step 6: Run tests and commit**

Run: `./gradlew test --tests "*.Scale*" --tests "*.ResizingSwordListenerTest"`

Expected: all resizing tests pass.

```text
git add src/main src/test
git commit -m "Add persistent resizing sword"
```

### Task 4: Hungry Berry Conversion and Instant Consumption

**Files:**
- Create: `src/main/java/dev/nullkeeper/strengthsmptrollitems/hungry/EdibleItemService.java`
- Create: `src/main/java/dev/nullkeeper/strengthsmptrollitems/hungry/HungryBerryListener.java`
- Create: `src/main/java/dev/nullkeeper/strengthsmptrollitems/hungry/EdibleInteractionListener.java`
- Test: `src/test/java/dev/nullkeeper/strengthsmptrollitems/hungry/EdibleItemServiceTest.java`
- Test: `src/test/java/dev/nullkeeper/strengthsmptrollitems/hungry/HungryBerryListenerTest.java`

**Interfaces:**
- Consumes: edible PDC methods from `TrollItemService` and `PluginConfig.edible()`.
- Produces: `EdibleItemService.convert(ItemStack)` returning a cloned marked stack and `consume(Player, EquipmentSlot)` returning whether one item was consumed.

- [ ] **Step 1: Write failing conversion tests**

Verify that conversion clones rather than mutates the input, marks the entire amount, and preserves material, damage, enchantments, name, lore, custom model data, and existing PDC. Verify one right-click decrements one, consumes the final item to air, works at full hunger, and changes neither hunger nor saturation at zero defaults.

- [ ] **Step 2: Run conversion tests and confirm red**

Run: `./gradlew test --tests "*.EdibleItemServiceTest"`

Expected: compilation fails because `EdibleItemService` does not exist.

- [ ] **Step 3: Implement immutable conversion and consumption**

Clone the target stack before adding `strengthsmptrollitems:edible`. On interaction, clone/decrement/replace the event hand stack, cancel the original right-click action, and apply configured nutrition/saturation without exceeding vanilla bounds. A converted item's original right-click action never runs.

```java
ItemStack converted = original.clone();
ItemMeta meta = converted.getItemMeta();
meta.getPersistentDataContainer().set(keys.edible(), PersistentDataType.BYTE, (byte) 1);
converted.setItemMeta(meta);
return converted;
```

```java
ItemStack remaining = held.clone();
remaining.setAmount(held.getAmount() - 1);
equipment.setItem(slot, remaining.getAmount() == 0 ? new ItemStack(Material.AIR) : remaining);
```

When `consumeTicks` is zero, consume in the event callback. For a configured positive delay, add the player/hand pair to a copy-on-write pending set and run one protected scheduler task; at completion, consume only if that hand still contains a marked edible item, then remove the pending key in `finally`. Repeated clicks for the same pending player/hand do not queue extra consumption.

- [ ] **Step 4: Write failing listener tests**

Cover marked-berry main-hand gating, player-only targets, target main hand only, empty main hand message, cancelled/no-final-damage no-op, attacker berry not consumed, and converted offhand consumption after later transfer.

- [ ] **Step 5: Implement listeners, test, and commit**

Run: `./gradlew test --tests "*.Edible*" --tests "*.HungryBerryListenerTest"`

Expected: all Hungry Berry tests pass.

```text
git add src/main src/test
git commit -m "Add hungry berry item conversion"
```

### Task 5: Persistent Ravager State, Spawning, and Targeting

**Files:**
- Create: `src/main/java/dev/nullkeeper/strengthsmptrollitems/ravager/RavagerAssignment.java`
- Create: `src/main/java/dev/nullkeeper/strengthsmptrollitems/ravager/RavagerAccessPolicy.java`
- Create: `src/main/java/dev/nullkeeper/strengthsmptrollitems/ravager/RavagerMetadataStore.java`
- Create: `src/main/java/dev/nullkeeper/strengthsmptrollitems/ravager/PrivateRavagerRegistry.java`
- Create: `src/main/java/dev/nullkeeper/strengthsmptrollitems/ravager/ProjectileHitTracker.java`
- Create: `src/main/java/dev/nullkeeper/strengthsmptrollitems/ravager/RavagerSpawner.java`
- Create: `src/main/java/dev/nullkeeper/strengthsmptrollitems/ravager/SpookyCrossbowListener.java`
- Create: `src/main/java/dev/nullkeeper/strengthsmptrollitems/ravager/RavagerTargetController.java`
- Test: `src/test/java/dev/nullkeeper/strengthsmptrollitems/ravager/RavagerAccessPolicyTest.java`
- Test: `src/test/java/dev/nullkeeper/strengthsmptrollitems/ravager/RavagerMetadataStoreTest.java`
- Test: `src/test/java/dev/nullkeeper/strengthsmptrollitems/ravager/SpookyCrossbowListenerTest.java`

**Interfaces:**
- Produces: `RavagerAssignment(UUID shooterId, UUID targetId)`.
- Produces: `RavagerAccessPolicy.canObserve/canInteract/canBeDamagedBy/canDamagePlayer`.
- Produces: `RavagerMetadataStore.write(Ravager, RavagerAssignment)` and `read(Ravager)`.
- Produces: `PrivateRavagerRegistry.register/unregister/find/snapshot` using copy-on-write immutable maps.
- Produces: `ProjectileHitTracker.markFirst(Projectile, UUID)` using projectile PDC so the ledger has the projectile's lifecycle and cannot leak a global UUID set.
- Produces: `RavagerSpawner.spawn(Player shooter, Player target, PluginConfig.RavagerSettings)` returning `SpawnResult(int requested, int spawned)`.

- [ ] **Step 1: Write failing policy and metadata tests**

Prove only shooter/target can observe, interact, or damage; only target may receive Ravager damage; malformed/missing UUID metadata returns empty; valid metadata survives entity serialization; registry snapshots cannot be mutated.

- [ ] **Step 2: Run tests and confirm red**

Run: `./gradlew test --tests "*.RavagerAccessPolicyTest" --tests "*.RavagerMetadataStoreTest"`

Expected: compilation fails because Ravager state classes do not exist.

- [ ] **Step 3: Implement state and safe spawning**

Choose nearby solid-ground positions inside the configured radius without loading new chunks. Spawn with `CreatureSpawnEvent.SpawnReason.CUSTOM`; set persistent, `removeWhenFarAway(false)`, `collidable(false)`, permanent Speed effect at configured level, shooter/target metadata, and registry membership. Continue after an individual cancelled spawn and return the partial count.

```java
Ravager ravager = world.spawn(location, Ravager.class, SpawnReason.CUSTOM, spawned -> {
    spawned.setPersistent(true);
    spawned.setRemoveWhenFarAway(false);
    spawned.setCollidable(false);
    spawned.addPotionEffect(new PotionEffect(
            PotionEffectType.SPEED,
            PotionEffect.INFINITE_DURATION,
            settings.speedLevel() - 1,
            false,
            false));
});
metadata.write(ravager, new RavagerAssignment(shooter.getUniqueId(), target.getUniqueId()));
registry.register(ravager);
```

- [ ] **Step 4: Write failing crossbow tests**

Cover marked-crossbow projectile tagging, unmarked no-op, player victim only, cancelled/no-final-damage no-op, five default spawns, five additional spawns on a second hit, one group per projectile/victim, different shooter assignments, and partial-spawn reporting.

- [ ] **Step 5: Implement projectile trigger and target controller**

Tag the projectile in `EntityShootBowEvent`. In positive uncancelled projectile damage, use `ProjectileHitTracker.markFirst(projectile, victimId)` before spawning; serialize the already-hit victim UUIDs in that projectile's PDC. On each protected task run, set the assigned player only when online, alive, same-world, and within the Ravager's current `FOLLOW_RANGE`; otherwise clear the live AI target while retaining metadata.

```java
if (eligible(ravager, target, followRange(ravager))) {
    ravager.setTarget(target);
} else if (ravager.getTarget() instanceof Player) {
    ravager.setTarget(null);
}
```

- [ ] **Step 6: Test and commit**

Run: `./gradlew test --tests "*.ravager.*"`

Expected: Ravager state, spawn, trigger, and target tests pass.

```text
git add src/main src/test
git commit -m "Add persistent spooky ravagers"
```

### Task 6: Private Visibility, ProtocolLib Isolation, and Lifecycle Recovery

**Files:**
- Create: `src/main/java/dev/nullkeeper/strengthsmptrollitems/ravager/RavagerVisibilityService.java`
- Create: `src/main/java/dev/nullkeeper/strengthsmptrollitems/ravager/ProtocolRavagerIsolation.java`
- Create: `src/main/java/dev/nullkeeper/strengthsmptrollitems/ravager/RavagerProtectionListener.java`
- Create: `src/main/java/dev/nullkeeper/strengthsmptrollitems/ravager/RavagerLifecycleListener.java`
- Test: `src/test/java/dev/nullkeeper/strengthsmptrollitems/ravager/RavagerVisibilityServiceTest.java`
- Test: `src/test/java/dev/nullkeeper/strengthsmptrollitems/ravager/RavagerProtectionListenerTest.java`
- Test: `src/test/java/dev/nullkeeper/strengthsmptrollitems/ravager/RavagerLifecycleListenerTest.java`

**Interfaces:**
- Consumes: registry, metadata store, and access policy from Task 5.
- Produces: `RavagerVisibilityService.refresh(Ravager)`, `refresh(Player)`, and `hideFromUnauthorized(Ravager)`.
- Produces: `ProtocolRavagerIsolation.start()` and `close()`.

- [ ] **Step 1: Write failing visibility and protection tests**

With shooter, target, and outsider mocks, prove Bukkit show/hide state; outsider damage/interaction cancellation; shooter and target may damage/kill; Ravager damage is cancelled for shooter/outsider but allowed for target; unrelated Ravagers are untouched.

- [ ] **Step 2: Run tests and confirm red**

Run: `./gradlew test --tests "*.RavagerVisibilityServiceTest" --tests "*.RavagerProtectionListenerTest"`

Expected: compilation fails because visibility/protection classes do not exist.

- [ ] **Step 3: Implement Bukkit visibility and event defense**

Call `showEntity(plugin, ravager)` only for shooter/target and `hideEntity(plugin, ravager)` for everyone else. Repeat authorization in entity damage, entity-target, and interaction events. Preserve normal block/world and non-player interactions.

```java
for (Player viewer : Bukkit.getOnlinePlayers()) {
    if (policy.canObserve(viewer.getUniqueId(), assignment)) {
        viewer.showEntity(plugin, ravager);
    } else {
        viewer.hideEntity(plugin, ravager);
    }
}
```

- [ ] **Step 4: Implement ProtocolLib fail-closed isolation**

Register packet adapters for Ravager entity-bound sound and outsider `USE_ENTITY` interaction. Resolve entity IDs through `ProtocolManager`, cancel when registry metadata identifies a private Ravager and policy denies the viewer, catch version-sensitive decode failures per packet, and disable this plugin with a detailed console error if adapters cannot register. `close()` must remove only this plugin's listeners.

```java
PacketAdapter soundFilter = new PacketAdapter(
        plugin,
        ListenerPriority.HIGHEST,
        PacketType.Play.Server.ENTITY_SOUND) {
    @Override
    public void onPacketSending(PacketEvent event) {
        Entity entity = event.getPacket().getEntityModifier(event.getPlayer().getWorld()).readSafely(0);
        if (entity instanceof Ravager ravager && denied(event.getPlayer(), ravager)) {
            event.setCancelled(true);
        }
    }
};
```

- [ ] **Step 5: Write and implement lifecycle recovery tests**

Verify startup and chunk-load scans register marked Ravagers, player join/world change refreshes visibility, target respawn preserves assignment, target logout clears only live AI target, and Ravager death unregisters without respawn.

- [ ] **Step 6: Run tests and commit**

Run: `./gradlew test --tests "*.ravager.*"`

Expected: all Ravager tests pass. Protocol packet wire behavior remains a documented manual test.

```text
git add src/main src/test
git commit -m "Isolate private ravager visibility"
```

### Task 7: Plugin Composition and Integration Verification

**Files:**
- Create: `src/main/java/dev/nullkeeper/strengthsmptrollitems/StrengthSmpTrollItemsPlugin.java`
- Create: `src/main/java/dev/nullkeeper/strengthsmptrollitems/RuntimeComponents.java`
- Test: `src/test/java/dev/nullkeeper/strengthsmptrollitems/PluginDescriptorTest.java`
- Test: `src/test/java/dev/nullkeeper/strengthsmptrollitems/PluginIntegrationTest.java`

**Interfaces:**
- Consumes all services/listeners from Tasks 1–6.
- Produces `RuntimeComponents.start(JavaPlugin, ProtocolManager)` and `RuntimeComponents.close()`.
- Produces the runtime plugin lifecycle declared in `plugin.yml`.

- [ ] **Step 1: Write failing descriptor and lifecycle tests**

Assert plugin name, main class, expanded version, `api-version: '26.1'`, `depend: [ProtocolLib]`, command registration, permission defaults, default config creation, listener registration, and task/listener cleanup on disable.

- [ ] **Step 2: Run tests and confirm red**

Run: `./gradlew test --tests "*.PluginDescriptorTest" --tests "*.PluginIntegrationTest"`

Expected: compilation fails because the plugin entrypoint does not exist.

- [ ] **Step 3: Wire the composition root**

In `onEnable`, save defaults, load one validated snapshot, create keys/services, register commands/listeners, scan loaded worlds, start ProtocolLib isolation, and schedule the protected retarget task. If initial config or ProtocolLib setup fails, log context and disable the plugin. In `onDisable`, close packet isolation and cancel the owned task.

```java
@Override
public void onEnable() {
    try {
        saveDefaultConfig();
        RuntimeComponents components = RuntimeComponents.start(this, ProtocolLibrary.getProtocolManager());
        this.runtime = components;
    } catch (RuntimeException exception) {
        getLogger().log(Level.SEVERE, "Strength SMP Troll Items could not start", exception);
        getServer().getPluginManager().disablePlugin(this);
    }
}
```

- [ ] **Step 4: Run the complete automated suite**

Run: `./gradlew clean test`

Expected: all tests pass with no skipped tests hiding an unimplemented MockBukkit operation; any unavoidable skipped network behavior is moved to the manual test list rather than asserted as automated coverage.

- [ ] **Step 5: Commit the integrated plugin**

```text
git add src/main src/test
git commit -m "Wire Strength SMP Troll Items plugin"
```

### Task 8: Documentation, Release Metadata, and Final Verification

**Files:**
- Create: `README.md`
- Create: `CHANGELOG.md`
- Create: `LICENSE`
- Modify: `src/main/resources/config.yml`
- Modify: `docs/superpowers/plans/2026-08-01-strength-smp-troll-items.md`

**Interfaces:**
- Consumes the completed commands, configuration schema, permissions, build, and verified behavior.
- Produces the `0.1.0` distributable and human verification handoff.

- [ ] **Step 1: Write project documentation**

Follow the repository README structure exactly: centered placeholder icon, title/tagline/badges, feature table, three action screenshot placeholders, Modrinth install link, ProtocolLib official listing, numbered use steps, commands/permissions/config, unbounded-Ravager warning, collapsible Java 25 Gradle build instructions, inspiration video/channel credits, and GPL-3.0-only footer for 2026 NullKeeper-dev.

- [ ] **Step 2: Add release metadata**

Create one changelog entry headed `## [0.1.0]` describing the complete initial feature set. Add the full GPL-3.0-only license. Confirm `gradle.properties` remains the only literal project version source and `plugin.yml` still contains `${version}`.

- [ ] **Step 3: Run formatting, secret, and repository checks**

Run:

```text
git diff --check
git diff --name-only
git grep -n -I -E "(BEGIN (RSA|OPENSSH|EC) PRIVATE KEY|gh[pousr]_[A-Za-z0-9]{20,}|AKIA[0-9A-Z]{16})" -- .
```

Expected: no whitespace errors, no credential-like matches, and only intended project files modified. Confirm `.gitignore` covers `.gradle/`, `build/`, `run/`, `.idea/`, and local environment files.

- [ ] **Step 4: Build and inspect the distributable**

Run: `./gradlew clean build`

Expected: tests pass and exactly one normal jar appears under `build/libs/` with no `-sources.jar`. Inspect it with `jar tf` and verify `plugin.yml`, `config.yml`, production classes, and no test/MockBukkit/ProtocolLib classes are bundled.

- [ ] **Step 5: Record manual verification status**

Document that this environment cannot replace the required three-player live-server matrix unless runnable servers, clients, and compatible ProtocolLib builds are available. List exact checks for scale rendering/persistence, private Ravager visual/audio/collision/damage behavior, stacking, world interaction, relog/death/restart, and arbitrary-item instant consumption across 26.1, 26.1.1, 26.1.2, and 26.2.

- [ ] **Step 6: Review the full unreleased diff**

Compare against commit `0c8b2f0` and the approved design. Fix critical/high correctness, security, compatibility, and documentation findings. Re-run `./gradlew clean build` after every fix.

- [ ] **Step 7: Commit release-ready project files**

```text
git add .gitignore README.md CHANGELOG.md LICENSE build.gradle.kts settings.gradle.kts gradle.properties gradle gradlew gradlew.bat src docs/superpowers/plans/2026-08-01-strength-smp-troll-items.md
git commit -m "Prepare Strength SMP Troll Items 0.1.0"
```
