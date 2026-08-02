# Paper/Purpur Native Ravagers and Damage Ticks Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove Spigot/CraftBukkit and ProtocolLib support, activate troll effects on valid damage ticks regardless of final health damage, and produce five separately compiled Paper/Purpur jars.

**Architecture:** A shared immutable `DamageTickPolicy` decides whether all three hit-driven features may activate. Paper's native default-visibility and show/hide APIs replace packet filtering for private Ravagers. Gradle compiles the same source tree independently against five pinned platform APIs and packages one labelled jar per target.

**Tech Stack:** Java 25, Gradle Kotlin DSL, Paper API, Purpur API, JUnit 6, MockBukkit 26.1.2.

## Global Constraints

- Supported targets are Paper 26.1.1, Paper 26.1.2, Paper 26.2, Purpur 26.1.2, and Purpur 26.2.
- Produce exactly five normal jars; do not produce a universal or sources jar.
- Compile each jar against its matching platform API.
- Use no Spigot API dependency, CraftBukkit/NMS internals, ProtocolLib, or other packet library.
- Preserve inherited `org.bukkit` types and `plugin.yml`, which remain part of Paper/Purpur plugin development.
- Private Ravager visuals are participant-only; sound leakage is accepted.
- Effects require a positive raw, uncancelled, new damage tick, but do not require positive final health damage.
- Creative/Spectator players, invulnerable entities, and immunity-window repeats do not qualify.
- Protection-plugin cancellations remain respected.
- Version `1.0.0` is approved because removing supported platforms is breaking.
- Work directly on `main`; do not create branches, worktrees, or subagent tasks.
- Leave the user's untracked `Prompt.txt` untouched.

## File Map

- `src/main/java/dev/nullkeeper/strengthsmptrollitems/combat/DamageTickPolicy.java`: one shared definition of a valid damage tick.
- `src/main/java/dev/nullkeeper/strengthsmptrollitems/{resize,hungry,ravager}/*Listener.java`: feature-specific gates that delegate damage eligibility to the policy.
- `src/main/java/dev/nullkeeper/strengthsmptrollitems/ravager/RavagerSpawner.java`: sets native hidden-by-default state in the pre-spawn initializer.
- `src/main/java/dev/nullkeeper/strengthsmptrollitems/ravager/RavagerVisibilityService.java`: explicitly shows participants and hides outsiders.
- `src/main/java/dev/nullkeeper/strengthsmptrollitems/ravager/RavagerLifecycleListener.java`: restores native hidden state and assignments after startup/chunk load.
- `src/main/java/dev/nullkeeper/strengthsmptrollitems/RuntimeComponents.java`: composes the policy and native visibility without ProtocolLib.
- `src/main/java/dev/nullkeeper/strengthsmptrollitems/StrengthSmpTrollItemsPlugin.java`: dependency-free plugin lifecycle.
- `build.gradle.kts`: five API classpaths, compile/resource/jar tasks, and distributable verification.
- `src/main/resources/plugin.yml`: dependency-free descriptor with per-target expanded API version.
- `README.md`, `CHANGELOG.md`, and design documents: supported matrix, install/build instructions, limitations, and release notes.

---

### Task 1: Shared Damage-Tick Eligibility

**Files:**
- Create: `src/main/java/dev/nullkeeper/strengthsmptrollitems/combat/DamageTickPolicy.java`
- Create: `src/test/java/dev/nullkeeper/strengthsmptrollitems/combat/DamageTickPolicyTest.java`
- Modify: `src/main/java/dev/nullkeeper/strengthsmptrollitems/resize/ResizingSwordListener.java`
- Modify: `src/main/java/dev/nullkeeper/strengthsmptrollitems/hungry/HungryBerryListener.java`
- Modify: `src/main/java/dev/nullkeeper/strengthsmptrollitems/ravager/SpookyCrossbowListener.java`
- Modify: `src/main/java/dev/nullkeeper/strengthsmptrollitems/RuntimeComponents.java`
- Modify: `src/test/java/dev/nullkeeper/strengthsmptrollitems/resize/ResizingSwordListenerTest.java`
- Modify: `src/test/java/dev/nullkeeper/strengthsmptrollitems/hungry/HungryBerryListenerTest.java`
- Modify: `src/test/java/dev/nullkeeper/strengthsmptrollitems/ravager/SpookyCrossbowListenerTest.java`
- Modify: `gradle.properties`
- Modify: `CHANGELOG.md`

**Interfaces:**
- Produces: `DamageTickPolicy.qualifies(EntityDamageByEntityEvent event, LivingEntity target) -> boolean`.
- Consumers: the three hit listeners receive one `DamageTickPolicy` in their constructors.
- Eligibility: uncancelled, `event.getDamage() > 0.0`, target not invulnerable, `target.getNoDamageTicks() == 0`, and player target not in Creative or Spectator.

- [x] **Step 1: Write failing policy tests**

Create tests for positive raw damage, zero final damage, cancelled events, zero raw damage, Creative, Spectator, explicit invulnerability, and positive no-damage ticks. Use an event subclass to prove final damage is ignored:

```java
private EntityDamageByEntityEvent zeroFinalDamage(PlayerMock attacker, LivingEntity target) {
    return new EntityDamageByEntityEvent(attacker, target, DamageCause.ENTITY_ATTACK, 1.0) {
        @Override
        public double getFinalDamage() {
            return 0.0;
        }
    };
}

@Test
void positiveRawDamageQualifiesEvenWhenFinalDamageIsZero() {
    assertTrue(policy.qualifies(zeroFinalDamage(attacker, target), target));
}

@Test
void immunityWindowRepeatDoesNotQualify() {
    target.setNoDamageTicks(5);
    assertFalse(policy.qualifies(damage(1.0), target));
}
```

- [x] **Step 2: Run the policy test and confirm red**

Run: `.\gradlew.bat test --tests "*.DamageTickPolicyTest"`

Expected: test compilation fails because `DamageTickPolicy` does not exist.

- [x] **Step 3: Implement the minimal policy**

```java
package dev.nullkeeper.strengthsmptrollitems.combat;

import java.util.Objects;
import org.bukkit.GameMode;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class DamageTickPolicy {
    public boolean qualifies(EntityDamageByEntityEvent event, LivingEntity target) {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(target, "target");
        if (event.isCancelled()
                || event.getDamage() <= 0.0
                || target.isInvulnerable()
                || target.getNoDamageTicks() > 0) {
            return false;
        }
        if (target instanceof Player player) {
            GameMode mode = player.getGameMode();
            return mode != GameMode.CREATIVE && mode != GameMode.SPECTATOR;
        }
        return true;
    }
}
```

- [x] **Step 4: Run the policy test and confirm green**

Run: `.\gradlew.bat test --tests "*.DamageTickPolicyTest"`

Expected: every policy case passes.

- [x] **Step 5: Write failing listener integration tests**

Replace each former “zero damage does nothing” assertion. For every feature, pass an event with raw damage `1.0` and overridden final damage `0.0`, then assert the scale changes, held stack converts, or five Ravagers spawn. Retain separate assertions proving cancellation, zero raw damage, Creative/Spectator, invulnerability, and `setNoDamageTicks(5)` do nothing.

```java
target.setGameMode(GameMode.CREATIVE);
listener.onDamage(damage(attacker, target, 1.0));
assertEquals(1.0, target.getAttribute(scaleAttribute).getBaseValue());

target.setGameMode(GameMode.SURVIVAL);
listener.onDamage(zeroFinalDamage(attacker, target));
assertEquals(1.05, target.getAttribute(scaleAttribute).getBaseValue());
```

- [x] **Step 6: Inject and use the policy in all three listeners**

Add `DamageTickPolicy damageTicks` to each constructor and replace every `event.getFinalDamage() <= 0.0` condition with `!damageTicks.qualifies(event, target)`. In `RuntimeComponents.registerListeners`, create one `DamageTickPolicy` and pass it to `ResizingSwordListener`, `HungryBerryListener`, and `SpookyCrossbowListener`.

```java
if (!(event.getDamager() instanceof Player attacker)
        || !(event.getEntity() instanceof LivingEntity target)
        || !damageTicks.qualifies(event, target)
        || !items.isType(attacker.getInventory().getItemInMainHand(),
                TrollItemType.RESIZING_SWORD)) {
    return;
}
```

- [x] **Step 7: Apply the approved version and pending changelog**

Set `version=1.0.0` in `gradle.properties`. Add one pending `## [1.0.0]` section above the historical `0.1.0` entry in `CHANGELOG.md`, recording the new valid-damage-tick behavior and the approved breaking support change. Keep `0.1.0` as release history.

- [x] **Step 8: Run focused and full tests**

Run:

```text
.\gradlew.bat test --tests "*.DamageTickPolicyTest" --tests "*.ResizingSwordListenerTest" --tests "*.HungryBerryListenerTest" --tests "*.SpookyCrossbowListenerTest"
.\gradlew.bat test
```

Expected: all focused tests and the complete existing suite pass.

- [x] **Step 9: Review and commit**

Run `git diff --check`, review only the Task 1 diff, scan changed files for credential-like assignments, then commit:

```text
git add gradle.properties CHANGELOG.md src/main/java/dev/nullkeeper/strengthsmptrollitems/combat src/main/java/dev/nullkeeper/strengthsmptrollitems/RuntimeComponents.java src/main/java/dev/nullkeeper/strengthsmptrollitems/resize/ResizingSwordListener.java src/main/java/dev/nullkeeper/strengthsmptrollitems/hungry/HungryBerryListener.java src/main/java/dev/nullkeeper/strengthsmptrollitems/ravager/SpookyCrossbowListener.java src/test
git commit -m "Trigger troll items on valid damage ticks"
```

### Task 2: Native Paper Ravager Privacy and ProtocolLib Removal

**Files:**
- Modify: `src/main/java/dev/nullkeeper/strengthsmptrollitems/ravager/RavagerSpawner.java`
- Modify: `src/main/java/dev/nullkeeper/strengthsmptrollitems/ravager/RavagerVisibilityService.java`
- Modify: `src/main/java/dev/nullkeeper/strengthsmptrollitems/ravager/RavagerLifecycleListener.java`
- Delete: `src/main/java/dev/nullkeeper/strengthsmptrollitems/ravager/ProtocolRavagerIsolation.java`
- Modify: `src/main/java/dev/nullkeeper/strengthsmptrollitems/RuntimeComponents.java`
- Modify: `src/main/java/dev/nullkeeper/strengthsmptrollitems/StrengthSmpTrollItemsPlugin.java`
- Modify: `src/main/resources/plugin.yml`
- Modify: `build.gradle.kts`
- Modify: `src/test/java/dev/nullkeeper/strengthsmptrollitems/ravager/RavagerVisibilityServiceTest.java`
- Modify: `src/test/java/dev/nullkeeper/strengthsmptrollitems/ravager/RavagerLifecycleListenerTest.java`
- Delete: `src/test/java/dev/nullkeeper/strengthsmptrollitems/ravager/ProtocolRavagerIsolationTest.java`
- Modify: `src/test/java/dev/nullkeeper/strengthsmptrollitems/PluginDescriptorTest.java`
- Modify: `src/test/java/dev/nullkeeper/strengthsmptrollitems/PluginIntegrationTest.java`
- Modify: `CHANGELOG.md`

**Interfaces:**
- `RavagerSpawner.SpawnOperation.spawn(Location, Consumer<Ravager>) -> Ravager` executes the supplied initializer before the production entity becomes trackable.
- `RavagerVisibilityService.refresh(Ravager)` establishes hidden-by-default state, then shows only shooter and target.
- `RuntimeComponents.start(JavaPlugin) -> RuntimeComponents` has no external dependency argument.

- [x] **Step 1: Write failing native-visibility tests**

Extend visibility and lifecycle tests with these assertions:

```java
visibility.refresh(ravager);
assertFalse(ravager.isVisibleByDefault());
assertTrue(shooter.canSee(ravager));
assertTrue(target.canSee(ravager));
assertFalse(outsider.canSee(ravager));
```

Update the injected spawner operation so the production initializer is exercised:

```java
(location, initializer) -> {
    Ravager spawned = location.getWorld().spawn(location, Ravager.class);
    initializer.accept(spawned);
    return spawned;
}
```

Assert a spawned private Ravager is persistent, non-despawning, non-collidable, and `isVisibleByDefault() == false` before participant refresh results are inspected. Assert startup/chunk-load recovery also resets a marked Ravager to hidden-by-default.

- [x] **Step 2: Run visibility tests and confirm red**

Run: `.\gradlew.bat test --tests "*.RavagerVisibilityServiceTest" --tests "*.RavagerLifecycleListenerTest"`

Expected: compilation or assertions fail because the spawn seam and native default visibility are not implemented.

- [x] **Step 3: Move visibility into the pre-spawn initializer**

Change `SpawnOperation` to accept `Consumer<Ravager>`. Production spawning remains `SpawnReason.CUSTOM` and applies this initializer in Paper's spawn consumer:

```java
private static void configure(Ravager ravager, RavagerSettings settings) {
    ravager.setVisibleByDefault(false);
    ravager.setPersistent(true);
    ravager.setRemoveWhenFarAway(false);
    ravager.setCollidable(false);
    ravager.addPotionEffect(new PotionEffect(
            PotionEffectType.SPEED,
            PotionEffect.INFINITE_DURATION,
            settings.speedLevel() - 1,
            false,
            false));
}

private static Ravager spawnCustom(Location location, Consumer<Ravager> initializer) {
    return location.getWorld().spawn(
            location,
            Ravager.class,
            SpawnReason.CUSTOM,
            true,
            initializer);
}
```

Call `spawnOperation.spawn(location, ravager -> configure(ravager, settings))`. In `RavagerVisibilityService.refresh(Ravager)` and lifecycle recovery, call `setVisibleByDefault(false)` before show/hide decisions. Continue using `showEntity` for participants and `hideEntity` for outsiders. Keep `RavagerProtectionListener` unchanged as server-side defense.

Implementation note: MockBukkit 4.114.0 throws for both native default
visibility and Ravager despawn configuration. Small injected platform seams
therefore verify that refresh and pre-spawn initialization are requested;
the concrete Paper operations remain covered by compilation and the live
startup/manual matrix.

- [x] **Step 4: Write failing dependency-free lifecycle tests**

Update `PluginDescriptorTest` to assert `descriptor.getStringList("depend")` is empty. Update `PluginIntegrationTest` to load `StrengthSmpTrollItemsPlugin.class` without creating a ProtocolLib mock or proxy and assert enable/disable still registers listeners and cleans tasks.

```java
StrengthSmpTrollItemsPlugin plugin = MockBukkit.load(StrengthSmpTrollItemsPlugin.class);
assertTrue(plugin.isEnabled());
server.getPluginManager().disablePlugin(plugin);
assertFalse(plugin.isEnabled());
```

- [x] **Step 5: Remove ProtocolLib from production and tests**

Delete both `ProtocolRavagerIsolation` files. Remove ProtocolLib imports, override constructors, manager lookup, startup, close, and failure cleanup. Make the plugin entrypoint a normal no-argument `JavaPlugin` and call `RuntimeComponents.start(this)`. Delete `depend: [ProtocolLib]` from `plugin.yml`.

In `build.gradle.kts`, remove the Spigot repository, Spigot API dependency, and both ProtocolLib dependencies. Use the oldest available Paper target for ordinary IDE/main compilation:

```kotlin
dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.1.build.29-alpha")
    testImplementation(platform("org.junit:junit-bom:6.1.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.papermc.paper:paper-api:26.1.2.build.74-stable")
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v26.1.2:4.114.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
```

- [x] **Step 6: Run focused and full tests**

Run:

```text
.\gradlew.bat clean test --tests "*.RavagerVisibilityServiceTest" --tests "*.RavagerLifecycleListenerTest" --tests "*.PluginDescriptorTest" --tests "*.PluginIntegrationTest"
.\gradlew.bat test
```

Expected: the plugin loads with no ProtocolLib plugin present, native visibility tests pass, and the full suite has no ProtocolLib imports or tests.

- [x] **Step 7: Update changelog, review, and commit**

Add Paper/Purpur-only support, native visual privacy, accepted sound leakage, and ProtocolLib removal to the existing pending `1.0.0` section. Run `rg -n "ProtocolLib|org\.spigotmc|hub\.spigotmc" build.gradle.kts src` and expect no matches. Run `git diff --check`, scan changed files for credentials, then commit:

```text
git add build.gradle.kts CHANGELOG.md src/main src/test
git commit -m "Replace ProtocolLib with native Paper visibility"
```

### Task 3: Five-Target Build and Packaging Matrix

**Files:**
- Modify: `build.gradle.kts`
- Modify: `src/main/resources/plugin.yml`
- Modify: `src/test/java/dev/nullkeeper/strengthsmptrollitems/PluginDescriptorTest.java`
- Modify: `README.md`
- Modify: `CHANGELOG.md`

**Interfaces:**
- Produces Gradle tasks `compilePaper2611Java`, `compilePaper2612Java`, `compilePaper262Java`, `compilePurpur2612Java`, and `compilePurpur262Java`.
- Produces matching `jar...` tasks plus `verifyDistributables`.
- Produces exactly the five archive classifiers `paper-26.1.1`, `paper-26.1.2`, `paper-26.2`, `purpur-26.1.2`, and `purpur-26.2`.

- [ ] **Step 1: Prove the current build does not meet the matrix**

Run: `.\gradlew.bat clean assemble`

Expected: only the old universal `strength-smp-troll-items-1.0.0.jar` exists; the five required labelled jars are absent.

- [ ] **Step 2: Make the descriptor API version target-expandable**

Change the descriptor to:

```yaml
api-version: '${apiVersion}'
```

Keep ordinary `processResources` expanding `apiVersion` to `26.1` so MockBukkit descriptor tests remain deterministic. Keep the descriptor test expecting `26.1` and an empty dependency list.

- [ ] **Step 3: Define the pinned target model**

Add this model near the top of `build.gradle.kts`:

```kotlin
data class ServerTarget(
    val id: String,
    val apiVersion: String,
    val coordinate: String,
)

val serverTargets = listOf(
    ServerTarget("paper-26.1.1", "26.1", "io.papermc.paper:paper-api:26.1.1.build.29-alpha"),
    ServerTarget("paper-26.1.2", "26.1", "io.papermc.paper:paper-api:26.1.2.build.74-stable"),
    ServerTarget("paper-26.2", "26.2", "io.papermc.paper:paper-api:26.2.build.87-stable"),
    ServerTarget("purpur-26.1.2", "26.1", "org.purpurmc.purpur:purpur-api:26.1.2.build.2592-stable"),
    ServerTarget("purpur-26.2", "26.2", "org.purpurmc.purpur:purpur-api:26.2.build.2618-stable"),
)
```

Add `maven("https://repo.purpurmc.org/snapshots")` beside the Paper repository.

- [ ] **Step 4: Register isolated compile, resource, and jar tasks**

For each target, create a resolvable API configuration containing only its coordinate. Register a `JavaCompile` with the main Java source and that target classpath, a `ProcessResources` expanding `version` and target `apiVersion`, and a `Jar` with the target classifier. Use Java release 25 for every compiler.

```kotlin
val targetJarTasks = serverTargets.map { target ->
    val suffix = target.id.split('-', '.').joinToString("") { segment ->
        segment.replaceFirstChar { character -> character.uppercase() }
    }
    val api = configurations.create("${suffix.replaceFirstChar { it.lowercase() }}Api") {
        isCanBeConsumed = false
        isCanBeResolved = true
    }
    dependencies.add(api.name, target.coordinate)

    val compile = tasks.register<JavaCompile>("compile${suffix}Java") {
        source(sourceSets.main.get().java)
        classpath = api
        destinationDirectory.set(layout.buildDirectory.dir("classes/targets/${target.id}"))
        options.encoding = "UTF-8"
        options.release.set(25)
    }
    val resources = tasks.register<ProcessResources>("process${suffix}Resources") {
        from(sourceSets.main.get().resources)
        destinationDir = layout.buildDirectory.dir("resources/targets/${target.id}").get().asFile
        inputs.property("version", project.version.toString())
        inputs.property("apiVersion", target.apiVersion)
        filesMatching("plugin.yml") {
            expand("version" to project.version.toString(), "apiVersion" to target.apiVersion)
        }
    }
    tasks.register<Jar>("jar${suffix}") {
        dependsOn(compile, resources)
        archiveBaseName.set("strength-smp-troll-items")
        archiveClassifier.set(target.id)
        from(compile.flatMap { it.destinationDirectory })
        from(resources)
        manifest {
            attributes(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
            )
        }
    }
}
```

Disable the generic `jar` task, make `assemble` depend on all five target jars, and add `verifyDistributables` that fails unless `build/libs` contains exactly the five expected `1.0.0` jar names and none containing `sources`:

```kotlin
tasks.jar {
    enabled = false
}

tasks.assemble {
    dependsOn(targetJarTasks)
}

val expectedDistributables = serverTargets
    .map { target -> "strength-smp-troll-items-${project.version}-${target.id}.jar" }
    .toSet()

tasks.register("verifyDistributables") {
    dependsOn(targetJarTasks)
    doLast {
        val actual = layout.buildDirectory.dir("libs").get().asFile
            .listFiles { file -> file.extension == "jar" }
            .orEmpty()
            .map { file -> file.name }
            .toSet()
        check(actual == expectedDistributables) {
            "Expected distributables $expectedDistributables but found $actual"
        }
        check(actual.none { name -> "sources" in name }) {
            "Source jars are not distributable targets: $actual"
        }
    }
}
```

- [ ] **Step 5: Compile and inspect all targets**

Run:

```text
.\gradlew.bat clean test assemble verifyDistributables
Get-ChildItem build\libs -Filter *.jar | Select-Object -ExpandProperty Name
```

Expected names:

```text
strength-smp-troll-items-1.0.0-paper-26.1.1.jar
strength-smp-troll-items-1.0.0-paper-26.1.2.jar
strength-smp-troll-items-1.0.0-paper-26.2.jar
strength-smp-troll-items-1.0.0-purpur-26.1.2.jar
strength-smp-troll-items-1.0.0-purpur-26.2.jar
```

For each jar, run `jar tf` and confirm `plugin.yml`, `config.yml`, and production classes exist while ProtocolLib and test classes do not. Extract `plugin.yml` into an ignored temporary directory under `run/jar-inspection/` and confirm 26.1-family jars declare `26.1`, 26.2 jars declare `26.2`, and all declare version `1.0.0` with no `depend` key.

- [ ] **Step 6: Update build documentation and changelog**

Update README build instructions to say `.\gradlew.bat clean build`, list all five filenames, and state that no universal jar is produced. Add the five-jar matrix to the pending changelog entry.

- [ ] **Step 7: Review and commit**

Run `git diff --check`, review the generated archive list, scan modified files for credentials, and commit:

```text
git add build.gradle.kts src/main/resources/plugin.yml src/test/java/dev/nullkeeper/strengthsmptrollitems/PluginDescriptorTest.java README.md CHANGELOG.md
git commit -m "Build five Paper and Purpur targets"
```

### Task 4: Documentation, Compatibility Smoke Tests, and Release Verification

**Files:**
- Modify: `README.md`
- Modify: `CHANGELOG.md`
- Modify: `docs/superpowers/specs/2026-08-01-strength-smp-troll-items-design.md`
- Modify: `docs/superpowers/specs/2026-08-02-paper-purpur-native-ravagers-and-damage-ticks-design.md`
- Modify: `docs/superpowers/plans/2026-08-02-paper-purpur-native-ravagers-and-damage-ticks.md`
- Runtime-only ignored files: `run/compatibility/<target>/`

**Interfaces:**
- README is the installation/build/manual-test contract.
- `CHANGELOG.md` contains one pending `1.0.0` section plus historical `0.1.0`.
- Each matching jar must enable without linkage or missing-dependency errors on its target server.

- [ ] **Step 1: Finish README platform and behavior documentation**

Replace Bukkit/Spigot badges and wording with Paper/Purpur. Remove every ProtocolLib installation/download instruction. The Install section must tell users to choose the jar matching their platform and exact version. Document the five supported combinations, the unavailable combinations, accepted Ravager sound leakage, valid damage-tick behavior, and the WorldGuard/protection-plugin cancellation behavior. Preserve the original video and creator links.

- [ ] **Step 2: Audit the pending release notes**

Ensure `## [1.0.0]` describes the entire unreleased change: Paper/Purpur-only breaking support, five separate jars, no ProtocolLib, native Ravager visual privacy with sound limitation, and zero-final-damage valid tick behavior. Keep the historical `## [0.1.0] - 2026-08-01` entry unchanged.

- [ ] **Step 3: Run final automated verification**

Run:

```text
.\gradlew.bat clean build verifyDistributables
git diff --check
rg -n "ProtocolLib|Spigot|CraftBukkit" build.gradle.kts src README.md CHANGELOG.md
```

Expected: build and all tests pass; exactly five jars exist; remaining README/changelog mentions of Spigot/CraftBukkit only explain that they are unsupported; no ProtocolLib references remain in build, production, descriptor, tests, or install instructions.

- [ ] **Step 4: Run the five-server startup matrix**

Use ignored directories under `run/compatibility/`. Download the newest build matching each pinned target from these official endpoints:

```text
https://fill.papermc.io/v3/projects/paper/versions/26.1.1/builds
https://fill.papermc.io/v3/projects/paper/versions/26.1.2/builds
https://fill.papermc.io/v3/projects/paper/versions/26.2/builds
https://api.purpurmc.org/v2/purpur/26.1.2/latest/download
https://api.purpurmc.org/v2/purpur/26.2/latest/download
```

For Paper, select build IDs `29`, `74`, and `87` respectively and download each response's `server:default` artifact URL. Put only the matching plugin jar into that server's `plugins/` directory. Create `eula.txt` containing `eula=true`, launch with Java 25 and `--nogui`, wait for the `Done` line, issue `stop`, and inspect `logs/latest.log`.

Expected for each target: the server reaches `Done`; Strength SMP Troll Items `1.0.0` enables; no `UnknownDependencyException`, `NoClassDefFoundError`, `NoSuchMethodError`, or linkage failure appears. Treat any target failure as a release blocker and fix/rebuild only after analyzing its exact log.

- [ ] **Step 5: Record the three-player manual handoff**

Report that startup smoke tests do not replace the live gameplay matrix. Give the human these exact checks on at least Paper 26.1.1 and Purpur 26.2, followed by smoke repetition on the other three targets:

1. With shooter, target, and outsider, confirm only shooter/target render each Ravager; outsider collision, attack, and interaction are blocked; sound leakage is acceptable.
2. Confirm five additional Ravagers per eligible tagged-projectile/player pair, persistence through restart/logout/death, normal block/world behavior, target-only player damage, and permanent removal when killed.
3. Confirm sword and berry trigger against Survival with maximum armor/Resistance even if final damage is zero.
4. Confirm Creative, Spectator, invulnerable targets, cancelled WorldGuard hits, zero-raw-damage events, and repeated swings during no-damage ticks do not trigger.
5. Confirm scale persistence and complete-stack edible conversion/instant consumption remain unchanged.

- [ ] **Step 6: Run the per-edit security and repository checks**

Scan only files changed by this implementation for private keys, tokens, credentials, or local absolute paths. Confirm no new tracked file type lacks `.gitignore` coverage. Verify `run/`, `build/`, and `.gradle/` remain ignored. Leave `Prompt.txt` untouched and untracked.

- [ ] **Step 7: Review the full unreleased diff and commit final documentation**

Review from `f08885d` through the working tree against both approved specs. Fix all critical/high correctness, security, performance, build-matrix, and documentation findings, then rerun `.\gradlew.bat clean build verifyDistributables`.

Commit only intended tracked files:

```text
git add README.md CHANGELOG.md docs/superpowers/specs docs/superpowers/plans/2026-08-02-paper-purpur-native-ravagers-and-damage-ticks.md
git commit -m "Document Paper and Purpur release matrix"
```

- [ ] **Step 8: Final handoff**

Report the five jar paths, test counts, startup-smoke results per target, any manual multiplayer checks still outstanding, the accepted Ravager sound limitation, ProtocolLib removal, compatibility risks in `RavagerVisibilityService.apply` and the MONITOR damage listeners, and the final clean/dirty Git status excluding the user's `Prompt.txt`.
