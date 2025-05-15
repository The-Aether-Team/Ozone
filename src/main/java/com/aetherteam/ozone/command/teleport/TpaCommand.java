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

public class TpaCommand {
    private static final SimpleCommandExceptionType ERROR_TELEPORT_SELF = new SimpleCommandExceptionType(Component.translatable("commands.ozone_utilities.tpa.error.self"));
    private static final SimpleCommandExceptionType ERROR_TELEPORT_ACCEPT = new SimpleCommandExceptionType(Component.translatable("commands.ozone_utilities.tpa.accept.error"));
    private static final SimpleCommandExceptionType ERROR_TELEPORT_ACCEPT_TARGET = new SimpleCommandExceptionType(Component.translatable("commands.ozone_utilities.tpa.accept.error.target"));
    private static final SimpleCommandExceptionType ERROR_TELEPORT_ACCEPT_SOURCE = new SimpleCommandExceptionType(Component.translatable("commands.ozone_utilities.tpa.accept.error.source"));
    private static final SimpleCommandExceptionType ERROR_TELEPORT_ACCEPT_MISMATCH = new SimpleCommandExceptionType(Component.translatable("commands.ozone_utilities.tpa.accept.error.mismatch"));
    private static final SimpleCommandExceptionType ERROR_TELEPORT_DENY = new SimpleCommandExceptionType(Component.translatable("commands.ozone_utilities.tpa.deny.error"));
    private static final SimpleCommandExceptionType ERROR_TELEPORT_DENY_CANCELLED = new SimpleCommandExceptionType(Component.translatable("commands.ozone_utilities.tpa.deny.error.cancelled"));
    private static final SimpleCommandExceptionType ERROR_TELEPORT_CANCEL = new SimpleCommandExceptionType(Component.translatable("commands.ozone_utilities.tpa.cancel.error"));
    private static final SimpleCommandExceptionType ERROR_TELEPORT_CANCEL_DENIED = new SimpleCommandExceptionType(Component.translatable("commands.ozone_utilities.tpa.cancel.error.denied"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tpa").then(Commands.argument("player", EntityArgument.player()).executes(stack -> request(stack.getSource(), EntityArgument.getPlayer(stack, "player")))));
        dispatcher.register(Commands.literal("tpahere").then(Commands.argument("player", EntityArgument.player()).executes(stack -> here(stack.getSource(), EntityArgument.getPlayer(stack, "player")))));

        dispatcher.register(Commands.literal("tpaaccept").executes(stack -> accept(stack.getSource())));

        dispatcher.register(Commands.literal("tpadeny").executes(stack -> deny(stack.getSource())));
        dispatcher.register(Commands.literal("tpacancel").executes(stack -> cancel(stack.getSource())));
    }

    // You teleport to other
    public static int request(CommandSourceStack source, ServerPlayer other) throws CommandSyntaxException { // correct i think
        if (source.getEntityOrException() instanceof ServerPlayer you) {
            if (!you.getUUID().equals(other.getUUID())) {
                you.getData(OzoneDataAttachments.PLAYER).setTpaTarget(other.getUUID());
                other.getData(OzoneDataAttachments.PLAYER).setTpaSource(you.getUUID());

                source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.tpa.request.source", other.getDisplayName()), false);
                other.sendSystemMessage(Component.translatable("commands.ozone_utilities.tpa.request.target", you.getDisplayName()));
            } else {
                throw ERROR_TELEPORT_SELF.create();
            }
        }
        return 1;
    }

    // Other teleports to you
    public static int here(CommandSourceStack source, ServerPlayer other) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer you) {
            if (!you.getUUID().equals(other.getUUID())) {
                other.getData(OzoneDataAttachments.PLAYER).setTpaTarget(you.getUUID());
                you.getData(OzoneDataAttachments.PLAYER).setTpaSource(other.getUUID());

                source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.tpa.here.source", other.getDisplayName()), false);
                other.sendSystemMessage(Component.translatable("commands.ozone_utilities.tpa.here.target", you.getDisplayName()));
            } else {
                throw ERROR_TELEPORT_SELF.create();
            }
        }
        return 1;
    }

    public static int accept(CommandSourceStack source) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer you) {
            Optional<UUID> yourTpaTarget = you.getData(OzoneDataAttachments.PLAYER).getTpaTarget();
            Optional<UUID> yourTpaSource = you.getData(OzoneDataAttachments.PLAYER).getTpaSource();

            if (yourTpaTarget.isPresent() && !yourTpaTarget.get().equals(you.getUUID())) { // tpahere
                ServerPlayer other = source.getServer().getPlayerList().getPlayer(yourTpaTarget.get());
                if (other != null) {
                    Optional<UUID> otherTpaSource = other.getData(OzoneDataAttachments.PLAYER).getTpaSource();
                    if (otherTpaSource.isPresent() && otherTpaSource.get().equals(you.getUUID())) {
                        return teleportToEntity(source, other, you);
                    } else {
                        throw ERROR_TELEPORT_ACCEPT_MISMATCH.create();
                    }
                } else {
                    throw ERROR_TELEPORT_ACCEPT_TARGET.create();
                }
            } else if (yourTpaSource.isPresent() && !yourTpaSource.get().equals(you.getUUID())) { // tpa
                ServerPlayer other = source.getServer().getPlayerList().getPlayer(yourTpaSource.get());
                if (other != null) {
                    Optional<UUID> otherTpaTarget = other.getData(OzoneDataAttachments.PLAYER).getTpaTarget();
                    if (otherTpaTarget.isPresent() && otherTpaTarget.get().equals(you.getUUID())) {
                        return teleportToEntity(source, you, other);
                    } else {
                        throw ERROR_TELEPORT_ACCEPT_MISMATCH.create();
                    }
                } else {
                    throw ERROR_TELEPORT_ACCEPT_SOURCE.create();
                }
            }
        }
        throw ERROR_TELEPORT_ACCEPT.create();
    }

    // When someone is trying to teleport to you
    public static int deny(CommandSourceStack source) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer you) {
            Optional<UUID> yourTpaSource = you.getData(OzoneDataAttachments.PLAYER).getTpaSource();
            if (yourTpaSource.isPresent()) {
                ServerPlayer other = source.getServer().getPlayerList().getPlayer(yourTpaSource.get());
                if (other != null) {
                    Optional<UUID> otherTpaTarget = other.getData(OzoneDataAttachments.PLAYER).getTpaTarget();
                    if (otherTpaTarget.isPresent() && otherTpaTarget.get().equals(you.getUUID())) {
                        you.getData(OzoneDataAttachments.PLAYER).clearTpaSource();
                        other.getData(OzoneDataAttachments.PLAYER).clearTpaTarget();

                        source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.deny.source", other.getDisplayName()), false);
                        other.sendSystemMessage(Component.translatable("commands.ozone_utilities.deny.target", you.getDisplayName()));
                        return 1;
                    } else {
                        throw ERROR_TELEPORT_DENY_CANCELLED.create();
                    }
                } else {
                    throw ERROR_TELEPORT_ACCEPT_SOURCE.create();
                }
            }
        }
        throw ERROR_TELEPORT_DENY.create();
    }

    // When you are trying to teleport to someone
    public static int cancel(CommandSourceStack source) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer you) {
            Optional<UUID> yourTpaTarget = you.getData(OzoneDataAttachments.PLAYER).getTpaTarget();
            if (yourTpaTarget.isPresent()) {
                ServerPlayer other = source.getServer().getPlayerList().getPlayer(yourTpaTarget.get());
                if (other != null) {
                    Optional<UUID> otherTpaSource = other.getData(OzoneDataAttachments.PLAYER).getTpaSource();
                    if (otherTpaSource.isPresent() && otherTpaSource.get().equals(you.getUUID())) {
                        you.getData(OzoneDataAttachments.PLAYER).clearTpaTarget();
                        other.getData(OzoneDataAttachments.PLAYER).clearTpaSource();

                        source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.tpa.cancel.source", other.getDisplayName()), false);
                        other.sendSystemMessage(Component.translatable("commands.ozone_utilities.tpa.cancel.target", you.getDisplayName()));
                    } else {
                        throw ERROR_TELEPORT_CANCEL_DENIED.create();
                    }
                } else {
                    throw ERROR_TELEPORT_ACCEPT_TARGET.create();
                }
            }
        }
        throw ERROR_TELEPORT_CANCEL.create();
    }

    private static int teleportToEntity(CommandSourceStack stack, ServerPlayer teleportTo, ServerPlayer entity) {
        OzoneCommands.performTeleport(entity, (ServerLevel) teleportTo.level(), teleportTo.getX(), teleportTo.getY(), teleportTo.getZ(), teleportTo.getYRot(), teleportTo.getXRot());
        stack.sendSuccess(() -> Component.translatable("commands.ozone_utilities.tpa.accept.source", teleportTo.getDisplayName()), false);
        teleportTo.sendSystemMessage(Component.translatable("commands.ozone_utilities.tpa.accept.target", entity.getDisplayName()));
        return 1;
    }
}
