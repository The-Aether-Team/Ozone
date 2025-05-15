package com.aetherteam.ozone.event.hooks;

import com.aetherteam.ozone.attachment.OzoneDataAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public class PlayerHooks {
    public static void playerTeleport(ServerPlayer serverPlayer, double x, double y, double z) {
        serverPlayer.getData(OzoneDataAttachments.PLAYER).setPreviousPosition(BlockPos.containing(x, y, z));
    }
}
