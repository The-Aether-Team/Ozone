package com.aetherteam.ozone.command.teleport;

import com.aetherteam.ozone.attachment.OzoneDataAttachments;
import com.aetherteam.ozone.command.OzoneCommands;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class SpawnCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spawn").executes(stack -> teleportToSpawn(stack.getSource()))
                .then(Commands.literal("set")).executes(stack -> setSpawn(stack.getSource())).requires((source) -> source.hasPermission(4))
                .then(Commands.literal("delete")).executes(stack -> deleteSpawn(stack.getSource())).requires((source) -> source.hasPermission(4))
        );
    }

    private static int teleportToSpawn(CommandSourceStack source) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer sender) {
            Optional<BlockPos> pos = source.getLevel().getData(OzoneDataAttachments.LEVEL).getServerSpawn();
            if (pos.isPresent()) {
                OzoneCommands.performTeleport(sender, (ServerLevel) sender.level(), pos.get().getX(), pos.get().getY(), pos.get().getZ(), sender.getYRot(), sender.getXRot());
                source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.spawn", sender.getDisplayName()), false);
            }
        }
        return 1;
    }

    private static int setSpawn(CommandSourceStack source) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer sender) {
            BlockPos pos = sender.blockPosition();
            source.getLevel().getData(OzoneDataAttachments.LEVEL.get()).setServerSpawn(pos);
            source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.spawn.set", pos.getX(), pos.getY(), pos.getZ()), true);
        }
        return 1;
    }

    private static int deleteSpawn(CommandSourceStack source) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer sender) {
            Optional<BlockPos> pos = source.getLevel().getData(OzoneDataAttachments.LEVEL).getServerSpawn();
            if (pos.isPresent()) {
                source.getLevel().getData(OzoneDataAttachments.LEVEL).clearServerSpawn();
                source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.spawn.delete"), true);
            }
        }
        return 1;
    }
}
