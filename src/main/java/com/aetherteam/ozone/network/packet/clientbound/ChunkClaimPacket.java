package com.aetherteam.ozone.network.packet.clientbound;

import java.util.UUID;

import com.aetherteam.ozone.Ozone;
import com.aetherteam.ozone.attachment.OzoneChunkAttachment;
import com.aetherteam.ozone.attachment.OzoneChunkAttachment.EntityFilter;
import com.aetherteam.ozone.attachment.OzoneChunkAttachment.Filters;
import com.aetherteam.ozone.attachment.OzoneChunkAttachment.PlayerFilter;
import com.aetherteam.ozone.attachment.OzoneDataAttachments;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public sealed interface ChunkClaimPacket extends CustomPacketPayload {
    ChunkPos pos();

    void execute(IPayloadContext context);

    public record ChangeOwner(ChunkPos pos, UUID newOwner) implements ChunkClaimPacket {
        public static final Type<ChangeOwner> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Ozone.MODID, "claim_change_owner"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ChangeOwner> STREAM_CODEC = StreamCodec.composite(
            ChunkPos.STREAM_CODEC,
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
            var level = Minecraft.getInstance().level;
            if (level != null) {
                ChunkAccess chunk = level.getChunk(pos.x, pos.z);
                OzoneChunkAttachment claim = chunk.getData(OzoneDataAttachments.CHUNK);
                claim.setOwner(newOwner);
                Ozone.LOGGER.info("Successfully executed packet {}", this);
            } else {
                Ozone.LOGGER.error("Unable to execute packet {} because level was null", this);
            }
        }
    }

    public record Delete(ChunkPos pos) implements ChunkClaimPacket {
        public static final Type<Delete> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Ozone.MODID, "claim_delete"));

        public static final StreamCodec<RegistryFriendlyByteBuf, Delete> STREAM_CODEC = StreamCodec.composite(
            ChunkPos.STREAM_CODEC,
            Delete::pos,
            Delete::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        @Override
        public void execute(IPayloadContext context) {
            var level = Minecraft.getInstance().level;
            if (level != null) {
                ChunkAccess chunk = level.getChunk(pos.x, pos.z);
                OzoneChunkAttachment claim = chunk.removeData(OzoneDataAttachments.CHUNK);
                if (claim != null) {
                    claim.setOwner(null);
                    claim.setSurveyorTableLocation(null);
                    Ozone.LOGGER.info("Successfully executed packet {}", this);
                } else {
                    Ozone.LOGGER.error("Unable to execute packet {} because claim was null", this);
                }
            } else {
                Ozone.LOGGER.error("Unable to execute packet {} because level was null", this);
            }
        }
    }

    public record ChangeSurveyorTableLocation(ChunkPos pos, BlockPos newSurveyorTableLocation) implements ChunkClaimPacket {
        public static final Type<ChangeSurveyorTableLocation> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Ozone.MODID, "claim_change_surveyor_table_loc"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ChangeSurveyorTableLocation> STREAM_CODEC = StreamCodec.composite(
            ChunkPos.STREAM_CODEC,
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
            var level = Minecraft.getInstance().level;
            if (level != null) {
                ChunkAccess chunk = level.getChunk(pos.x, pos.z);
                if (chunk.hasData(OzoneDataAttachments.CHUNK)) {
                    OzoneChunkAttachment claim = chunk.getData(OzoneDataAttachments.CHUNK);
                    if (claim.hasOwner()) {
                        claim.setSurveyorTableLocation(newSurveyorTableLocation);
                        Ozone.LOGGER.info("Successfully executed packet {}", this);
                    } else {
                        Ozone.LOGGER.error("Unable to execute packet {} because claim had no owner", this);
                    }
                } else {
                    Ozone.LOGGER.error("Unable to execute packet {} because claim was null", this);
                }
            } else {
                Ozone.LOGGER.error("Unable to execute packet {} because level was null", this);
            }
        }
    }

    public record DeleteSurveyorTableLocation(ChunkPos pos) implements ChunkClaimPacket {
        public static final Type<DeleteSurveyorTableLocation> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Ozone.MODID, "claim_delete_surveyor_table_loc"));

        public static final StreamCodec<RegistryFriendlyByteBuf, DeleteSurveyorTableLocation> STREAM_CODEC = StreamCodec.composite(
            ChunkPos.STREAM_CODEC,
            DeleteSurveyorTableLocation::pos,
            DeleteSurveyorTableLocation::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        @Override
        public void execute(IPayloadContext context) {
            var level = Minecraft.getInstance().level;
            if (level != null) {
                ChunkAccess chunk = level.getChunk(pos.x, pos.z);
                if (chunk.hasData(OzoneDataAttachments.CHUNK)) {
                    OzoneChunkAttachment claim = chunk.getData(OzoneDataAttachments.CHUNK);
                    if (claim.hasOwner()) {
                        claim.setSurveyorTableLocation(null);
                        Ozone.LOGGER.info("Successfully executed packet {}", this);
                    } else {
                        Ozone.LOGGER.error("Unable to execute packet {} because claim had no owner", this);
                    }
                } else {
                    Ozone.LOGGER.error("Unable to execute packet {} because claim was null", this);
                }
            } else {
                Ozone.LOGGER.error("Unable to execute packet {} because level was null", this);
            }
        }
    }

    public record ChangeOwnerAndSurveyorTableLocation(ChunkPos pos, UUID newOwner, BlockPos newSurveyorTableLocation) implements ChunkClaimPacket {
        public static final Type<ChangeOwnerAndSurveyorTableLocation> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Ozone.MODID, "claim_change_owner_and_table_loc"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ChangeOwnerAndSurveyorTableLocation> STREAM_CODEC = StreamCodec.composite(
            ChunkPos.STREAM_CODEC,
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
            var level = Minecraft.getInstance().level;
            if (level != null) {
                ChunkAccess chunk = level.getChunk(pos.x, pos.z);
                OzoneChunkAttachment claim = chunk.getData(OzoneDataAttachments.CHUNK);
                claim.setOwner(newOwner);
                claim.setSurveyorTableLocation(newSurveyorTableLocation);
                Ozone.LOGGER.info("Successfully executed packet {}", this);
            } else {
                Ozone.LOGGER.error("Unable to execute packet {} because level was null", this);
            }
        }
    }

    public record ChangePlayerFilter(ChunkPos pos, OzoneChunkAttachment.PlayerFilterField field, PlayerFilter newFilter) implements ChunkClaimPacket {
        public static final Type<ChangePlayerFilter> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Ozone.MODID, "claim_change_player_filter"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ChangePlayerFilter> STREAM_CODEC = StreamCodec.composite(
            ChunkPos.STREAM_CODEC,
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
            var level = Minecraft.getInstance().level;
            if (level != null) {
                ChunkAccess chunk = level.getChunk(pos.x, pos.z);
                OzoneChunkAttachment claim = chunk.getData(OzoneDataAttachments.CHUNK);
                if (claim.hasOwner()) {
                    field.set(claim, newFilter);
                    Ozone.LOGGER.info("Successfully executed packet {}", this);
                } else {
                    Ozone.LOGGER.error("Unable to execute packet {} because claim had no owner", this);
                }
            } else {
                Ozone.LOGGER.error("Unable to execute packet {} because level was null", this);
            }
        }
    }

    public record ChangeEntityFilter(ChunkPos pos, OzoneChunkAttachment.EntityFilterField field, EntityFilter newFilter) implements ChunkClaimPacket {
        public static final Type<ChangeEntityFilter> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Ozone.MODID, "claim_change_entity_filter"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ChangeEntityFilter> STREAM_CODEC = StreamCodec.composite(
            ChunkPos.STREAM_CODEC,
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
            var level = Minecraft.getInstance().level;
            if (level != null) {
                ChunkAccess chunk = level.getChunk(pos.x, pos.z);
                OzoneChunkAttachment claim = chunk.getData(OzoneDataAttachments.CHUNK);
                if (claim.hasOwner()) {
                    field.set(claim, newFilter);
                    Ozone.LOGGER.info("Successfully executed packet {}", this);
                } else {
                    Ozone.LOGGER.error("Unable to execute packet {} because claim had no owner", this);
                }
            } else {
                Ozone.LOGGER.error("Unable to execute packet {} because level was null", this);
            }
        }
    }

    public record ChangeFilters(ChunkPos pos, Filters filters) implements ChunkClaimPacket {
        public static final Type<ChangeFilters> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Ozone.MODID, "claim_filters"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ChangeFilters> STREAM_CODEC = StreamCodec.composite(
            ChunkPos.STREAM_CODEC,
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
            var level = Minecraft.getInstance().level;
            if (level != null) {
                ChunkAccess chunk = level.getChunk(pos.x, pos.z);
                OzoneChunkAttachment claim = chunk.getData(OzoneDataAttachments.CHUNK);
                if (claim.hasOwner()) {
                    claim.setFilters(filters);
                    Ozone.LOGGER.info("Successfully executed packet {}", this);
                } else {
                    Ozone.LOGGER.error("Unable to execute packet {} because claim had no owner", this);
                }
            } else {
                Ozone.LOGGER.error("Unable to execute packet {} because level was null", this);
            }
        }
    }
}
