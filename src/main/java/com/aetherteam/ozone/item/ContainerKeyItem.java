package com.aetherteam.ozone.item;

import com.aetherteam.ozone.Ozone;
import com.aetherteam.ozone.attachment.OzoneDataAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = Ozone.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ContainerKeyItem extends Item {
    public ContainerKeyItem(Properties properties) {
        super(properties);
    }

    @SubscribeEvent
    public static void onRightClickContainer(PlayerInteractEvent.RightClickBlock event) { //todo lock multipart chests together
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        if (!level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof BaseContainerBlockEntity blockEntity) {
                blockEntity.getData(OzoneDataAttachments.OWNER.get()).ifPresent(uuid -> {
                    if (player.getUUID().equals(uuid)) {
                        if (stack.is(Ozone.CONTAINER_KEY)) {
                            blockEntity.setData(OzoneDataAttachments.LOCKED.get(), !blockEntity.getData(OzoneDataAttachments.LOCKED.get())); //todo packet. gonna need to figure out how to sync on player login though
                            event.setCanceled(true);
                            event.setCancellationResult(InteractionResult.SUCCESS_SERVER);
                        }
                    } else if (blockEntity.getData(OzoneDataAttachments.LOCKED.get())) {
                        if (player.getPermissionLevel() < 1) {
                            event.setCanceled(true);
                            event.setCancellationResult(InteractionResult.FAIL);
                            player.displayClientMessage(Component.literal("test"), true);
                        }
                    }
                });
            }
        }
    }

    @SubscribeEvent
    public static void onLeftClickContainer(PlayerInteractEvent.LeftClickBlock event) { //todo lock multipart chests together
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        if (!level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof BaseContainerBlockEntity blockEntity) {
                blockEntity.getData(OzoneDataAttachments.OWNER.get()).ifPresent(uuid -> {
                    if (!player.getUUID().equals(uuid) && player.getPermissionLevel() < 1 && blockEntity.getData(OzoneDataAttachments.LOCKED.get())) {
                        event.setCanceled(true);
                        player.displayClientMessage(Component.literal("test"), true);
                    }
                });
            }
        }
    }
}
