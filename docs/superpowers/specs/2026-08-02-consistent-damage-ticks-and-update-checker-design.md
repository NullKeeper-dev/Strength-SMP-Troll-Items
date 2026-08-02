# Consistent Damage Ticks and Update Checker Design

**Date:** 2026-08-02
**Status:** Approved for planning
**Amends:** `2026-08-01-strength-smp-troll-items-design.md` and
`2026-08-02-paper-purpur-native-ravagers-and-damage-ticks-design.md`

## Damage-Tick Correction

An uncancelled `EntityDamageByEntityEvent` with positive raw damage is the
authority that a damage tick occurred. The plugin will no longer reject an
event because the target's `noDamageTicks` value is positive. That additional
check can suppress legitimate damage events Paper has already accepted and is
the cause of inconsistent Resizing Sword activation.

The shared policy continues rejecting cancelled events, zero raw damage,
explicitly invulnerable entities, and Creative or Spectator players. Final
health damage remains irrelevant, so armor, enchantments, and Resistance do
not suppress troll effects. This shared correction applies to the Resizing
Sword, Hungry Berry, and Spooky Crossbow.

The Resizing Sword changes every living target by the configured step on every
qualifying direct main-hand melee hit. Standing grows and sneaking shrinks.

## Modrinth Update Checker

The plugin checks the public Modrinth project
`strength-smp-troll-items` asynchronously after enable. It requests the
project's published versions from the Modrinth v2 API with an identifying user
agent and no authentication token. Network work never blocks the server
thread.

The checker compares the plugin's Gradle-provided semantic version with the
highest valid semantic version returned by Modrinth. Invalid version strings
are ignored. An empty, malformed, timed-out, non-successful, or otherwise
failed response logs one warning and does not affect gameplay or plugin
startup.

`update-checker.enabled` defaults to `true`. Setting it to `false` disables the
request and all update notifications. The result is cached for the current
plugin runtime; no periodic polling is required.

Players with `trollitems.update-notify`, defaulting to operators, receive an
outdated-version notice after joining. Each eligible player is notified at
most once per server restart. The notice includes the installed version,
latest version, Modrinth page, and a second line explaining that
`update-checker.enabled: false` disables the alert system. If the asynchronous
result is not ready when a player joins, that join produces no message; a
later join after the result is ready can notify them.

## Configuration and Messages

The following Bukkit YAML values are added:

- `update-checker.enabled: true`
- `messages.update-available`, with `{current}`, `{latest}`, and `{url}`
  placeholders
- `messages.update-disable-hint`

Reloading configuration updates the enabled flag and messages. Enabling the
checker through reload starts a check when no successful result is cached;
disabling it immediately suppresses join notifications.

## Verification

MockBukkit tests cover qualifying hits while `noDamageTicks` is positive,
existing rejection rules, configuration defaults, notification permission and
once-per-restart behavior. Isolated tests cover semantic-version ordering,
Modrinth response extraction, disabled checks, outdated/up-to-date results,
and failures. HTTP transport is injected so tests make no live network calls.

The universal jar must compile against all five supported Paper/Purpur APIs.
Each target must start, enable the plugin, reach ready, and stop cleanly. A
human must retest repeated Resizing Sword hits on 26.2 because MockBukkit cannot
prove live damage timing.

## Compatibility Risk

`ResizingSwordListener.onDamage`, `HungryBerryListener.onDamage`, and
`SpookyCrossbowListener.onDamage` continue to respect cancellations visible at
`MONITOR`. Protection plugins remain authoritative. Another plugin that
incorrectly changes cancellation state at `MONITOR` may create listener-order
dependent behavior.
