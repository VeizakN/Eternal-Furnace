package com.eternalfurnace.mixin;

import com.eternalfurnace.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceBlockEntityMixin {
    private static final int HELLFIRE_BURN_TIME_TICKS = 2;
    private static final int SLOWDOWN_NUMERATOR = 9;
    private static final int SLOWDOWN_DENOMINATOR = 5;

    /**
     * Hellfire replaces the next fuel burn whenever the furnace has no active burn time.
     * Fuel that is already burning is allowed to finish, but an item waiting in the fuel
     * slot is not consumed once Hellfire can take over. This preserves the 1.20.1 behavior.
     *
     * We inject at HEAD instead of relying on a bytecode ordinal. Minecraft 26.2
     * decrements the burn timer before it validates the recipe, so a value of two
     * becomes the one live Hellfire tick that vanilla observes later in this tick.
     *
     * The recipe and result-slot checks deliberately mirror vanilla serverTick.
     * Supplying burn time before those checks would leave the block visually lit
     * for an invalid input or a recipe whose output cannot fit.
     */
    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void eternalFurnace$applyHellfire(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            AbstractFurnaceBlockEntity entity,
            CallbackInfo callbackInfo
    ) {
        if (entity.getType() != BlockEntityTypes.FURNACE) {
            return;
        }

        AbstractFurnaceAccessor accessor = (AbstractFurnaceAccessor) entity;
        boolean hasHellfire = level.getBlockState(pos.below()).is(ModBlocks.HELLFIRE_NETHERRACK.get());
        int litTimeRemaining = accessor.eternalFurnace$getLitTimeRemaining();
        boolean wasPoweredByHellfire = accessor.eternalFurnace$getLitTotalTime() == HELLFIRE_BURN_TIME_TICKS;

        // The overwhelmingly common vanilla path must not perform a second recipe
        // lookup. We only inspect the recipe when Hellfire is present or when its
        // persisted marker needs to restore vanilla timing after removal.
        if (!hasHellfire && !wasPoweredByHellfire) {
            return;
        }

        // An ordinary fuel burn owns the furnace until its last effective tick.
        if (hasHellfire && litTimeRemaining > 1 && !wasPoweredByHellfire) {
            return;
        }

        CookingState cookingState = getCookingState(level, entity);
        if (cookingState == null) {
            return;
        }

        int normalCookTime = Math.max(1, cookingState.recipe().value().cookingTime());
        int slowCookTime = slowCookTime(normalCookTime);

        if (hasHellfire && litTimeRemaining <= 1 && cookingState.canBurn()) {
            // Changing the timer at HEAD makes vanilla treat the furnace as already
            // lit, so it will not perform its usual block-state transition. The
            // recipe/result checks above make this explicit transition safe.
            if (!state.getValue(AbstractFurnaceBlock.LIT)) {
                level.setBlock(pos, state.setValue(AbstractFurnaceBlock.LIT, true), 3);
            }
            accessor.eternalFurnace$setLitTimeRemaining(HELLFIRE_BURN_TIME_TICKS);
            accessor.eternalFurnace$setLitTotalTime(HELLFIRE_BURN_TIME_TICKS);
            changeCookingTime(accessor, slowCookTime);
            return;
        }

        // litTotalTime == HELLFIRE_BURN_TIME_TICKS is the persistent marker for Hellfire's renewable tick.
        // It also lets removal restore normal timing after a blocked result slot has
        // already made vanilla clear the visible LIT state.
        if (!hasHellfire
                && litTimeRemaining <= 1
                && wasPoweredByHellfire) {
            changeCookingTime(accessor, normalCookTime);
        }
    }

    private static CookingState getCookingState(ServerLevel level, AbstractFurnaceBlockEntity entity) {
        ItemStack ingredient = entity.getItem(0);
        if (ingredient.isEmpty()) {
            return null;
        }

        SingleRecipeInput input = new SingleRecipeInput(ingredient);
        RecipeHolder<? extends AbstractCookingRecipe> recipe = level.recipeAccess()
                .getRecipeFor(RecipeType.SMELTING, input, level)
                .orElse(null);
        if (recipe == null) {
            return null;
        }

        ItemStack result = recipe.value().assemble(input);
        return new CookingState(recipe, !result.isEmpty() && canBurn(entity, result));
    }

    private static boolean canBurn(AbstractFurnaceBlockEntity entity, ItemStack recipeResult) {
        ItemStack currentResult = entity.getItem(2);
        if (currentResult.isEmpty()) {
            return true;
        }
        if (!ItemStack.isSameItemSameComponents(currentResult, recipeResult)) {
            return false;
        }

        int combinedCount = currentResult.getCount() + recipeResult.getCount();
        int maximumCount = Math.min(entity.getMaxStackSize(), recipeResult.getMaxStackSize());
        return combinedCount <= maximumCount;
    }

    private static int slowCookTime(int normalCookTime) {
        long scaled = ((long) normalCookTime * SLOWDOWN_NUMERATOR
                + SLOWDOWN_DENOMINATOR - 1L) / SLOWDOWN_DENOMINATOR;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, scaled));
    }

    private static void changeCookingTime(AbstractFurnaceAccessor accessor, int newTotalTime) {
        int oldTotalTime = accessor.eternalFurnace$getCookingTotalTime();
        if (oldTotalTime == newTotalTime) {
            return;
        }

        int oldProgress = Math.max(0, accessor.eternalFurnace$getCookingTimer());
        int newProgress = 0;

        if (oldTotalTime > 0 && oldProgress > 0) {
            long scaledProgress = (long) oldProgress * newTotalTime / oldTotalTime;
            newProgress = (int) Math.min((long) newTotalTime - 1L, scaledProgress);
        }

        accessor.eternalFurnace$setCookingTimer(newProgress);
        accessor.eternalFurnace$setCookingTotalTime(newTotalTime);
    }

    private record CookingState(RecipeHolder<? extends AbstractCookingRecipe> recipe, boolean canBurn) {
    }
}
