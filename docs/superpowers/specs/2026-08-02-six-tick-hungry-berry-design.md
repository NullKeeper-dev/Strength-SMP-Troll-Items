# Six-Tick Hungry Berry Design

**Date:** 2026-08-02

**Status:** Approved

This document updates the Hungry Berry eating behavior in
`2026-08-01-strength-smp-troll-items-design.md`. Where the two documents
conflict, this document is authoritative.

## Goal

Converted stacks behave like fast vanilla food. A player must continuously
hold right-click for six game ticks (0.3 seconds) before exactly one item is
consumed.

## Behavior

- Hungry Berry conversion still permanently marks the target player's complete
  main-hand stack as edible without consuming the attacking berry.
- A converted item uses the vanilla eating animation and requires an
  uninterrupted right-click hold for `edible.consume-delay-ticks`, which
  defaults to `6`.
- Releasing right-click early, changing the active item or hand, or another
  normal use interruption cancels the attempt without consuming an item.
- Completing the hold consumes exactly one item from the active hand.
- Converted items can be eaten at full hunger and restore zero nutrition and
  zero saturation by default.
- Right-clicking a converted placeable or normally usable item eats it instead
  of placing, blocking, charging, or performing its original use action.
- The behavior applies in either hand after conversion. The main-hand
  restriction applies only to the stack selected by the Hungry Berry hit.

## Implementation

Conversion returns a copied stack with the existing persistent edible marker
plus Paper's native `FOOD` and `CONSUMABLE` data components. The food component
stores the configured nutrition and saturation and permits eating at full
hunger. The consumable component uses the eating animation and converts the
configured tick duration to seconds.

The interaction listener denies block interaction for marked stacks while
allowing their native item-use action. This prevents placeable items from
placing and lets the server's ordinary use lifecycle enforce holding,
interruption, animation, and one-item consumption. The old click-triggered
scheduler and manual stack decrement path are removed.

Marked stacks created by the earlier implementation are upgraded with the
native components before use so their permanent edible behavior remains
compatible.

Configuration reloads affect future eating attempts. Item names and lore still
change only when troll items are newly issued.

## Universal Build

The project ships one universal jar for the complete supported matrix:

| Platform | Minecraft version | API version in `plugin.yml` |
| --- | --- | --- |
| Paper | 26.1.1 | 26.1 |
| Paper | 26.1.2 | 26.1 |
| Paper | 26.2 | 26.2 |
| Purpur | 26.1.2 | 26.1 |
| Purpur | 26.2 | 26.2 |

The universal jar compiles against the lowest supported API, Paper 26.1.1, and
declares `api-version: '26.1'`. Its production code must use only API members
verified across all five targets. Purpur supports Paper API plugins, so no
platform-specific compilation or runtime adapter is required.

Paper's 26.1 announcement describes the 26.1 release family. The official
repository does not publish a standalone exact `26.1` server/API artifact, so
the project does not claim a standalone 26.1 server target.

## Verification

Automated tests cover component creation, default and configured durations,
zero/default food values, permanent markers, stack metadata preservation, and
configuration validation.

The source must compile against each of the five exact API targets as a
compatibility check, but only one normal distributable jar is produced. That
jar must pass a startup smoke test on every target. Manual tests on every
target cover:

1. Six-tick uninterrupted main-hand and offhand eating.
2. Early release and item-switch cancellation without consumption.
3. Eating at full hunger with zero nutrition and saturation.
4. Converted blocks, bows, shields, and ordinary items using eating instead of
   their original right-click behavior.
5. Exactly one item consumed per completed use, with the remaining stack still
   edible.
6. A previously marked stack gaining the new native behavior.
