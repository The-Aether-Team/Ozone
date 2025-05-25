package com.aetherteam.ozone;

import com.aetherteam.ozone.attachment.OzoneChunkAttachment;
import com.aetherteam.ozone.attachment.OzoneChunkAttachment.BlockCategory;
import com.aetherteam.ozone.attachment.OzoneDataAttachments;
import com.aetherteam.ozone.event.hooks.PlayerHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
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
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class OzoneEventListeners {
    public static void register(IEventBus bus) {
        // Player
        bus.addListener(OzoneEventListeners::onPlayerLogin);
        bus.addListener(OzoneEventListeners::onPlayerChangedDimension);
        bus.addListener(OzoneEventListeners::onPlayerTeleport);
        bus.addListener(OzoneEventListeners::onPlayerPostTick);
        bus.addListener(OzoneEventListeners::onPlayerInteractWithEntity);
        bus.addListener(OzoneEventListeners::onPlayerInteractWithEntitySpecific);
        bus.addListener(OzoneEventListeners::onPlayerRightClickBlock);
        // Entity
        bus.addListener(OzoneEventListeners::onBlockPlace);
        bus.addListener(OzoneEventListeners::onMultiBlockPlace);
        bus.addListener(OzoneEventListeners::onEntityBlockBreak);
        // Other
        bus.addListener(OzoneEventListeners::onBlockBreak);
        bus.addListener(OzoneEventListeners::onExplosionStart);
        bus.addListener(OzoneEventListeners::onExplosionDetonate);
    }

    public static void onPlayerLogin(PlayerLoggedInEvent event) {
        Player player = event.getEntity();

        player.getData(OzoneDataAttachments.PLAYER).login(player);
    }

    public static void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
        Player player = event.getEntity();

        player.getData(OzoneDataAttachments.PLAYER).changeDimension(player);
    }

    public static void onPlayerTeleport(EntityTeleportEvent.TeleportCommand event) {
        Entity entity = event.getEntity();
        double prevX = event.getPrevX();
        double prevY = event.getPrevY();
        double prevZ = event.getPrevZ();

        if (entity instanceof ServerPlayer serverPlayer) {
            PlayerHooks.playerTeleport(serverPlayer, serverPlayer.level().dimension(), prevX, prevY, prevZ);
        }
    }

    public static void onPlayerPostTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        player.getData(OzoneDataAttachments.PLAYER).postTickUpdate(player);
    }

    public static void onPlayerInteractWithEntity(PlayerInteractEvent.EntityInteract event) {

    }

    public static void onPlayerInteractWithEntitySpecific(PlayerInteractEvent.EntityInteractSpecific event) {

    }

    public static void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ChunkAccess chunk = event.getLevel().getChunk(event.getPos());
        OzoneChunkAttachment claim = chunk.getData(OzoneDataAttachments.CHUNK);
        
        if (claim.hasOwner()) {
            var categories = BlockCategory.of(event.getLevel().getBlockState(event.getPos()));
            if (!categories.isEmpty()) {
                for (var category : categories) {
                    if (!category.isInteractAllowed(event.getEntity(), claim)) {
                        event.setCancellationResult(InteractionResult.FAIL);
                        event.setCanceled(true);
                        break;
                    }
                }
            }
        }
    }

    public static void onBlockPlace(EntityPlaceEvent event) {
        ChunkAccess chunk = event.getLevel().getChunk(event.getPos());
        OzoneChunkAttachment claim = chunk.getData(OzoneDataAttachments.CHUNK);
        
        if (!claim.allowPlace(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    public static void onMultiBlockPlace(EntityMultiPlaceEvent event) {
        for (var block : event.getReplacedBlockSnapshots()) {
            ChunkAccess chunk = event.getLevel().getChunk(block.getPos());
            OzoneChunkAttachment claim = chunk.getData(OzoneDataAttachments.CHUNK);
            
            if (!claim.allowPlace(event.getEntity())) {
                event.setCanceled(true);
                break;
            }
        }
    }

    public static void onBlockBreak(BreakEvent event) {
        ChunkAccess chunk = event.getLevel().getChunk(event.getPos());
        OzoneChunkAttachment claim = chunk.getData(OzoneDataAttachments.CHUNK);
        
        if (!claim.allowBreak(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    public static void onEntityBlockBreak(LivingDestroyBlockEvent event) {
        ChunkAccess chunk = event.getEntity().level().getChunk(event.getPos());
        OzoneChunkAttachment claim = chunk.getData(OzoneDataAttachments.CHUNK);
        
        if (!claim.allowBreak(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    public static void onExplosionStart(ExplosionEvent.Start event) {
        ChunkAccess chunk = event.getLevel().getChunk(BlockPos.containing(event.getExplosion().center()));
        OzoneChunkAttachment claim = chunk.getData(OzoneDataAttachments.CHUNK);
        
        if (!claim.allowExplosions(event.getExplosion().getIndirectSourceEntity())) {
            event.setCanceled(true);
        }
    }

    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        var blocks = event.getAffectedBlocks();
        if (blocks.isEmpty()) return;
        try {
            final var sourceEntity = event.getExplosion().getIndirectSourceEntity();
            blocks.removeIf(blockPos -> {
                ChunkAccess chunk = event.getLevel().getChunk(blockPos);
                OzoneChunkAttachment claim = chunk.getData(OzoneDataAttachments.CHUNK);
                return !claim.allowBreak(sourceEntity);
            });
        } catch (UnsupportedOperationException e) {
            // event doesn't support modification of effected blocks T_T
        }
    }
}
