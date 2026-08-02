# Six-Tick Hungry Berry and Universal Jar Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace click-triggered Hungry Berry consumption with native six-tick hold-to-eat behavior and ship one `2.0.0` jar that runs on all five supported Paper/Purpur targets.

**Architecture:** Converted stacks retain their PDC marker and gain Paper `FOOD` and `CONSUMABLE` data components. The interaction listener blocks the item's original block action while allowing the native item-use lifecycle; the build compiles production source against the lowest API, recompiles it against every supported API as a compatibility gate, and packages the lowest-API classes once.

**Tech Stack:** Java 25, Paper API data components, Bukkit events/PDC, MockBukkit, JUnit 6, Gradle Kotlin DSL.

## Global Constraints

- Ship exactly one normal jar: `build/libs/strength-smp-troll-items-2.0.0.jar`.
- Support Paper 26.1.1, Paper 26.1.2, Paper 26.2, Purpur 26.1.2, and Purpur 26.2.
- Compile the distributable against Paper 26.1.1 and declare `api-version: '26.1'`.
- Keep compile-only compatibility checks for every supported exact API.
- Require an uninterrupted right-click hold for `6` ticks by default; releasing or changing the active item cancels consumption through the native server lifecycle.
- Eat exactly one item at full hunger with `0` nutrition and `0.0` saturation by default.
- Preserve the complete converted stack's material, amount, durability, enchantments, name, lore, custom model data, and unrelated PDC.
- Converted blocks, bows, shields, and other usable items must eat instead of performing their original right-click behavior.
- ProtocolLib, Bukkit, Spigot, and standalone Minecraft 26.1 are not supported targets.
- Do not modify or commit the user's untracked `Prompt.txt`.
- Use test-first red/green cycles for production behavior and commit each independently reviewable task directly to `main`.

## File Structure

- `src/main/java/dev/nullkeeper/strengthsmptrollitems/hungry/EdibleItemService.java`: creates immutable edible copies with native data components and refreshes legacy marked stacks.
- `src/main/java/dev/nullkeeper/strengthsmptrollitems/hungry/EdibleInteractionListener.java`: routes marked right-clicks into native item use while denying original interactions.
- `src/main/java/dev/nullkeeper/strengthsmptrollitems/RuntimeComponents.java`: wires the simplified interaction listener.
- `src/main/java/dev/nullkeeper/strengthsmptrollitems/config/ConfigLoader.java`: supplies the six-tick fallback.
- `src/main/resources/config.yml`: exposes the six-tick default.
- `src/test/java/dev/nullkeeper/strengthsmptrollitems/config/ConfigLoaderTest.java`: protects the documented default and validation boundary.
- `src/test/java/dev/nullkeeper/strengthsmptrollitems/hungry/EdibleItemServiceTest.java`: protects component values, metadata preservation, and legacy-stack refresh.
- `src/test/java/dev/nullkeeper/strengthsmptrollitems/hungry/HungryBerryListenerTest.java`: protects interaction routing without pretending MockBukkit simulates client hold/release packets.
- `build.gradle.kts`: packages one jar and retains five exact compile gates.
- `gradle.properties`: records approved major version `2.0.0`.
- `README.md`, `CHANGELOG.md`, and `AGENTS.md`: document the new behavior, universal artifact, release contract, and manual checks.

---

### Task 1: Make six ticks the configuration default

**Files:**
- Modify: `src/test/java/dev/nullkeeper/strengthsmptrollitems/config/ConfigLoaderTest.java`
- Modify: `src/main/java/dev/nullkeeper/strengthsmptrollitems/config/ConfigLoader.java`
- Modify: `src/main/resources/config.yml`

**Interfaces:**
- Consumes: `PluginConfig.EdibleSettings(int nutrition, float saturation, int consumeDelayTicks)`.
- Produces: `ConfigLoader.load(ConfigurationSection)` returns `consumeDelayTicks() == 6` when the key is bundled or absent.

- [ ] **Step 1: Write the failing default tests**

Change the bundled-default assertion and add the missing-key assertion:

```java
assertEquals(6, config.edible().consumeDelayTicks());
```

Keep the existing invalid boundary cases `-1` and `72001`; a configured `0` remains valid for server owners who intentionally want instant native consumption.

- [ ] **Step 2: Run the focused tests and verify red**

Run:

```powershell
.\gradlew.bat test --tests "dev.nullkeeper.strengthsmptrollitems.config.ConfigLoaderTest"
```

Expected: FAIL because the bundled YAML and Java fallback still return `0`, not `6`.

- [ ] **Step 3: Implement the default**

Use the same literal in the runtime fallback and bundled config:

```java
int consumeTicks = integer(root, "edible.consume-delay-ticks", 6);
```

```yaml
edible:
  nutrition: 0
  saturation: 0.0
  consume-delay-ticks: 6
```

- [ ] **Step 4: Run the focused tests and verify green**

Run the Task 1 command again. Expected: all `ConfigLoaderTest` tests PASS.

- [ ] **Step 5: Review and commit**

```powershell
git diff --check
git add -- src/main/java/dev/nullkeeper/strengthsmptrollitems/config/ConfigLoader.java src/main/resources/config.yml src/test/java/dev/nullkeeper/strengthsmptrollitems/config/ConfigLoaderTest.java
git commit -m "Default Hungry Berry eating to six ticks"
```

### Task 2: Attach native food and consumable components

**Files:**
- Modify: `src/test/java/dev/nullkeeper/strengthsmptrollitems/hungry/EdibleItemServiceTest.java`
- Modify: `src/main/java/dev/nullkeeper/strengthsmptrollitems/hungry/EdibleItemService.java`

**Interfaces:**
- Consumes: current `EdibleSettings` from `Supplier<EdibleSettings>` and the PDC identity methods in `TrollItemService`.
- Produces: `ItemStack convert(ItemStack original)` and `ItemStack prepareForUse(ItemStack marked)`; both return new stacks with current native components.

- [ ] **Step 1: Replace manual-consumption tests with failing component tests**

Keep `conversionClonesAndPreservesTheEntireStackMetadata`, then extend it with hand-derived expectations:

```java
FoodProperties food = converted.getData(DataComponentTypes.FOOD);
Consumable consumable = converted.getData(DataComponentTypes.CONSUMABLE);

assertNotNull(food);
assertEquals(0, food.nutrition());
assertEquals(0.0f, food.saturation());
assertTrue(food.canAlwaysEat());
assertNotNull(consumable);
assertEquals(0.3f, consumable.consumeSeconds(), 0.0001f);
assertEquals(ItemUseAnimation.EAT, consumable.animation());
```

Add a configured-values test using `new EdibleSettings(4, 1.5f, 10)` and literal expectations `4`, `1.5f`, and `0.5f`. Add a legacy migration test that creates `trollItems.markEdible(new ItemStack(Material.SHIELD, 2))`, calls `prepareForUse`, and asserts the original is unchanged while the returned copy has both components and remains marked.

Delete the tests for `consume(...)`; that manual decrement API is being removed in favor of server-native completion.

- [ ] **Step 2: Run the focused tests and verify red**

Run:

```powershell
.\gradlew.bat test --tests "dev.nullkeeper.strengthsmptrollitems.hungry.EdibleItemServiceTest"
```

Expected: compilation/test failure because `prepareForUse` does not exist and `convert` does not attach `FOOD` or `CONSUMABLE`.

- [ ] **Step 3: Implement immutable native component preparation**

Replace manual player/inventory consumption with component preparation:

```java
public ItemStack convert(ItemStack original) {
    return prepareForUse(items.markEdible(original));
}

public ItemStack prepareForUse(ItemStack marked) {
    Objects.requireNonNull(marked, "marked");
    if (!items.isEdible(marked)) {
        throw new IllegalArgumentException("Only marked edible stacks can be prepared");
    }
    EdibleSettings settings = settingsSource.get();
    ItemStack prepared = marked.clone();
    prepared.setData(
            DataComponentTypes.FOOD,
            FoodProperties.food()
                    .nutrition(settings.nutrition())
                    .saturation(settings.saturation())
                    .canAlwaysEat(true));
    prepared.setData(
            DataComponentTypes.CONSUMABLE,
            Consumable.consumable()
                    .consumeSeconds(settings.consumeDelayTicks() / 20.0f)
                    .animation(ItemUseAnimation.EAT));
    return prepared;
}
```

Remove the `Player`, `EquipmentSlot`, `PlayerInventory`, and `Material` imports and the `consume`, `heldItem`, `replace`, and `applyNutrition` methods.

- [ ] **Step 4: Run the focused tests and verify green**

Run the Task 2 command again. Expected: all `EdibleItemServiceTest` tests PASS without warnings or errors.

- [ ] **Step 5: Review and commit**

```powershell
git diff --check
git add -- src/main/java/dev/nullkeeper/strengthsmptrollitems/hungry/EdibleItemService.java src/test/java/dev/nullkeeper/strengthsmptrollitems/hungry/EdibleItemServiceTest.java
git commit -m "Use native components for edible stacks"
```

### Task 3: Route right-clicks through native item use

**Files:**
- Modify: `src/test/java/dev/nullkeeper/strengthsmptrollitems/hungry/HungryBerryListenerTest.java`
- Modify: `src/main/java/dev/nullkeeper/strengthsmptrollitems/hungry/EdibleInteractionListener.java`
- Modify: `src/main/java/dev/nullkeeper/strengthsmptrollitems/RuntimeComponents.java`

**Interfaces:**
- Consumes: `EdibleItemService.prepareForUse(ItemStack)` and `TrollItemService.isEdible(ItemStack)`.
- Produces: `EdibleInteractionListener(TrollItemService items, EdibleItemService edibles)` and `onInteract(PlayerInteractEvent)` that denies block use, allows native item use, refreshes the active hand copy, and never decrements the stack itself.

- [ ] **Step 1: Write failing interaction-policy tests**

Replace instant/delayed scheduler assertions with these observable listener boundaries:

```java
interactionListener.onInteract(event);

assertEquals(Event.Result.DENY, event.useInteractedBlock());
assertEquals(Event.Result.ALLOW, event.useItemInHand());
assertEquals(2, target.getInventory().getItemInOffHand().getAmount());
```

Add separate tests proving:

- a right-click refreshes a legacy PDC-only stack in the correct hand with `FOOD` and `CONSUMABLE`;
- changing the `AtomicReference<EdibleSettings>` to ten ticks before a new interaction refreshes `consumeSeconds()` to literal `0.5f`;
- left-click and unmarked right-click events remain untouched;
- an already-cancelled right-click remains cancelled, protecting region-plugin decisions.

Initialize the desired two-argument constructor in `setUp()`:

```java
interactionListener = new EdibleInteractionListener(items, edibles);
```

- [ ] **Step 2: Run the focused tests and verify red**

Run:

```powershell
.\gradlew.bat test --tests "dev.nullkeeper.strengthsmptrollitems.hungry.HungryBerryListenerTest"
```

Expected: compilation/assertion failure because the old listener requires scheduler dependencies, cancels native item use, and consumes immediately.

- [ ] **Step 3: Implement native routing**

Reduce the listener to immutable preparation and event routing:

```java
@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
public void onInteract(PlayerInteractEvent event) {
    EquipmentSlot hand = event.getHand();
    if (event.isCancelled()
            || !isRightClick(event.getAction())
            || !isHand(hand)
            || !items.isEdible(event.getItem())) {
        return;
    }

    ItemStack prepared = edibles.prepareForUse(event.getItem());
    replace(event.getPlayer().getInventory(), hand, prepared);
    event.setUseInteractedBlock(Event.Result.DENY);
    event.setUseItemInHand(Event.Result.ALLOW);
}
```

Implement focused `isRightClick`, `isHand`, and `replace` helpers. Delete the scheduler, pending-set, click-guard, manual completion, logging, and plugin/settings fields. Update `RuntimeComponents` to construct `new EdibleInteractionListener(items, edibles)`.

- [ ] **Step 4: Run Hungry Berry tests and verify green**

Run:

```powershell
.\gradlew.bat test --tests "dev.nullkeeper.strengthsmptrollitems.hungry.*"
```

Expected: all Hungry Berry tests PASS; amounts remain unchanged after the listener because Minecraft, not the listener, completes consumption.

- [ ] **Step 5: Review and commit**

Review the `HIGHEST` listener for conflicts: it deliberately respects prior cancellation but can conflict with later listeners that change `useInteractedBlock` or `useItemInHand`.

```powershell
git diff --check
git add -- src/main/java/dev/nullkeeper/strengthsmptrollitems/hungry/EdibleInteractionListener.java src/main/java/dev/nullkeeper/strengthsmptrollitems/RuntimeComponents.java src/test/java/dev/nullkeeper/strengthsmptrollitems/hungry/HungryBerryListenerTest.java
git commit -m "Require native hold-to-eat interaction"
```

### Task 4: Produce one universal jar with five compile gates

**Files:**
- Modify: `build.gradle.kts`

**Interfaces:**
- Consumes: the existing five `ServerTarget` API coordinates.
- Produces: default `jar` artifact `strength-smp-troll-items-2.0.0.jar`, five `compile<Target>Java` tasks, and `verifyDistributables` that rejects any extra jar.

- [ ] **Step 1: Run a failing universal-artifact check**

Run the current build followed by an assertion for the approved output:

```powershell
.\gradlew.bat clean assemble
$jars = @(Get-ChildItem build\libs -Filter '*.jar')
if ($jars.Count -ne 1 -or $jars[0].Name -ne 'strength-smp-troll-items-2.0.0.jar') {
    throw "Expected one universal 2.0.0 jar; found $($jars.Name -join ', ')"
}
```

Expected: FAIL because the current build creates five classified `1.0.0` jars.

- [ ] **Step 2: Refactor packaging without weakening compatibility checks**

Enable the normal jar and remove per-target resource/jar tasks:

```kotlin
tasks.jar {
    enabled = true
    archiveBaseName.set("strength-smp-troll-items")
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
        )
    }
}
```

Retain the five resolvable API configurations and `compile<Target>Java` tasks. Store those tasks in `compatibilityCompileTasks`, then make verification depend on the normal jar plus every compatibility compile:

```kotlin
val expectedDistributables = setOf("strength-smp-troll-items-$releaseVersion.jar")

tasks.register("verifyDistributables") {
    dependsOn(tasks.jar, compatibilityCompileTasks)
    doLast {
        val actual = layout.buildDirectory.dir("libs").get().asFile
            .listFiles { file -> file.extension == "jar" }
            .orEmpty()
            .map { file -> file.name }
            .toSet()
        check(actual == expectedDistributables) {
            "Expected distributables $expectedDistributables but found $actual"
        }
    }
}
```

Keep `processResources` expanding `apiVersion` to `26.1`. Make `assemble` use the default jar only; `check` and `verifyDistributables` retain all five compatibility compilations.

- [ ] **Step 3: Set the approved release version**

Change `gradle.properties` to:

```properties
version=2.0.0
```

- [ ] **Step 4: Build and verify green**

Run:

```powershell
.\gradlew.bat clean test check verifyDistributables
Get-ChildItem build\libs -Filter '*.jar' | Select-Object Name, Length
```

Expected: all five compatibility compile tasks and all tests PASS, and exactly `strength-smp-troll-items-2.0.0.jar` exists.

- [ ] **Step 5: Inspect the universal artifact**

Read the archive and verify its descriptor/manifest rather than trusting the filename:

```powershell
jar tf build\libs\strength-smp-troll-items-2.0.0.jar
```

Extract into a newly created temporary directory, inspect `plugin.yml` for `version: '2.0.0'` and `api-version: '26.1'`, inspect `META-INF/MANIFEST.MF` for `Implementation-Version: 2.0.0`, and confirm no ProtocolLib classes/dependency or source files are present.

- [ ] **Step 6: Review and commit**

```powershell
git diff --check
git add -- build.gradle.kts gradle.properties
git commit -m "Build one universal plugin jar"
```

### Task 5: Update release documentation and standing contract

**Files:**
- Modify: `README.md`
- Modify: `CHANGELOG.md`
- Modify: `AGENTS.md`

**Interfaces:**
- Consumes: the verified artifact name, five-target matrix, approved `2.0.0` bump, and six-tick behavior.
- Produces: one pending changelog entry `## [2.0.0]` and user-facing instructions that select no platform/version suffix.

- [ ] **Step 1: Update README behavior and installation**

Replace every instant-consumption statement with an uninterrupted six-tick hold. Replace the five suffix table and “no universal jar” text with one universal-jar statement covering the exact five targets. Update the build section to list only:

```text
build/libs/strength-smp-troll-items-2.0.0.jar
```

Keep the existing inspiration links, Paper/Purpur-only statement, `INSERT MODRINTH LINK HERE`, image placeholders, commands, permissions, and compatibility warning. Update manual check 5 to cover holding, early release, full hunger, one-item consumption, and original-action suppression.

- [ ] **Step 2: Add the sole pending changelog entry**

Add `## [2.0.0]` above `## [1.0.0]` with these complete release changes:

- Hungry Berry items now use a six-tick native eating lifecycle instead of instant click consumption.
- Releasing early or switching the active item cancels consumption.
- Native food/consumable components preserve full-hunger use and configurable nutrition/saturation.
- Legacy marked stacks refresh before use.
- Five classified artifacts are replaced by one universal jar, while all five exact APIs remain compile-tested.

- [ ] **Step 3: Correct the standing project contract**

In `AGENTS.md`, set platform support to Paper/Purpur only, remove ProtocolLib as a dependency/release blocker, list one universal jar for the exact five targets, and update §8a Hungry Berry text from instant right-click consumption to a six-tick uninterrupted hold. Point §8a to the 2026-08-02 addendum as the authoritative override for eating and packaging.

- [ ] **Step 4: Review documentation and commit**

```powershell
rg -n "instant|five jars|no universal|ProtocolLib|Bukkit/Spigot|CraftBukkit" README.md CHANGELOG.md AGENTS.md
git diff --check
git add -- README.md CHANGELOG.md AGENTS.md
git commit -m "Document universal 2.0.0 release"
```

Expected: remaining historical statements are clearly scoped to older changelog/spec entries, not current support claims.

### Task 6: Complete release verification

**Files:**
- Verify only: all tracked source, test, build, and documentation files modified above.
- Do not commit: `run/`, `build/`, `.gradle/`, server logs/worlds, or `Prompt.txt`.

**Interfaces:**
- Consumes: `build/libs/strength-smp-troll-items-2.0.0.jar`.
- Produces: evidence for automated tests, exact API compilation, one-jar packaging, five-server startup, security scan, and remaining human gameplay tests.

- [ ] **Step 1: Run the clean automated gate**

```powershell
.\gradlew.bat clean test check verifyDistributables
```

Expected: all JUnit tests PASS, all five compatibility compiles PASS, and exactly one normal jar is produced.

- [ ] **Step 2: Audit the resulting jar**

Confirm the archive contains `plugin.yml`, the plugin entrypoint, Hungry Berry classes, native Ravager classes, and GPL-relevant project metadata; confirm it contains no `.java`, ProtocolLib package, or embedded Paper/Purpur API classes. Re-read expanded `plugin.yml` and the manifest from the actual archive.

- [ ] **Step 3: Run the five-server startup matrix**

For each directory below, replace the old classified plugin jar with the same universal jar, start its existing `server.jar` using Java 25 and `--nogui`, wait for the `Done` line, issue `stop`, and inspect `logs/latest.log`:

```text
run/compatibility/paper-26.1.1
run/compatibility/paper-26.1.2
run/compatibility/paper-26.2
run/compatibility/purpur-26.1.2
run/compatibility/purpur-26.2
```

Each log must show StrengthSmpTrollItems 2.0.0 enabled with no linkage, plugin-load, missing-dependency, or stack-trace errors. The same jar hash must be installed in all five directories.

- [ ] **Step 4: Run scoped safety and repository checks**

```powershell
git diff --check
git status --short
git ls-files --modified --others --exclude-standard | Where-Object { $_ -ne 'Prompt.txt' }
```

Scan only created/modified tracked files for credential-like assignments and confirm `.gitignore` still covers `.gradle/`, `build/`, and `run/`. Verify `Prompt.txt` remains untracked and unchanged.

- [ ] **Step 5: Record manual multiplayer work still required**

Startup smoke tests do not prove client hold/release packets or animation. Report these human checks as outstanding on every target:

1. Hold a converted normal item for six ticks and consume exactly one.
2. Release before six ticks and switch slots before six ticks; consume nothing.
3. Eat at full hunger without changing hunger/saturation defaults.
4. Repeat with a block, bow, shield, main hand, and offhand; see the eating animation and no original action.
5. Consume from a legacy PDC-only stack and verify it remains permanently edible after splitting/transferring.

- [ ] **Step 6: Final self-review and handoff**

Review the entire diff from commit `922f3e6` through `HEAD` for requirements, error isolation, immutable stack handling, listener priority conflicts, unsupported API calls, stale documentation, and accidental generated files. Report the universal jar path/hash, test count, five compatibility compile results, five startup results, manual work remaining, `EdibleInteractionListener.onInteract` compatibility risk, and final Git status.
