package com.aetherteam.ozone.command;

import com.aetherteam.ozone.Ozone;
import com.aetherteam.ozone.command.teleport.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Relative;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;

import java.util.EnumSet;

@EventBusSubscriber(modid = Ozone.MODID, bus = EventBusSubscriber.Bus.GAME)
public class OzoneCommands {
    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        BackCommand.register(event.getDispatcher());
        TpaCommand.register(event.getDispatcher());
        RtpCommand.register(event.getDispatcher());
        SpawnCommand.register(event.getDispatcher());
        HomeCommand.register(event.getDispatcher());
        WarpCommand.register(event.getDispatcher());
    }

    public static void performTeleport(ServerPlayer entity, ServerLevel level, double x, double y, double z, float yRot, float xRot) {
        EntityTeleportEvent.TeleportCommand event = EventHooks.onEntityTeleportCommand(entity, x, y, z);
        if (!event.isCanceled()) {
            double d0 = event.getTargetX();
            double d1 = event.getTargetY();
            double d2 = event.getTargetZ();
            teleport(entity, level, d0, d1, d2, yRot, xRot);
        }
    }

    public static void teleport(ServerPlayer entity, ServerLevel level, double x, double y, double z, float yRot, float xRot) {
        float f2 = Mth.wrapDegrees(yRot);
        float f3 = Mth.wrapDegrees(xRot);
        if (entity.teleportTo(level, x, y, z, EnumSet.noneOf(Relative.class), f2, f3, true)) {
            label47: {
                if (entity.isFallFlying()) {
                    break label47;
                }
                entity.setDeltaMovement(entity.getDeltaMovement().multiply(1.0F, 0.0F, 1.0F));
                entity.setOnGround(true);
            }
        }
    }
}
