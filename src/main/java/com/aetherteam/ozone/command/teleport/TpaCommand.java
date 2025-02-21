package com.aetherteam.ozone.command.teleport;

import com.aetherteam.ozone.attachment.OzoneDataAttachments;
import com.aetherteam.ozone.command.OzoneCommands;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public class TpaCommand { //todo testing
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tpa")
                .then(Commands.literal("accept")).executes(stack -> accept(stack.getSource()))
                .then(Commands.literal("cancel")).executes(stack -> cancel(stack.getSource()))
                .then(Commands.argument("player", EntityArgument.player())).executes(stack -> request(stack.getSource(), EntityArgument.getPlayer(stack, "player")))
        );
    }

    public static int accept(CommandSourceStack stack) throws CommandSyntaxException { //todo failure message
        if (stack.getEntityOrException() instanceof ServerPlayer serverPlayer) {
            if (serverPlayer.getData(OzoneDataAttachments.PLAYER).getOtherPlayerRequestingTeleport().isPresent()) {
                UUID otherUUID = serverPlayer.getData(OzoneDataAttachments.PLAYER).getOtherPlayerRequestingTeleport().get();
                ServerPlayer otherPlayer = stack.getServer().getPlayerList().getPlayer(otherUUID);
                if (otherPlayer != null) {
                    return teleportToEntity(stack, serverPlayer, otherPlayer);
                }
            }
        }
        return 1;
    }

    public static int cancel(CommandSourceStack stack) throws CommandSyntaxException { //todo failure message
        if (stack.getEntityOrException() instanceof ServerPlayer serverPlayer) {
            if (serverPlayer.getData(OzoneDataAttachments.PLAYER).getOtherPlayerRequestingTeleport().isPresent()) {
                serverPlayer.getData(OzoneDataAttachments.PLAYER).clearPreviousPosition();
            }
        }
        return 1;
    }

    public static int request(CommandSourceStack stack, ServerPlayer otherPlayer) throws CommandSyntaxException { //todo failure message
        if (stack.getEntityOrException() instanceof ServerPlayer serverPlayer) {
            serverPlayer.getData(OzoneDataAttachments.PLAYER).setRequestingTeleportToOtherPlayer(otherPlayer.getUUID());
            otherPlayer.getData(OzoneDataAttachments.PLAYER).setOtherPlayerRequestingTeleport(serverPlayer.getUUID());
        }
        return 1;
    }

    private static int teleportToEntity(CommandSourceStack stack, ServerPlayer target, ServerPlayer entity) {
        OzoneCommands.performTeleport(target, (ServerLevel) entity.level(), entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
        stack.sendSuccess(() -> Component.translatable("commands.teleport.success.entity.single", target.getDisplayName(), entity.getDisplayName()), true);
        return 1;
    }
}
