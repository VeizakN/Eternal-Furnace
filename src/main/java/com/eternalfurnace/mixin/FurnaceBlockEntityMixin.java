package com.eternalfurnace.mixin;

import com.eternalfurnace.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects into AbstractFurnaceBlockEntity.serverTick to implement Hellfire Netherrack behaviour:
 * - Only affects vanilla Furnace (not Blast Furnace or Smoker).
 * - Existing vanilla fuel burns normally and is never shortened by Hellfire Netherrack.
 * - Once vanilla fuel runs out, Hellfire keeps litTime positive and slows the current recipe to
 *   1.8 times its normal cooking time.
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceBlockEntityMixin {

    private static final int SLOWDOWN_NUMERATOR = 9;
    private static final int SLOWDOWN_DENOMINATOR = 5;

    /**
     * Runs after vanilla has captured its previous lit state and decremented litTime, but before it
     * reads the furnace inventory. This lets vanilla's own end-of-tick comparison update LIT.
     */
    @Inject(
            method = "serverTick",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;items:Lnet/minecraft/core/NonNullList;",
                    opcode = Opcodes.GETFIELD,
                    ordinal = 0
            )
    )
    private static void eternalfurnace$applyHellfireAfterFuelTick(
            Level level, BlockPos pos, BlockState state,
            AbstractFurnaceBlockEntity blockEntity, CallbackInfo ci) {

        // Match the exact vanilla block entity type, not subclasses used by other mods.
        if (blockEntity.getType() != BlockEntityType.FURNACE) return;

        AbstractFurnaceAccessor accessor = (AbstractFurnaceAccessor) blockEntity;

        ItemStack input = blockEntity.getItem(0);
        boolean hasInput = !input.isEmpty();
        boolean hasHellfire = hasInput
                && level.getBlockState(pos.below()).is(ModBlocks.HELLFIRE_NETHERRACK.get());

        // At this injection point vanilla has already reduced litTime by one. A positive value is
        // therefore genuine remaining fuel and must not be shortened or replaced.
        boolean usingHellfire = hasHellfire && accessor.getLitTime() <= 0;

        // Usually no work is needed without Hellfire. The second condition handles the one tick in
        // which an active Hellfire furnace loses its support: the block is still visually lit, but
        // vanilla has just decremented the synthetic final burn tick to zero.
        boolean mayNeedVanillaTimeRestore = !hasHellfire
                && hasInput
                && accessor.getLitTime() <= 0
                && state.getValue(AbstractFurnaceBlock.LIT);
        if (!usingHellfire && !mayNeedVanillaTimeRestore) return;

        int normalCookTime = Math.max(
                1,
                AbstractFurnaceAccessor.invokeGetTotalCookTime(level, blockEntity)
        );
        int hellfireCookTime = slowCookTime(normalCookTime);

        if (usingHellfire) {
            // One tick is enough because vanilla's decrement has already happened this tick.
            accessor.setLitTime(1);
            changeCookingTime(accessor, hellfireCookTime);
        } else if (accessor.getCookingTotalTime() == hellfireCookTime) {
            // Restore vanilla timing when Hellfire is removed. Rescaling progress prevents a
            // progress value above the shorter vanilla total, which would otherwise never finish
            // because vanilla completes a recipe only when progress == total.
            changeCookingTime(accessor, normalCookTime);
        }
    }

    private static int slowCookTime(int normalCookTime) {
        long scaled = ((long) normalCookTime * SLOWDOWN_NUMERATOR
                + SLOWDOWN_DENOMINATOR - 1L) / SLOWDOWN_DENOMINATOR;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, scaled));
    }

    private static void changeCookingTime(AbstractFurnaceAccessor accessor, int newTotalTime) {
        int oldTotalTime = accessor.getCookingTotalTime();
        if (oldTotalTime == newTotalTime) return;

        int oldProgress = Math.max(0, accessor.getCookingProgress());
        int newProgress = 0;
        if (oldTotalTime > 0 && oldProgress > 0) {
            long proportionalProgress = (long) oldProgress * newTotalTime / oldTotalTime;
            newProgress = (int) Math.min(newTotalTime - 1L, proportionalProgress);
        }

        accessor.setCookingProgress(newProgress);
        accessor.setCookingTotalTime(newTotalTime);
    }
}
