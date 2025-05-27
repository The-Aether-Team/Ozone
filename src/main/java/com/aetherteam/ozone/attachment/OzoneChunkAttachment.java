package com.aetherteam.ozone.attachment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.lang3.Validate;

import com.aetherteam.ozone.block.OzoneBlocks;
import com.aetherteam.ozone.network.packet.clientbound.ChunkClaimPacket;
import com.aetherteam.ozone.tags.OzoneBlockTags;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

public class OzoneChunkAttachment {
    public final ChunkPos chunkPos;
    @Nullable
    private OwnerInfo owner;
    private final Filters filters;
    
    public void resetPermissions() {
        filters.reset();
    }

    public static final Codec<OzoneChunkAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ChunkPos.CODEC.fieldOf("chunkPos").forGetter(OzoneChunkAttachment::getChunkPos),
            OwnerInfo.CODEC.optionalFieldOf("owner").forGetter(OzoneChunkAttachment::getOwnerInfo),
            Filters.CODEC.optionalFieldOf("filters").forGetter($this -> Optional.of($this.filters))
    ).apply(instance, OzoneChunkAttachment::new));

    public OzoneChunkAttachment(ChunkPos chunkPos) {
        this.chunkPos = Validate.notNull(chunkPos, "chunkPos was null");
        this.filters = new Filters();
        resetPermissions();
    }

    public OzoneChunkAttachment(ChunkPos chunkPos, Optional<OwnerInfo> owner, Optional<Filters> filters) {
        this.chunkPos = Validate.notNull(chunkPos, "chunkPos was null");
        this.owner = owner.orElse(null);
        this.filters = filters.orElseGet(Filters::new);
    }

    public void sendClaimCreatedMessageToOwner(MinecraftServer server, BlockPos claimPos) {
        var owner = this.owner;
        if (owner == null) return;
        var player = server.getPlayerList().getPlayer(owner.uuid);
        if (player != null) {
            player.displayClientMessage(Component.translatable("ozone_utilities.claim.created", claimPos.getX(), claimPos.getY(), claimPos.getZ()).withStyle(ChatFormatting.GREEN), false);
        }
    }

    public void sendClaimRemovedMessageToOwner(MinecraftServer server, BlockPos claimPos) {
        var owner = this.owner;
        if (owner == null) return;
        var player = server.getPlayerList().getPlayer(owner.uuid);
        if (player != null) {
            player.displayClientMessage(Component.translatable("ozone_utilities.claim.destroyed", claimPos.getX(), claimPos.getY(), claimPos.getZ()).withStyle(ChatFormatting.RED), false);
        }
    }

    public void sendClaimLimitReachedMessage(Player player, int limit) {
        player.displayClientMessage(Component.translatable("ozone_utilities.claim.limit_reached", (limit == 1? Component.translatable("ozone_utilities.claim.limit.singular") : Component.translatable("ozone_utilities.claim.limit.plural", Integer.toUnsignedString(limit))).withStyle(ChatFormatting.RED)).withStyle(ChatFormatting.DARK_RED), true);
    }

    public void sendDenyMessage(Player player) {
        if (owner == null) {
            sendDenyMessage(player, Component.translatable("ozone_utilities.claim.owner.no_one"));
        } else if (player.getServer() != null) {
            sendDenyMessage(player, player.getServer().getProfileCache().get(owner.uuid).map(OzoneChunkAttachment::getProfileName).or(() -> Optional.of(player.getServer().getPlayerList().getPlayer(owner.uuid).getName())).orElseGet(OzoneChunkAttachment::getSomeoneElse));
        } else {
            SkullBlockEntity.fetchGameProfile(owner.uuid).thenAcceptAsync(profile -> {
                sendDenyMessage(player, profile.map(OzoneChunkAttachment::getProfileName).orElseGet(OzoneChunkAttachment::getSomeoneElse));
            }, SkullBlockEntity.CHECKED_MAIN_THREAD_EXECUTOR);
        }
    }

    private void sendDenyMessage(Player player, Component ownerName) {
        player.displayClientMessage(Component.translatable("ozone_utilities.claim.action_blocked", ownerName).withStyle(ChatFormatting.DARK_RED), true);
    }

    private static Component getProfileName(GameProfile profile) {
        return Component.literal(profile.getName()).withStyle(ChatFormatting.RED);
    }

    private static Component getSomeoneElse() {
        return Component.translatable("ozone_utilities.claim.owner.someone_else");
    }

    public void sendSurveyorTableAlreadyExistsMessage(Player player) {
        player.displayClientMessage(Component.translatable("ozone_utilities.claim.surveyor_table_already_exists").withStyle(ChatFormatting.RED), true);
    }

    public ChunkPos getChunkPos() {
        return chunkPos;
    }

    public Optional<OwnerInfo> getOwnerInfo() {
        return Optional.ofNullable(owner);
    }

    public Optional<UUID> getOwner() {
        return owner == null? Optional.empty() : Optional.of(owner.uuid);
    }

    public void setOwner(UUID owner, BlockPos surveyorTableLocation) {
        Validate.notNull(owner, "owner was null");
        if (!chunkPos.equals(new ChunkPos(surveyorTableLocation))) {
            throw new IllegalArgumentException("desired surveyor table location (" + surveyorTableLocation.getX() + ", " + surveyorTableLocation.getY() + ", " + surveyorTableLocation.getZ() + ") is not within this object's chunk (" + chunkPos.x + ", " + chunkPos.z + ")");
        }
        if (this.owner == null) {
            this.owner = new OwnerInfo(owner, surveyorTableLocation);
        } else {
            this.owner.uuid = owner;
            this.owner.surveyorTableLocation = surveyorTableLocation;
        }
    }

    public void setOwner(ServerLevel level, UUID owner, BlockPos surveyorTableLocation) {
        setOwner(level, owner, surveyorTableLocation, true);
    }

    public void setOwner(ServerLevel level, UUID owner, BlockPos surveyorTableLocation, boolean removeOldSurveyorTable) {
        Validate.notNull(owner, "owner was null");
        if (!chunkPos.equals(new ChunkPos(surveyorTableLocation))) {
            throw new IllegalArgumentException("desired surveyor table location (" + surveyorTableLocation.getX() + ", " + surveyorTableLocation.getY() + ", " + surveyorTableLocation.getZ() + ") is not within this object's chunk (" + chunkPos.x + ", " + chunkPos.z + ")");
        }
        var pos = GlobalChunkPos.of(level.dimension(), chunkPos);
        ServerLevel overworld = level.getServer().overworld();
        OzoneLevelAttachment levelData = overworld.getData(OzoneDataAttachments.LEVEL);
        if (this.owner == null) {
            this.owner = new OwnerInfo(owner, surveyorTableLocation);
            levelData.addPlayerClaim(owner, pos);
            overworld.setData(OzoneDataAttachments.LEVEL, levelData);
            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangeOwnerAndSurveyorTableLocation(pos, owner, surveyorTableLocation));
        } else if (!owner.equals(this.owner.uuid) || !surveyorTableLocation.equals(this.owner.surveyorTableLocation)) {
            if (!owner.equals(this.owner.uuid)) {
                levelData.removePlayerClaim(this.owner.uuid, pos);
                levelData.addPlayerClaim(owner, pos);
                overworld.setData(OzoneDataAttachments.LEVEL, levelData);
            }
            this.owner.uuid = owner;
            if (surveyorTableLocation.equals(this.owner.surveyorTableLocation)) {
                PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangeOwner(pos, owner));
            } else {
                if (removeOldSurveyorTable && level.getBlockState(this.owner.surveyorTableLocation).getBlock() == OzoneBlocks.SURVEYOR_TABLE.get()) {
                    level.destroyBlock(this.owner.surveyorTableLocation, true);
                }
                this.owner.surveyorTableLocation = surveyorTableLocation;
                PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangeOwnerAndSurveyorTableLocation(pos, owner, surveyorTableLocation));
            }
        }
    }

    public void setOwner(Level level, UUID owner, BlockPos surveyorTableLocation) {
        if (level.isClientSide) {
            setOwner(owner, surveyorTableLocation);
        } else {
            setOwner((ServerLevel)level, owner, surveyorTableLocation);
        }
    }

    public void setOwner(@Nullable OwnerInfo owner) {
        if (!Objects.equals(this.owner, owner)) {
            if (owner == null) {
                removeOwner();
            } else {
                setOwner(owner.uuid, owner.surveyorTableLocation);
            }
        }
    }

    public void setOwner(ServerLevel level, @Nullable OwnerInfo owner) {
        if (!Objects.equals(this.owner, owner)) {
            if (owner == null) {
                removeOwner(level);
            } else {
                setOwner(level, owner.uuid, owner.surveyorTableLocation);
            }
        }
    }

    public void setOwner(Level level, @Nullable OwnerInfo owner) {
        if (level.isClientSide) {
            setOwner(owner);
        } else {
            setOwner((ServerLevel)level, owner);
        }
    }

    public void changeOwner(UUID owner) {
        if (this.owner == null) {
            throw new IllegalStateException("Cannot change owner until owner has been set");
        }
        this.owner.uuid = owner;
    }

    public void changeOwner(ServerLevel level, UUID owner) {
        if (this.owner == null) {
            throw new IllegalStateException("Cannot change owner until owner has been set");
        }
        if (!owner.equals(this.owner.uuid)) {
            ServerLevel overworld = level.getServer().overworld();
            OzoneLevelAttachment levelData = overworld.getData(OzoneDataAttachments.LEVEL);
            var pos = GlobalChunkPos.of(level.dimension(), chunkPos);
            levelData.removePlayerClaim(this.owner.uuid, pos);
            levelData.addPlayerClaim(owner, pos);
            overworld.setData(OzoneDataAttachments.LEVEL, levelData);
            this.owner.uuid = owner;
            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangeOwner(pos, owner));
        }
    }

    public void changeOwner(Level level, UUID owner) {
        if (level.isClientSide) {
            changeOwner(owner);
        } else {
            changeOwner((ServerLevel)level, owner);
        }
    }

    public void removeOwner() {
        if (owner != null) {
            this.owner = null;
            resetPermissions();
        }
    }

    public void removeOwner(ServerLevel level) {
        removeOwner(level, true);
    }

    public void removeOwner(ServerLevel level, boolean destroyClaimBlock) {
        if (owner != null) {
            if (destroyClaimBlock && level.getBlockState(owner.surveyorTableLocation).getBlock() == OzoneBlocks.SURVEYOR_TABLE.get()) {
                level.destroyBlock(owner.surveyorTableLocation, true);
            }
            var pos = GlobalChunkPos.of(level.dimension(), chunkPos);
            ServerLevel overworld = level.getServer().overworld();
            OzoneLevelAttachment levelData = overworld.getData(OzoneDataAttachments.LEVEL);
            levelData.removePlayerClaim(owner.uuid, pos);
            overworld.setData(OzoneDataAttachments.LEVEL, levelData);
            this.owner = null;
            resetPermissions();
            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.Delete(pos));
        }
    }

    public void removeOwner(Level level) {
        if (level.isClientSide) {
            removeOwner();
        } else {
            removeOwner((ServerLevel)level);
        }
    }

    public boolean hasOwner() {
        return owner != null;
    }

    public boolean isOwner(@Nullable UUID uuid) {
        var owner = this.owner;
        return owner == null? uuid == null : owner.uuid.equals(uuid);
    }

    public boolean isOwner(@Nullable Entity entity) {
        return isOwner(entity == null? null : entity.getUUID());
    }

    public Optional<BlockPos> getSurveyorTableLocation() {
        return owner == null? Optional.empty() : Optional.of(owner.surveyorTableLocation);
    }

    public void changeSurveyorTableLocation(BlockPos surveyorTableLocation) {
        if (owner == null) {
            throw new IllegalStateException("Cannot change surveyorTableLocation until owner has been set");
        }
        if (!chunkPos.equals(new ChunkPos(surveyorTableLocation))) {
            throw new IllegalArgumentException("desired surveyor table location (" + surveyorTableLocation.getX() + ", " + surveyorTableLocation.getY() + ", " + surveyorTableLocation.getZ() + ") is not within this object's chunk (" + chunkPos.x + ", " + chunkPos.z + ")");
        }
        owner.surveyorTableLocation = surveyorTableLocation;
    }

    public void changeSurveyorTableLocation(ServerLevel level, BlockPos surveyorTableLocation) {
        var owner = this.owner;
        if (owner == null) {
            throw new IllegalStateException("Cannot change surveyorTableLocation until owner has been set");
        }
        if (!surveyorTableLocation.equals(owner.surveyorTableLocation)) {
            if (!this.chunkPos.equals(new ChunkPos(surveyorTableLocation))) {
                throw new IllegalArgumentException("desired surveyor table location (" + surveyorTableLocation.getX() + ", " + surveyorTableLocation.getY() + ", " + surveyorTableLocation.getZ() + ") is not within this object's chunk (" + chunkPos.x + ", " + chunkPos.z + ")");
            }
            if (level.getBlockState(owner.surveyorTableLocation).getBlock() == OzoneBlocks.SURVEYOR_TABLE.get()) {
                level.destroyBlock(owner.surveyorTableLocation, true);
            }
            owner.surveyorTableLocation = surveyorTableLocation;
            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangeSurveyorTableLocation(GlobalChunkPos.of(level.dimension(), chunkPos), surveyorTableLocation));
        }
    }

    public void changeSurveyorTableLocation(Level level, BlockPos surveyorTableLocation) {
        if (level.isClientSide) {
            changeSurveyorTableLocation(surveyorTableLocation);
        } else {
            changeSurveyorTableLocation((ServerLevel)level, surveyorTableLocation);
        }
    }

    public boolean isSurveyorTableLocation(@Nullable BlockPos pos) {
        var owner = this.owner;
        return owner == null? pos == null : owner.surveyorTableLocation.equals(pos);
    }

    public Filters getFilters() {
        return filters;
    }

    public void setFilters(Filters filters) {
        if (this.filters != filters) {
            this.filters.assignFrom(filters);
        }
    }

    public void setFilters(ServerLevel level, Filters filters) {
        if (this.filters != filters) {
            this.filters.assignFrom(filters);
        }
        PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangeFilters(GlobalChunkPos.of(level.dimension(), chunkPos), this.filters));
    }

    public void setFilters(Level level, Filters filters) {
        if (level.isClientSide) {
            setFilters(filters);
        } else {
            setFilters((ServerLevel)level, filters);
        }
    }

    public EntityFilter getAllowExplosions() {
        return filters.allowExplosions;
    }

    public void setAllowExplosions(EntityFilter allowExplosions) {
        filters.setAllowExplosions(allowExplosions);
    }

    public void setAllowExplosions(ServerLevel level, EntityFilter allowExplosions) {
        if (filters.allowExplosions != allowExplosions) {
            filters.setAllowExplosions(allowExplosions);
            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangeEntityFilter(GlobalChunkPos.of(level.dimension(), chunkPos), EntityFilterField.allowExplosions, filters.allowExplosions));
        }
    }

    public void setAllowExplosions(Level level, EntityFilter allowExplosions) {
        if (level.isClientSide) {
            setAllowExplosions(allowExplosions);
        } else {
            setAllowExplosions((ServerLevel)level, allowExplosions);
        }
    }

    public boolean allowExplosions(Entity entity) {
        var owner = this.owner;
        return owner == null || filters.allowExplosions.test(entity, owner.uuid);
    }

    public EntityFilter getAllowPlace() {
        return filters.allowPlace;
    }

    public void setAllowPlace(EntityFilter allowPlace) {
        filters.setAllowPlace(allowPlace);
    }

    public void setAllowPlace(ServerLevel level, EntityFilter allowPlace) {
        if (filters.allowPlace != allowPlace) {
            filters.setAllowPlace(allowPlace);
            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangeEntityFilter(GlobalChunkPos.of(level.dimension(), chunkPos), EntityFilterField.allowPlace, filters.allowPlace));
        }
    }

    public void setAllowPlace(Level level, EntityFilter allowPlace) {
        if (level.isClientSide) {
            setAllowPlace(allowPlace);
        } else {
            setAllowPlace((ServerLevel)level, allowPlace);
        }
    }

    public boolean allowPlace(@Nullable BlockState state, Entity entity) {
        var owner = this.owner;
        return owner == null || filters.allowPlace.test(entity, owner.uuid) && (state == null || state.getBlock() != OzoneBlocks.SURVEYOR_TABLE.get() || entity.getUUID().equals(owner.uuid));
    }

    public EntityFilter getAllowBreak() {
        return filters.allowBreak;
    }

    public void setAllowBreak(EntityFilter allowBreak) {
        filters.setAllowBreak(allowBreak);
    }

    public void setAllowBreak(ServerLevel level, EntityFilter allowBreak) {
        if (filters.allowBreak != allowBreak) {
            filters.setAllowBreak(allowBreak);
            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangeEntityFilter(GlobalChunkPos.of(level.dimension(), chunkPos), EntityFilterField.allowBreak, filters.allowBreak));
        }
    }

    public void setAllowBreak(Level level, EntityFilter allowBreak) {
        if (level.isClientSide) {
            setAllowBreak(allowBreak);
        } else {
            setAllowBreak((ServerLevel)level, allowBreak);
        }
    }

    public boolean allowBreak(@Nullable BlockState state, Entity entity) {
        var owner = this.owner;
        return owner == null || filters.allowBreak.test(entity, owner.uuid) && (state == null || state.getBlock() != OzoneBlocks.SURVEYOR_TABLE.get() || entity.getUUID().equals(owner.uuid));
    }

    public PlayerFilter getAllowInteractWithContainers() {
        return filters.allowInteractWithContainers;
    }

    public void setAllowInteractWithContainers(PlayerFilter allowInteractWithContainers) {
        filters.setAllowInteractWithContainers(allowInteractWithContainers);
    }

    public void setAllowInteractWithContainers(ServerLevel level, PlayerFilter allowInteractWithContainers) {
        if (filters.allowInteractWithContainers != allowInteractWithContainers) {
            filters.setAllowInteractWithContainers(allowInteractWithContainers);
            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangePlayerFilter(GlobalChunkPos.of(level.dimension(), chunkPos), PlayerFilterField.allowInteractWithContainers, filters.allowInteractWithContainers));
        }
    }

    public void setAllowInteractWithContainers(Level level, PlayerFilter allowInteractWithContainers) {
        if (level.isClientSide) {
            setAllowInteractWithContainers(allowInteractWithContainers);
        } else {
            setAllowInteractWithContainers((ServerLevel)level, allowInteractWithContainers);
        }
    }

    public boolean allowInteractWithContainers(@Nullable BlockState state, Player player) {
        var owner = this.owner;
        return owner == null || filters.allowInteractWithContainers.test(player, owner.uuid) && (state == null || state.getBlock() != OzoneBlocks.SURVEYOR_TABLE.get() || player.getUUID().equals(owner.uuid));
    }

    protected boolean allowInteractWithContainers(Entity entity) {
        var owner = this.owner;
        return owner == null || entity instanceof Player player && filters.allowInteractWithContainers.test(player, owner.uuid);
    }

    public EntityFilter getAllowInteractWithDoors() {
        return filters.allowInteractWithDoors;
    }

    public void setAllowInteractWithDoors(EntityFilter allowInteractWithDoors) {
        filters.setAllowInteractWithDoors(allowInteractWithDoors);
    }

    public void setAllowInteractWithDoors(ServerLevel level, EntityFilter allowInteractWithDoors) {
        if (filters.allowInteractWithDoors != allowInteractWithDoors) {
            filters.setAllowInteractWithDoors(allowInteractWithDoors);
            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangeEntityFilter(GlobalChunkPos.of(level.dimension(), chunkPos), EntityFilterField.allowInteractWithDoors, filters.allowInteractWithDoors));
        }
    }

    public void setAllowInteractWithDoors(Level level, EntityFilter allowInteractWithDoors) {
        if (level.isClientSide) {
            setAllowInteractWithDoors(allowInteractWithDoors);
        } else {
            setAllowInteractWithDoors((ServerLevel)level, allowInteractWithDoors);
        }
    }

    public boolean allowInteractWithDoors(Entity entity) {
        var owner = this.owner;
        return owner == null || filters.allowInteractWithDoors.test(entity, owner.uuid);
    }
    
    public PlayerFilter getAllowInteractWithRedstone() {
        return filters.allowInteractWithRedstone;
    }

    public void setAllowInteractWithRedstone(PlayerFilter allowInteractWithRedstone) {
        filters.setAllowInteractWithRedstone(allowInteractWithRedstone);
    }

    public void setAllowInteractWithRedstone(ServerLevel level, PlayerFilter allowInteractWithRedstone) {
        if (filters.allowInteractWithRedstone != allowInteractWithRedstone) {
            filters.setAllowInteractWithRedstone(allowInteractWithRedstone);
            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangePlayerFilter(GlobalChunkPos.of(level.dimension(), chunkPos), PlayerFilterField.allowInteractWithRedstone, filters.allowInteractWithRedstone));
        }
    }

    public void setAllowInteractWithRedstone(Level level, PlayerFilter allowInteractWithRedstone) {
        if (level.isClientSide) {
            setAllowInteractWithRedstone(allowInteractWithRedstone);
        } else {
            setAllowInteractWithRedstone((ServerLevel)level, allowInteractWithRedstone);
        }
    }

    public boolean allowInteractWithRedstone(Player player) {
        var owner = this.owner;
        return owner == null || filters.allowInteractWithRedstone.test(player, owner.uuid);
    }

    protected boolean allowInteractWithRedstone(Entity entity) {
        var owner = this.owner;
        return owner == null || entity instanceof Player player && filters.allowInteractWithRedstone.test(player, owner.uuid);
    }

    public EntityFilter getAllowInteractWithRedstoneActivators() {
        return filters.allowInteractWithRedstoneActivators;
    }

    public void setAllowInteractWithRedstoneActivators(EntityFilter allowInteractWithRedstoneActivators) {
        filters.setAllowInteractWithRedstoneActivators(allowInteractWithRedstoneActivators);
    }

    public void setAllowInteractWithRedstoneActivators(ServerLevel level, EntityFilter allowInteractWithRedstoneActivators) {
        if (filters.allowInteractWithRedstoneActivators != allowInteractWithRedstoneActivators) {
            filters.setAllowInteractWithRedstoneActivators(allowInteractWithRedstoneActivators);
            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangeEntityFilter(GlobalChunkPos.of(level.dimension(), chunkPos), EntityFilterField.allowInteractWithRedstoneActivators, filters.allowInteractWithRedstoneActivators));
        }
    }

    public void setAllowInteractWithRedstoneActivators(Level level, EntityFilter allowInteractWithRedstoneActivators) {
        if (level.isClientSide) {
            setAllowInteractWithRedstoneActivators(allowInteractWithRedstoneActivators);
        } else {
            setAllowInteractWithRedstoneActivators((ServerLevel)level, allowInteractWithRedstoneActivators);
        }
    }

    public boolean allowInteractWithRedstoneActivators(Entity entity) {
        var owner = this.owner;
        return owner == null || filters.allowInteractWithRedstoneActivators.test(entity, owner.uuid);
    }

    public PlayerFilter getAllowInteractWithSigns() {
        return filters.allowInteractWithSigns;
    }

    public void setAllowInteractWithSigns(PlayerFilter allowInteractWithSigns) {
        filters.setAllowInteractWithSigns(allowInteractWithSigns);
    }

    public void setAllowInteractWithSigns(ServerLevel level, PlayerFilter allowInteractWithSigns) {
        if (filters.allowInteractWithSigns != allowInteractWithSigns) {
            filters.setAllowInteractWithSigns(allowInteractWithSigns);
            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangePlayerFilter(GlobalChunkPos.of(level.dimension(), chunkPos), PlayerFilterField.allowInteractWithSigns, filters.allowInteractWithSigns));
        }
    }

    public void setAllowInteractWithSigns(Level level, PlayerFilter allowInteractWithSigns) {
        if (level.isClientSide) {
            setAllowInteractWithSigns(allowInteractWithSigns);
        } else {
            setAllowInteractWithSigns((ServerLevel)level, allowInteractWithSigns);
        }
    }

    public boolean allowInteractWithSigns(Player player) {
        var owner = this.owner;
        return owner == null || filters.allowInteractWithSigns.test(player, owner.uuid);
    }

    protected boolean allowInteractWithSigns(Entity entity) {
        var owner = this.owner;
        return owner == null || entity instanceof Player player && filters.allowInteractWithSigns.test(player, owner.uuid);
    }

    public EntityFilter getAllowInteractWithOther() {
        return filters.allowInteractWithOther;
    }

    public void setAllowInteractWithOther(EntityFilter allowInteractWithOther) {
        filters.setAllowInteractWithOther(allowInteractWithOther);
    }

    public void setAllowInteractWithOther(ServerLevel level, EntityFilter allowInteractWithOther) {
        if (filters.allowInteractWithOther != allowInteractWithOther) {
            filters.setAllowInteractWithOther(allowInteractWithOther);
            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangeEntityFilter(GlobalChunkPos.of(level.dimension(), chunkPos), EntityFilterField.allowInteractWithOther, filters.allowInteractWithOther));
        }
    }

    public void setAllowInteractWithOther(Level level, EntityFilter allowInteractWithOther) {
        if (level.isClientSide) {
            setAllowInteractWithOther(allowInteractWithOther);
        } else {
            setAllowInteractWithOther((ServerLevel)level, allowInteractWithOther);
        }
    }

    public boolean allowInteractWithOther(Entity entity) {
        var owner = this.owner;
        return owner == null || filters.allowInteractWithOther.test(entity, owner.uuid);
    }

    public EntityFilter getAllowDrop() {
        return filters.allowDrop;
    }

    public void setAllowDrop(EntityFilter allowDrop) {
        filters.setAllowDrop(allowDrop);
    }

    public void setAllowDrop(ServerLevel level, EntityFilter allowDrop) {
        if (filters.allowDrop != allowDrop) {
            filters.setAllowDrop(allowDrop);
            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangeEntityFilter(GlobalChunkPos.of(level.dimension(), chunkPos), EntityFilterField.allowDrop, filters.allowDrop));
        }
    }

    public void setAllowDrop(Level level, EntityFilter allowDrop) {
        if (level.isClientSide) {
            setAllowDrop(allowDrop);
        } else {
            setAllowDrop((ServerLevel)level, allowDrop);
        }
    }

    public boolean allowDrop(Entity entity) {
        var owner = this.owner;
        return owner == null || filters.allowDrop.test(entity, owner.uuid);
    }

    private static String cleanName(String string) {
        return string.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
    }

    public static class Filters {
        private EntityFilter allowExplosions;
        private EntityFilter allowPlace;
        private EntityFilter allowBreak;
        private PlayerFilter allowInteractWithContainers;
        private EntityFilter allowInteractWithDoors;
        private PlayerFilter allowInteractWithRedstone;
        private EntityFilter allowInteractWithRedstoneActivators;
        private PlayerFilter allowInteractWithSigns;
        private EntityFilter allowInteractWithOther;
        private EntityFilter allowDrop;

        public static final Codec<Filters> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EntityFilter.CODEC.fieldOf("allowExplosions").forGetter(Filters::getAllowExplosions),
            EntityFilter.CODEC.fieldOf("allowPlace").forGetter(Filters::getAllowPlace),
            EntityFilter.CODEC.fieldOf("allowBreak").forGetter(Filters::getAllowBreak),
            PlayerFilter.CODEC.fieldOf("allowInteractWithContainers").forGetter(Filters::getAllowInteractWithContainers),
            EntityFilter.CODEC.fieldOf("allowInteractWithDoors").forGetter(Filters::getAllowInteractWithDoors),
            PlayerFilter.CODEC.fieldOf("allowInteractWithRedstone").forGetter(Filters::getAllowInteractWithRedstone),
            EntityFilter.CODEC.fieldOf("allowInteractWithRedstoneActivators").forGetter(Filters::getAllowInteractWithRedstoneActivators),
            PlayerFilter.CODEC.fieldOf("allowInteractWithSigns").forGetter(Filters::getAllowInteractWithSigns),
            EntityFilter.CODEC.fieldOf("allowInteractWithOther").forGetter(Filters::getAllowInteractWithOther),
            EntityFilter.CODEC.fieldOf("allowDrop").forGetter(Filters::getAllowDrop)
        ).apply(instance, Filters::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Filters> STREAM_CODEC = new StreamCodec<>() {
            public void encode(RegistryFriendlyByteBuf buffer, Filters value) {
                buffer.writeEnum(value.allowExplosions);
                buffer.writeEnum(value.allowPlace);
                buffer.writeEnum(value.allowBreak);
                buffer.writeEnum(value.allowInteractWithContainers);
                buffer.writeEnum(value.allowInteractWithDoors);
                buffer.writeEnum(value.allowInteractWithRedstone);
                buffer.writeEnum(value.allowInteractWithRedstoneActivators);
                buffer.writeEnum(value.allowInteractWithSigns);
                buffer.writeEnum(value.allowInteractWithOther);
                buffer.writeEnum(value.allowDrop);
            }

            public Filters decode(RegistryFriendlyByteBuf buffer) {
                var value = new Filters(0);
                value.allowExplosions = buffer.readEnum(EntityFilter.class);
                value.allowPlace = buffer.readEnum(EntityFilter.class);
                value.allowBreak = buffer.readEnum(EntityFilter.class);
                value.allowInteractWithContainers = buffer.readEnum(PlayerFilter.class);
                value.allowInteractWithDoors = buffer.readEnum(EntityFilter.class);
                value.allowInteractWithRedstone = buffer.readEnum(PlayerFilter.class);
                value.allowInteractWithRedstoneActivators = buffer.readEnum(EntityFilter.class);
                value.allowInteractWithSigns = buffer.readEnum(PlayerFilter.class);
                value.allowInteractWithOther = buffer.readEnum(EntityFilter.class);
                value.allowDrop = buffer.readEnum(EntityFilter.class);
                return value;
            }
        };

        public Filters() {
            reset();
        }

        private Filters(int unused) {}

        public Filters(
                EntityFilter allowExplosions, EntityFilter allowPlace,
                EntityFilter allowBreak, PlayerFilter allowInteractWithContainers, EntityFilter allowInteractWithDoors,
                PlayerFilter allowInteractWithRedstone, EntityFilter allowInteractWithRedstoneActivators,
                PlayerFilter allowInteractWithSigns, EntityFilter allowInteractWithOther,
                EntityFilter allowDrop) {
            this.allowExplosions = allowExplosions;
            this.allowPlace = allowPlace.atLeast(EntityFilter.OWNER_ONLY);
            this.allowBreak = allowBreak.atLeast(EntityFilter.OWNER_ONLY);
            this.allowInteractWithContainers = allowInteractWithContainers.atLeast(PlayerFilter.OWNER_ONLY);
            this.allowInteractWithDoors = allowInteractWithDoors.atLeast(EntityFilter.OWNER_ONLY);
            this.allowInteractWithRedstone = allowInteractWithRedstone.atLeast(PlayerFilter.OWNER_ONLY);
            this.allowInteractWithRedstoneActivators = allowInteractWithRedstoneActivators.atLeast(EntityFilter.OWNER_ONLY);
            this.allowInteractWithSigns = allowInteractWithSigns.atLeast(PlayerFilter.OWNER_ONLY);
            this.allowInteractWithOther = allowInteractWithOther.atLeast(EntityFilter.OWNER_ONLY);
            this.allowDrop = allowDrop.atLeast(EntityFilter.OWNER_ONLY);
        }

        public void reset() {
            allowExplosions = EntityFilter.OWNER_AND_MOBS;
            allowPlace = EntityFilter.FRIENDS_AND_MOBS;
            allowBreak = EntityFilter.FRIENDS_AND_MOBS;
            allowInteractWithContainers = PlayerFilter.OWNER_ONLY;
            allowInteractWithDoors = EntityFilter.ANYONE;
            allowInteractWithRedstone = PlayerFilter.ANYONE;
            allowInteractWithRedstoneActivators = EntityFilter.ANYONE;
            allowInteractWithSigns = PlayerFilter.OWNER_ONLY;
            allowInteractWithOther = EntityFilter.FRIENDS_AND_MOBS;
            allowDrop = EntityFilter.ANYONE;
        }

        public void assignFrom(Filters filters) {
            if (filters == this) return;
            allowExplosions = filters.allowExplosions;
            allowPlace = filters.allowPlace;
            allowBreak = filters.allowBreak;
            allowInteractWithContainers = filters.allowInteractWithContainers;
            allowInteractWithDoors = filters.allowInteractWithDoors;
            allowInteractWithRedstone = filters.allowInteractWithRedstone;
            allowInteractWithRedstoneActivators = filters.allowInteractWithRedstoneActivators;
            allowInteractWithSigns = filters.allowInteractWithSigns;
            allowInteractWithOther = filters.allowInteractWithOther;
            allowDrop = filters.allowDrop;
        }

        public EntityFilter getAllowExplosions() {
            return allowExplosions;
        }

        public void setAllowExplosions(EntityFilter allowExplosions) {
            this.allowExplosions = allowExplosions;
        }

        public EntityFilter getAllowPlace() {
            return allowPlace;
        }

        public void setAllowPlace(EntityFilter allowPlace) {
            this.allowPlace = allowPlace.atLeast(EntityFilter.OWNER_ONLY);
        }

        public EntityFilter getAllowBreak() {
            return allowBreak;
        }

        public void setAllowBreak(EntityFilter allowBreak) {
            this.allowBreak = allowBreak.atLeast(EntityFilter.OWNER_ONLY);
        }

        public PlayerFilter getAllowInteractWithContainers() {
            return allowInteractWithContainers;
        }

        public void setAllowInteractWithContainers(PlayerFilter allowInteractWithContainers) {
            this.allowInteractWithContainers = allowInteractWithContainers.atLeast(PlayerFilter.OWNER_ONLY);
        }

        public EntityFilter getAllowInteractWithDoors() {
            return allowInteractWithDoors;
        }

        public void setAllowInteractWithDoors(EntityFilter allowInteractWithDoors) {
            this.allowInteractWithDoors = allowInteractWithDoors.atLeast(EntityFilter.OWNER_ONLY);
        }

        public PlayerFilter getAllowInteractWithRedstone() {
            return allowInteractWithRedstone;
        }

        public void setAllowInteractWithRedstone(PlayerFilter allowInteractWithRedstone) {
            this.allowInteractWithRedstone = allowInteractWithRedstone.atLeast(PlayerFilter.OWNER_ONLY);
        }

        public EntityFilter getAllowInteractWithRedstoneActivators() {
            return allowInteractWithRedstoneActivators;
        }

        public void setAllowInteractWithRedstoneActivators(EntityFilter allowInteractWithRedstoneActivators) {
            this.allowInteractWithRedstoneActivators = allowInteractWithRedstoneActivators.atLeast(EntityFilter.OWNER_ONLY);
        }

        public PlayerFilter getAllowInteractWithSigns() {
            return allowInteractWithSigns;
        }

        public void setAllowInteractWithSigns(PlayerFilter allowInteractWithSigns) {
            this.allowInteractWithSigns = allowInteractWithSigns.atLeast(PlayerFilter.OWNER_ONLY);
        }

        public EntityFilter getAllowInteractWithOther() {
            return allowInteractWithOther;
        }

        public void setAllowInteractWithOther(EntityFilter allowInteractWithOther) {
            this.allowInteractWithOther = allowInteractWithOther.atLeast(EntityFilter.OWNER_ONLY);
        }

        public EntityFilter getAllowDrop() {
            return allowDrop;
        }

        public void setAllowDrop(EntityFilter allowDrop) {
            this.allowDrop = allowDrop.atLeast(EntityFilter.OWNER_ONLY);
        }
    }
    
    public enum PlayerFilter implements StringRepresentable, BiPredicate<Player, UUID> {
        NO_ONE((player, ownerUuid) -> false),
        OWNER_ONLY((player, ownerUuid) -> player.getUUID().equals(ownerUuid)),
        FRIENDS((player, ownerUuid) -> player.getUUID().equals(ownerUuid)),
        ANYONE((player, ownerUuid) -> true);

        public static final Codec<PlayerFilter> CODEC = StringRepresentable.fromEnum(PlayerFilter::values);
        public static final StreamCodec<RegistryFriendlyByteBuf, PlayerFilter> STREAM_CODEC = new StreamCodec<>() {
            public void encode(RegistryFriendlyByteBuf buffer, PlayerFilter value) {
                buffer.writeEnum(value);
            }

            public PlayerFilter decode(RegistryFriendlyByteBuf buffer) {
                return buffer.readEnum(PlayerFilter.class);
            }
        };
        private static final Map<String, PlayerFilter> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toMap(e -> cleanName(e.name), e -> e));

        private final String name = name().toLowerCase(Locale.ROOT);
        private final BiPredicate<Player, UUID> predicate;

        private PlayerFilter(BiPredicate<Player, UUID> predicate) {
            this.predicate = predicate;
        }

        public PlayerFilter atLeast(PlayerFilter min) {
            return this.compareTo(min) < 0? min : this;
        }

        @Override
        public boolean test(Player player, UUID ownerUuid) {
            return predicate.test(player, ownerUuid);
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        public String getName() {
            return name;
        }

        public Component getDescription() {
            return Component.translatable("argument.ozone_utilities.entity_filter." + name);
        }

        @Nullable
        public static PlayerFilter getByName(@Nullable String friendlyName) {
            return friendlyName == null? null : BY_NAME.get(cleanName(friendlyName));
        }

        public static Collection<String> getNames() {
            return getNames(EnumSet.range(PlayerFilter.NO_ONE, PlayerFilter.ANYONE));
        }

        public static Collection<String> getNames(PlayerFilter min) {
            return getNames(EnumSet.range(min, PlayerFilter.ANYONE));
        }

        public static Collection<String> getNames(Collection<PlayerFilter> filters) {
            var list = new ArrayList<String>(filters.size());
            
            for (var playerFilter : filters) {
                list.add(playerFilter.getName());
            }
            
            return list;
        }
    }

    public enum EntityFilter implements StringRepresentable, BiPredicate<Entity, UUID> {
        NO_ONE((entity, ownerUuid) -> false),
        OWNER_ONLY((entity, ownerUuid) -> entity instanceof Player player && player.getUUID().equals(ownerUuid)),
        OWNER_AND_MOBS((entity, ownerUuid) -> !(entity instanceof Player player) || player.getUUID().equals(ownerUuid)),
        FRIENDS((entity, ownerUuid) -> entity instanceof Player player && player.getUUID().equals(ownerUuid)),
        FRIENDS_AND_MOBS((entity, ownerUuid) -> !(entity instanceof Player player) || player.getUUID().equals(ownerUuid)),
        PLAYERS((entity, ownerUuid) -> entity instanceof Player),
        MOBS((entity, ownerUuid) -> !(entity instanceof Player)),
        ANYONE((entity, ownerUuid) -> true);

        public static final Codec<EntityFilter> CODEC = StringRepresentable.fromEnum(EntityFilter::values);
        public static final StreamCodec<RegistryFriendlyByteBuf, EntityFilter> STREAM_CODEC = new StreamCodec<>() {
            public void encode(RegistryFriendlyByteBuf buffer, EntityFilter value) {
                buffer.writeEnum(value);
            }

            public EntityFilter decode(RegistryFriendlyByteBuf buffer) {
                return buffer.readEnum(EntityFilter.class);
            }
        };
        private static final Map<String, EntityFilter> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toMap(e -> cleanName(e.name), e -> e));

        private final String name = name().toLowerCase(Locale.ROOT);
        private final BiPredicate<Entity, UUID> predicate;

        private EntityFilter(BiPredicate<Entity, UUID> predicate) {
            this.predicate = predicate;
        }

        public EntityFilter atLeast(EntityFilter min) {
            return this.compareTo(min) < 0? min : this;
        }

        @Override
        public boolean test(Entity entity, UUID ownerUuid) {
            return predicate.test(entity, ownerUuid);
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        public String getName() {
            return name;
        }

        public Component getDescription() {
            return Component.translatable("argument.ozone_utilities.entity_filter." + name);
        }

        @Nullable
        public static EntityFilter getByName(@Nullable String friendlyName) {
            return friendlyName == null? null : BY_NAME.get(cleanName(friendlyName));
        }

        public static Collection<String> getNames() {
            return getNames(EnumSet.range(EntityFilter.NO_ONE, EntityFilter.ANYONE));
        }

        public static Collection<String> getNames(EntityFilter min) {
            return getNames(EnumSet.range(min, EntityFilter.ANYONE));
        }

        public static Collection<String> getNames(Collection<EntityFilter> filters) {
            var list = new ArrayList<String>(filters.size());
            
            for (var entityFilter : filters) {
                list.add(entityFilter.getName());
            }
            
            return list;
        }
    }

    public enum BlockCategory implements BiPredicate<Entity, OzoneChunkAttachment> {
        DOORS(OzoneBlockTags.DOORS, OzoneChunkAttachment::allowInteractWithDoors),
        CONTAINERS(OzoneBlockTags.CONTAINERS, OzoneChunkAttachment::allowInteractWithContainers) {
            @Override
            public boolean has(BlockState state) {
                return state.hasBlockEntity() && state.getBlock() != Blocks.ENDER_CHEST && super.has(state);
            }
        },
        REDSTONE(OzoneBlockTags.REDSTONE, OzoneChunkAttachment::allowInteractWithRedstone),
        REDSTONE_ACTIVATORS(OzoneBlockTags.REDSTONE_ACTIVATORS, OzoneChunkAttachment::allowInteractWithRedstoneActivators),
        SIGNS(BlockTags.ALL_SIGNS, OzoneChunkAttachment::allowInteractWithSigns),
        OTHER(OzoneBlockTags.OTHER, OzoneChunkAttachment::allowInteractWithOther);
        
        private final TagKey<Block> tag;
        private final BiPredicate<OzoneChunkAttachment, Entity> predicate;

        private BlockCategory(TagKey<Block> tag, BiPredicate<OzoneChunkAttachment, Entity> predicate) {
            this.tag = tag;
            this.predicate = predicate;
        }

        @Override
        public boolean test(Entity entity, OzoneChunkAttachment claim) {
            return predicate.test(claim, entity);
        }

        public boolean isInteractAllowed(Entity entity, OzoneChunkAttachment claim) {
            return predicate.test(claim, entity);
        }

        public boolean has(BlockState state) {
            return state.is(tag);
        }

        private static final BlockCategory[] VALUES = values();
        
        @Nullable
        public static Set<BlockCategory> of(BlockState state) {
            EnumSet<BlockCategory> results = null;
            for (var category : BlockCategory.VALUES) {
                if (category.has(state)) {
                    if (results == null) {
                        results = EnumSet.noneOf(BlockCategory.class);
                    }
                    results.add(category);
                }
            }
            return results == null? Set.of() : results;
        }
    }

    public static enum PlayerFilterField implements BiConsumer<OzoneChunkAttachment, PlayerFilter> {
        allowInteractWithContainers(OzoneChunkAttachment::setAllowInteractWithContainers, PlayerFilter.OWNER_ONLY),
        allowInteractWithRedstone(OzoneChunkAttachment::setAllowInteractWithRedstone, PlayerFilter.OWNER_ONLY),
        allowInteractWithSigns(OzoneChunkAttachment::setAllowInteractWithSigns, PlayerFilter.OWNER_ONLY);
        
        public static final StreamCodec<RegistryFriendlyByteBuf, PlayerFilterField> STREAM_CODEC = new StreamCodec<>() {
            public void encode(RegistryFriendlyByteBuf buffer, PlayerFilterField value) {
                buffer.writeEnum(value);
            }
    
            public PlayerFilterField decode(RegistryFriendlyByteBuf buffer) {
                return buffer.readEnum(PlayerFilterField.class);
            }
        };
    
        private final BiConsumer<OzoneChunkAttachment, PlayerFilter> setter;
        private final PlayerFilter min;
    
        private PlayerFilterField(BiConsumer<OzoneChunkAttachment, PlayerFilter> setter, PlayerFilter min) {
            this.setter = setter;
            this.min = min;
        }

        public PlayerFilter getMinValue() {
            return min;
        }
    
        public void set(OzoneChunkAttachment claim, PlayerFilter value) {
            setter.accept(claim, value);
        }
    
        @Override
        public void accept(OzoneChunkAttachment claim, PlayerFilter value) {
            setter.accept(claim, value);
        }
    
        public BiConsumer<OzoneChunkAttachment, PlayerFilter> getSetter() {
            return setter;
        }
    }

    public static enum EntityFilterField implements BiConsumer<OzoneChunkAttachment, EntityFilter> {
        allowExplosions(OzoneChunkAttachment::setAllowExplosions, EntityFilter.NO_ONE),
        allowPlace(OzoneChunkAttachment::setAllowPlace, EntityFilter.OWNER_ONLY),
        allowBreak(OzoneChunkAttachment::setAllowBreak, EntityFilter.OWNER_ONLY),
        allowInteractWithDoors(OzoneChunkAttachment::setAllowInteractWithDoors, EntityFilter.OWNER_ONLY),
        allowInteractWithRedstoneActivators(OzoneChunkAttachment::setAllowInteractWithRedstoneActivators, EntityFilter.OWNER_ONLY),
        allowInteractWithOther(OzoneChunkAttachment::setAllowInteractWithOther, EntityFilter.OWNER_ONLY),
        allowDrop(OzoneChunkAttachment::setAllowDrop, EntityFilter.OWNER_ONLY);
    
        public static final StreamCodec<RegistryFriendlyByteBuf, EntityFilterField> STREAM_CODEC = new StreamCodec<>() {
            public void encode(RegistryFriendlyByteBuf buffer, EntityFilterField value) {
                buffer.writeEnum(value);
            }
    
            public EntityFilterField decode(RegistryFriendlyByteBuf buffer) {
                return buffer.readEnum(EntityFilterField.class);
            }
        };
    
        private final BiConsumer<OzoneChunkAttachment, EntityFilter> setter;
        private final EntityFilter min;
    
        private EntityFilterField(BiConsumer<OzoneChunkAttachment, EntityFilter> setter, EntityFilter min) {
            this.setter = setter;
            this.min = min;
        }

        public EntityFilter getMinValue() {
            return min;
        }
    
        public void set(OzoneChunkAttachment claim, EntityFilter value) {
            setter.accept(claim, value);
        }
    
        @Override
        public void accept(OzoneChunkAttachment claim, EntityFilter value) {
            setter.accept(claim, value);
        }
    
        public BiConsumer<OzoneChunkAttachment, EntityFilter> getSetter() {
            return setter;
        }
    }

    public static final class OwnerInfo {
        private UUID uuid;
        private BlockPos surveyorTableLocation;

        public static final Codec<OwnerInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("uuid").forGetter(OwnerInfo::getUUID),
            BlockPos.CODEC.fieldOf("surveyorTableLocation").forGetter(OwnerInfo::getSurveyorTableLocation)
        ).apply(instance, OwnerInfo::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, OwnerInfo> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, OwnerInfo::getUUID,
            BlockPos.STREAM_CODEC, OwnerInfo::getSurveyorTableLocation,
            OwnerInfo::new
        );

        public OwnerInfo(UUID uuid, BlockPos surveyorTableLocation) {
            this.uuid = Validate.notNull(uuid, "uuid was null");
            this.surveyorTableLocation = Validate.notNull(surveyorTableLocation, "surveyorTableLocation was null");
        }

        public UUID getUUID() {
            return uuid;
        }

        public void setUUID(UUID uuid) {
            this.uuid = Validate.notNull(uuid, "uuid was null");
        }

        public BlockPos getSurveyorTableLocation() {
            return surveyorTableLocation;
        }

        public void setSurveyorTableLocation(BlockPos surveyorTableLocation) {
            this.surveyorTableLocation = Validate.notNull(surveyorTableLocation, "surveyorTableLocation was null");
        }

        @Override
        public int hashCode() {
            return 31 * uuid.hashCode() + surveyorTableLocation.hashCode();
        }

        @Override
        public String toString() {
            return "OwnerInfo[uuid=" + uuid + ", surveyorTableLocation=" + surveyorTableLocation + "]";
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || obj instanceof OwnerInfo other && this.uuid.equals(other.uuid) && this.surveyorTableLocation.equals(other.surveyorTableLocation);
        }
    }
}
