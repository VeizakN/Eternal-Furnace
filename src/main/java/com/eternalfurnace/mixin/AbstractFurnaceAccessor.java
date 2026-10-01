package com.eternalfurnace.mixin;

import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractFurnaceBlockEntity.class)
public interface AbstractFurnaceAccessor {

    @Accessor("litTime")
    int getLitTime();

    @Accessor("litTime")
    void setLitTime(int value);

    @Accessor("cookingProgress")
    int getCookingProgress();

    @Accessor("cookingProgress")
    void setCookingProgress(int value);

    @Accessor("cookingTotalTime")
    int getCookingTotalTime();

    @Accessor("cookingTotalTime")
    void setCookingTotalTime(int value);

    @Invoker("getTotalCookTime")
    static int invokeGetTotalCookTime(Level level, AbstractFurnaceBlockEntity blockEntity) {
        throw new AssertionError();
    }
}
