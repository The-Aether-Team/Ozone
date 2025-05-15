package com.aetherteam.ozone.command.teleport;

import com.aetherteam.ozone.attachment.OzoneDataAttachments;
import com.aetherteam.ozone.command.OzoneCommands;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class BackCommand {
    private static final SimpleCommandExceptionType ERROR_BACK = new SimpleCommandExceptionType(Component.translatable("commands.ozone_utilities.back.error"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("back").executes((context) -> returnToPreviousPosition(context.getSource())));
    }

    private static int returnToPreviousPosition(CommandSourceStack source) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer sender) {
            Optional<BlockPos> previousPosition = sender.getData(OzoneDataAttachments.PLAYER.get()).getPreviousPosition();
            if (previousPosition.isPresent()) {
                BlockPos blockPos = previousPosition.get();
                OzoneCommands.teleport(sender, source.getLevel(), blockPos.getX(), blockPos.getY(), blockPos.getZ(), sender.getYRot(), sender.getXRot());
            } else {
                throw ERROR_BACK.create();
            }
        }
        return 1;
    }
}
