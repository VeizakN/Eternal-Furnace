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
