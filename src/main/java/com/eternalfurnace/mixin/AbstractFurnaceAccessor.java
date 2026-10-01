package com.eternalfurnace.mixin;

import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractFurnaceBlockEntity.class)
public interface AbstractFurnaceAccessor {
    @Accessor("litTimeRemaining")
    int eternalFurnace$getLitTimeRemaining();

    @Accessor("litTimeRemaining")
    void eternalFurnace$setLitTimeRemaining(int value);

    @Accessor("litTotalTime")
    int eternalFurnace$getLitTotalTime();

    @Accessor("litTotalTime")
    void eternalFurnace$setLitTotalTime(int value);

    @Accessor("cookingTimer")
    int eternalFurnace$getCookingTimer();

    @Accessor("cookingTimer")
    void eternalFurnace$setCookingTimer(int value);

    @Accessor("cookingTotalTime")
    int eternalFurnace$getCookingTotalTime();

    @Accessor("cookingTotalTime")
    void eternalFurnace$setCookingTotalTime(int value);
}
