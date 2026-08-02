# Consistent Damage Ticks and Update Checker Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make every valid Paper damage event activate troll-item effects consistently and notify eligible administrators once per restart when a newer Modrinth release exists.

**Architecture:** `DamageTickPolicy` will trust an uncancelled positive-raw-damage event instead of independently consulting `noDamageTicks`. A small `update` package will isolate semantic-version parsing, Modrinth HTTP transport, cached update state, and join notifications; `RuntimeComponents` will compose it and the reload command will request a refresh after successful configuration reloads.

**Tech Stack:** Java 25, Paper API 26.1.1 baseline, Paper/Purpur compatibility compile tasks, JDK `HttpClient` and `CompletableFuture`, Bukkit YAML, MockBukkit, JUnit 6.

## Global Constraints

- Produce one universal jar for Paper 26.1.1, Paper 26.1.2, Paper 26.2, Purpur 26.1.2, and Purpur 26.2.
- Add no runtime plugin dependency and no authentication token.
- Keep all HTTP work off the server thread and enforce a finite request timeout.
- Respect cancelled damage events and protection plugins.
- Notify only `trollitems.update-notify` holders, defaulting to operators, at most once per server restart.
- Leave the untracked `Prompt.txt` untouched.
- Apply the cumulative unreleased SemVer calculation before the final build.

---

### Task 1: Correct shared damage-tick eligibility

**Files:**
- Modify: `src/test/java/dev/nullkeeper/strengthsmptrollitems/combat/DamageTickPolicyTest.java`
- Modify: `src/test/java/dev/nullkeeper/strengthsmptrollitems/resize/ResizingSwordListenerTest.java`
- Modify: `src/test/java/dev/nullkeeper/strengthsmptrollitems/hungry/HungryBerryListenerTest.java`
- Modify: `src/test/java/dev/nullkeeper/strengthsmptrollitems/ravager/SpookyCrossbowListenerTest.java`
- Modify: `src/main/java/dev/nullkeeper/strengthsmptrollitems/combat/DamageTickPolicy.java`

**Interfaces:**
- Consumes: `boolean DamageTickPolicy.qualifies(EntityDamageByEntityEvent event, LivingEntity target)`.
- Produces: one shared policy where an uncancelled positive raw damage event qualifies even while `target.getNoDamageTicks() > 0`.

- [ ] **Step 1: Replace the obsolete immunity-window policy test with the failing regression**

```java
@Test
void positiveDamageEventQualifiesWhileNoDamageTicksIsPositive() {
    target.setNoDamageTicks(5);

    assertTrue(policy.qualifies(damage(target, 1.0), target));
}
```

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```powershell
.\gradlew.bat test --tests "dev.nullkeeper.strengthsmptrollitems.combat.DamageTickPolicyTest.positiveDamageEventQualifiesWhileNoDamageTicksIsPositive"
```

Expected: FAIL because the current `target.getNoDamageTicks() > 0` branch returns `false`.

- [ ] **Step 3: Remove only the disproven guard**

Change the first policy condition to:

```java
if (event.isCancelled()
        || event.getDamage() <= 0.0
        || target.isInvulnerable()) {
    return false;
}
```

- [ ] **Step 4: Align feature tests with the corrected shared contract**

Remove assertions that classify positive damage events during `noDamageTicks` as rejected. Add this Resizing Sword regression:

```java
@Test
void positiveDamageEventDuringNoDamageTicksStillGrows() {
    target.setNoDamageTicks(5);

    listener.onDamage(damage(attacker, target, 1.0));

    assertEquals(1.05, target.getAttribute(scaleAttribute).getBaseValue());
}
```

Keep cancelled, zero-raw-damage, Creative, Spectator, and explicit-invulnerability rejection assertions for all affected features.

- [ ] **Step 5: Run all combat feature tests and verify GREEN**

```powershell
.\gradlew.bat test --tests "dev.nullkeeper.strengthsmptrollitems.combat.*" --tests "dev.nullkeeper.strengthsmptrollitems.resize.*" --tests "dev.nullkeeper.strengthsmptrollitems.hungry.*" --tests "dev.nullkeeper.strengthsmptrollitems.ravager.SpookyCrossbowListenerTest"
```

Expected: PASS.

- [ ] **Step 6: Review and commit the isolated correction**

```powershell
git diff --check
git add src/main/java/dev/nullkeeper/strengthsmptrollitems/combat/DamageTickPolicy.java src/test/java/dev/nullkeeper/strengthsmptrollitems/combat/DamageTickPolicyTest.java src/test/java/dev/nullkeeper/strengthsmptrollitems/resize/ResizingSwordListenerTest.java src/test/java/dev/nullkeeper/strengthsmptrollitems/hungry/HungryBerryListenerTest.java src/test/java/dev/nullkeeper/strengthsmptrollitems/ravager/SpookyCrossbowListenerTest.java
git commit -m "Fix troll effects on valid damage ticks"
```

---

### Task 2: Add update-checker configuration and permission

**Files:**
- Modify: `src/test/java/dev/nullkeeper/strengthsmptrollitems/config/ConfigLoaderTest.java`
- Modify: `src/test/java/dev/nullkeeper/strengthsmptrollitems/PluginDescriptorTest.java`
- Modify: `src/main/java/dev/nullkeeper/strengthsmptrollitems/config/PluginConfig.java`
- Modify: `src/main/java/dev/nullkeeper/strengthsmptrollitems/config/ConfigLoader.java`
- Modify: `src/main/resources/config.yml`
- Modify: `src/main/resources/plugin.yml`

**Interfaces:**
- Produces: `PluginConfig.UpdateCheckerSettings(boolean enabled)` through `PluginConfig.updateChecker()`.
- Produces: `Messages.updateAvailable()` and `Messages.updateDisableHint()`.
- Produces: permission `trollitems.update-notify`, default `op`.

- [ ] **Step 1: Write failing default and descriptor assertions**

Add to `loadsApprovedDefaultsFromBundledYaml()`:

```java
assertTrue(config.updateChecker().enabled());
assertEquals(
        "&eStrength SMP Troll Items {current} is outdated. Version {latest} is available: {url}",
        config.messages().updateAvailable());
assertEquals(
        "&7Disable update alerts with update-checker.enabled: false in config.yml.",
        config.messages().updateDisableHint());
```

Add to `PluginDescriptorTest`:

```java
assertEquals("op", descriptor.getString("permissions.trollitems.update-notify.default"));
```

- [ ] **Step 2: Run both test classes and verify RED**

```powershell
.\gradlew.bat test --tests "dev.nullkeeper.strengthsmptrollitems.config.ConfigLoaderTest" --tests "dev.nullkeeper.strengthsmptrollitems.PluginDescriptorTest"
```

Expected: compilation or assertion failure because the settings, messages, and permission do not exist.

- [ ] **Step 3: Extend the immutable configuration model and loader**

Add `UpdateCheckerSettings updateChecker` before `Messages messages` in `PluginConfig`, require it non-null, and add:

```java
public record UpdateCheckerSettings(boolean enabled) {}
```

Add the two message fields to `Messages`. Teach `ConfigLoader` to read:

```java
new UpdateCheckerSettings(root.getBoolean("update-checker.enabled", true))
```

Register all three new YAML paths in `KNOWN_PATHS` and supply the exact message defaults asserted by the tests.

- [ ] **Step 4: Add bundled YAML and permission defaults**

Add to `config.yml`:

```yaml
update-checker:
  enabled: true
```

Add under `messages`:

```yaml
update-available: "&eStrength SMP Troll Items {current} is outdated. Version {latest} is available: {url}"
update-disable-hint: "&7Disable update alerts with update-checker.enabled: false in config.yml."
```

Add to `plugin.yml`:

```yaml
trollitems.update-notify:
  description: Receive available-update notices after joining.
  default: op
```

- [ ] **Step 5: Run the focused tests and verify GREEN**

```powershell
.\gradlew.bat test --tests "dev.nullkeeper.strengthsmptrollitems.config.ConfigLoaderTest" --tests "dev.nullkeeper.strengthsmptrollitems.PluginDescriptorTest"
```

- [ ] **Step 6: Review and commit configuration support**

```powershell
git diff --check
git add src/main/java/dev/nullkeeper/strengthsmptrollitems/config src/main/resources/config.yml src/main/resources/plugin.yml src/test/java/dev/nullkeeper/strengthsmptrollitems/config/ConfigLoaderTest.java src/test/java/dev/nullkeeper/strengthsmptrollitems/PluginDescriptorTest.java
git commit -m "Add update notification settings"
```

---

### Task 3: Parse and compare published Modrinth versions

**Files:**
- Create: `src/test/java/dev/nullkeeper/strengthsmptrollitems/update/SemanticVersionTest.java`
- Create: `src/test/java/dev/nullkeeper/strengthsmptrollitems/update/ModrinthVersionParserTest.java`
- Create: `src/main/java/dev/nullkeeper/strengthsmptrollitems/update/SemanticVersion.java`
- Create: `src/main/java/dev/nullkeeper/strengthsmptrollitems/update/ModrinthVersionParser.java`

**Interfaces:**
- Produces: `Optional<SemanticVersion> SemanticVersion.parse(String value)` and natural ordering.
- Produces: `Optional<SemanticVersion> ModrinthVersionParser.latest(String responseBody)`.

- [ ] **Step 1: Write failing semantic-version tests**

```java
@Test
void stableReleaseSortsAfterPrerelease() {
    SemanticVersion beta = SemanticVersion.parse("2.1.0-beta.2").orElseThrow();
    SemanticVersion stable = SemanticVersion.parse("2.1.0").orElseThrow();

    assertTrue(stable.compareTo(beta) > 0);
}

@Test
void parsesOptionalVPrefixAndBuildMetadata() {
    assertEquals(
            SemanticVersion.parse("2.1.0").orElseThrow(),
            SemanticVersion.parse("v2.1.0+paper.26.2").orElseThrow());
}

@Test
void rejectsNonSemanticVersion() {
    assertTrue(SemanticVersion.parse("August release").isEmpty());
}
```

- [ ] **Step 2: Write failing Modrinth response tests**

```java
@Test
void selectsHighestValidVersionNumber() {
    String body = """
            [{"version_number":"2.0.1"},{"version_number":"2.1.0"},{"version_number":"notes"}]
            """;

    assertEquals("2.1.0", ModrinthVersionParser.latest(body).orElseThrow().toString());
}

@Test
void emptyResponseHasNoLatestVersion() {
    assertTrue(ModrinthVersionParser.latest("[]").isEmpty());
}
```

- [ ] **Step 3: Run the new tests and verify RED**

```powershell
.\gradlew.bat test --tests "dev.nullkeeper.strengthsmptrollitems.update.SemanticVersionTest" --tests "dev.nullkeeper.strengthsmptrollitems.update.ModrinthVersionParserTest"
```

Expected: compilation failure because both production types are absent.

- [ ] **Step 4: Implement strict SemVer parsing and ordering**

Use a record that stores `major`, `minor`, `patch`, and immutable prerelease identifiers. Strip only one leading `v`, ignore build metadata for precedence, reject negative/missing numeric components, compare numeric prerelease identifiers numerically, and make a stable release sort above its prereleases.

- [ ] **Step 5: Implement bounded field extraction**

Reject response bodies over 1,048,576 characters. Extract only JSON strings matching the `version_number` field, unescape JSON quote/backslash escapes, parse each through `SemanticVersion.parse`, and return `max(Comparator.naturalOrder())`.

- [ ] **Step 6: Run the new tests and verify GREEN**

```powershell
.\gradlew.bat test --tests "dev.nullkeeper.strengthsmptrollitems.update.*"
```

- [ ] **Step 7: Review and commit the isolated version domain**

```powershell
git diff --check
git add src/main/java/dev/nullkeeper/strengthsmptrollitems/update/SemanticVersion.java src/main/java/dev/nullkeeper/strengthsmptrollitems/update/ModrinthVersionParser.java src/test/java/dev/nullkeeper/strengthsmptrollitems/update/SemanticVersionTest.java src/test/java/dev/nullkeeper/strengthsmptrollitems/update/ModrinthVersionParserTest.java
git commit -m "Add Modrinth version comparison"
```

---

### Task 4: Fetch and cache update status asynchronously

**Files:**
- Create: `src/test/java/dev/nullkeeper/strengthsmptrollitems/update/UpdateCheckerTest.java`
- Create: `src/main/java/dev/nullkeeper/strengthsmptrollitems/update/VersionSource.java`
- Create: `src/main/java/dev/nullkeeper/strengthsmptrollitems/update/ModrinthApiClient.java`
- Create: `src/main/java/dev/nullkeeper/strengthsmptrollitems/update/UpdateChecker.java`
- Create: `src/main/java/dev/nullkeeper/strengthsmptrollitems/update/UpdateInfo.java`

**Interfaces:**
- Produces: `CompletableFuture<String> VersionSource.fetch()`.
- Produces: `void UpdateChecker.refresh()`, `Optional<UpdateInfo> availableUpdate()`, and `void close()`.
- Produces: `UpdateInfo(String current, String latest, String pageUrl)`.

- [ ] **Step 1: Write failing checker behavior tests using an injected source**

```java
@Test
void cachesOutdatedResultFromCompletedSource() {
    VersionSource source = () -> CompletableFuture.completedFuture(
            "[{\"version_number\":\"2.1.0\"}]");
    UpdateChecker checker = checker(true, "2.0.0", source);

    checker.refresh();

    assertEquals("2.1.0", checker.availableUpdate().orElseThrow().latest());
}

@Test
void disabledCheckerDoesNotCallSource() {
    AtomicInteger calls = new AtomicInteger();
    VersionSource source = () -> {
        calls.incrementAndGet();
        return CompletableFuture.completedFuture("[]");
    };

    checker(false, "2.0.0", source).refresh();

    assertEquals(0, calls.get());
}

@Test
void currentOrNewerInstallHasNoAvailableUpdate() {
    UpdateChecker checker = checker(true, "2.1.0", () ->
            CompletableFuture.completedFuture("[{\"version_number\":\"2.1.0\"}]"));

    checker.refresh();

    assertTrue(checker.availableUpdate().isEmpty());
}
```

Also cover exceptional futures, malformed/empty responses, duplicate refresh while in flight, and `close()` suppressing late completion.

- [ ] **Step 2: Run `UpdateCheckerTest` and verify RED**

```powershell
.\gradlew.bat test --tests "dev.nullkeeper.strengthsmptrollitems.update.UpdateCheckerTest"
```

- [ ] **Step 3: Implement the source and cached checker**

`UpdateChecker` receives `Supplier<PluginConfig>`, the current version string, `VersionSource`, and `Logger`. Use immutable `Optional<UpdateInfo>` in an `AtomicReference`, an `AtomicBoolean` for in-flight state, and a closed flag. `refresh()` returns immediately, starts at most one request, caches a successful result, and logs one concise warning for failures without throwing into Bukkit.

- [ ] **Step 4: Implement the Modrinth HTTP client**

Use:

```java
URI.create("https://api.modrinth.com/v2/project/strength-smp-troll-items/version?include_changelog=false")
```

Build a GET request with an eight-second timeout and:

```java
"User-Agent", "NullKeeper-dev/Strength-SMP-Troll-Items/" + pluginVersion
```

Use `HttpClient.sendAsync(..., BodyHandlers.ofString(StandardCharsets.UTF_8))`. Accept only status `200`; other statuses complete exceptionally with a message containing only the status code, never response content.

- [ ] **Step 5: Run all update-package tests and verify GREEN**

```powershell
.\gradlew.bat test --tests "dev.nullkeeper.strengthsmptrollitems.update.*"
```

- [ ] **Step 6: Review and commit the asynchronous update core**

```powershell
git diff --check
git add src/main/java/dev/nullkeeper/strengthsmptrollitems/update src/test/java/dev/nullkeeper/strengthsmptrollitems/update
git commit -m "Add asynchronous Modrinth update checker"
```

---

### Task 5: Notify administrators once and wire reload behavior

**Files:**
- Create: `src/test/java/dev/nullkeeper/strengthsmptrollitems/update/UpdateNotificationListenerTest.java`
- Create: `src/main/java/dev/nullkeeper/strengthsmptrollitems/update/UpdateNotificationListener.java`
- Modify: `src/test/java/dev/nullkeeper/strengthsmptrollitems/command/TrollItemsCommandTest.java`
- Modify: `src/main/java/dev/nullkeeper/strengthsmptrollitems/command/TrollItemsCommand.java`
- Modify: `src/test/java/dev/nullkeeper/strengthsmptrollitems/PluginIntegrationTest.java`
- Modify: `src/main/java/dev/nullkeeper/strengthsmptrollitems/RuntimeComponents.java`

**Interfaces:**
- Produces: `UpdateNotificationListener(Supplier<PluginConfig>, Supplier<Optional<UpdateInfo>>)`.
- Changes: `TrollItemsCommand(ConfigService, GiveItemService, Logger, Runnable successfulReloadHook)`.
- Consumes: `UpdateChecker.refresh()` after startup and successful reload.

- [ ] **Step 1: Write failing join-notification tests**

Create a configured player with `trollitems.update-notify`, fire two `PlayerJoinEvent` instances during the same test, and assert exactly two total messages—the update and disable hint—after the first event and no additional messages after the second. Add separate tests proving no message for missing permission, disabled configuration, or empty update state.

Core assertion:

```java
listener.onJoin(new PlayerJoinEvent(admin, "joined"));
listener.onJoin(new PlayerJoinEvent(admin, "joined again"));

assertEquals(
        ChatColor.YELLOW + "Strength SMP Troll Items 2.0.0 is outdated. Version 2.1.0 is available: https://modrinth.com/project/strength-smp-troll-items",
        admin.nextMessage());
assertEquals(
        ChatColor.GRAY + "Disable update alerts with update-checker.enabled: false in config.yml.",
        admin.nextMessage());
assertNull(admin.nextMessage());
```

- [ ] **Step 2: Run the listener test and verify RED**

```powershell
.\gradlew.bat test --tests "dev.nullkeeper.strengthsmptrollitems.update.UpdateNotificationListenerTest"
```

- [ ] **Step 3: Implement immutable once-per-runtime tracking**

Use `AtomicReference<Set<UUID>>` initialized to `Set.of()`. On join, require the enabled flag, permission, and an available update. Atomically replace the set with `Set.copyOf(...)`; only the thread that adds the UUID sends the two current configured messages through `LegacyText.format`.

- [ ] **Step 4: Add a successful-reload hook test before changing the command**

Construct `TrollItemsCommand` with an `AtomicInteger::incrementAndGet` hook. Assert a successful `/trollitems reload` increments once and a rejected reload does not increment.

Run:

```powershell
.\gradlew.bat test --tests "dev.nullkeeper.strengthsmptrollitems.command.TrollItemsCommandTest"
```

Expected: compilation failure until the constructor and hook behavior exist.

- [ ] **Step 5: Implement the command hook and runtime composition**

Invoke `successfulReloadHook.run()` only after `ConfigService.reloadFromDisk()` succeeds. In `RuntimeComponents.start`:

1. Create `ModrinthApiClient` with JDK `HttpClient` and the dynamic plugin version.
2. Create `UpdateChecker` from `configs::current`.
3. Pass `updateChecker::refresh` to `TrollItemsCommand`.
4. Register `UpdateNotificationListener` with the other listeners.
5. Schedule one guarded next-tick `updateChecker.refresh()` call so MockBukkit startup tests do not make live requests without advancing ticks.
6. Call `updateChecker.close()` during runtime cleanup and failed startup.

Update `PluginIntegrationTest` to assert at least nine registered listeners and advance no scheduler ticks, keeping the integration test network-free.

- [ ] **Step 6: Run command, listener, and integration tests and verify GREEN**

```powershell
.\gradlew.bat test --tests "dev.nullkeeper.strengthsmptrollitems.command.TrollItemsCommandTest" --tests "dev.nullkeeper.strengthsmptrollitems.update.UpdateNotificationListenerTest" --tests "dev.nullkeeper.strengthsmptrollitems.PluginIntegrationTest"
```

- [ ] **Step 7: Review and commit runtime integration**

```powershell
git diff --check
git add src/main/java/dev/nullkeeper/strengthsmptrollitems/RuntimeComponents.java src/main/java/dev/nullkeeper/strengthsmptrollitems/command/TrollItemsCommand.java src/main/java/dev/nullkeeper/strengthsmptrollitems/update/UpdateNotificationListener.java src/test/java/dev/nullkeeper/strengthsmptrollitems/PluginIntegrationTest.java src/test/java/dev/nullkeeper/strengthsmptrollitems/command/TrollItemsCommandTest.java src/test/java/dev/nullkeeper/strengthsmptrollitems/update/UpdateNotificationListenerTest.java
git commit -m "Notify administrators about plugin updates"
```

---

### Task 6: Finalize release metadata, documentation, and verification

**Files:**
- Modify: `gradle.properties`
- Modify: `CHANGELOG.md`
- Modify: `README.md`

**Interfaces:**
- Produces: one versioned universal release jar with dynamic `plugin.yml` metadata.

- [ ] **Step 1: Apply cumulative SemVer**

The committed baseline is `2.0.0`. The update checker is backward-compatible new functionality, so set:

```properties
version=2.1.0
```

Replace the single pending changelog heading with `## [2.1.0]`; retain its existing cumulative content and add bullets for consistent positive damage events and the configurable administrator update notice.

- [ ] **Step 2: Update README behavior and configuration documentation**

Document that every uncancelled positive raw damage event activates the applicable troll effect, add `update-checker.enabled` and `trollitems.update-notify`, describe once-per-restart administrator notices, and keep the build artifact expressed as `strength-smp-troll-items-<version>.jar`.

- [ ] **Step 3: Run the complete clean verification build**

```powershell
.\gradlew.bat clean test check verifyDistributables
```

Expected: all tests pass; all five exact API compile tasks run; exactly `build/libs/strength-smp-troll-items-2.1.0.jar` exists; no sources jar exists.

- [ ] **Step 4: Audit the final artifact**

Verify `plugin.yml` has version `2.1.0`, API version `26.1`, no dependency entries, and the new permission. Verify the entrypoint and update classes exist and no `.java`, ProtocolLib, Paper API, or Purpur API classes are bundled. Record the SHA-256 hash.

- [ ] **Step 5: Run the five-server startup matrix**

Install the exact same jar in Paper 26.1.1, Paper 26.1.2, Paper 26.2, Purpur 26.1.2, and Purpur 26.2. For each target, wait for its ready line, confirm `StrengthSmpTrollItems v2.1.0` enabled, send `stop`, require exit code `0`, and reject plugin errors or exceptions in `logs/latest.log`.

The Modrinth project may not yet expose a public version. A `404` update-check warning is acceptable for this pre-publication smoke matrix if plugin startup and gameplay remain unaffected.

- [ ] **Step 6: Run scoped security and repository checks**

Scan only files changed since commit `a4abd70` for credential patterns. Confirm `.gitignore` still covers `.env`, `local.properties`, `.gradle/`, `build/`, `run/`, and IDE files. Run:

```powershell
git diff --check
git status --short
```

Expected status before the release commit: only intended tracked files plus untouched `?? Prompt.txt`.

- [ ] **Step 7: Commit release documentation and metadata**

```powershell
git add gradle.properties CHANGELOG.md README.md
git commit -m "Prepare 2.1.0 release"
```

- [ ] **Step 8: Hand off manual 26.2 verification**

Ask the user to repeatedly hit players and non-player living entities while standing and sneaking, including targets with armor and Resistance. Every uncancelled positive damage event must move scale exactly `0.05`; Creative, Spectator, invulnerable, cancelled, and zero-raw-damage cases must remain unchanged. Also publish or expose a Modrinth version newer than the installed jar, join twice as an operator, and confirm exactly one two-line alert until the next server restart.
