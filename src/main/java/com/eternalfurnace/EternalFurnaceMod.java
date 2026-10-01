package com.eternalfurnace;

import com.eternalfurnace.init.ModBlocks;
import com.eternalfurnace.init.ModItems;
import com.eternalfurnace.init.ModRecipeSerializers;
import com.eternalfurnace.test.ModGameTests;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(EternalFurnaceMod.MOD_ID)
public final class EternalFurnaceMod {
    public static final String MOD_ID = "eternalfurnace";

    public EternalFurnaceMod(IEventBus modEventBus) {
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModRecipeSerializers.register(modEventBus);
        ModGameTests.register(modEventBus);
        modEventBus.addListener(EternalFurnaceMod::addCreativeTabContents);
    }

    private static void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(ModItems.HELLFIRE_NETHERRACK.get());
        }
    }
}
