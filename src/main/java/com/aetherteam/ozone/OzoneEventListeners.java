package com.aetherteam.ozone;

import com.aetherteam.ozone.event.hooks.PlayerHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;

public class OzoneEventListeners {
    public static void listen(IEventBus bus) {
        bus.addListener(OzoneEventListeners::onPlayerTeleport);
    }

    public static void onPlayerTeleport(EntityTeleportEvent.TeleportCommand event) {
        Entity entity = event.getEntity();
        double prevX = event.getPrevX();
        double prevY = event.getPrevY();
        double prevZ = event.getPrevZ();

        if (entity instanceof ServerPlayer serverPlayer) {
            PlayerHooks.playerTeleport(serverPlayer, prevX, prevY, prevZ);
        }
    }
}
