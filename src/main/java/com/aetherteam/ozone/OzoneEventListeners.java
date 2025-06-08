package com.aetherteam.ozone;

import java.util.UUID;

import com.aetherteam.ozone.attachment.GlobalChunkPos;
import com.aetherteam.ozone.attachment.OzoneChunkAttachment;
import com.aetherteam.ozone.attachment.OzoneChunkAttachment.BlockInteractionCategory;
import com.aetherteam.ozone.attachment.OzoneDataAttachments;
import com.aetherteam.ozone.attachment.OzoneLevelAttachment;
import com.aetherteam.ozone.block.OzoneBlocks;
import com.aetherteam.ozone.event.hooks.PlayerHooks;
import com.aetherteam.ozone.network.packet.clientbound.ChunkClaimPacket;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingDestroyBlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;
import net.neoforged.neoforge.event.level.BlockEvent.EntityMultiPlaceEvent;
import net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public class OzoneEventListeners {
    public static void register(IEventBus bus) {
        // Player
        bus.addListener(OzoneEventListeners::onPlayerLogin);
        bus.addListener(OzoneEventListeners::onPlayerChangedDimension);
        bus.addListener(OzoneEventListeners::onPlayerTeleport);
        bus.addListener(OzoneEventListeners::onPlayerPostTick);
        bus.addListener(OzoneEventListeners::onPlayerInteractWithEntity);
        bus.addListener(OzoneEventListeners::onPlayerInteractWithEntitySpecific);
        bus.addListener(OzoneEventListeners::onPlayerLeftClickBlock);
        bus.addListener(OzoneEventListeners::onPlayerRightClickBlock);
        bus.addListener(OzoneEventListeners::onPlayerRightClickItem);
        bus.addListener(OzoneEventListeners::onPlayerRightClickEmpty);
        bus.addListener(OzoneEventListeners::onBlockBreak);
        bus.addListener(OzoneEventListeners::onPlayerWatchChunk);
        // Entity
        bus.addListener(OzoneEventListeners::onBlockPlace);
        bus.addListener(OzoneEventListeners::onMultiBlockPlace);
        bus.addListener(OzoneEventListeners::onEntityBlockBreak);
        // Other
        bus.addListener(OzoneEventListeners::onExplosionStart);
        bus.addListener(OzoneEventListeners::onExplosionDetonate);
    }

    private static void onPlayerLogin(PlayerLoggedInEvent event) {
        Player player = event.getEntity();

        player.getData(OzoneDataAttachments.PLAYER).login(player);
    }

    private static void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
        Player player = event.getEntity();

        player.getData(OzoneDataAttachments.PLAYER).changeDimension(player);
    }

    private static void onPlayerTeleport(EntityTeleportEvent.TeleportCommand event) {
        Entity entity = event.getEntity();
        double prevX = event.getPrevX();
        double prevY = event.getPrevY();
        double prevZ = event.getPrevZ();

        if (entity instanceof ServerPlayer serverPlayer) {
            PlayerHooks.playerTeleport(serverPlayer, serverPlayer.level().dimension(), prevX, prevY, prevZ);
        }
    }

    private static void onPlayerPostTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        player.getData(OzoneDataAttachments.PLAYER).postTickUpdate(player);
    }

    private static void onPlayerWatchChunk(ChunkWatchEvent.Sent event) {
        ChunkAccess chunk = event.getChunk();
        ServerLevel level = event.getLevel();
        GlobalChunkPos pos = GlobalChunkPos.of(level.dimension(), chunk.getPos());
        ServerPlayer player = event.getPlayer();
        if (chunk.hasData(OzoneDataAttachments.CHUNK)) {
            OzoneChunkAttachment claim = chunk.getData(OzoneDataAttachments.CHUNK);
            claim.getOwnerInfo().ifPresent(owner -> {
                PacketDistributor.sendToPlayer(player,
                    new ChunkClaimPacket.ChangeOwnerAndSurveyorTableLocation(pos, owner.getUUID(), owner.getSurveyorTableLocation()),
                    new ChunkClaimPacket.ChangeFilters(pos, claim.getFilters()));
            });
        }
    }

    private static void onPlayerInteractWithEntity(PlayerInteractEvent.EntityInteract event) {

    }

    private static void onPlayerInteractWithEntitySpecific(PlayerInteractEvent.EntityInteractSpecific event) {

    }

    private static void onPlayerLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        switch (event.getAction()) {
            case START -> {
                ChunkAccess chunk = event.getLevel().getChunk(event.getPos());
                if (!chunk.hasData(OzoneDataAttachments.CHUNK)) return;
                OzoneChunkAttachment claim = chunk.getData(OzoneDataAttachments.CHUNK);
                
                if (!claim.allowBreak(event.getLevel().getBlockState(event.getPos()), event.getEntity())) {
                    event.setCanceled(true);
                    if (!event.getLevel().isClientSide) {
                        claim.sendDenyMessage(event.getEntity());
                    }
                    Ozone.LOGGER.info("Canceled left-click interaction at ({}, {}, {}) for {} (uuid {})", event.getPos().getX(), event.getPos().getY(), event.getPos().getZ(), event.getEntity().getGameProfile().getName(), event.getEntity().getUUID());
                } 
            }
            default -> {}
        }
    }

    private static void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ChunkAccess chunk = event.getLevel().getChunk(event.getPos());
        if (!chunk.hasData(OzoneDataAttachments.CHUNK)) return;
        OzoneChunkAttachment claim = chunk.getData(OzoneDataAttachments.CHUNK);
        
        if (claim.hasOwner()) {
            var state = event.getLevel().getBlockState(event.getPos());
            if (state.getBlock() == OzoneBlocks.SURVEYOR_TABLE.get()) {
                if(!claim.isOwner(event.getEntity())) {
                    if (!event.getLevel().isClientSide) {
                        event.setCancellationResult(InteractionResult.PASS);
                        event.setCanceled(true);
                        claim.sendDenyMessage(event.getEntity());
                        Ozone.LOGGER.info("Canceled right-click interaction at ({}, {}, {}) for {} (uuid {})", event.getPos().getX(), event.getPos().getY(), event.getPos().getZ(), event.getEntity().getGameProfile().getName(), event.getEntity().getUUID());
                    }
                }
                return;
            }
            var categories = BlockInteractionCategory.of(state);
            if (!categories.isEmpty()) {                
                for (var category : categories) {
                    if (!category.isInteractAllowed(state, event.getEntity(), claim)) {
                        if (!event.getLevel().isClientSide) {
                            event.setCancellationResult(InteractionResult.PASS);
                            event.setCanceled(true);
                            claim.sendDenyMessage(event.getEntity());
                            Ozone.LOGGER.info("Canceled right-click interaction at ({}, {}, {}) for {} (uuid {})", event.getPos().getX(), event.getPos().getY(), event.getPos().getZ(), event.getEntity().getGameProfile().getName(), event.getEntity().getUUID());
                        }
                        break;
                    }
                }
            }
        }
    }

    private static void onPlayerRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Ozone.LOGGER.info("{} right-clicked item at ({}, {}, {})", event.getEntity().getGameProfile().getName(), event.getPos().getX(), event.getPos().getY(), event.getPos().getZ());
    }

    private static void onPlayerRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        Ozone.LOGGER.info("{} right-clicked air at ({}, {}, {})", event.getEntity().getGameProfile().getName(), event.getPos().getX(), event.getPos().getY(), event.getPos().getZ());
    }

    private static boolean checkForMaxClaims(BlockState blockState, Entity entity, UUID ownerUUID) {
        if (blockState.getBlock() != OzoneBlocks.SURVEYOR_TABLE.get()) return false;
        if (!entity.getUUID().equals(ownerUUID)) return false;
        Level overworld = entity.level();
        if (overworld.dimension() != Level.OVERWORLD) {
            if (overworld.getServer() == null) return false;
            overworld = overworld.getServer().overworld();
        }
        OzoneLevelAttachment levelData = overworld.getData(OzoneDataAttachments.LEVEL);
        return levelData.getPlayerClaimCount(ownerUUID) >= OzoneConfig.getMaxChunkClaims();
    }

    private static void onBlockPlace(EntityPlaceEvent event) {
        ChunkAccess chunk = event.getLevel().getChunk(event.getPos());
        if (!chunk.hasData(OzoneDataAttachments.CHUNK)) return;
        OzoneChunkAttachment claim = chunk.getData(OzoneDataAttachments.CHUNK);
        claim.getOwnerInfo().ifPresent(owner -> {
            if (!claim.allowPlace(event.getPlacedBlock(), event.getEntity())) {
                event.setCanceled(true);
                if (!event.getLevel().isClientSide()) {
                    if (event.getEntity() instanceof Player player) {
                        claim.sendDenyMessage(player);
                    }
                }
                Ozone.LOGGER.info("Canceled block placement at ({}, {}, {}) for {} (uuid {})", event.getPos().getX(), event.getPos().getY(), event.getPos().getZ(), event.getEntity().getName().getString(), event.getEntity().getUUID());
            }/*  else if (checkForMaxClaims(event.getPlacedBlock(), event.getEntity(), owner.getUUID())) {
                event.setCanceled(true);
                if (!event.getLevel().isClientSide()) {
                    if (event.getEntity() instanceof Player player) {
                        claim.sendClaimLimitReachedMessage(player, OzoneConfig.getMaxChunkClaims());
                    }
                }
                Ozone.LOGGER.info("Canceled block placement at ({}, {}, {}) for {} (uuid {})", event.getPos().getX(), event.getPos().getY(), event.getPos().getZ(), event.getEntity().getName().getString(), event.getEntity().getUUID());
            } */
        });
    }

    private static void onMultiBlockPlace(EntityMultiPlaceEvent event) {
        for (var block : event.getReplacedBlockSnapshots()) {
            ChunkAccess chunk = event.getLevel().getChunk(block.getPos());
            if (!chunk.hasData(OzoneDataAttachments.CHUNK)) continue;
            OzoneChunkAttachment claim = chunk.getData(OzoneDataAttachments.CHUNK);
            if (!claim.hasOwner()) continue;            
            if (!claim.allowPlace(block.getState(), event.getEntity())) {
                event.setCanceled(true);
                if (event.getEntity() instanceof Player player) {
                    claim.sendDenyMessage(player);
                }
                Ozone.LOGGER.info("Canceled multi block placement at ({}, {}, {}) for {} (uuid {})", event.getPos().getX(), event.getPos().getY(), event.getPos().getZ(), event.getEntity().getName().getString(), event.getEntity().getUUID());
                break;
            }/*  else if (checkForMaxClaims(block.getState(), event.getEntity(), claim.getOwner().get())) {
                event.setCanceled(true);
                if (!event.getLevel().isClientSide()) {
                    if (event.getEntity() instanceof Player player) {
                        claim.sendClaimLimitReachedMessage(player, OzoneConfig.getMaxChunkClaims());
                    }
                }
                Ozone.LOGGER.info("Canceled block placement at ({}, {}, {}) for {} (uuid {})", event.getPos().getX(), event.getPos().getY(), event.getPos().getZ(), event.getEntity().getName().getString(), event.getEntity().getUUID());
            } */
        }
    }

    private static void onBlockBreak(BreakEvent event) {
        ChunkAccess chunk = event.getLevel().getChunk(event.getPos());
        if (!chunk.hasData(OzoneDataAttachments.CHUNK)) return;
        OzoneChunkAttachment claim = chunk.getData(OzoneDataAttachments.CHUNK);
        
        if (!claim.allowBreak(event.getState(), event.getPlayer())) {
            event.setCanceled(true);
            if (!event.getLevel().isClientSide()) {
                claim.sendDenyMessage(event.getPlayer());
            }
            Ozone.LOGGER.info("Canceled block breaking at ({}, {}, {}) for {} (uuid {})", event.getPos().getX(), event.getPos().getY(), event.getPos().getZ(), event.getPlayer().getName().getString(), event.getPlayer().getUUID());
        }
    }

    private static void onEntityBlockBreak(LivingDestroyBlockEvent event) {
        ChunkAccess chunk = event.getEntity().level().getChunk(event.getPos());
        if (!chunk.hasData(OzoneDataAttachments.CHUNK)) return;
        OzoneChunkAttachment claim = chunk.getData(OzoneDataAttachments.CHUNK);
        
        if (!claim.allowBreak(event.getState(), event.getEntity())) {
            event.setCanceled(true);
            if (!event.getEntity().level().isClientSide) {
                if (event.getEntity() instanceof Player player) {
                    claim.sendDenyMessage(player);
                }
            }
            Ozone.LOGGER.info("Canceled block breaking at ({}, {}, {}) for {} (uuid {})", event.getPos().getX(), event.getPos().getY(), event.getPos().getZ(), event.getEntity().getName().getString(), event.getEntity().getUUID());
        }
    }

    private static void onExplosionStart(ExplosionEvent.Start event) {
        ChunkAccess chunk = event.getLevel().getChunk(BlockPos.containing(event.getExplosion().center()));
        if (!chunk.hasData(OzoneDataAttachments.CHUNK)) return;
        OzoneChunkAttachment claim = chunk.getData(OzoneDataAttachments.CHUNK);
        
        if (!claim.allowExplosions(event.getExplosion().getIndirectSourceEntity())) {
            event.setCanceled(true);

            if (Ozone.LOGGER.isDebugEnabled()) {
                var source = event.getExplosion().getIndirectSourceEntity();
                if (source != null) {
                    if (!event.getLevel().isClientSide) {
                        if (source instanceof Player player) {
                            claim.sendDenyMessage(player);
                        }
                    }
                    Ozone.LOGGER.info("Canceled explosion starting at ({}, {}, {}) caused by {} (uuid {})", event.getExplosion().center().x, event.getExplosion().center().y, event.getExplosion().center().z, source.getName().getString(), source.getUUID());
                } else {
                    Ozone.LOGGER.info("Canceled explosion starting at ({}, {}, {})", event.getExplosion().center().x, event.getExplosion().center().y, event.getExplosion().center().z);
                }
            }
        }
    }

    private static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        var blocks = event.getAffectedBlocks();
        if (blocks.isEmpty()) return;
        try {
            final var sourceEntity = event.getExplosion().getIndirectSourceEntity();
            final var level = event.getLevel();
            blocks.removeIf(blockPos -> {
                ChunkAccess chunk = level.getChunk(blockPos);
                if (!chunk.hasData(OzoneDataAttachments.CHUNK)) return false;
                OzoneChunkAttachment claim = chunk.getData(OzoneDataAttachments.CHUNK);
                return !claim.allowBreak(level.getBlockState(blockPos), sourceEntity);
            });
        } catch (UnsupportedOperationException e) {
            // event doesn't support modification of effected blocks T_T
            var source = event.getExplosion().getIndirectSourceEntity();
            if (source != null) {
                Ozone.LOGGER.error("Unable to remove protected blocks from explosion at ({}, {}, {}) caused by {} (uuid {})", event.getExplosion().center().x, event.getExplosion().center().y, event.getExplosion().center().z, source.getName().getString(), source.getUUID());
            } else {
                Ozone.LOGGER.error("Unable to remove protected blocks from explosion at ({}, {}, {})", event.getExplosion().center().x, event.getExplosion().center().y, event.getExplosion().center().z);
            }
        }
    }
}
