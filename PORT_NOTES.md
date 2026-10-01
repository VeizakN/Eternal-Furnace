# Eternal Furnace — 1.20.1 → 26.2 port notes

Target: Minecraft 26.2 + NeoForge 26.2.0.88 + Java 25.

## Preserved behavior

- Hellfire Netherrack only affects the vanilla furnace, not smoker/blast furnace.
- Fuel that is already burning is allowed to finish. Once active burn time reaches zero, Hellfire takes over before another fuel item can be consumed (including from a cold start).
- Hellfire is infinite but smelts at 1.8× the normal recipe duration (ceil(normal * 9 / 5)).
- Switching between normal and Hellfire timing preserves the percentage of current cooking progress.
- Recipe remains Flint and Steel + Netherrack → Hellfire Netherrack.
- Existing gray-tinted Netherrack appearance is preserved.

## Changes made

- Forge 47 / Java 17 project converted to NeoForge 26.2 / Java 25.
- Registration converted to `DeferredRegister.Blocks` and `.Items`.
- Removed empty custom block subclass.
- Removed runtime client color handlers; constant gray tint is now model data (`neoforge_data.color`).
- Updated client item definition to `assets/eternalfurnace/items/`.
- Updated singular modern datapack paths (`recipe`, `loot_table`, `tags/block`).
- Updated recipe result to item-stack `id` syntax.
- Furnace fields updated: `litTimeRemaining`, `cookingTimer`, `cookingTotalTime`.
- Recipe lookup now uses the 26.2 `RecipeManager` API and validates that the
  recipe result can actually fit before Hellfire supplies heat.
- Replaced fragile `GETFIELD items ordinal=0` injection with a method `HEAD` injection and explicit state handling.
- Added a durable shapeless recipe serializer so Flint and Steel remains in the
  crafting grid result and takes one durability point.
- Added the Gradle 9.2.1 wrapper used by the official 26.2 ModDevGradle MDK.
- Added development GameTests for fuel handoff, invalid/blocked inputs,
  proportional timing changes, persistence, concurrent furnaces, the crafting
  remainder, and the Furnace-only restriction.
- Added a no-Hellfire fast path before recipe lookup; ordinary furnaces and
  still-burning ordinary fuel no longer pay for the extra recipe query.
- Unified Flint and Steel remainder damage across player crafting, previews and
  automation: components are preserved and exactly one durability is spent.
- Added a GameTest-only 100-tick smelting recipe in an isolated source set to
  verify a real input switch between recipes with different cooking times. It
  is not packaged in the release JAR.
- Added a server-side check of the exact `FurnaceMenu.isLit()` and
  `getLitProgress()` values used to render the GUI flame sprite.

## Test checklist

1. Place Hellfire Netherrack directly below a normal furnace.
2. Smelt an item with an empty fuel slot. Furnace should stay lit and complete at 1.8× normal duration.
3. With Hellfire below a cold furnace, put coal in the fuel slot. Coal should remain unconsumed while Hellfire powers the furnace.
3a. Start a furnace on coal first, then place Hellfire below it. The current coal burn should finish before Hellfire takes over.
4. Remove Hellfire during a partial cook. Progress should keep roughly the same percentage while speed returns to vanilla.
5. Add Hellfire during a partial cook after ordinary fuel expires. Progress should keep roughly the same percentage while duration becomes 1.8×.
6. Verify smoker/blast furnace are unchanged.
7. Verify block item renders gray both in-world and inventory.
8. Verify dedicated server starts without loading any client-only class.

## Build

Use an official NeoForge 26.2 ModDevGradle MDK (or copy the Gradle wrapper files into this folder), then run:

    ./gradlew build

On Windows:

    gradlew.bat build

The build needs a Java 25 toolchain; the standard MDK Foojay resolver can provision it.

## GameTest synchronization audit

The server runs 18 required Eternal Furnace tests plus one default Minecraft test
(19 total). None of the old absolute test ticks were part of a gameplay contract:
they were waits for block-entity updates, which may start later in a fresh chunk.
All registrations and their existing timeouts are retained.

| Test | Previous scheduling assumption | Current observation |
| --- | --- | --- |
| `smelts_without_fuel` | Visually lit by tick 5 | Cooking starts, then LIT is asserted; await exactly one ingot |
| `does_not_consume_waiting_fuel` | Already state-based | Unchanged: await output while the waiting coal remains |
| `burning_fuel_finishes_before_hellfire` | Coal consumed by tick 5; still ordinary fuel on tick 6 | Observe a real coal burn before adding Hellfire; observe the shortened burn countdown and exact proportional handoff |
| `empty_input_does_not_light` | Tick 5 assumed the idle furnace had updated | Observe vanilla clearing an idle progress sentinel; assert no LIT on every observed tick |
| `invalid_input_does_not_light` | Same tick-5 assumption | Same idle observation, retaining no-LIT/no-output assertions |
| `blocked_result_does_not_light` | Same tick-5 assumption | Same idle observation, retaining no-LIT/input/output assertions |
| `blocked_during_work_stops_safely` | Working by tick 20; stopped by tick 25 | Wait for partial work before blocking; observe burn ending and assert reduced progress and unchanged slots |
| `heat_mode_rescales_progress` | First update completed by tick 2 | Observe seeded progress changing; immediately assert 360 total and exactly 91 progress (90 scaled + one cooking tick) |
| `removal_rescales_progress` | First update completed by tick 2 | Remove Hellfire from a marked, visibly lit fixture; observe burn ending; immediately assert 200 total, 48 progress (50 scaled - two cooldown units), and no LIT |
| `input_change_is_safe` | Cooking had started before input replacement on tick 20 | Observe cooking before replacement; await the single replacement result |
| `different_recipe_times_switch_safely` | First recipe working on tick 10; replacement working on tick 12 | Observe both recipes starting; assert 180 -> synchronous 200/reset -> 360 and fresh progress before awaiting output |
| `multiple_furnaces_are_independent` | Already state-based | Unchanged: await both distinct results and empty fuel slots |
| `save_reload_preserves_work` | Reloaded entity resumed by tick 3 | Assert persisted values before reattachment, then await resumed cooking |
| `gui_flame_indicator_is_lit` | Menu values updated by tick 5 | Await the actual menu flame state and positive flame height without fuel |
| `recipe_damages_flint_and_steel` | Synchronous recipe API | Unchanged, including the final durability use |
| `player_crafting_damages_flint_and_steel` | Synchronous server crafting path | Unchanged: real `ResultSlot.onTake` and exactly one durability spent |
| `smoker_is_unchanged` | Idle smoker had updated by tick 5 | Observe an idle tick; retain no-LIT/no-output assertions |
| `blast_furnace_is_unchanged` | Idle blast furnace had updated by tick 5 | Observe an idle tick; retain no-LIT/no-output assertions |

`succeedWhen` (or a sequence's `thenWaitUntil`) only waits for observable states.
Transition assertions run in `thenExecute`, so a wrong value at the first observed
update fails immediately instead of being retried until it happens to match.
The idle sentinel is cooking progress, not supplied heat; vanilla clears it with
no fuel, and `onEachTick` keeps the original negative assertions active while waiting.
The mixin, public mechanics, CI commands and test timeouts are unchanged.

Run both checks with Java 25:

    ./gradlew build
    ./gradlew runGameTestServer
