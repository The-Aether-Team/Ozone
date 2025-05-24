package com.aetherteam.ozone.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CartographyTableBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class SurveyorTableBlock extends Block {
    public static final MapCodec<CartographyTableBlock> CODEC = simpleCodec(CartographyTableBlock::new);
//    private static final Component CONTAINER_TITLE = Component.translatable("container.cartography_table");

    public MapCodec<CartographyTableBlock> codec() {
        return CODEC;
    }

    public SurveyorTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {

        return InteractionResult.SUCCESS;
    }

//    @Nullable
//    protected MenuProvider getMenuProvider(BlockState p_51364_, Level p_51365_, BlockPos p_51366_) {
//        return new SimpleMenuProvider((p_51353_, p_51354_, p_51355_) -> new CartographyTableMenu(p_51353_, p_51354_, ContainerLevelAccess.create(p_51365_, p_51366_)), CONTAINER_TITLE);
//    }
}
