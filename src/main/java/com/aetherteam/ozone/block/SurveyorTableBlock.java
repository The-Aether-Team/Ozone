package com.aetherteam.ozone.block;

import com.aetherteam.ozone.Ozone;
import com.aetherteam.ozone.OzoneConfig;
import com.aetherteam.ozone.attachment.OzoneChunkAttachment;
import com.aetherteam.ozone.attachment.OzoneDataAttachments;
import com.aetherteam.ozone.attachment.OzoneLevelAttachment;
import com.aetherteam.ozone.block.entity.SurveyorTableBlockEntity;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.BlockHitResult;

public class SurveyorTableBlock extends BaseEntityBlock {
    public static final MapCodec<SurveyorTableBlock> CODEC = simpleCodec(SurveyorTableBlock::new);
//    private static final Component CONTAINER_TITLE = Component.translatable("container.cartography_table");

    public MapCodec<SurveyorTableBlock> codec() {
        return CODEC;
    }

    public SurveyorTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SurveyorTableBlockEntity(pos, state);
    }

    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {

        return InteractionResult.SUCCESS;
    }
    
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        ChunkAccess chunk = level.getChunk(pos);
        if (chunk.hasData(OzoneDataAttachments.CHUNK)) {
            OzoneChunkAttachment claim = chunk.getData(OzoneDataAttachments.CHUNK);
            if (claim.hasOwner()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        ChunkAccess chunk = level.getChunk(pos);
        OzoneChunkAttachment claim = chunk.getData(OzoneDataAttachments.CHUNK);
        if (placer instanceof Player player) {
            if (!claim.hasOwner()) {
                var server = level.getServer();
                if (server != null) {
                    OzoneLevelAttachment levelData = server.overworld().getData(OzoneDataAttachments.LEVEL);
                    int maxChunkClaims = OzoneConfig.getMaxChunkClaims();
                    if (levelData.getPlayerClaimCount(player.getUUID()) >= maxChunkClaims) {
                        if (!level.isClientSide) {
                            claim.sendClaimLimitReachedMessage(player, maxChunkClaims);
                        }
                        level.destroyBlock(pos, true);
                        Ozone.LOGGER.info("Claim block could not be added at ({}, {}, {}) (chunk {}, {}) because claim limit was reached by {} (uuid {})", pos.getX(), pos.getY(), pos.getZ(), chunk.getPos().x, chunk.getPos().z, player.getGameProfile().getName(), player.getUUID());
                        return;
                    }
                }
            }
            if (!level.isClientSide) {                
                claim.setOwner((ServerLevel)level, player.getUUID(), pos, false);
                claim.sendClaimCreatedMessageToOwner(level.getServer(), pos);            
            } else {
                claim.setOwner(player.getUUID(), pos);
            }
            chunk.setData(OzoneDataAttachments.CHUNK, claim);
            
            Ozone.LOGGER.info("Claim block added at ({}, {}, {}) (chunk {}, {}) by {} (uuid {})", pos.getX(), pos.getY(), pos.getZ(), chunk.getPos().x, chunk.getPos().z, player.getGameProfile().getName(), player.getUUID());
        } else {
            level.destroyBlock(pos, true);
        }
    }

    // @Override
    // protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
    //     super.onPlace(state, level, pos, oldState, movedByPiston);
        
    // }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);

        ChunkAccess chunk = level.getChunk(pos);
        if (chunk.hasData(OzoneDataAttachments.CHUNK)) {
            OzoneChunkAttachment claim = chunk.getData(OzoneDataAttachments.CHUNK);
            claim.getOwnerInfo().ifPresent(owner -> {
                if (!level.isClientSide) {
                    claim.sendClaimRemovedMessageToOwner(level.getServer(), pos);
                    level.getServer().getProfileCache().get(owner.getUUID()).ifPresent(profile -> {
                        level.getServer().getPlayerList();
                    });
                    claim.removeOwner((ServerLevel)level);
                    chunk.removeData(OzoneDataAttachments.CHUNK);
                } else {
                    claim.removeOwner();
                }
                Ozone.LOGGER.info("Claim block removed at ({}, {}, {}) (chunk {}, {})", pos.getX(), pos.getY(), pos.getZ(), chunk.getPos().x, chunk.getPos().z);
            });
        }
    }

//    @Nullable
//    protected MenuProvider getMenuProvider(BlockState p_51364_, Level p_51365_, BlockPos p_51366_) {
//        return new SimpleMenuProvider((p_51353_, p_51354_, p_51355_) -> new CartographyTableMenu(p_51353_, p_51354_, ContainerLevelAccess.create(p_51365_, p_51366_)), CONTAINER_TITLE);
//    }
}
