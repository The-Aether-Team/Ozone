package com.aetherteam.ozone;

import com.aetherteam.ozone.attachment.OzoneDataAttachments;
import com.aetherteam.ozone.event.hooks.PlayerHooks;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public class OzoneEventListeners {
    public static void register(IEventBus bus) {
        // Player
        bus.addListener(OzoneEventListeners::onPlayerLogin);
        bus.addListener(OzoneEventListeners::onPlayerChangedDimension);
        bus.addListener(OzoneEventListeners::onPlayerTeleport);
        bus.addListener(OzoneEventListeners::onPlayerPostTick);
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
}
