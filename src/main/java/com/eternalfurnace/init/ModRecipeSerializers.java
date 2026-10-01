package com.eternalfurnace.init;

import com.eternalfurnace.EternalFurnaceMod;
import com.eternalfurnace.recipe.DurableShapelessRecipe;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, EternalFurnaceMod.MOD_ID);

    public static final Supplier<RecipeSerializer<DurableShapelessRecipe>> DURABLE_SHAPELESS =
            RECIPE_SERIALIZERS.register(
                    "durable_shapeless",
                    () -> new RecipeSerializer<>(DurableShapelessRecipe.CODEC, DurableShapelessRecipe.STREAM_CODEC)
            );

    private ModRecipeSerializers() {
    }

    public static void register(IEventBus modEventBus) {
        RECIPE_SERIALIZERS.register(modEventBus);
    }
}
