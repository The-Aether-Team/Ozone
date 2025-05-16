package com.aetherteam.ozone.command.teleport;

import com.aetherteam.ozone.attachment.OzoneDataAttachments;
import com.aetherteam.ozone.attachment.OzoneLevelAttachment;
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

import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public class TpaCommand { //todo change messages and allow clicking them to confirm or deny.
    private static final SimpleCommandExceptionType ERROR_TELEPORT_SELF = new SimpleCommandExceptionType(Component.translatable("commands.ozone_utilities.tpa.error.self"));
    private static final SimpleCommandExceptionType ERROR_TELEPORT_EXISTS = new SimpleCommandExceptionType(Component.translatable("commands.ozone_utilities.tpa.error.exists"));
    private static final SimpleCommandExceptionType ERROR_TELEPORT_ACCEPT = new SimpleCommandExceptionType(Component.translatable("commands.ozone_utilities.tpa.accept.error"));
    private static final SimpleCommandExceptionType ERROR_TELEPORT_ACCEPT_NOT_FOUND = new SimpleCommandExceptionType(Component.translatable("commands.ozone_utilities.tpa.accept.error.not_found"));
    private static final SimpleCommandExceptionType ERROR_TELEPORT_DENY = new SimpleCommandExceptionType(Component.translatable("commands.ozone_utilities.tpa.deny.error"));
    private static final SimpleCommandExceptionType ERROR_TELEPORT_CANCEL = new SimpleCommandExceptionType(Component.translatable("commands.ozone_utilities.tpa.cancel.error"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tpa").then(Commands.argument("player", EntityArgument.player()).executes(stack -> request(stack.getSource(), EntityArgument.getPlayer(stack, "player")))));
        dispatcher.register(Commands.literal("tpahere").then(Commands.argument("player", EntityArgument.player()).executes(stack -> here(stack.getSource(), EntityArgument.getPlayer(stack, "player")))));

        dispatcher.register(Commands.literal("tpaaccept").executes(stack -> accept(stack.getSource())));

        dispatcher.register(Commands.literal("tpadeny").executes(stack -> deny(stack.getSource())));
        dispatcher.register(Commands.literal("tpacancel").executes(stack -> cancel(stack.getSource())));
    }

    // You teleport to other
    public static int request(CommandSourceStack source, ServerPlayer other) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer you) {
            if (!you.getUUID().equals(other.getUUID())) {
                OzoneLevelAttachment.TeleportRequest request = new OzoneLevelAttachment.TeleportRequest(you.getUUID(), other.getUUID());
                ServerLevel overworld = OzoneLevelAttachment.getOverworld(source.getServer());
                if (overworld != null) {
                    Set<OzoneLevelAttachment.TeleportRequest> queue = overworld.getData(OzoneDataAttachments.LEVEL).getTpaQueue();
                    if (!queue.contains(request)) {
                        overworld.getData(OzoneDataAttachments.LEVEL).addTpaRequest(request);

                        source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.tpa.request.source", other.getDisplayName()), false);
                        other.sendSystemMessage(Component.translatable("commands.ozone_utilities.tpa.request.target", you.getDisplayName()));
                    } else {
                        throw ERROR_TELEPORT_EXISTS.create();
                    }
                }
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
                OzoneLevelAttachment.TeleportRequest request = new OzoneLevelAttachment.TeleportRequest(other.getUUID(), you.getUUID());
                ServerLevel overworld = OzoneLevelAttachment.getOverworld(source.getServer());
                if (overworld != null) {
                    Set<OzoneLevelAttachment.TeleportRequest> queue = overworld.getData(OzoneDataAttachments.LEVEL).getTpaQueue();
                    if (!queue.contains(request)) {
                        overworld.getData(OzoneDataAttachments.LEVEL).addTpaRequest(request);

                        source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.tpa.here.source", other.getDisplayName()), false);
                        other.sendSystemMessage(Component.translatable("commands.ozone_utilities.tpa.here.target", you.getDisplayName()));
                    } else {
                        throw ERROR_TELEPORT_EXISTS.create();
                    }
                }
            } else {
                throw ERROR_TELEPORT_SELF.create();
            }
        }
        return 1;
    }

    public static int accept(CommandSourceStack source) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer you) {
            UUID yourUUID = you.getUUID();
            ServerLevel overworld = OzoneLevelAttachment.getOverworld(source.getServer());
            if (overworld != null) {
                Set<OzoneLevelAttachment.TeleportRequest> queue = overworld.getData(OzoneDataAttachments.LEVEL).getTpaQueue();

                for (Iterator<OzoneLevelAttachment.TeleportRequest> iterator = queue.iterator(); iterator.hasNext(); ) {
                    OzoneLevelAttachment.TeleportRequest request = iterator.next();
                    UUID teleportSubject = request.teleportSubject();
                    UUID teleportTarget = request.teleportTarget();

                    if (teleportSubject.equals(yourUUID) || teleportTarget.equals(yourUUID)) {
                        ServerPlayer subject = source.getServer().getPlayerList().getPlayer(teleportSubject);
                        ServerPlayer target = source.getServer().getPlayerList().getPlayer(teleportTarget);
                        if (subject != null && target != null) {
                            iterator.remove();
                            return teleportToEntity(source, target, subject);
                        } else {
                            throw ERROR_TELEPORT_ACCEPT_NOT_FOUND.create();
                        }
                    }
                }
            }
        }
        throw ERROR_TELEPORT_ACCEPT.create();
    }

    // When someone is trying to teleport to you
    public static int deny(CommandSourceStack source) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer you) {
            UUID yourUUID = you.getUUID();
            ServerLevel overworld = OzoneLevelAttachment.getOverworld(source.getServer());
            if (overworld != null) {
                Set<OzoneLevelAttachment.TeleportRequest> queue = overworld.getData(OzoneDataAttachments.LEVEL).getTpaQueue();

                for (Iterator<OzoneLevelAttachment.TeleportRequest> iterator = queue.iterator(); iterator.hasNext(); ) {
                    OzoneLevelAttachment.TeleportRequest request = iterator.next();
                    UUID teleportTarget = request.teleportTarget();

                    if (teleportTarget.equals(yourUUID)) {
                        iterator.remove();

                        ServerPlayer other = source.getServer().getPlayerList().getPlayer(request.teleportSubject());
                        if (other != null) {
                            source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.tpa.deny.source", other.getDisplayName()), false);
                            other.sendSystemMessage(Component.translatable("commands.ozone_utilities.tpa.deny.target", you.getDisplayName()));
                        } else {
                            source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.tpa.deny"), false);
                        }
                        return 1;
                    }
                }
            }
        }
        throw ERROR_TELEPORT_DENY.create();
    }

    // When you are trying to teleport to someone
    public static int cancel(CommandSourceStack source) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer you) {
            UUID yourUUID = you.getUUID();
            ServerLevel overworld = OzoneLevelAttachment.getOverworld(source.getServer());
            if (overworld != null) {
                Set<OzoneLevelAttachment.TeleportRequest> queue = overworld.getData(OzoneDataAttachments.LEVEL).getTpaQueue();

                for (Iterator<OzoneLevelAttachment.TeleportRequest> iterator = queue.iterator(); iterator.hasNext(); ) {
                    OzoneLevelAttachment.TeleportRequest request = iterator.next();
                    UUID teleportSubject = request.teleportSubject();

                    if (teleportSubject.equals(yourUUID)) {
                        iterator.remove();

                        ServerPlayer other = source.getServer().getPlayerList().getPlayer(request.teleportTarget());
                        if (other != null) {
                            source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.tpa.cancel.source", other.getDisplayName()), false);
                            other.sendSystemMessage(Component.translatable("commands.ozone_utilities.tpa.cancel.target", you.getDisplayName()));
                        } else {
                            source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.cancel"), false);
                        }
                        return 1;
                    }
                }
            }
        }
        throw ERROR_TELEPORT_CANCEL.create();
    }

    private static int teleportToEntity(CommandSourceStack stack, ServerPlayer target, ServerPlayer subject) {
        OzoneCommands.performTeleport(subject, (ServerLevel) target.level(), target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());

        stack.sendSuccess(() -> Component.translatable("commands.ozone_utilities.tpa.accept.source", target.getDisplayName()), false);
        target.sendSystemMessage(Component.translatable("commands.ozone_utilities.tpa.accept.target", subject.getDisplayName()));

        return 1;
    }
}
