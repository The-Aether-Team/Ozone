package com.aetherteam.ozone.block.entity;

import com.aetherteam.ozone.Ozone;
import com.aetherteam.ozone.block.OzoneBlocks;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class OzoneBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Ozone.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SurveyorTableBlockEntity>> SURVEYOR_TABLE = BLOCK_ENTITIES.register("surveyor_table", () -> new BlockEntityType<>(SurveyorTableBlockEntity::new, OzoneBlocks.SURVEYOR_TABLE.get()));

    private OzoneBlockEntities() {}
}
