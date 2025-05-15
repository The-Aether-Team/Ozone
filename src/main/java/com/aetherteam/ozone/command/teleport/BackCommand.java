package com.aetherteam.ozone.command.teleport;

import com.aetherteam.ozone.attachment.OzoneDataAttachments;
import com.aetherteam.ozone.command.OzoneCommands;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class BackCommand {
    private static final SimpleCommandExceptionType ERROR_BACK = new SimpleCommandExceptionType(Component.translatable("commands.ozone_utilities.back.error"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("back").executes((context) -> returnToPreviousPosition(context.getSource())));
    }

    private static int returnToPreviousPosition(CommandSourceStack source) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer sender) {
            Optional<GlobalPos> previousPosition = sender.getData(OzoneDataAttachments.PLAYER.get()).getPreviousPosition();
            if (previousPosition.isPresent()) {
                GlobalPos globalPos = previousPosition.get();
                ServerLevel targetLevel = source.getServer().getLevel(globalPos.dimension());
                if (targetLevel != null) {
                    OzoneCommands.performTeleport(sender, targetLevel, globalPos.pos().getX(), globalPos.pos().getY(), globalPos.pos().getZ(), sender.getYRot(), sender.getXRot());
                    source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.back"), false);
                    return 1;
                }
            }
        }
        throw ERROR_BACK.create();
    }
}
