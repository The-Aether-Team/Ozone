package com.aetherteam.ozone.event.hooks;

import com.aetherteam.ozone.attachment.OzoneDataAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class PlayerHooks {
    public static void playerTeleport(ServerPlayer serverPlayer, ResourceKey<Level> dimension, double x, double y, double z) {
        serverPlayer.getData(OzoneDataAttachments.PLAYER).setPreviousPosition(GlobalPos.of(dimension, BlockPos.containing(x, y, z)));
    }
}
