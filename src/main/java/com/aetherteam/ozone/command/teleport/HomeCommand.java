package com.aetherteam.ozone.command.teleport;

import com.aetherteam.ozone.attachment.OzoneDataAttachments;
import com.aetherteam.ozone.command.OzoneCommands;
import com.aetherteam.ozone.command.argument.HomeArgument;
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

public class HomeCommand { //todo limits
    private static final SimpleCommandExceptionType ERROR_HOME = new SimpleCommandExceptionType(Component.translatable("commands.ozone_utilities.home.error"));
    private static final SimpleCommandExceptionType ERROR_HOME_DOESNT_EXIST = new SimpleCommandExceptionType(Component.translatable("commands.ozone_utilities.home.error.doesnt_exist"));
    private static final SimpleCommandExceptionType ERROR_HOME_ALREADY_EXISTS = new SimpleCommandExceptionType(Component.translatable("commands.ozone_utilities.home.error.already_exists"));
    private static final SimpleCommandExceptionType ERROR_HOME_SET = new SimpleCommandExceptionType(Component.translatable("commands.ozone_utilities.home.set.error"));
    private static final SimpleCommandExceptionType ERROR_HOME_DELETE = new SimpleCommandExceptionType(Component.translatable("commands.ozone_utilities.home.delete.error"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("home").executes(stack -> teleportToHome(stack.getSource(), "home"))
                .then(Commands.argument("homes", HomeArgument.home()).executes(stack -> teleportToHome(stack.getSource(), HomeArgument.getHome(stack, "homes")))));
        dispatcher.register(Commands.literal("sethome").executes(stack -> setHome(stack.getSource(), "home"))
                .then(Commands.argument("name", StringArgumentType.word()).executes(stack -> setHome(stack.getSource(), StringArgumentType.getString(stack, "name")))));
        dispatcher.register(Commands.literal("delhome").executes(stack -> deleteHome(stack.getSource(), "home"))
                .then(Commands.argument("homes", HomeArgument.home()).executes(stack -> deleteHome(stack.getSource(), HomeArgument.getHome(stack, "homes")))));
    }

    private static int teleportToHome(CommandSourceStack source, String homeName) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer sender) {
            Map<String, GlobalPos> homes = sender.getData(OzoneDataAttachments.PLAYER).getHomes();
            if (homes.containsKey(homeName)) {
                GlobalPos globalPos = homes.get(homeName);
                ServerLevel targetLevel = source.getServer().getLevel(globalPos.dimension());
                if (targetLevel != null) {
                    OzoneCommands.performTeleport(sender, targetLevel, globalPos.pos().getX(), globalPos.pos().getY(), globalPos.pos().getZ(), sender.getYRot(), sender.getXRot());
                    source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.home", sender.getDisplayName(), homeName), false);
                    return 1;
                }
            } else {
                throw ERROR_HOME_DOESNT_EXIST.create();
            }
        }
        throw ERROR_HOME.create();
    }

    private static int setHome(CommandSourceStack source, String homeName) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer sender) {
            Map<String, GlobalPos> homes = sender.getData(OzoneDataAttachments.PLAYER).getHomes();
            if (!homes.containsKey(homeName)) {
                ResourceKey<Level> dimension = sender.level().dimension();
                BlockPos pos = OzoneCommands.getCorrectPosition(sender);
                sender.getData(OzoneDataAttachments.PLAYER).addHome(sender, homeName, GlobalPos.of(dimension, pos));
                source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.home.set", homeName, pos.getX(), pos.getY(), pos.getZ(), dimension.location().toString()), false);
                return 1;
            } else {
                throw ERROR_HOME_ALREADY_EXISTS.create();
            }
        }
        throw ERROR_HOME_SET.create();
    }

    private static int deleteHome(CommandSourceStack source, String homeName) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer sender) {
            Map<String, GlobalPos> homes = sender.getData(OzoneDataAttachments.PLAYER).getHomes();
            if (homes.containsKey(homeName)) {
                sender.getData(OzoneDataAttachments.PLAYER).removeHome(sender, homeName);
                source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.home.delete", homeName), false);
                return 1;
            } else {
                throw ERROR_HOME_DOESNT_EXIST.create();
            }
        }
        throw ERROR_HOME_DELETE.create();
    }
}
