package com.aetherteam.ozone.command.teleport;

import com.aetherteam.ozone.command.OzoneCommands;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;

public class RtpCommand { //todo cooldown/cost
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rtp").executes((context) -> randomTeleport(context.getSource())));
    }

    private static int randomTeleport(CommandSourceStack source) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer sender) {
            RandomSource random = sender.getRandom();
            WorldBorder border = source.getLevel().getWorldBorder();
            BlockPos newPos;
            do {
                source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.rtp.search"), false);
                double randomX = border.getCenterX() + random.nextInt((int) (border.getSize() / 2)) * (random.nextBoolean() ? 1 : -1);
                double randomZ = border.getCenterZ() + random.nextInt((int) (border.getSize() / 2)) * (random.nextBoolean() ? 1 : -1);
                double randomY = source.getLevel().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) randomX, (int) randomZ);
                BlockPos randomPos = BlockPos.containing(randomX, randomY, randomZ);
                newPos = sender.adjustSpawnLocation(source.getLevel(), randomPos);
            } while (!sender.level().canSeeSky(newPos));

            OzoneCommands.performTeleport(sender, source.getLevel(), newPos.getX(), newPos.getY(), newPos.getZ(), sender.getYRot(), sender.getXRot());
            source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.rtp"), false);
        }
        return 1;
    }
}
