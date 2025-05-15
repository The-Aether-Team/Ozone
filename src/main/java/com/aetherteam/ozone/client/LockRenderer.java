package com.aetherteam.ozone.client;

import com.aetherteam.ozone.Ozone;
import com.aetherteam.ozone.attachment.OzoneDataAttachments;
import com.aetherteam.ozone.item.OzoneItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import javax.annotation.Nullable;
import java.util.*;

@EventBusSubscriber(modid = Ozone.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class LockRenderer {
    private static final HashMap<Integer, List<BlockPos>> positionsForTypes = new HashMap<>();

    @SubscribeEvent
    public static void onRenderLevelLast(RenderLevelStageEvent event) {
        RenderLevelStageEvent.Stage stage = event.getStage();
        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();
        Frustum frustum = event.getFrustum();
        Minecraft minecraft = Minecraft.getInstance();
        if (stage == RenderLevelStageEvent.Stage.AFTER_PARTICLES && minecraft.level != null) {
            LocalPlayer player = minecraft.player;
            ClientLevel level = minecraft.level;
            RenderBuffers renderBuffers = minecraft.renderBuffers();
            int range = 32; // Range for how far the overlays can be rendered at.
            if (player != null) {
                BlockPos playerPos = player.blockPosition();
                ItemStack stack = player.getMainHandItem();
                // Check to remove overlays from the map.
                updatePositions(player, playerPos, level, range, !stack.is(OzoneItems.CONTAINER_KEY)); // Check to add overlays to the map.
                for (int i = 0; i < positionsForTypes.size(); i++) {
                    renderOverlays(level, poseStack, renderBuffers, camera, frustum, i); // Render any overlays at positions in the map.
                }
            }
        }
    }

    private static void updatePositions(Player player, BlockPos playerPos, ClientLevel level, int range, boolean depopulate) {
        // Initial setup of the different IDs in the map.
        positionsForTypes.putIfAbsent(0, new ArrayList<>());
        positionsForTypes.putIfAbsent(1, new ArrayList<>());
        // Loop and select random positions to check and see whether an overlay should be rendered there.
        for (int c = 0; c < 667; ++c) {
            int x = playerPos.getX() + level.getRandom().nextInt(range) - level.getRandom().nextInt(range);
            int y = playerPos.getY() + level.getRandom().nextInt(range) - level.getRandom().nextInt(range);
            int z = playerPos.getZ() + level.getRandom().nextInt(range) - level.getRandom().nextInt(range);
            if (!depopulate) { // For checking to add overlays to the world.
                BlockPos pos = new BlockPos(x, y, z);
                int type = -1;
                if (level.getBlockEntity(pos) instanceof BaseContainerBlockEntity blockEntity) {
                    Ozone.LOGGER.info(String.valueOf(blockEntity.getData(OzoneDataAttachments.LOCKED)));
                }
                if (level.getBlockEntity(pos) instanceof BaseContainerBlockEntity blockEntity && blockEntity.getData(OzoneDataAttachments.LOCKED)) {
                    Optional<UUID> optionalUUID = blockEntity.getData(OzoneDataAttachments.OWNER.get());
                    if (optionalUUID.isPresent()) {
                        type = optionalUUID.get().equals(player.getUUID()) ? 0 : 1;
                    }
                }
                if (type != -1) { // Add an overlay if the corresponding dungeon block item is held.
                    if (!positionsForTypes.get(type).contains(pos)) {
                        positionsForTypes.get(type).add(pos);
                    }
                }
            } else { // For checking to remove overlays from the world.
                for (int i = 0; i < positionsForTypes.size(); i++) {
                    List<BlockPos> positions = positionsForTypes.get(i);
                    if (!positions.isEmpty() && level.getRandom().nextInt(10) == 0) {
                        BlockPos pos = positions.get(level.getRandom().nextInt(positions.size()));
                        positions.remove(pos);
                        positionsForTypes.put(i, positions);
                    }
                }
            }
        }
    }

    private static void renderOverlays(ClientLevel level, PoseStack poseStack, RenderBuffers renderBuffers, Camera camera, Frustum frustum, int type) {
        for (BlockPos blockPos : positionsForTypes.get(type)) {
            if (frustum.isVisible(new AABB(blockPos)) && level.getBlockState(blockPos).getRenderShape() != RenderShape.INVISIBLE) {
                drawSurfaces(renderBuffers.bufferSource(), poseStack.last(), blockPos, camera,
                        (float) (blockPos.getX() - camera.getPosition().x()) - 0.001F,
                        (float) (blockPos.getZ() - camera.getPosition().z()) - 0.001F,
                        (float) (blockPos.getX() - camera.getPosition().x()) + 1.001F,
                        (float) (blockPos.getZ() - camera.getPosition().z()) + 1.001F,
                        (float) (blockPos.getY() - camera.getPosition().y()) - 0.001F,
                        (float) (blockPos.getY() - camera.getPosition().y()) + 1.001F,
                        type);
            }
        }
        renderBuffers.bufferSource().endBatch();
    }

    private static void drawSurfaces(MultiBufferSource buffer, PoseStack.Pose pose, BlockPos blockPos, Camera camera, float startX, float startZ, float endX, float endZ, float botY, float topY, int type) {
        VertexConsumer builder = buffer.getBuffer(RenderType.cutout());
        TextureAtlasSprite sprite = spriteForId(type);

        if (sprite != null) {
            float minU = sprite.getU1();
            float maxU = sprite.getU0();
            float minV = sprite.getV1();
            float maxV = sprite.getV0();

            // Renders an overlay on the bottom face of a block if the camera is below it, i.e. the camera can see the block face.
            if (camera.getPosition().y() < blockPos.getY() + botY) {
                buildVertex(builder, pose, startX, botY, startZ, minU, minV, 0, -1, 0);
                buildVertex(builder, pose, endX, botY, startZ, maxU, minV, 0, -1, 0);
                buildVertex(builder, pose, endX, botY, endZ, maxU, maxV, 0, -1, 0);
                buildVertex(builder, pose, startX, botY, endZ, minU, maxV, 0, -1, 0);
            }

            // Renders an overlay on the top face of a block if the camera is above it, i.e. the camera can see the block face.
            if (camera.getPosition().y() > blockPos.getY() + topY) {
                buildVertex(builder, pose, endX, topY, startZ, minU, minV, 0, 1, 0);
                buildVertex(builder, pose, startX, topY, startZ, maxU, minV, 0, 1, 0);
                buildVertex(builder, pose, startX, topY, endZ, maxU, maxV, 0, 1, 0);
                buildVertex(builder, pose, endX, topY, endZ, minU, maxV, 0, 1, 0);
            }

            // Renders an overlay on the north face of a block if the camera's z-coordinate is less than the block's z-coordinate, i.e. the camera can see the block face.
            if (camera.getPosition().z() < blockPos.getZ() + startZ) {
                buildVertex(builder, pose, startX, botY, startZ, minU, minV, 0, 0, -1);
                buildVertex(builder, pose, startX, topY, startZ, minU, maxV, 0, 0, -1);
                buildVertex(builder, pose, endX, topY, startZ, maxU, maxV, 0, 0, -1);
                buildVertex(builder, pose, endX, botY, startZ, maxU, minV, 0, 0, -1);
            }

            // Renders an overlay on the south face of a block if the camera's z-coordinate is greater than the block's z-coordinate, i.e. the camera can see the block face.
            if (camera.getPosition().z() > blockPos.getZ() + endZ) {
                buildVertex(builder, pose, endX, botY, endZ, minU, minV, 0, 0, 1);
                buildVertex(builder, pose, endX, topY, endZ, minU, maxV, 0, 0, 1);
                buildVertex(builder, pose, startX, topY, endZ, maxU, maxV, 0, 0, 1);
                buildVertex(builder, pose, startX, botY, endZ, maxU, minV, 0, 0, 1);
            }

            // Renders an overlay on the west face of a block if the camera's x-coordinate is less than the block's x-coordinate, i.e. the camera can see the block face.
            if (camera.getPosition().x() < blockPos.getX() + startX) {
                buildVertex(builder, pose, startX, botY, endZ, minU, minV, -1, 0, 0);
                buildVertex(builder, pose, startX, topY, endZ, minU, maxV, -1, 0, 0);
                buildVertex(builder, pose, startX, topY, startZ, maxU, maxV, -1, 0, 0);
                buildVertex(builder, pose, startX, botY, startZ, maxU, minV, -1, 0, 0);
            }

            // Renders an overlay on the east face of a block if the camera's x-coordinate is greater than the block's x-coordinate, i.e. the camera can see the block face.
            if (camera.getPosition().x() > blockPos.getX() + endX) {
                buildVertex(builder, pose, endX, botY, startZ, minU, minV, 1, 0, 0);
                buildVertex(builder, pose, endX, topY, startZ, minU, maxV, 1, 0, 0);
                buildVertex(builder, pose, endX, topY, endZ, maxU, maxV, 1, 0, 0);
                buildVertex(builder, pose, endX, botY, endZ, maxU, minV, 1, 0, 0);
            }
        }
    }

    private static void buildVertex(VertexConsumer builder, PoseStack.Pose pose, float x, float y, float z, float u, float v, float normalX, float normalY, float normalZ) {
        builder.addVertex(pose, x, y, z).setColor(0xFF, 0xFF, 0xFF, 0xAA).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(240).setNormal(pose, normalX, normalY, normalZ);
    }

    @Nullable
    private static TextureAtlasSprite spriteForId(int id) {
        switch (id) {
            case 0 -> {
                return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ResourceLocation.fromNamespaceAndPath(Ozone.MODID, "block/owned_lock"));
            }
            case 1 -> {
                return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ResourceLocation.fromNamespaceAndPath(Ozone.MODID, "block/other_lock"));
            }
            default -> {
                return null;
            }
        }
    }
}
