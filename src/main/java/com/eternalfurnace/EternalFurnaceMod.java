package com.eternalfurnace;

import com.eternalfurnace.init.ModBlocks;
import com.eternalfurnace.init.ModItems;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(EternalFurnaceMod.MOD_ID)
public class EternalFurnaceMod {

    public static final String MOD_ID = "eternalfurnace";

    public EternalFurnaceMod() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        bus.addListener(this::addCreative);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(ModItems.HELLFIRE_NETHERRACK);
        }
    }
}
