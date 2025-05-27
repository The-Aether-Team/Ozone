package com.aetherteam.ozone.command.teleport;

import com.aetherteam.ozone.attachment.OzoneDataAttachments;
import com.aetherteam.ozone.attachment.OzoneLevelAttachment;
import com.aetherteam.ozone.command.OzoneCommands;
import com.aetherteam.ozone.command.argument.WarpArgument;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
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

import java.util.Map;

public class WarpCommand {
    private static final SimpleCommandExceptionType ERROR_WARP = new SimpleCommandExceptionType(Component.translatable("commands.ozone_utilities.warp.error"));
    private static final SimpleCommandExceptionType ERROR_WARP_DOESNT_EXIST = new SimpleCommandExceptionType(Component.translatable("commands.ozone_utilities.warp.error.doesnt_exist"));
    private static final SimpleCommandExceptionType ERROR_WARP_ALREADY_EXISTS = new SimpleCommandExceptionType(Component.translatable("commands.ozone_utilities.warp.error.already_exists"));
    private static final SimpleCommandExceptionType ERROR_WARP_SET = new SimpleCommandExceptionType(Component.translatable("commands.ozone_utilities.warp.set.error"));
    private static final SimpleCommandExceptionType ERROR_WARP_DELETE = new SimpleCommandExceptionType(Component.translatable("commands.ozone_utilities.warp.delete.error"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("warp").then(Commands.argument("warps", WarpArgument.warp()).executes(stack -> teleportToWarp(stack.getSource(), WarpArgument.getWarp(stack, "warps")))));
        dispatcher.register(Commands.literal("setwarp").then(Commands.argument("name", StringArgumentType.word()).executes(stack -> setWarp(stack.getSource(), StringArgumentType.getString(stack, "name"))).requires((source) -> source.hasPermission(3))));
        dispatcher.register(Commands.literal("delwarp").then(Commands.argument("warps", WarpArgument.warp()).executes(stack -> deleteWarp(stack.getSource(), WarpArgument.getWarp(stack, "warps"))).requires((source) -> source.hasPermission(3))));
    }

    private static int teleportToWarp(CommandSourceStack source, String warpName) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer sender) {
            ServerLevel overworld = source.getServer().overworld();
            if (overworld != null) {
                Map<String, GlobalPos> warps = overworld.getData(OzoneDataAttachments.LEVEL).getWarps();
                if (warps.containsKey(warpName)) {
                    GlobalPos globalPos = warps.get(warpName);
                    ServerLevel targetLevel = source.getServer().getLevel(globalPos.dimension());
                    if (targetLevel != null) {
                        OzoneCommands.performTeleport(sender, targetLevel, globalPos.pos().getX(), globalPos.pos().getY(), globalPos.pos().getZ(), sender.getYRot(), sender.getXRot());
                        source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.warp", sender.getDisplayName(), warpName), false);
                        return 1;
                    }
                } else {
                    throw ERROR_WARP_DOESNT_EXIST.create();
                }
            }
        }
        throw ERROR_WARP.create();
    }

    private static int setWarp(CommandSourceStack source, String warpName) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer sender) {
            ServerLevel overworld = source.getServer().overworld();
            if (overworld != null) {
                Map<String, GlobalPos> warps = overworld.getData(OzoneDataAttachments.LEVEL).getWarps();
                if (!warps.containsKey(warpName)) {
                    ResourceKey<Level> dimension = sender.level().dimension();
                    BlockPos pos = OzoneCommands.getCorrectPosition(sender);
                    overworld.getData(OzoneDataAttachments.LEVEL).addWarp(warpName, GlobalPos.of(dimension, pos));
                    source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.warp.set", warpName, pos.getX(), pos.getY(), pos.getZ(), dimension.location().toString()), true);
                    return 1;
                } else {
                    throw ERROR_WARP_ALREADY_EXISTS.create();
                }
            }
        }
        throw ERROR_WARP_SET.create();
    }

    private static int deleteWarp(CommandSourceStack source, String warpName) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer sender) {
            ServerLevel overworld = source.getServer().overworld();
            if (overworld != null) {
                Map<String, GlobalPos> warps = overworld.getData(OzoneDataAttachments.LEVEL).getWarps();
                if (warps.containsKey(warpName)) {
                    overworld.getData(OzoneDataAttachments.LEVEL).removeWarp(warpName);
                    source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.warp.delete", warpName), true);
                    return 1;
                } else {
                    throw ERROR_WARP_DOESNT_EXIST.create();
                }
            }
        }
        throw ERROR_WARP_DELETE.create();
    }
}
