package com.aetherteam.ozone.command.teleport;

import com.aetherteam.ozone.attachment.OzoneDataAttachments;
import com.aetherteam.ozone.command.OzoneCommands;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public class WarpCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) { //todo warp argument also probably make separate command names for each.
        dispatcher.register(Commands.literal("warp").then(Commands.argument("name", StringArgumentType.word()).executes(stack -> teleportToWarp(stack.getSource(), StringArgumentType.getString(stack, "name"))))
                .then(Commands.literal("set").then(Commands.argument("name", StringArgumentType.word()).executes(stack -> setWarp(stack.getSource(), StringArgumentType.getString(stack, "name"))).requires((source) -> source.hasPermission(4))))
                .then(Commands.literal("delete").then(Commands.argument("name", StringArgumentType.word()).executes(stack -> deleteWarp(stack.getSource(), StringArgumentType.getString(stack, "name"))).requires((source) -> source.hasPermission(4))))
                .then(Commands.literal("list").executes(stack -> listWarps(stack.getSource())))
        );
    }

    private static int teleportToWarp(CommandSourceStack source, String warpName) throws CommandSyntaxException { //todo success sending
        if (source.getEntityOrException() instanceof ServerPlayer sender) {
            Map<String, BlockPos> warps = source.getLevel().getData(OzoneDataAttachments.LEVEL).getWarps();
            if (warps.containsKey(warpName)) {
                BlockPos pos = warps.get(warpName);
                OzoneCommands.performTeleport(sender, source.getLevel(), pos.getX(), pos.getY(), pos.getZ(), sender.getYRot(), sender.getXRot());
            }
        }
        return 1;
    }

    private static int setWarp(CommandSourceStack source, String warpName) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer sender) {
            Map<String, BlockPos> warps = source.getLevel().getData(OzoneDataAttachments.LEVEL).getWarps();
            if (!warps.containsKey(warpName)) {
                source.getLevel().getData(OzoneDataAttachments.LEVEL).addWarp(warpName, sender.blockPosition());
            }
        }
        return 1;
    }

    private static int deleteWarp(CommandSourceStack source, String warpName) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer sender) {
            Map<String, BlockPos> warps = source.getLevel().getData(OzoneDataAttachments.LEVEL).getWarps();
            if (warps.containsKey(warpName)) {
                source.getLevel().getData(OzoneDataAttachments.LEVEL).removeWarp(warpName);
            }
        }
        return 1;
    }

    private static int listWarps(CommandSourceStack source) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer sender) {
            Map<String, BlockPos> warps = source.getLevel().getData(OzoneDataAttachments.LEVEL).getWarps();
            source.sendSuccess(() -> Component.literal(warps.keySet().toString()), true); //todo better output listing.
        }
        return 1;
    }
}
