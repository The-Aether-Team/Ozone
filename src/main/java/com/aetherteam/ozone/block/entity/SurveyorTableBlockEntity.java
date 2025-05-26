package com.aetherteam.ozone.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class SurveyorTableBlockEntity extends BlockEntity {
    public SurveyorTableBlockEntity(BlockPos pos, BlockState state) {
        this(OzoneBlockEntities.SURVEYOR_TABLE.get(), pos, state);
    }

    protected SurveyorTableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }
}
