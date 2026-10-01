package com.eternalfurnace.init;

import com.eternalfurnace.EternalFurnaceMod;
import com.eternalfurnace.block.HellfireNetherrackBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, EternalFurnaceMod.MOD_ID);

    public static final RegistryObject<Block> HELLFIRE_NETHERRACK = BLOCKS.register(
            "hellfire_netherrack",
            () -> new HellfireNetherrackBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.NETHER)
                            .strength(0.4F)
                            .sound(SoundType.NETHERRACK)
                            .requiresCorrectToolForDrops()
            )
    );
}
