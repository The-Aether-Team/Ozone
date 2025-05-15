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

public class HomeCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) { //todo home argument also probably make separate command names for each.
        dispatcher.register(Commands.literal("home").then(Commands.argument("name", StringArgumentType.word()).executes(stack -> teleportToHome(stack.getSource(), StringArgumentType.getString(stack, "name"))))
                .then(Commands.literal("set").then(Commands.argument("name", StringArgumentType.word()).executes(stack -> setHome(stack.getSource(), StringArgumentType.getString(stack, "name")))))
                .then(Commands.literal("delete").then(Commands.argument("name", StringArgumentType.word()).executes(stack -> deleteHome(stack.getSource(), StringArgumentType.getString(stack, "name")))))
                .then(Commands.literal("list").executes(stack -> listHomes(stack.getSource())))
        );
    }

    private static int teleportToHome(CommandSourceStack source, String homeName) throws CommandSyntaxException { //todo success sending
        if (source.getEntityOrException() instanceof ServerPlayer sender) {
            Map<String, BlockPos> homes = sender.getData(OzoneDataAttachments.PLAYER).getHomes();
            if (homes.containsKey(homeName)) {
                BlockPos pos = homes.get(homeName);
                OzoneCommands.performTeleport(sender, source.getLevel(), pos.getX(), pos.getY(), pos.getZ(), sender.getYRot(), sender.getXRot());
            }
        }
        return 1;
    }

    private static int setHome(CommandSourceStack source, String homeName) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer sender) {
            Map<String, BlockPos> homes = sender.getData(OzoneDataAttachments.PLAYER).getHomes();
            if (!homes.containsKey(homeName)) {
                sender.getData(OzoneDataAttachments.PLAYER).addHome(homeName, sender.blockPosition());
            }
        }
        return 1;
    }

    private static int deleteHome(CommandSourceStack source, String homeName) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer sender) {
            Map<String, BlockPos> homes = sender.getData(OzoneDataAttachments.PLAYER).getHomes();
            if (homes.containsKey(homeName)) {
                sender.getData(OzoneDataAttachments.PLAYER).removeHome(homeName);
            }
        }
        return 1;
    }

    private static int listHomes(CommandSourceStack source) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer sender) {
            Map<String, BlockPos> homes = sender.getData(OzoneDataAttachments.PLAYER).getHomes();
            source.sendSuccess(() -> Component.literal(homes.keySet().toString()), true); //todo better output listing.
        }
        return 1;
    }
}
