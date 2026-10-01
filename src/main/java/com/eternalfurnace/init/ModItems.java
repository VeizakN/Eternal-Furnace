package com.eternalfurnace.init;

import com.eternalfurnace.EternalFurnaceMod;
import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EternalFurnaceMod.MOD_ID);

    public static final DeferredItem<BlockItem> HELLFIRE_NETHERRACK =
            ITEMS.registerSimpleBlockItem(ModBlocks.HELLFIRE_NETHERRACK);

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
