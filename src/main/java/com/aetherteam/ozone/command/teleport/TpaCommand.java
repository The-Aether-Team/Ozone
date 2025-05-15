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

import java.util.Optional;
import java.util.UUID;

public class TpaCommand { //todo get the argument to not show selectors because thats not good
    private static final SimpleCommandExceptionType ERROR_TELEPORT_ACCEPT = new SimpleCommandExceptionType(Component.translatable("commands.ozone_utilities.tpa.accept.error"));
    private static final SimpleCommandExceptionType ERROR_TELEPORT_DENY = new SimpleCommandExceptionType(Component.translatable("commands.ozone_utilities.tpa.deny.error"));
    private static final SimpleCommandExceptionType ERROR_TELEPORT_CANCEL = new SimpleCommandExceptionType(Component.translatable("commands.ozone_utilities.tpa.cancel.error"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tpa").then(Commands.argument("player", EntityArgument.player()).executes(stack -> request(stack.getSource(), EntityArgument.getPlayer(stack, "player")))));
        dispatcher.register(Commands.literal("tpahere").then(Commands.argument("player", EntityArgument.player()).executes(stack -> here(stack.getSource(), EntityArgument.getPlayer(stack, "player")))));

        dispatcher.register(Commands.literal("tpaaccept").executes(stack -> accept(stack.getSource())));
        dispatcher.register(Commands.literal("tpadeny").executes(stack -> deny(stack.getSource())));
        dispatcher.register(Commands.literal("tpacancel").executes(stack -> cancel(stack.getSource())));
    }

    public static int request(CommandSourceStack source, ServerPlayer otherPlayer) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer sender) {
            source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.tpa.request.source", otherPlayer.getDisplayName()), false);
            otherPlayer.sendSystemMessage(Component.translatable("commands.ozone_utilities.tpa.request.target", sender.getDisplayName()));

            sender.getData(OzoneDataAttachments.PLAYER).setRequestingTeleportToOtherPlayer(otherPlayer.getUUID());
            otherPlayer.getData(OzoneDataAttachments.PLAYER).setOtherPlayerRequestingTeleport(sender.getUUID());
        }
        return 1;
    }

    public static int here(CommandSourceStack source, ServerPlayer otherPlayer) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer sender) {
            source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.tpa.here.source", otherPlayer.getDisplayName()), false);
            otherPlayer.sendSystemMessage(Component.translatable("commands.ozone_utilities.tpa.here.target", sender.getDisplayName()));

            otherPlayer.getData(OzoneDataAttachments.PLAYER).setRequestingTeleportToOtherPlayer(sender.getUUID());
            sender.getData(OzoneDataAttachments.PLAYER).setOtherPlayerRequestingTeleport(otherPlayer.getUUID());
        }
        return 1;
    }

    public static int accept(CommandSourceStack source) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer requestingPlayer) {
            if (requestingPlayer.getData(OzoneDataAttachments.PLAYER).getOtherPlayerRequestingTeleport().isPresent()) {
                UUID otherUUID = requestingPlayer.getData(OzoneDataAttachments.PLAYER).getOtherPlayerRequestingTeleport().get();
                ServerPlayer otherPlayer = source.getServer().getPlayerList().getPlayer(otherUUID);
                if (otherPlayer != null) {
                    if (otherPlayer.getData(OzoneDataAttachments.PLAYER).getRequestingTeleportToOtherPlayer().isPresent()) {
                        return teleportToEntity(source, otherPlayer, requestingPlayer);
                    }
                }
            }
        }
        throw ERROR_TELEPORT_ACCEPT.create();
    }

    public static int deny(CommandSourceStack source) throws CommandSyntaxException { //todo potentially a cleaner way to do this.
        if (source.getEntityOrException() instanceof ServerPlayer sender) {
            Optional<UUID> otherUUID = sender.getData(OzoneDataAttachments.PLAYER).getRequestingTeleportToOtherPlayer();
            if (otherUUID.isPresent()) {
                ServerPlayer otherPlayer = source.getServer().getPlayerList().getPlayer(otherUUID.get());
                if (otherPlayer != null) {
                    otherPlayer.getData(OzoneDataAttachments.PLAYER).clearOtherPlayerRequestingTeleport();

                    source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.deny.source", otherPlayer.getDisplayName()), true);
                    otherPlayer.sendSystemMessage(Component.translatable("commands.ozone_utilities.deny.source", sender.getDisplayName()));
                }
                sender.getData(OzoneDataAttachments.PLAYER).clearOtherPlayerRequestingTeleport();
                return 1;
            }
        }
        throw ERROR_TELEPORT_DENY.create();
    }

    public static int cancel(CommandSourceStack source) throws CommandSyntaxException { //todo potentially a cleaner way to do this.
        if (source.getEntityOrException() instanceof ServerPlayer sender) {
            Optional<UUID> otherUUID = sender.getData(OzoneDataAttachments.PLAYER).getOtherPlayerRequestingTeleport();
            if (otherUUID.isPresent()) {
                ServerPlayer otherPlayer = source.getServer().getPlayerList().getPlayer(otherUUID.get());
                if (otherPlayer != null) {
                    otherPlayer.getData(OzoneDataAttachments.PLAYER).clearOtherPlayerRequestingTeleport();

                    source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.cancel.source", otherPlayer.getDisplayName()), true);
                    otherPlayer.sendSystemMessage(Component.translatable("commands.ozone_utilities.cancel.source", sender.getDisplayName()));
                }
                sender.getData(OzoneDataAttachments.PLAYER).clearRequestingTeleportToOtherPlayer();
                return 1;
            }
        }
        throw ERROR_TELEPORT_CANCEL.create();
    }

    private static int teleportToEntity(CommandSourceStack stack, ServerPlayer target, ServerPlayer entity) {
        OzoneCommands.performTeleport(target, (ServerLevel) entity.level(), entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
        stack.sendSuccess(() -> Component.translatable("commands.ozone_utilities.tpa.accept.source", target.getDisplayName()), false);
        target.sendSystemMessage(Component.translatable("commands.ozone_utilities.tpa.accept.target", entity.getDisplayName()));
        return 1;
    }
}
