package com.eternalfurnace.test;

import com.eternalfurnace.EternalFurnaceMod;
import com.eternalfurnace.init.ModBlocks;
import com.eternalfurnace.init.ModItems;
import com.eternalfurnace.mixin.AbstractFurnaceAccessor;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.entity.SmokerBlockEntity;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** Development-only GameTests registered when NeoForge enables its GameTest registries. */
public final class ModGameTests {
    private static final int HELLFIRE_BURN_TIME_TICKS = 2;
    private static final BlockPos FURNACE_POS = new BlockPos(1, 2, 1);
    private static final BlockPos HEAT_POS = FURNACE_POS.below();
    private static final BlockPos SECOND_FURNACE_POS = new BlockPos(2, 2, 1);
    private static final BlockPos SECOND_HEAT_POS = SECOND_FURNACE_POS.below();
    private static final Map<Identifier, TestDefinition> TESTS = new LinkedHashMap<>();

    static {
        add("smelts_without_fuel", 400, ModGameTests::smeltsWithoutFuel);
        add("does_not_consume_waiting_fuel", 400, ModGameTests::doesNotConsumeWaitingFuel);
        add("burning_fuel_finishes_before_hellfire", 30, ModGameTests::burningFuelFinishesBeforeHellfire);
        add("empty_input_does_not_light", 20, ModGameTests::emptyInputDoesNotLight);
        add("invalid_input_does_not_light", 20, ModGameTests::invalidInputDoesNotLight);
        add("blocked_result_does_not_light", 20, ModGameTests::blockedResultDoesNotLight);
        add("blocked_during_work_stops_safely", 40, ModGameTests::blockedDuringWorkStopsSafely);
        add("heat_mode_rescales_progress", 20, ModGameTests::heatModeRescalesProgress);
        add("removal_rescales_progress", 20, ModGameTests::removalRescalesProgress);
        add("input_change_is_safe", 420, ModGameTests::inputChangeIsSafe);
        add("different_recipe_times_switch_safely", 420, ModGameTests::differentRecipeTimesSwitchSafely);
        add("multiple_furnaces_are_independent", 400, ModGameTests::multipleFurnacesAreIndependent);
        add("save_reload_preserves_work", 20, ModGameTests::saveReloadPreservesWork);
        add("gui_flame_indicator_is_lit", 20, ModGameTests::guiFlameIndicatorIsLit);
        add("recipe_damages_flint_and_steel", 20, ModGameTests::recipeDamagesFlintAndSteel);
        add("player_crafting_damages_flint_and_steel", 20, ModGameTests::playerCraftingDamagesFlintAndSteel);
        add("smoker_is_unchanged", 20, ModGameTests::smokerIsUnchanged);
        add("blast_furnace_is_unchanged", 20, ModGameTests::blastFurnaceIsUnchanged);
    }

    private ModGameTests() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModGameTests::registerTests);
    }

    private static void add(String name, int maxTicks, Consumer<GameTestHelper> function) {
        TESTS.put(id(name), new TestDefinition(maxTicks, function));
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                id("default_environment"),
                new TestEnvironmentDefinition.AllOf()
        );
        for (Map.Entry<Identifier, TestDefinition> entry : TESTS.entrySet()) {
            TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                    environment,
                    Identifier.withDefaultNamespace("empty"),
                    entry.getValue().maxTicks(),
                    0,
                    true
            );
            event.registerTest(
                    entry.getKey(),
                    new DirectGameTestInstance(data, entry.getValue().function())
            );
        }
    }

    private static void smeltsWithoutFuel(GameTestHelper helper) {
        FurnaceBlockEntity furnace = placeFurnace(helper, true);
        furnace.setItem(0, new ItemStack(Items.RAW_IRON));
        AbstractFurnaceAccessor accessor = (AbstractFurnaceAccessor) furnace;

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(accessor.eternalFurnace$getCookingTimer() > 0,
                        "Hellfire furnace has not started cooking yet"))
                .thenExecute(() -> helper.assertTrue(
                        helper.getBlockState(FURNACE_POS).getValue(AbstractFurnaceBlock.LIT),
                        "A valid Hellfire-powered recipe did not visually light the furnace"
                ))
                .thenWaitUntil(() -> {
                    helper.assertTrue(furnace.getItem(1).isEmpty(), "Hellfire inserted fuel into an empty fuel slot");
                    helper.assertTrue(
                            furnace.getItem(2).is(Items.IRON_INGOT) && furnace.getItem(2).getCount() == 1,
                            "Hellfire furnace did not produce exactly one iron ingot"
                    );
                })
                .thenSucceed();
    }

    private static void doesNotConsumeWaitingFuel(GameTestHelper helper) {
        FurnaceBlockEntity furnace = placeFurnace(helper, true);
        furnace.setItem(0, new ItemStack(Items.RAW_IRON));
        furnace.setItem(1, new ItemStack(Items.COAL));
        AbstractFurnaceAccessor accessor = (AbstractFurnaceAccessor) furnace;

        helper.succeedWhen(() -> {
            helper.assertTrue(
                    furnace.getItem(1).is(Items.COAL) && furnace.getItem(1).getCount() == 1,
                    "Hellfire consumed fuel that was waiting in the fuel slot"
            );
            helper.assertTrue(furnace.getItem(2).is(Items.IRON_INGOT),
                    "Hellfire furnace did not finish smelting: progress="
                            + accessor.eternalFurnace$getCookingTimer()
                            + "/" + accessor.eternalFurnace$getCookingTotalTime()
                            + ", lit=" + accessor.eternalFurnace$getLitTimeRemaining()
                            + "/" + accessor.eternalFurnace$getLitTotalTime());
        });
    }

    private static void burningFuelFinishesBeforeHellfire(GameTestHelper helper) {
        FurnaceBlockEntity furnace = placeFurnace(helper, false);
        furnace.setItem(0, new ItemStack(Items.RAW_IRON));
        furnace.setItem(1, new ItemStack(Items.COAL));
        AbstractFurnaceAccessor accessor = (AbstractFurnaceAccessor) furnace;

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(accessor.eternalFurnace$getLitTimeRemaining() > 0,
                        "Coal has not started burning yet"))
                .thenExecute(() -> {
                    helper.assertTrue(furnace.getItem(1).isEmpty(), "Coal was not consumed as ordinary fuel");
                    helper.assertTrue(accessor.eternalFurnace$getLitTotalTime() > HELLFIRE_BURN_TIME_TICKS,
                            "Coal did not start an ordinary burn");
                    helper.assertTrue(accessor.eternalFurnace$getCookingTotalTime() == 200,
                            "Ordinary fuel did not use vanilla timing");
                    helper.assertTrue(accessor.eternalFurnace$getCookingTimer() > 0,
                            "Ordinary fuel did not start cooking");
                    helper.assertTrue(helper.getBlockState(FURNACE_POS).getValue(AbstractFurnaceBlock.LIT),
                            "Ordinary fuel did not visually light the furnace");
                    helper.setBlock(HEAT_POS, ModBlocks.HELLFIRE_NETHERRACK.get());
                    // Shorten a proven coal burn, rather than waiting for all 1600 ticks.
                    accessor.eternalFurnace$setLitTimeRemaining(4);
                    accessor.eternalFurnace$setCookingTimer(50);
                })
                .thenWaitUntil(() -> helper.assertTrue(accessor.eternalFurnace$getLitTimeRemaining() < 4,
                        "The active coal burn has not ticked yet"))
                .thenExecute(() -> {
                    helper.assertTrue(accessor.eternalFurnace$getLitTimeRemaining() == 3,
                            "Hellfire changed the active coal burn timer");
                    helper.assertTrue(accessor.eternalFurnace$getCookingTotalTime() == 200,
                            "Hellfire interrupted fuel that was still burning");
                    helper.assertTrue(accessor.eternalFurnace$getLitTotalTime() > HELLFIRE_BURN_TIME_TICKS,
                            "Hellfire replaced active fuel too early");
                    helper.assertTrue(accessor.eternalFurnace$getCookingTimer() == 51,
                            "Ordinary fuel did not preserve cooking progress");
                })
                .thenWaitUntil(() -> helper.assertTrue(accessor.eternalFurnace$getLitTimeRemaining() <= 1,
                        "The active coal burn has not reached its last effective tick"))
                .thenExecute(() -> {
                    helper.assertTrue(accessor.eternalFurnace$getLitTotalTime() > HELLFIRE_BURN_TIME_TICKS,
                            "Hellfire took over before the last ordinary fuel tick finished");
                    helper.assertTrue(accessor.eternalFurnace$getCookingTotalTime() == 200,
                            "Hellfire changed timing before ordinary fuel finished");
                    helper.assertTrue(accessor.eternalFurnace$getCookingTimer() == 53,
                            "Ordinary fuel lost progress before the handoff");
                })
                .thenWaitUntil(() -> helper.assertTrue(
                        accessor.eternalFurnace$getLitTotalTime() == HELLFIRE_BURN_TIME_TICKS,
                        "Hellfire did not take over when coal ended"))
                .thenExecute(() -> {
                    helper.assertTrue(accessor.eternalFurnace$getCookingTotalTime() == 360,
                            "Hellfire timing was not applied after coal ended");
                    // Three ordinary ticks: 50 -> 53; then floor(53 * 360 / 200) + 1.
                    helper.assertTrue(accessor.eternalFurnace$getCookingTimer() == 96,
                            "Progress was not proportionally preserved when Hellfire took over");
                    helper.assertTrue(furnace.getItem(1).isEmpty(), "Hellfire created or retained consumed fuel");
                    helper.assertTrue(furnace.getItem(0).is(Items.RAW_IRON) && furnace.getItem(0).getCount() == 1,
                            "Fuel handoff consumed the unfinished input");
                    helper.assertTrue(furnace.getItem(2).isEmpty(), "Fuel handoff produced premature output");
                })
                .thenSucceed();
    }

    private static void emptyInputDoesNotLight(GameTestHelper helper) {
        FurnaceBlockEntity furnace = placeFurnace(helper, true);

        checkAfterIdleTick(helper, furnace, () -> helper.assertFalse(
                helper.getBlockState(FURNACE_POS).getValue(AbstractFurnaceBlock.LIT), "Empty input lit the furnace"
        ));
    }

    private static void invalidInputDoesNotLight(GameTestHelper helper) {
        FurnaceBlockEntity furnace = placeFurnace(helper, true);
        furnace.setItem(0, new ItemStack(Items.DIRT));

        checkAfterIdleTick(helper, furnace, () -> {
            helper.assertFalse(helper.getBlockState(FURNACE_POS).getValue(AbstractFurnaceBlock.LIT), "Invalid input lit the furnace");
            helper.assertTrue(furnace.getItem(2).isEmpty(), "Invalid input produced an item");
        });
    }

    private static void blockedResultDoesNotLight(GameTestHelper helper) {
        FurnaceBlockEntity furnace = placeFurnace(helper, true);
        furnace.setItem(0, new ItemStack(Items.RAW_IRON));
        furnace.setItem(2, new ItemStack(Items.IRON_INGOT, 64));

        checkAfterIdleTick(helper, furnace, () -> {
            helper.assertFalse(helper.getBlockState(FURNACE_POS).getValue(AbstractFurnaceBlock.LIT), "Blocked output lit the furnace");
            helper.assertTrue(furnace.getItem(0).getCount() == 1, "Blocked output consumed the input");
            helper.assertTrue(furnace.getItem(2).getCount() == 64, "Blocked output duplicated or removed items");
        });
    }

    private static void blockedDuringWorkStopsSafely(GameTestHelper helper) {
        FurnaceBlockEntity furnace = placeFurnace(helper, true);
        furnace.setItem(0, new ItemStack(Items.RAW_IRON));
        AbstractFurnaceAccessor accessor = (AbstractFurnaceAccessor) furnace;
        int[] progressWhenBlocked = new int[1];

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(accessor.eternalFurnace$getCookingTimer() >= 10,
                        "Furnace has not accumulated partial cooking progress yet"))
                .thenExecute(() -> {
                    helper.assertTrue(helper.getBlockState(FURNACE_POS).getValue(AbstractFurnaceBlock.LIT),
                            "Furnace was not working before its output was blocked");
                    progressWhenBlocked[0] = accessor.eternalFurnace$getCookingTimer();
                    furnace.setItem(2, new ItemStack(Items.IRON_INGOT, 64));
                })
                .thenWaitUntil(() -> helper.assertTrue(accessor.eternalFurnace$getLitTimeRemaining() == 0,
                        "The blocked furnace has not stopped burning yet"))
                .thenExecute(() -> {
                    helper.assertFalse(helper.getBlockState(FURNACE_POS).getValue(AbstractFurnaceBlock.LIT),
                            "A newly blocked output left the furnace lit");
                    helper.assertTrue(accessor.eternalFurnace$getCookingTimer() < progressWhenBlocked[0],
                            "Cooking progress continued while the output was blocked");
                    helper.assertTrue(furnace.getItem(0).is(Items.RAW_IRON) && furnace.getItem(0).getCount() == 1,
                            "A newly blocked output consumed input");
                    helper.assertTrue(furnace.getItem(2).is(Items.IRON_INGOT) && furnace.getItem(2).getCount() == 64,
                            "A newly blocked output duplicated or removed items");
                    helper.assertTrue(furnace.getItem(1).isEmpty(), "A newly blocked output changed the fuel slot");
                })
                .thenSucceed();
    }

    private static void heatModeRescalesProgress(GameTestHelper helper) {
        FurnaceBlockEntity furnace = placeFurnace(helper, true);
        furnace.setItem(0, new ItemStack(Items.RAW_IRON));
        AbstractFurnaceAccessor accessor = (AbstractFurnaceAccessor) furnace;
        accessor.eternalFurnace$setCookingTotalTime(200);
        accessor.eternalFurnace$setCookingTimer(50);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(accessor.eternalFurnace$getCookingTimer() != 50,
                        "Furnace has not ticked since Hellfire was added"))
                .thenExecute(() -> {
                    int progress = accessor.eternalFurnace$getCookingTimer();
                    helper.assertTrue(accessor.eternalFurnace$getLitTotalTime() == HELLFIRE_BURN_TIME_TICKS,
                            "Furnace did not enter Hellfire mode");
                    helper.assertTrue(accessor.eternalFurnace$getCookingTotalTime() == 360,
                            "Hellfire did not apply the 1.8x total time");
                    // 50 / 200 becomes 90 / 360, followed by one vanilla cooking tick.
                    helper.assertTrue(progress == 91,
                            "Progress was not proportionally rescaled to Hellfire timing: " + progress);
                })
                .thenSucceed();
    }

    private static void removalRescalesProgress(GameTestHelper helper) {
        FurnaceBlockEntity furnace = placeFurnace(helper, true);
        furnace.setItem(0, new ItemStack(Items.RAW_IRON));
        AbstractFurnaceAccessor accessor = (AbstractFurnaceAccessor) furnace;
        accessor.eternalFurnace$setLitTimeRemaining(1);
        accessor.eternalFurnace$setLitTotalTime(HELLFIRE_BURN_TIME_TICKS);
        accessor.eternalFurnace$setCookingTotalTime(360);
        accessor.eternalFurnace$setCookingTimer(90);
        helper.setBlock(FURNACE_POS, helper.getBlockState(FURNACE_POS).setValue(AbstractFurnaceBlock.LIT, true));
        helper.assertTrue(accessor.eternalFurnace$getLitTotalTime() == HELLFIRE_BURN_TIME_TICKS
                        && accessor.eternalFurnace$getCookingTotalTime() == 360
                        && accessor.eternalFurnace$getCookingTimer() == 90,
                "Removal fixture was not in the expected Hellfire-powered state");
        helper.setBlock(HEAT_POS, Blocks.AIR);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(accessor.eternalFurnace$getLitTimeRemaining() == 0,
                        "Furnace has not ticked since Hellfire was removed"))
                .thenExecute(() -> {
                    int progress = accessor.eternalFurnace$getCookingTimer();
                    helper.assertTrue(accessor.eternalFurnace$getCookingTotalTime() == 200,
                            "Removing Hellfire did not restore normal total time");
                    // 90 / 360 becomes 50 / 200; unheated vanilla then cools by two.
                    helper.assertTrue(progress == 48,
                            "Progress was not proportionally rescaled after removal: " + progress);
                    helper.assertFalse(helper.getBlockState(FURNACE_POS).getValue(AbstractFurnaceBlock.LIT),
                            "Furnace stayed lit after Hellfire removal");
                    helper.assertTrue(furnace.getItem(1).isEmpty(), "Hellfire removal changed the fuel slot");
                })
                .thenSucceed();
    }

    private static void inputChangeIsSafe(GameTestHelper helper) {
        FurnaceBlockEntity furnace = placeFurnace(helper, true);
        furnace.setItem(0, new ItemStack(Items.RAW_IRON));
        AbstractFurnaceAccessor accessor = (AbstractFurnaceAccessor) furnace;

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(accessor.eternalFurnace$getCookingTimer() > 0,
                        "Original input has not started cooking yet"))
                .thenExecute(() -> furnace.setItem(0, new ItemStack(Items.RAW_GOLD)))
                .thenWaitUntil(() -> {
                    helper.assertTrue(furnace.getItem(0).isEmpty(), "Replacement input was not consumed exactly once");
                    helper.assertTrue(furnace.getItem(1).isEmpty(), "Changing input inserted or consumed fuel");
                    helper.assertTrue(
                            furnace.getItem(2).is(Items.GOLD_INGOT) && furnace.getItem(2).getCount() == 1,
                            "Changing input produced the wrong item or duplicated output"
                    );
                })
                .thenSucceed();
    }

    private static void differentRecipeTimesSwitchSafely(GameTestHelper helper) {
        RecipeHolder<? extends AbstractCookingRecipe> fastRecipe = helper.getLevel().recipeAccess()
                .getRecipeFor(
                        RecipeType.SMELTING,
                        new SingleRecipeInput(new ItemStack(Items.STICK)),
                        helper.getLevel()
                )
                .orElseThrow(() -> new AssertionError("GameTest-only 100-tick smelting recipe was not loaded"));
        helper.assertTrue(fastRecipe.value().cookingTime() == 100,
                "GameTest recipe has an unexpected cooking time");

        FurnaceBlockEntity furnace = placeFurnace(helper, true);
        furnace.setItem(0, new ItemStack(Items.STICK));
        AbstractFurnaceAccessor accessor = (AbstractFurnaceAccessor) furnace;

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(accessor.eternalFurnace$getCookingTimer() > 0,
                        "The first recipe did not begin cooking"))
                .thenExecute(() -> {
                    helper.assertTrue(accessor.eternalFurnace$getCookingTotalTime() == 180,
                            "Hellfire did not scale the 100-tick recipe to 180 ticks");
                    furnace.setItem(0, new ItemStack(Items.RAW_IRON));
                    helper.assertTrue(accessor.eternalFurnace$getCookingTotalTime() == 200,
                            "Changing input did not select the new recipe's vanilla time");
                    helper.assertTrue(accessor.eternalFurnace$getCookingTimer() == 0,
                            "Vanilla progress was incorrectly carried into a different recipe");
                })
                .thenWaitUntil(() -> helper.assertTrue(accessor.eternalFurnace$getCookingTimer() > 0,
                        "The replacement recipe did not start from fresh progress"))
                .thenExecute(() -> {
                    helper.assertTrue(accessor.eternalFurnace$getCookingTotalTime() == 360,
                            "Hellfire did not scale the replacement 200-tick recipe to 360 ticks");
                    helper.assertTrue(accessor.eternalFurnace$getCookingTimer() == 1,
                            "The replacement recipe carried stale progress into its first cooking tick");
                })
                .thenWaitUntil(() -> {
                    helper.assertTrue(furnace.getItem(0).isEmpty(), "Replacement input was not consumed");
                    helper.assertTrue(furnace.getItem(2).is(Items.IRON_INGOT) && furnace.getItem(2).getCount() == 1,
                            "Switching recipes produced a stale or duplicated result");
                })
                .thenSucceed();
    }

    private static void multipleFurnacesAreIndependent(GameTestHelper helper) {
        FurnaceBlockEntity first = placeFurnace(helper, true);
        helper.setBlock(SECOND_HEAT_POS, ModBlocks.HELLFIRE_NETHERRACK.get());
        helper.setBlock(SECOND_FURNACE_POS, Blocks.FURNACE);
        FurnaceBlockEntity second = helper.getBlockEntity(SECOND_FURNACE_POS, FurnaceBlockEntity.class);
        first.setItem(0, new ItemStack(Items.RAW_IRON));
        second.setItem(0, new ItemStack(Items.RAW_GOLD));

        helper.succeedWhen(() -> {
            helper.assertTrue(first.getItem(2).is(Items.IRON_INGOT) && first.getItem(2).getCount() == 1,
                    "First furnace produced an incorrect result");
            helper.assertTrue(second.getItem(2).is(Items.GOLD_INGOT) && second.getItem(2).getCount() == 1,
                    "Second furnace produced an incorrect result");
            helper.assertTrue(first.getItem(1).isEmpty() && second.getItem(1).isEmpty(),
                    "Parallel furnaces unexpectedly used fuel");
        });
    }

    private static void saveReloadPreservesWork(GameTestHelper helper) {
        FurnaceBlockEntity furnace = placeFurnace(helper, true);
        furnace.setItem(0, new ItemStack(Items.RAW_IRON));
        AbstractFurnaceAccessor accessor = (AbstractFurnaceAccessor) furnace;
        accessor.eternalFurnace$setLitTimeRemaining(1);
        accessor.eternalFurnace$setLitTotalTime(HELLFIRE_BURN_TIME_TICKS);
        accessor.eternalFurnace$setCookingTotalTime(360);
        accessor.eternalFurnace$setCookingTimer(90);

        CompoundTag saved = furnace.saveWithFullMetadata(helper.getLevel().registryAccess());
        BlockPos absolutePos = helper.absolutePos(FURNACE_POS);
        BlockEntity loaded = BlockEntity.loadStatic(
                absolutePos,
                helper.getBlockState(FURNACE_POS),
                saved,
                helper.getLevel().registryAccess()
        );
        helper.assertTrue(loaded instanceof FurnaceBlockEntity, "Saved furnace could not be loaded");
        FurnaceBlockEntity reloaded = (FurnaceBlockEntity) loaded;
        AbstractFurnaceAccessor reloadedAccessor = (AbstractFurnaceAccessor) reloaded;
        // Check persisted values before Hellfire has a chance to recreate them.
        helper.assertTrue(reloaded.getItem(0).is(Items.RAW_IRON), "Reload lost the furnace input");
        helper.assertTrue(reloadedAccessor.eternalFurnace$getLitTotalTime() == HELLFIRE_BURN_TIME_TICKS,
                "Reload lost the Hellfire marker");
        helper.assertTrue(reloadedAccessor.eternalFurnace$getCookingTotalTime() == 360, "Reload lost the slowed cook time");
        helper.assertTrue(reloadedAccessor.eternalFurnace$getCookingTimer() == 90, "Reload lost cooking progress");
        helper.getLevel().removeBlockEntity(absolutePos);
        helper.getLevel().setBlockEntity(loaded);

        helper.succeedWhen(() -> {
            helper.assertTrue(reloadedAccessor.eternalFurnace$getCookingTimer() > 90, "Reloaded furnace did not resume work");
            helper.assertTrue(reloaded.getItem(0).is(Items.RAW_IRON), "Reload lost the furnace input");
            helper.assertTrue(reloadedAccessor.eternalFurnace$getLitTotalTime() == HELLFIRE_BURN_TIME_TICKS,
                    "Reload lost the Hellfire marker");
            helper.assertTrue(reloadedAccessor.eternalFurnace$getCookingTotalTime() == 360, "Reload lost the slowed cook time");
        });
    }

    private static void guiFlameIndicatorIsLit(GameTestHelper helper) {
        FurnaceBlockEntity furnace = placeFurnace(helper, true);
        furnace.setItem(0, new ItemStack(Items.RAW_IRON));
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        AbstractFurnaceMenu menu = (AbstractFurnaceMenu) furnace.createMenu(
                1,
                player.getInventory(),
                player
        );

        helper.succeedWhen(() -> {
            helper.assertTrue(menu.isLit(), "FurnaceMenu did not expose the Hellfire flame as lit");
            helper.assertTrue(menu.getLitProgress() > 0.0F,
                    "FurnaceMenu exposed zero flame height while Hellfire was working");
            helper.assertTrue(furnace.getItem(1).isEmpty(),
                    "GUI flame test unexpectedly used a fuel item");
        });
    }

    private static void recipeDamagesFlintAndSteel(GameTestHelper helper) {
        ItemStack flintAndSteel = new ItemStack(Items.FLINT_AND_STEEL);
        CraftingInput input = CraftingInput.of(2, 1, List.of(
                flintAndSteel,
                new ItemStack(Items.NETHERRACK)
        ));
        RecipeHolder<CraftingRecipe> recipe = helper.getLevel().recipeAccess()
                .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .orElseThrow(() -> new AssertionError("Hellfire crafting recipe was not loaded"));
        ItemStack result = recipe.value().assemble(input);
        NonNullList<ItemStack> remaining = recipe.value().getRemainingItems(input);

        helper.assertTrue(result.is(ModItems.HELLFIRE_NETHERRACK.get()) && result.getCount() == 1,
                "Hellfire recipe produced an incorrect result");
        helper.assertTrue(remaining.get(0).is(Items.FLINT_AND_STEEL), "Recipe consumed Flint and Steel");
        helper.assertTrue(remaining.get(0).getDamageValue() == 1, "Recipe did not damage Flint and Steel once");
        helper.assertTrue(remaining.get(1).isEmpty(), "Recipe returned consumed Netherrack");

        ItemStack almostBroken = new ItemStack(Items.FLINT_AND_STEEL);
        almostBroken.setDamageValue(almostBroken.getMaxDamage() - 1);
        CraftingInput finalUse = CraftingInput.of(2, 1, List.of(
                almostBroken,
                new ItemStack(Items.NETHERRACK)
        ));
        helper.assertTrue(recipe.value().getRemainingItems(finalUse).get(0).isEmpty(),
                "The final durability use did not break Flint and Steel");
        helper.succeed();
    }

    private static void playerCraftingDamagesFlintAndSteel(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        AbstractContainerMenu menu = new AbstractContainerMenu(null, 1) {
            @Override
            public ItemStack quickMoveStack(Player ignoredPlayer, int ignoredSlot) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(Player ignoredPlayer) {
                return true;
            }
        };
        TransientCraftingContainer crafting = new TransientCraftingContainer(menu, 2, 1);
        ResultContainer result = new ResultContainer();
        ResultSlot resultSlot = new ResultSlot(player, crafting, result, 0, 0, 0);

        ItemStack flintAndSteel = new ItemStack(Items.FLINT_AND_STEEL);
        flintAndSteel.setDamageValue(7);
        crafting.setItem(0, flintAndSteel);
        crafting.setItem(1, new ItemStack(Items.NETHERRACK));
        RecipeHolder<CraftingRecipe> recipe = helper.getLevel().recipeAccess()
                .getRecipeFor(RecipeType.CRAFTING, crafting.asCraftInput(), helper.getLevel())
                .orElseThrow(() -> new AssertionError("Player crafting path could not resolve the Hellfire recipe"));
        result.setItem(0, recipe.value().assemble(crafting.asCraftInput()));

        ItemStack crafted = resultSlot.remove(1);
        resultSlot.onTake(player, crafted);

        helper.assertTrue(crafted.is(ModItems.HELLFIRE_NETHERRACK.get()) && crafted.getCount() == 1,
                "Player did not receive exactly one crafted Hellfire Netherrack");
        helper.assertTrue(crafting.getItem(0).is(Items.FLINT_AND_STEEL),
                "Player crafting consumed Flint and Steel");
        helper.assertTrue(crafting.getItem(0).getDamageValue() == 8,
                "Player crafting did not apply exactly one durability damage");
        helper.assertTrue(crafting.getItem(1).isEmpty(),
                "Player crafting did not consume Netherrack");
        helper.succeed();
    }

    private static void smokerIsUnchanged(GameTestHelper helper) {
        helper.setBlock(HEAT_POS, ModBlocks.HELLFIRE_NETHERRACK.get());
        helper.setBlock(FURNACE_POS, Blocks.SMOKER);
        SmokerBlockEntity smoker = helper.getBlockEntity(FURNACE_POS, SmokerBlockEntity.class);
        smoker.setItem(0, new ItemStack(Items.BEEF));

        checkAfterIdleTick(helper, smoker, () -> {
            helper.assertFalse(helper.getBlockState(FURNACE_POS).getValue(AbstractFurnaceBlock.LIT), "Hellfire affected a smoker");
            helper.assertTrue(smoker.getItem(2).isEmpty(), "Smoker produced output without fuel");
        });
    }

    private static void blastFurnaceIsUnchanged(GameTestHelper helper) {
        helper.setBlock(HEAT_POS, ModBlocks.HELLFIRE_NETHERRACK.get());
        helper.setBlock(FURNACE_POS, Blocks.BLAST_FURNACE);
        BlastFurnaceBlockEntity blastFurnace = helper.getBlockEntity(FURNACE_POS, BlastFurnaceBlockEntity.class);
        blastFurnace.setItem(0, new ItemStack(Items.RAW_IRON));

        checkAfterIdleTick(helper, blastFurnace, () -> {
            helper.assertFalse(helper.getBlockState(FURNACE_POS).getValue(AbstractFurnaceBlock.LIT),
                    "Hellfire affected a blast furnace");
            helper.assertTrue(blastFurnace.getItem(2).isEmpty(), "Blast furnace produced output without fuel");
        });
    }

    private static void checkAfterIdleTick(GameTestHelper helper, AbstractFurnaceBlockEntity furnace, Runnable assertions) {
        AbstractFurnaceAccessor accessor = (AbstractFurnaceAccessor) furnace;
        // An unheated vanilla furnace clears this sentinel on its first serverTick.
        // A negative test must not pass merely because its initial state is still untouched.
        accessor.eternalFurnace$setCookingTimer(1);
        helper.onEachTick(assertions);
        helper.succeedWhen(() -> {
            helper.assertTrue(accessor.eternalFurnace$getCookingTimer() == 0, "Idle furnace has not ticked yet");
            assertions.run();
        });
    }

    private static FurnaceBlockEntity placeFurnace(GameTestHelper helper, boolean withHellfire) {
        helper.setBlock(HEAT_POS, withHellfire ? ModBlocks.HELLFIRE_NETHERRACK.get() : Blocks.AIR);
        helper.setBlock(FURNACE_POS, Blocks.FURNACE);
        return helper.getBlockEntity(FURNACE_POS, FurnaceBlockEntity.class);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EternalFurnaceMod.MOD_ID, path);
    }

    private record TestDefinition(int maxTicks, Consumer<GameTestHelper> function) {
    }

    /**
     * RegisterGameTestsEvent accepts instances directly, which avoids trying to
     * mutate Minecraft's already-bootstrapped TEST_FUNCTION registry.
     */
    private static final class DirectGameTestInstance extends GameTestInstance {
        private final Consumer<GameTestHelper> function;

        private DirectGameTestInstance(
                TestData<Holder<TestEnvironmentDefinition<?>>> data,
                Consumer<GameTestHelper> function
        ) {
            super(data);
            this.function = function;
        }

        @Override
        public void run(GameTestHelper helper) {
            function.accept(helper);
        }

        @Override
        public com.mojang.serialization.MapCodec<? extends GameTestInstance> codec() {
            // Directly registered development tests are never data-pack decoded.
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("Eternal Furnace direct test");
        }
    }
}
