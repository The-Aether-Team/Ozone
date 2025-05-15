package com.aetherteam.ozone.command;

import com.aetherteam.ozone.Ozone;
import com.aetherteam.ozone.command.teleport.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;

import java.util.EnumSet;

@EventBusSubscriber(modid = Ozone.MODID, bus = EventBusSubscriber.Bus.GAME)
public class OzoneCommands { //todo more detailed failure messages for everything like home already exists, etc.
    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        BackCommand.register(event.getDispatcher());
        TpaCommand.register(event.getDispatcher());
        RtpCommand.register(event.getDispatcher());
        SpawnCommand.register(event.getDispatcher());
        HomeCommand.register(event.getDispatcher());
        WarpCommand.register(event.getDispatcher());
    }

    public static BlockPos getCorrectPosition(ServerPlayer entity) {
        Vec3 position = entity.position();
        return new BlockPos(Mth.floor(position.x()), Mth.ceil(position.y()), Mth.floor(position.z()));
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

        BlockPos blockPos = BlockPos.containing(x, y, z).below();
        BlockState blockState = level.getBlockState(blockPos);
        VoxelShape shape = blockState.getShape(level, blockPos);
        double height;
        if (shape.isEmpty()) {
            height = 0.0;
        } else {
            height = shape.max(Direction.Axis.Y);
        }

        if (entity.teleportTo(level, x + 0.5, y - 1.0 + height, z + 0.5, EnumSet.noneOf(Relative.class), f2, f3, true)) {
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
