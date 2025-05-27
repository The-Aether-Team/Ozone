package com.aetherteam.ozone.network.packet.clientbound;

import java.util.UUID;

import javax.annotation.Nullable;

import com.aetherteam.ozone.Ozone;
import com.aetherteam.ozone.attachment.GlobalChunkPos;
import com.aetherteam.ozone.attachment.OzoneChunkAttachment;
import com.aetherteam.ozone.attachment.OzoneChunkAttachment.EntityFilter;
import com.aetherteam.ozone.attachment.OzoneChunkAttachment.Filters;
import com.aetherteam.ozone.attachment.OzoneChunkAttachment.PlayerFilter;
import com.aetherteam.ozone.attachment.OzoneDataAttachments;
import com.aetherteam.ozone.attachment.OzoneLevelAttachment;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public sealed interface ChunkClaimPacket extends CustomPacketPayload {
    GlobalChunkPos pos();

    void execute(IPayloadContext context);

    public record ChangeOwner(GlobalChunkPos pos, UUID newOwner) implements ChunkClaimPacket {
        public static final Type<ChangeOwner> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Ozone.MODID, "claim_change_owner"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ChangeOwner> STREAM_CODEC = StreamCodec.composite(
            GlobalChunkPos.STREAM_CODEC,
            ChangeOwner::pos,
            UUIDUtil.STREAM_CODEC,
            ChangeOwner::newOwner,
            ChangeOwner::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        @Override
        public void execute(IPayloadContext context) {
            Level level = context.player().level();
            if (level != null && level.dimension().location().equals(pos.dimension().location())) {
                ChunkAccess chunk = level.getChunk(pos.getX(), pos.getZ());
                OzoneChunkAttachment claim = chunk.getData(OzoneDataAttachments.CHUNK);
                var originalOwnerOpt = claim.getOwner();
                try {
                    claim.changeOwner(newOwner);
                    chunk.setData(OzoneDataAttachments.CHUNK, claim);
                    Level overworld;
                    Player player = context.player();
                    UUID originalOwner = originalOwnerOpt.get();
                    if (!originalOwner.equals(newOwner) && (overworld = getOverworld(level, player)) != null) {
                        if (originalOwner.equals(player.getUUID())) {
                            OzoneLevelAttachment levelData = overworld.getData(OzoneDataAttachments.LEVEL);
                            levelData.removePlayerClaim(originalOwner, pos);
                        } else if (newOwner.equals(player.getUUID())) {
                            OzoneLevelAttachment levelData = overworld.getData(OzoneDataAttachments.LEVEL);
                            levelData.addPlayerClaim(newOwner, pos);
                        }
                    }
                } catch (IllegalStateException e) {
                    Ozone.LOGGER.error("Unable to execute packet " + this + " because of IllegalStateException", e);
                    return;
                }
                Ozone.LOGGER.info("Successfully executed packet {}", this);
            } else {
                Ozone.LOGGER.error("Unable to execute packet {} because level was null or dimensions differ", this);
            }
        }
    }

    public record Delete(GlobalChunkPos pos) implements ChunkClaimPacket {
        public static final Type<Delete> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Ozone.MODID, "claim_delete"));

        public static final StreamCodec<RegistryFriendlyByteBuf, Delete> STREAM_CODEC = StreamCodec.composite(
            GlobalChunkPos.STREAM_CODEC,
            Delete::pos,
            Delete::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        @Override
        public void execute(IPayloadContext context) {
            Level level = context.player().level();
            if (level != null && level.dimension().location().equals(pos.dimension().location())) {
                ChunkAccess chunk = level.getChunk(pos.getX(), pos.getZ());
                if (chunk.hasData(OzoneDataAttachments.CHUNK)) {
                    OzoneChunkAttachment claim = chunk.getData(OzoneDataAttachments.CHUNK);
                    claim.getOwnerInfo().ifPresent(owner -> {
                        Level overworld;
                        Player player = context.player();
                        if (player.getUUID().equals(owner.getUUID()) && (overworld = getOverworld(level, player)) != null) {
                            OzoneLevelAttachment levelData = overworld.getData(OzoneDataAttachments.LEVEL);
                            levelData.removePlayerClaim(player.getUUID(), pos);
                        }
                        claim.removeOwner();
                    });
                    Ozone.LOGGER.info("Successfully executed packet {}", this);
                } else {
                    Ozone.LOGGER.error("Unable to execute packet {} because claim was null", this);
                }
            } else {
                Ozone.LOGGER.error("Unable to execute packet {} because level was null or dimensions differ", this);
            }
        }
    }

    public record ChangeSurveyorTableLocation(GlobalChunkPos pos, BlockPos newSurveyorTableLocation) implements ChunkClaimPacket {
        public static final Type<ChangeSurveyorTableLocation> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Ozone.MODID, "claim_change_surveyor_table_loc"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ChangeSurveyorTableLocation> STREAM_CODEC = StreamCodec.composite(
            GlobalChunkPos.STREAM_CODEC,
            ChangeSurveyorTableLocation::pos,
            BlockPos.STREAM_CODEC,
            ChangeSurveyorTableLocation::newSurveyorTableLocation,
            ChangeSurveyorTableLocation::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        @Override
        public void execute(IPayloadContext context) {
            Level level = context.player().level();
            if (level != null && level.dimension().location().equals(pos.dimension().location())) {
                ChunkAccess chunk = level.getChunk(pos.getX(), pos.getZ());
                if (chunk.hasData(OzoneDataAttachments.CHUNK)) {
                    OzoneChunkAttachment claim = chunk.getData(OzoneDataAttachments.CHUNK);
                    if (claim.hasOwner()) {
                        try {
                            claim.changeSurveyorTableLocation(newSurveyorTableLocation);
                            chunk.setData(OzoneDataAttachments.CHUNK, claim);
                        } catch (IllegalStateException e) {
                            Ozone.LOGGER.error("Unable to execute packet " + this + " because of IllegalStateException", e);
                            return;
                        } catch (IllegalArgumentException e) {
                            Ozone.LOGGER.error("Unable to execute packet " + this + " because of IllegalArgumentException", e);
                            return;
                        }
                        Ozone.LOGGER.info("Successfully executed packet {}", this);
                    } else {
                        Ozone.LOGGER.error("Unable to execute packet {} because claim had no owner", this);
                    }
                } else {
                    Ozone.LOGGER.error("Unable to execute packet {} because claim was null", this);
                }
            } else {
                Ozone.LOGGER.error("Unable to execute packet {} because level was null or dimensions differ", this);
            }
        }
    }

    public record ChangeOwnerAndSurveyorTableLocation(GlobalChunkPos pos, UUID newOwner, BlockPos newSurveyorTableLocation) implements ChunkClaimPacket {
        public static final Type<ChangeOwnerAndSurveyorTableLocation> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Ozone.MODID, "claim_change_owner_and_table_loc"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ChangeOwnerAndSurveyorTableLocation> STREAM_CODEC = StreamCodec.composite(
            GlobalChunkPos.STREAM_CODEC,
            ChangeOwnerAndSurveyorTableLocation::pos,
            UUIDUtil.STREAM_CODEC,
            ChangeOwnerAndSurveyorTableLocation::newOwner,
            BlockPos.STREAM_CODEC,
            ChangeOwnerAndSurveyorTableLocation::newSurveyorTableLocation,
            ChangeOwnerAndSurveyorTableLocation::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        @Override
        public void execute(IPayloadContext context) {
            Level level = context.player().level();
            if (level != null && level.dimension().location().equals(pos.dimension().location())) {
                ChunkAccess chunk = level.getChunk(pos.getX(), pos.getZ());
                OzoneChunkAttachment claim = chunk.getData(OzoneDataAttachments.CHUNK);
                var originalOwnerOpt = claim.getOwner();
                try {
                    claim.setOwner(newOwner, newSurveyorTableLocation);
                    chunk.setData(OzoneDataAttachments.CHUNK, claim);
                    Player player = context.player();
                    originalOwnerOpt.ifPresentOrElse(
                        originalOwner -> {
                            Level overworld;
                            if (!originalOwner.equals(newOwner) && (overworld = getOverworld(level, player)) != null) {
                                if (originalOwner.equals(player.getUUID())) {
                                    OzoneLevelAttachment levelData = overworld.getData(OzoneDataAttachments.LEVEL);
                                    levelData.removePlayerClaim(originalOwner, pos);
                                } else if (newOwner.equals(player.getUUID())) {
                                    OzoneLevelAttachment levelData = overworld.getData(OzoneDataAttachments.LEVEL);
                                    levelData.addPlayerClaim(newOwner, pos);
                                }
                            }
                        },
                        () -> {
                            Level overworld;
                            if (newOwner.equals(player.getUUID()) && (overworld = getOverworld(level, player)) != null) {
                                OzoneLevelAttachment levelData = overworld.getData(OzoneDataAttachments.LEVEL);
                                levelData.addPlayerClaim(newOwner, pos);
                            }
                        }
                    );
                } catch (IllegalArgumentException e) {
                    Ozone.LOGGER.error("Unable to execute packet " + this + " because of IllegalArgumentException", e);
                    return;
                }
                Ozone.LOGGER.info("Successfully executed packet {}", this);
            } else {
                Ozone.LOGGER.error("Unable to execute packet {} because level was null or dimensions differ", this);
            }
        }
    }

    public record ChangePlayerFilter(GlobalChunkPos pos, OzoneChunkAttachment.PlayerFilterField field, PlayerFilter newFilter) implements ChunkClaimPacket {
        public static final Type<ChangePlayerFilter> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Ozone.MODID, "claim_change_player_filter"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ChangePlayerFilter> STREAM_CODEC = StreamCodec.composite(
            GlobalChunkPos.STREAM_CODEC,
            ChangePlayerFilter::pos,
            OzoneChunkAttachment.PlayerFilterField.STREAM_CODEC,
            ChangePlayerFilter::field,
            PlayerFilter.STREAM_CODEC,
            ChangePlayerFilter::newFilter,
            ChangePlayerFilter::new
        );
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        @Override
        public void execute(IPayloadContext context) {
            Level level = context.player().level();
            if (level != null && level.dimension().location().equals(pos.dimension().location())) {
                ChunkAccess chunk = level.getChunk(pos.getX(), pos.getZ());
                OzoneChunkAttachment claim = chunk.getData(OzoneDataAttachments.CHUNK);
                if (claim.hasOwner()) {
                    field.set(claim, newFilter);
                    chunk.setData(OzoneDataAttachments.CHUNK, claim);
                    Ozone.LOGGER.info("Successfully executed packet {}", this);
                } else {
                    Ozone.LOGGER.error("Unable to execute packet {} because claim had no owner", this);
                }
            } else {
                Ozone.LOGGER.error("Unable to execute packet {} because level was null or dimensions differ", this);
            }
        }
    }

    public record ChangeEntityFilter(GlobalChunkPos pos, OzoneChunkAttachment.EntityFilterField field, EntityFilter newFilter) implements ChunkClaimPacket {
        public static final Type<ChangeEntityFilter> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Ozone.MODID, "claim_change_entity_filter"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ChangeEntityFilter> STREAM_CODEC = StreamCodec.composite(
            GlobalChunkPos.STREAM_CODEC,
            ChangeEntityFilter::pos,
            OzoneChunkAttachment.EntityFilterField.STREAM_CODEC,
            ChangeEntityFilter::field,
            EntityFilter.STREAM_CODEC,
            ChangeEntityFilter::newFilter,
            ChangeEntityFilter::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        @Override
        public void execute(IPayloadContext context) {
            Level level = context.player().level();
            if (level != null && level.dimension().location().equals(pos.dimension().location())) {
                ChunkAccess chunk = level.getChunk(pos.getX(), pos.getZ());
                OzoneChunkAttachment claim = chunk.getData(OzoneDataAttachments.CHUNK);
                if (claim.hasOwner()) {
                    field.set(claim, newFilter);
                    chunk.setData(OzoneDataAttachments.CHUNK, claim);
                    Ozone.LOGGER.info("Successfully executed packet {}", this);
                } else {
                    Ozone.LOGGER.error("Unable to execute packet {} because claim had no owner", this);
                }
            } else {
                Ozone.LOGGER.error("Unable to execute packet {} because level was null or dimensions differ", this);
            }
        }
    }

    public record ChangeFilters(GlobalChunkPos pos, Filters filters) implements ChunkClaimPacket {
        public static final Type<ChangeFilters> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Ozone.MODID, "claim_filters"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ChangeFilters> STREAM_CODEC = StreamCodec.composite(
            GlobalChunkPos.STREAM_CODEC,
            ChangeFilters::pos,
            Filters.STREAM_CODEC,
            ChangeFilters::filters,
            ChangeFilters::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        @Override
        public void execute(IPayloadContext context) {
            Level level = context.player().level();
            if (level != null && level.dimension().location().equals(pos.dimension().location())) {
                ChunkAccess chunk = level.getChunk(pos.getX(), pos.getZ());
                OzoneChunkAttachment claim = chunk.getData(OzoneDataAttachments.CHUNK);
                if (claim.hasOwner()) {
                    claim.setFilters(filters);
                    chunk.setData(OzoneDataAttachments.CHUNK, claim);
                    Ozone.LOGGER.info("Successfully executed packet {}", this);
                } else {
                    Ozone.LOGGER.error("Unable to execute packet {} because claim had no owner", this);
                }
            } else {
                Ozone.LOGGER.error("Unable to execute packet {} because level was null or dimensions differ", this);
            }
        }
    }

    @Nullable
    private static Level getOverworld(Level level, Player player) {
        Level overworld = level;
        if (overworld != null && overworld.dimension() == Level.OVERWORLD) {
            return overworld;
        }
        var server = player.getServer();
        if (server == null) {
            server = level.getServer();
        }
        if (server == null) {
            return null;
        }
        return server.overworld();
    }
}
