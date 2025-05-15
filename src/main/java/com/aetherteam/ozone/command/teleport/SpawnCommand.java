package com.aetherteam.ozone.command.teleport;

import com.aetherteam.ozone.attachment.OzoneDataAttachments;
import com.aetherteam.ozone.attachment.OzoneLevelAttachment;
import com.aetherteam.ozone.command.OzoneCommands;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class SpawnCommand {
    private static final SimpleCommandExceptionType ERROR_SPAWN = new SimpleCommandExceptionType(Component.translatable("commands.ozone_utilities.spawn.error"));
    private static final SimpleCommandExceptionType ERROR_SPAWN_SET = new SimpleCommandExceptionType(Component.translatable("commands.ozone_utilities.spawn.set.error"));
    private static final SimpleCommandExceptionType ERROR_SPAWN_DELETE = new SimpleCommandExceptionType(Component.translatable("commands.ozone_utilities.spawn.delete.error"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spawn").executes(stack -> teleportToSpawn(stack.getSource()))
                .then(Commands.literal("set").executes(stack -> setSpawn(stack.getSource())).requires((source) -> source.hasPermission(3)))
                .then(Commands.literal("delete").executes(stack -> deleteSpawn(stack.getSource())).requires((source) -> source.hasPermission(3)))
        );
    }

    private static int teleportToSpawn(CommandSourceStack source) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer sender) {
            ServerLevel overworld = OzoneLevelAttachment.getOverworld(source.getServer());
            if (overworld != null) {
                Optional<GlobalPos> pos = overworld.getData(OzoneDataAttachments.LEVEL).getServerSpawn();
                if (pos.isPresent()) {
                    ServerLevel spawnLevel = source.getServer().getLevel(pos.get().dimension());
                    if (spawnLevel != null) {
                        OzoneCommands.performTeleport(sender, spawnLevel, pos.get().pos().getX(), pos.get().pos().getY(), pos.get().pos().getZ(), sender.getYRot(), sender.getXRot());
                        source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.spawn", sender.getDisplayName()), false);
                        return 1;
                    }
                }
            }
        }
        throw ERROR_SPAWN.create();
    }

    private static int setSpawn(CommandSourceStack source) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer sender) {
            ServerLevel overworld = OzoneLevelAttachment.getOverworld(source.getServer());
            if (overworld != null) {
                ResourceKey<Level> dimension = sender.level().dimension();
                BlockPos pos = sender.blockPosition();
                overworld.getData(OzoneDataAttachments.LEVEL).setServerSpawn(GlobalPos.of(dimension, pos));
                source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.spawn.set", pos.getX(), pos.getY(), pos.getZ(), dimension.location().toString()), true);
                return 1;
            }
        }
        throw ERROR_SPAWN_SET.create();
    }

    private static int deleteSpawn(CommandSourceStack source) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer sender) {
            ServerLevel overworld = OzoneLevelAttachment.getOverworld(source.getServer());
            if (overworld != null) {
                Optional<GlobalPos> pos = overworld.getData(OzoneDataAttachments.LEVEL).getServerSpawn();
                if (pos.isPresent()) {
                    overworld.getData(OzoneDataAttachments.LEVEL).clearServerSpawn();
                    source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.spawn.delete"), true);
                    return 1;
                }
            }
        }
        throw ERROR_SPAWN_DELETE.create();
    }
}
