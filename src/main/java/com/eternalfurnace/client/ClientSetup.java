package com.eternalfurnace.client;

import com.eternalfurnace.EternalFurnaceMod;
import com.eternalfurnace.init.ModBlocks;
import com.eternalfurnace.init.ModItems;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Applies a 30%-darker tint to Hellfire Netherrack by multiplying the vanilla
 * netherrack texture with a gray tint (178/255 ≈ 0.70 per channel).
 */
@Mod.EventBusSubscriber(modid = EternalFurnaceMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    // 70% of 255 = 178 = 0xB2; applied to all RGB channels → 30% darker
    private static final int HELLFIRE_TINT = 0xFFB2B2B2;

    @SubscribeEvent
    public static void onBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register(
                (state, tintGetter, pos, tintIndex) -> HELLFIRE_TINT,
                ModBlocks.HELLFIRE_NETHERRACK.get()
        );
    }

    @SubscribeEvent
    public static void onItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(
                (stack, tintIndex) -> HELLFIRE_TINT,
                ModItems.HELLFIRE_NETHERRACK.get()
        );
    }
}
