package com.aetherteam.ozone.command.teleport;

import com.aetherteam.ozone.attachment.OzoneDataAttachments;
import com.aetherteam.ozone.command.OzoneCommands;
import com.aetherteam.ozone.command.argument.WarpArgument;
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
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("warp").then(Commands.argument("warps", WarpArgument.warp()).executes(stack -> teleportToWarp(stack.getSource(), WarpArgument.getWarp(stack, "warps")))));
        dispatcher.register(Commands.literal("setwarp").then(Commands.argument("name", StringArgumentType.word()).executes(stack -> setWarp(stack.getSource(), StringArgumentType.getString(stack, "name"))).requires((source) -> source.hasPermission(4))));
        dispatcher.register(Commands.literal("delwarp").then(Commands.argument("warps", WarpArgument.warp()).executes(stack -> deleteWarp(stack.getSource(), WarpArgument.getWarp(stack, "warps"))).requires((source) -> source.hasPermission(4))));
    }

    private static int teleportToWarp(CommandSourceStack source, String warpName) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer sender) {
            Map<String, BlockPos> warps = source.getLevel().getData(OzoneDataAttachments.LEVEL).getWarps();
            if (warps.containsKey(warpName)) {
                BlockPos pos = warps.get(warpName);
                OzoneCommands.performTeleport(sender, source.getLevel(), pos.getX(), pos.getY(), pos.getZ(), sender.getYRot(), sender.getXRot());
                source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.warp", sender.getDisplayName(), warpName), false);
            }
        }
        return 1;
    }

    private static int setWarp(CommandSourceStack source, String warpName) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer sender) {
            Map<String, BlockPos> warps = source.getLevel().getData(OzoneDataAttachments.LEVEL).getWarps();
            if (!warps.containsKey(warpName)) {
                BlockPos pos = sender.blockPosition();
                source.getLevel().getData(OzoneDataAttachments.LEVEL).addWarp(warpName, pos);
                source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.warp.set", warpName, pos.getX(), pos.getY(), pos.getZ()), true);
            }
        }
        return 1;
    }

    private static int deleteWarp(CommandSourceStack source, String warpName) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer sender) {
            Map<String, BlockPos> warps = source.getLevel().getData(OzoneDataAttachments.LEVEL).getWarps();
            if (warps.containsKey(warpName)) {
                source.getLevel().getData(OzoneDataAttachments.LEVEL).removeWarp(warpName);
                source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.warp.delete", warpName), true);
            }
        }
        return 1;
    }
}
