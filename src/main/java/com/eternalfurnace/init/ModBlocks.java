package com.eternalfurnace.init;

import com.eternalfurnace.EternalFurnaceMod;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(EternalFurnaceMod.MOD_ID);

    public static final DeferredBlock<Block> HELLFIRE_NETHERRACK = BLOCKS.registerSimpleBlock(
            "hellfire_netherrack",
            properties -> properties
                    .mapColor(MapColor.NETHER)
                    .strength(0.4F)
                    .sound(SoundType.NETHERRACK)
                    .requiresCorrectToolForDrops()
    );

    private ModBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
