package com.aetherteam.ozone.command.teleport;

import com.aetherteam.ozone.attachment.OzoneDataAttachments;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public class BackCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("back").executes((context) -> returnToPreviousPosition(context.getSource())));
    }

    /**
     * Returns the day time (time wrapped within a day)
     */
    private static int returnToPreviousPosition(CommandSourceStack source) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer sender) {
            sender.getData(OzoneDataAttachments.PLAYER.get()).getPreviousPosition().ifPresent(blockPos -> sender.teleportTo(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
        }
        return 1;
    }
}
