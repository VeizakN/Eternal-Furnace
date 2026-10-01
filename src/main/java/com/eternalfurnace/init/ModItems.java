package com.eternalfurnace.init;

import com.eternalfurnace.EternalFurnaceMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, EternalFurnaceMod.MOD_ID);

    public static final RegistryObject<Item> HELLFIRE_NETHERRACK = ITEMS.register(
            "hellfire_netherrack",
            () -> new BlockItem(ModBlocks.HELLFIRE_NETHERRACK.get(), new Item.Properties())
    );
}
