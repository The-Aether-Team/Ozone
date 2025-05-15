package com.aetherteam.ozone.command.teleport;

import com.aetherteam.ozone.attachment.OzoneDataAttachments;
import com.aetherteam.ozone.command.OzoneCommands;
import com.aetherteam.ozone.command.argument.HomeArgument;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public class HomeCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("home").then(Commands.argument("homes", HomeArgument.home()).executes(stack -> teleportToHome(stack.getSource(), HomeArgument.getHome(stack, "homes")))));
        dispatcher.register(Commands.literal("sethome").then(Commands.argument("name", StringArgumentType.word()).executes(stack -> setHome(stack.getSource(), StringArgumentType.getString(stack, "name")))));
        dispatcher.register(Commands.literal("delhome").then(Commands.argument("homes", HomeArgument.home()).executes(stack -> deleteHome(stack.getSource(), HomeArgument.getHome(stack, "homes")))));
    }

    private static int teleportToHome(CommandSourceStack source, String homeName) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer sender) {
            Map<String, BlockPos> homes = sender.getData(OzoneDataAttachments.PLAYER).getHomes();
            if (homes.containsKey(homeName)) {
                BlockPos pos = homes.get(homeName);
                OzoneCommands.performTeleport(sender, source.getLevel(), pos.getX(), pos.getY(), pos.getZ(), sender.getYRot(), sender.getXRot());
                source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.home", sender.getDisplayName(), homeName), false);
            }
        }
        return 1;
    }

    private static int setHome(CommandSourceStack source, String homeName) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer sender) {
            Map<String, BlockPos> homes = sender.getData(OzoneDataAttachments.PLAYER).getHomes();
            if (!homes.containsKey(homeName)) {
                BlockPos pos = sender.blockPosition();
                sender.getData(OzoneDataAttachments.PLAYER).addHome(sender, homeName, pos);
                source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.home.set", homeName, pos.getX(), pos.getY(), pos.getZ()), false);
            }
        }
        return 1;
    }

    private static int deleteHome(CommandSourceStack source, String homeName) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer sender) {
            Map<String, BlockPos> homes = sender.getData(OzoneDataAttachments.PLAYER).getHomes();
            if (homes.containsKey(homeName)) {
                sender.getData(OzoneDataAttachments.PLAYER).removeHome(sender, homeName);
                source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.home.delete", homeName), false);
            }
        }
        return 1;
    }
}
