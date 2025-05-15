package com.aetherteam.ozone.command.teleport;

import com.aetherteam.ozone.attachment.OzoneDataAttachments;
import com.aetherteam.ozone.command.OzoneCommands;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public class TpaCommand {
    private static final SimpleCommandExceptionType ERROR_TELEPORT_ACCEPT = new SimpleCommandExceptionType(Component.translatable("commands.ozone_utilities.teleport.accept.error"));
    private static final SimpleCommandExceptionType ERROR_TELEPORT_CANCEL = new SimpleCommandExceptionType(Component.translatable("commands.ozone_utilities.teleport.cancel.error"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tpa")
                .then(Commands.literal("accept")).executes(stack -> accept(stack.getSource()))
                .then(Commands.literal("cancel")).executes(stack -> cancel(stack.getSource()))
                .then(Commands.argument("player", EntityArgument.player())).executes(stack -> request(stack.getSource(), EntityArgument.getPlayer(stack, "player")))
        );
    }

    public static int accept(CommandSourceStack source) throws CommandSyntaxException { //todo test
        if (source.getEntityOrException() instanceof ServerPlayer requestingPlayer) {
            UUID senderUUID = requestingPlayer.getUUID();
            if (requestingPlayer.getData(OzoneDataAttachments.PLAYER).getOtherPlayerRequestingTeleport().isPresent()) {
                UUID otherUUID = requestingPlayer.getData(OzoneDataAttachments.PLAYER).getOtherPlayerRequestingTeleport().get();
                ServerPlayer otherPlayer = source.getServer().getPlayerList().getPlayer(otherUUID);
                if (otherPlayer != null) {
                    if (otherPlayer.getData(OzoneDataAttachments.PLAYER).getRequestingTeleportToOtherPlayer().isPresent()) {
                        UUID requestingUUID = requestingPlayer.getData(OzoneDataAttachments.PLAYER).getRequestingTeleportToOtherPlayer().get();
                        if (requestingUUID.equals(senderUUID)) {
                            return teleportToEntity(source, requestingPlayer, otherPlayer);
                        }
                    }
                }
            }
        }
        throw ERROR_TELEPORT_ACCEPT.create();
    }

    public static int cancel(CommandSourceStack source) throws CommandSyntaxException { //todo potentially a cleaner way to do this.
        if (source.getEntityOrException() instanceof ServerPlayer sender) {
            Optional<UUID> otherUUID = sender.getData(OzoneDataAttachments.PLAYER).getOtherPlayerRequestingTeleport();
            if (otherUUID.isPresent()) {
                ServerPlayer otherPlayer = source.getServer().getPlayerList().getPlayer(otherUUID.get());
                if (otherPlayer != null) {
                    source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.teleport.cancel", otherPlayer.getDisplayName()), true);
                    otherPlayer.getData(OzoneDataAttachments.PLAYER).clearOtherPlayerRequestingTeleport();
                }
                sender.getData(OzoneDataAttachments.PLAYER).clearRequestingTeleportToOtherPlayer();
            } else {
                throw ERROR_TELEPORT_CANCEL.create();
            }
        }
        return 1;
    }

    public static int request(CommandSourceStack source, ServerPlayer otherPlayer) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer sender) {
            source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.teleport.request", otherPlayer.getDisplayName()), true);
            sender.getData(OzoneDataAttachments.PLAYER).setRequestingTeleportToOtherPlayer(otherPlayer.getUUID());
            otherPlayer.getData(OzoneDataAttachments.PLAYER).setOtherPlayerRequestingTeleport(sender.getUUID());
        }
        return 1;
    }

    private static int teleportToEntity(CommandSourceStack stack, ServerPlayer target, ServerPlayer entity) {
        OzoneCommands.performTeleport(target, (ServerLevel) entity.level(), entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
        stack.sendSuccess(() -> Component.translatable("commands.teleport.success.entity.single", target.getDisplayName(), entity.getDisplayName()), true);
        return 1;
    }
}
