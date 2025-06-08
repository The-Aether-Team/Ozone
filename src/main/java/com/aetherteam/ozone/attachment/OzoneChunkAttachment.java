package com.aetherteam.ozone.attachment;

import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiPredicate;

import javax.annotation.Nullable;

import org.apache.commons.lang3.Validate;

import com.aetherteam.ozone.block.OzoneBlocks;
import com.aetherteam.ozone.claims.EntityFilter;
import com.aetherteam.ozone.claims.EntityFilterField;
import com.aetherteam.ozone.claims.Filters;
import com.aetherteam.ozone.claims.IFilters;
import com.aetherteam.ozone.claims.PlayerFilter;
import com.aetherteam.ozone.claims.PlayerFilterField;
import com.aetherteam.ozone.network.packet.clientbound.ChunkClaimPacket;
import com.aetherteam.ozone.tags.OzoneBlockTags;
import com.aetherteam.ozone.tags.OzoneEntityTypeTags;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.TriPredicate;
import net.neoforged.neoforge.network.PacketDistributor;

public class OzoneChunkAttachment implements IFilters {
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

    @Override
    public EntityFilter getAllowExplosions() {
        return filters.getAllowExplosions();
    }

    @Override
    public void setAllowExplosions(EntityFilter allowExplosions) {
        filters.setAllowExplosions(allowExplosions);
    }

    public void setAllowExplosions(ServerLevel level, EntityFilter allowExplosions) {
        if (filters.getAllowExplosions() != allowExplosions) {
            filters.setAllowExplosions(allowExplosions);
            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangeEntityFilter(GlobalChunkPos.of(level.dimension(), chunkPos), EntityFilterField.allowExplosions, filters.getAllowExplosions()));
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
        return owner == null || filters.getAllowExplosions().test(entity, owner.uuid);
    }

    @Override
    public EntityFilter getAllowPlace() {
        return filters.getAllowPlace();
    }

    @Override
    public void setAllowPlace(EntityFilter allowPlace) {
        filters.setAllowPlace(allowPlace);
    }

    public void setAllowPlace(ServerLevel level, EntityFilter allowPlace) {
        if (filters.getAllowPlace() != allowPlace) {
            filters.setAllowPlace(allowPlace);
            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangeEntityFilter(GlobalChunkPos.of(level.dimension(), chunkPos), EntityFilterField.allowPlace, filters.getAllowPlace()));
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
        return owner == null || filters.getAllowPlace().test(entity, owner.uuid) && (state == null || state.getBlock() != OzoneBlocks.SURVEYOR_TABLE.get() || entity.getUUID().equals(owner.uuid));
    }

    @Override
    public EntityFilter getAllowBreak() {
        return filters.getAllowBreak();
    }

    @Override
    public void setAllowBreak(EntityFilter allowBreak) {
        filters.setAllowBreak(allowBreak);
    }

    public void setAllowBreak(ServerLevel level, EntityFilter allowBreak) {
        if (filters.getAllowBreak() != allowBreak) {
            filters.setAllowBreak(allowBreak);
            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangeEntityFilter(GlobalChunkPos.of(level.dimension(), chunkPos), EntityFilterField.allowBreak, filters.getAllowBreak()));
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
        return owner == null || filters.getAllowBreak().test(entity, owner.uuid) && (state == null || state.getBlock() != OzoneBlocks.SURVEYOR_TABLE.get() || entity.getUUID().equals(owner.uuid));
    }

    @Override
    public PlayerFilter getAllowInteractWithContainers() {
        return filters.getAllowInteractWithContainers();
    }

    @Override
    public void setAllowInteractWithContainers(PlayerFilter allowInteractWithContainers) {
        filters.setAllowInteractWithContainers(allowInteractWithContainers);
    }

    public void setAllowInteractWithContainers(ServerLevel level, PlayerFilter allowInteractWithContainers) {
        if (filters.getAllowInteractWithContainers() != allowInteractWithContainers) {
            filters.setAllowInteractWithContainers(allowInteractWithContainers);
            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangePlayerFilter(GlobalChunkPos.of(level.dimension(), chunkPos), PlayerFilterField.allowInteractWithContainers, filters.getAllowInteractWithContainers()));
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
        return owner == null || filters.getAllowInteractWithContainers().test(player, owner.uuid) && (state == null || state.getBlock() != OzoneBlocks.SURVEYOR_TABLE.get() || player.getUUID().equals(owner.uuid));
    }

    @Override
    public EntityFilter getAllowInteractWithDoors() {
        return filters.getAllowInteractWithDoors();
    }

    @Override
    public void setAllowInteractWithDoors(EntityFilter allowInteractWithDoors) {
        filters.setAllowInteractWithDoors(allowInteractWithDoors);
    }

    public void setAllowInteractWithDoors(ServerLevel level, EntityFilter allowInteractWithDoors) {
        if (filters.getAllowInteractWithDoors() != allowInteractWithDoors) {
            filters.setAllowInteractWithDoors(allowInteractWithDoors);
            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangeEntityFilter(GlobalChunkPos.of(level.dimension(), chunkPos), EntityFilterField.allowInteractWithDoors, filters.getAllowInteractWithDoors()));
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
        return owner == null || filters.getAllowInteractWithDoors().test(entity, owner.uuid);
    }
    
    @Override
    public EntityFilter getAllowInteractWithRedstone() {
        return filters.getAllowInteractWithRedstone();
    }

    @Override
    public void setAllowInteractWithRedstone(EntityFilter allowInteractWithRedstone) {
        filters.setAllowInteractWithRedstone(allowInteractWithRedstone);
    }

    public void setAllowInteractWithRedstone(ServerLevel level, EntityFilter allowInteractWithRedstone) {
        if (filters.getAllowInteractWithRedstone() != allowInteractWithRedstone) {
            filters.setAllowInteractWithRedstone(allowInteractWithRedstone);
            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangeEntityFilter(GlobalChunkPos.of(level.dimension(), chunkPos), EntityFilterField.allowInteractWithRedstone, filters.getAllowInteractWithRedstone()));
        }
    }

    public void setAllowInteractWithRedstone(Level level, EntityFilter allowInteractWithRedstone) {
        if (level.isClientSide) {
            setAllowInteractWithRedstone(allowInteractWithRedstone);
        } else {
            setAllowInteractWithRedstone((ServerLevel)level, allowInteractWithRedstone);
        }
    }

    public boolean allowInteractWithRedstone(Entity entity) {
        var owner = this.owner;
        return owner == null || filters.getAllowInteractWithRedstone().test(entity, owner.uuid);
    }

    @Override
    public EntityFilter getAllowConfiguration() {
        return filters.getAllowConfiguration();
    }

    @Override
    public void setAllowConfiguration(EntityFilter allowInteractWithRedstoneActivators) {
        filters.setAllowConfiguration(allowInteractWithRedstoneActivators);
    }

    public void setAllowConfiguration(ServerLevel level, EntityFilter allowInteractWithRedstoneActivators) {
        if (filters.getAllowConfiguration() != allowInteractWithRedstoneActivators) {
            filters.setAllowConfiguration(allowInteractWithRedstoneActivators);
            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangeEntityFilter(GlobalChunkPos.of(level.dimension(), chunkPos), EntityFilterField.allowConfiguration, filters.getAllowConfiguration()));
        }
    }

    public void setAllowConfiguration(Level level, EntityFilter allowInteractWithRedstoneActivators) {
        if (level.isClientSide) {
            setAllowConfiguration(allowInteractWithRedstoneActivators);
        } else {
            setAllowConfiguration((ServerLevel)level, allowInteractWithRedstoneActivators);
        }
    }

    public boolean allowConfiguration(Entity entity) {
        var owner = this.owner;
        return owner == null || filters.getAllowConfiguration().test(entity, owner.uuid);
    }

    @Override
    public PlayerFilter getAllowInteractWithSigns() {
        return filters.getAllowInteractWithSigns();
    }

    @Override
    public void setAllowInteractWithSigns(PlayerFilter allowInteractWithSigns) {
        filters.setAllowInteractWithSigns(allowInteractWithSigns);
    }

    public void setAllowInteractWithSigns(ServerLevel level, PlayerFilter allowInteractWithSigns) {
        if (filters.getAllowInteractWithSigns() != allowInteractWithSigns) {
            filters.setAllowInteractWithSigns(allowInteractWithSigns);
            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangePlayerFilter(GlobalChunkPos.of(level.dimension(), chunkPos), PlayerFilterField.allowInteractWithSigns, filters.getAllowInteractWithSigns()));
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
        return owner == null || filters.getAllowInteractWithSigns().test(player, owner.uuid);
    }

    @Override
    public EntityFilter getAllowInteractWithOther() {
        return filters.getAllowInteractWithOther();
    }

    @Override
    public void setAllowInteractWithOther(EntityFilter allowInteractWithOther) {
        filters.setAllowInteractWithOther(allowInteractWithOther);
    }

    public void setAllowInteractWithOther(ServerLevel level, EntityFilter allowInteractWithOther) {
        if (filters.getAllowInteractWithOther() != allowInteractWithOther) {
            filters.setAllowInteractWithOther(allowInteractWithOther);
            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangeEntityFilter(GlobalChunkPos.of(level.dimension(), chunkPos), EntityFilterField.allowInteractWithOther, filters.getAllowInteractWithOther()));
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
        return owner == null || filters.getAllowInteractWithOther().test(entity, owner.uuid);
    }

    @Override
    public EntityFilter getAllowDrop() {
        return filters.getAllowDrop();
    }

    @Override
    public void setAllowDrop(EntityFilter allowDrop) {
        filters.setAllowDrop(allowDrop);
    }

    public void setAllowDrop(ServerLevel level, EntityFilter allowDrop) {
        if (filters.getAllowDrop() != allowDrop) {
            filters.setAllowDrop(allowDrop);
            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangeEntityFilter(GlobalChunkPos.of(level.dimension(), chunkPos), EntityFilterField.allowDrop, filters.getAllowDrop()));
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
        return owner == null || filters.getAllowDrop().test(entity, owner.uuid);
    }

    @Override
    public PlayerFilter getAllowInteractWithWorkbenches() {
        return filters.getAllowInteractWithWorkbenches();
    }

    @Override
    public void setAllowInteractWithWorkbenches(PlayerFilter allowInteractWithWorkbenches) {
        filters.setAllowInteractWithWorkbenches(allowInteractWithWorkbenches);
    }

    public void setAllowInteractWithWorkbenches(ServerLevel level, PlayerFilter allowInteractWithWorkbenches) {
        if (filters.getAllowInteractWithWorkbenches() != allowInteractWithWorkbenches) {
            filters.setAllowInteractWithWorkbenches(allowInteractWithWorkbenches);
            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangePlayerFilter(GlobalChunkPos.of(level.dimension(), chunkPos), PlayerFilterField.allowInteractWithWorkbenches, filters.getAllowInteractWithWorkbenches()));
        }
    }

    public void setAllowInteractWithWorkbenches(Level level, PlayerFilter allowInteractWithWorkbenches) {
        if (level.isClientSide) {
            setAllowInteractWithWorkbenches(allowInteractWithWorkbenches);
        } else {
            setAllowInteractWithWorkbenches((ServerLevel)level, allowInteractWithWorkbenches);
        }
    }

    public boolean allowInteractWithWorkbenches(Player player) {
        var owner = this.owner;
        return owner == null || filters.getAllowInteractWithWorkbenches().test(player, owner.uuid);
    }

    @Override
    public PlayerFilter getAllowInteractWithSpecial() {
        return filters.getAllowInteractWithSpecial();
    }

    @Override
    public void setAllowInteractWithSpecial(PlayerFilter allowInteractWithSpecial) {
        filters.setAllowInteractWithSpecial(allowInteractWithSpecial);
    }

    public void setAllowInteractWithSpecial(ServerLevel level, PlayerFilter allowInteractWithSpecial) {
        if (filters.getAllowInteractWithSpecial() != allowInteractWithSpecial) {
            filters.setAllowInteractWithSpecial(allowInteractWithSpecial);
            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangePlayerFilter(GlobalChunkPos.of(level.dimension(), chunkPos), PlayerFilterField.allowInteractWithSpecial, filters.getAllowInteractWithSpecial()));
        }
    }

    public void setAllowInteractWithSpecial(Level level, PlayerFilter allowInteractWithSpecial) {
        if (level.isClientSide) {
            setAllowInteractWithSpecial(allowInteractWithSpecial);
        } else {
            setAllowInteractWithSpecial((ServerLevel)level, allowInteractWithSpecial);
        }
    }

    public boolean allowInteractWithSpecial(Player player) {
        var owner = this.owner;
        return owner == null || filters.getAllowInteractWithSpecial().test(player, owner.uuid);
    }

    @Override
    public PlayerFilter getAllowInteractWithPlants() {
        return filters.getAllowInteractWithPlants();
    }

    @Override
    public void setAllowInteractWithPlants(PlayerFilter allowInteractWithPlants) {
        filters.setAllowInteractWithPlants(allowInteractWithPlants);
    }

    public void setAllowInteractWithPlants(ServerLevel level, PlayerFilter allowInteractWithPlants) {
        if (filters.getAllowInteractWithPlants() != allowInteractWithPlants) {
            filters.setAllowInteractWithPlants(allowInteractWithPlants);
            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangePlayerFilter(GlobalChunkPos.of(level.dimension(), chunkPos), PlayerFilterField.allowInteractWithPlants, filters.getAllowInteractWithPlants()));
        }
    }

    public void setAllowInteractWithPlants(Level level, PlayerFilter allowInteractWithPlants) {
        if (level.isClientSide) {
            setAllowInteractWithPlants(allowInteractWithPlants);
        } else {
            setAllowInteractWithPlants((ServerLevel)level, allowInteractWithPlants);
        }
    }

    public boolean allowInteractWithPlants(Player player) {
        var owner = this.owner;
        return owner == null || filters.getAllowInteractWithPlants().test(player, owner.uuid);
    }

    @Override
    public PlayerFilter getAllowInteractWithVillagers() {
        return filters.getAllowInteractWithVillagers();
    }

    @Override
    public void setAllowInteractWithVillagers(PlayerFilter allowInteractWithVillagers) {
        filters.setAllowInteractWithVillagers(allowInteractWithVillagers);
    }

    public void setAllowInteractWithVillagers(ServerLevel level, PlayerFilter allowInteractWithVillagers) {
        if (filters.getAllowInteractWithVillagers() != allowInteractWithVillagers) {
            filters.setAllowInteractWithVillagers(allowInteractWithVillagers);
            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangePlayerFilter(GlobalChunkPos.of(level.dimension(), chunkPos), PlayerFilterField.allowInteractWithVillagers, filters.getAllowInteractWithVillagers()));
        }
    }

    public void setAllowInteractWithVillagers(Level level, PlayerFilter allowInteractWithVillagers) {
        if (level.isClientSide) {
            setAllowInteractWithVillagers(allowInteractWithVillagers);
        } else {
            setAllowInteractWithVillagers((ServerLevel)level, allowInteractWithVillagers);
        }
    }

    public boolean allowInteractWithVillagers(Player player) {
        var owner = this.owner;
        return owner == null || filters.getAllowInteractWithVillagers().test(player, owner.uuid);
    }

    @Override
    public EntityFilter getAllowHurtVillagers() {
        return filters.getAllowHurtVillagers();
    }

    @Override
    public void setAllowHurtVillagers(EntityFilter allowHurtVillagers) {
        filters.setAllowHurtVillagers(allowHurtVillagers);
    }

    public void setAllowHurtVillagers(ServerLevel level, EntityFilter allowHurtVillagers) {
        if (filters.getAllowHurtVillagers() != allowHurtVillagers) {
            filters.setAllowHurtVillagers(allowHurtVillagers);
            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangeEntityFilter(GlobalChunkPos.of(level.dimension(), chunkPos), EntityFilterField.allowHurtVillagers, filters.getAllowHurtVillagers()));
        }
    }

    public void setAllowHurtVillagers(Level level, EntityFilter allowHurtVillagers) {
        if (level.isClientSide) {
            setAllowHurtVillagers(allowHurtVillagers);
        } else {
            setAllowHurtVillagers((ServerLevel)level, allowHurtVillagers);
        }
    }

    public boolean allowHurtVillagers(Entity entity, @Nullable AbstractVillager hurtEntity) {
        var owner = this.owner;
        return owner == null || isOwner(entity, hurtEntity) || filters.getAllowHurtVillagers().test(entity, owner.uuid);
    }

    @Override
    public EntityFilter getAllowHurtPets() {
        return filters.getAllowHurtPets();
    }

    @Override
    public void setAllowHurtPets(EntityFilter allowHurtPets) {
        filters.setAllowHurtPets(allowHurtPets);
    }

    public void setAllowHurtPets(ServerLevel level, EntityFilter allowHurtPets) {
        if (filters.getAllowHurtPets() != allowHurtPets) {
            filters.setAllowHurtPets(allowHurtPets);
            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangeEntityFilter(GlobalChunkPos.of(level.dimension(), chunkPos), EntityFilterField.allowHurtPets, filters.getAllowHurtPets()));
        }
    }

    public void setAllowHurtPets(Level level, EntityFilter allowHurtPets) {
        if (level.isClientSide) {
            setAllowHurtPets(allowHurtPets);
        } else {
            setAllowHurtPets((ServerLevel)level, allowHurtPets);
        }
    }

    public boolean allowHurtPets(Entity entity, @Nullable Entity hurtEntity) {
        var owner = this.owner;
        return owner == null || isOwner(entity, hurtEntity) || filters.getAllowHurtPets().test(entity, owner.uuid);
    }

    @Override
    public EntityFilter getAllowHurtPassiveMobs() {
        return filters.getAllowHurtPassiveMobs();
    }

    @Override
    public void setAllowHurtPassiveMobs(EntityFilter allowHurtPassiveMobs) {
        filters.setAllowHurtPassiveMobs(allowHurtPassiveMobs);
    }

    public void setAllowHurtPassiveMobs(ServerLevel level, EntityFilter allowHurtPassiveMobs) {
        if (filters.getAllowHurtPassiveMobs() != allowHurtPassiveMobs) {
            filters.setAllowHurtPassiveMobs(allowHurtPassiveMobs);
            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangeEntityFilter(GlobalChunkPos.of(level.dimension(), chunkPos), EntityFilterField.allowHurtPassiveMobs, filters.getAllowHurtPassiveMobs()));
        }
    }

    public void setAllowHurtPassiveMobs(Level level, EntityFilter allowHurtPassiveMobs) {
        if (level.isClientSide) {
            setAllowHurtPassiveMobs(allowHurtPassiveMobs);
        } else {
            setAllowHurtPassiveMobs((ServerLevel)level, allowHurtPassiveMobs);
        }
    }

    public boolean allowHurtPassiveMobs(Entity entity, @Nullable Entity hurtEntity) {
        var owner = this.owner;
        return owner == null || isOwner(entity, hurtEntity) || filters.getAllowHurtPassiveMobs().test(entity, owner.uuid);
    }

    @Override
    public EntityFilter getAllowHurtHostileMobs() {
        return filters.getAllowHurtHostileMobs();
    }

    @Override
    public void setAllowHurtHostileMobs(EntityFilter allowHurtHostileMobs) {
        filters.setAllowHurtHostileMobs(allowHurtHostileMobs);
    }

    public void setAllowHurtHostileMobs(ServerLevel level, EntityFilter allowHurtHostileMobs) {
        if (filters.getAllowHurtHostileMobs() != allowHurtHostileMobs) {
            filters.setAllowHurtHostileMobs(allowHurtHostileMobs);
            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangeEntityFilter(GlobalChunkPos.of(level.dimension(), chunkPos), EntityFilterField.allowHurtHostileMobs, filters.getAllowHurtHostileMobs()));
        }
    }

    public void setAllowHurtHostileMobs(Level level, EntityFilter allowHurtHostileMobs) {
        if (level.isClientSide) {
            setAllowHurtHostileMobs(allowHurtHostileMobs);
        } else {
            setAllowHurtHostileMobs((ServerLevel)level, allowHurtHostileMobs);
        }
    }

    public boolean allowHurtHostileMobs(Entity entity, @Nullable Entity hurtEntity) {
        var owner = this.owner;
        return owner == null || isOwner(entity, hurtEntity) || filters.getAllowHurtHostileMobs().test(entity, owner.uuid);
    }

    @Override
    public PlayerFilter getAllowBreeding() {
        return filters.getAllowBreeding();
    }

    @Override
    public void setAllowBreeding(PlayerFilter allowBreeding) {
        filters.setAllowBreeding(allowBreeding);
    }

    public void setAllowBreeding(ServerLevel level, PlayerFilter allowBreeding) {
        if (filters.getAllowBreeding() != allowBreeding) {
            filters.setAllowBreeding(allowBreeding);
            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangePlayerFilter(GlobalChunkPos.of(level.dimension(), chunkPos), PlayerFilterField.allowBreeding, filters.getAllowBreeding()));
        }
    }

    public void setAllowBreeding(Level level, PlayerFilter allowBreeding) {
        if (level.isClientSide) {
            setAllowBreeding(allowBreeding);
        } else {
            setAllowBreeding((ServerLevel)level, allowBreeding);
        }
    }

    public boolean allowBreeding(Player player, @Nullable Animal bredAnimal) {
        var owner = this.owner;
        return owner == null || isOwner(player, bredAnimal) ||  filters.getAllowBreeding().test(player, owner.uuid);
    }

    @Override
    public PlayerFilter getAllowTaming() {
        return filters.getAllowTaming();
    }

    @Override
    public void setAllowTaming(PlayerFilter allowTaming) {
        filters.setAllowTaming(allowTaming);
    }

    public void setAllowTaming(ServerLevel level, PlayerFilter allowTaming) {
        if (filters.getAllowTaming() != allowTaming) {
            filters.setAllowTaming(allowTaming);
            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangePlayerFilter(GlobalChunkPos.of(level.dimension(), chunkPos), PlayerFilterField.allowTaming, filters.getAllowTaming()));
        }
    }

    public void setAllowTaming(Level level, PlayerFilter allowTaming) {
        if (level.isClientSide) {
            setAllowTaming(allowTaming);
        } else {
            setAllowTaming((ServerLevel)level, allowTaming);
        }
    }

    public boolean allowTaming(Player player, @Nullable TamableAnimal tamableAnimal) {
        var owner = this.owner;
        return owner == null || filters.getAllowTaming().test(player, owner.uuid);
    }

    @Override
    public PlayerFilter getAllowLeashing() {
        return filters.getAllowLeashing();
    }

    @Override
    public void setAllowLeashing(PlayerFilter allowLeashing) {
        filters.setAllowLeashing(allowLeashing);
    }

    public void setAllowLeashing(ServerLevel level, PlayerFilter allowLeashing) {
        if (filters.getAllowLeashing() != allowLeashing) {
            filters.setAllowLeashing(allowLeashing);
            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangePlayerFilter(GlobalChunkPos.of(level.dimension(), chunkPos), PlayerFilterField.allowLeashing, filters.getAllowLeashing()));
        }
    }

    public void setAllowLeashing(Level level, PlayerFilter allowLeashing) {
        if (level.isClientSide) {
            setAllowLeashing(allowLeashing);
        } else {
            setAllowLeashing((ServerLevel)level, allowLeashing);
        }
    }

    public boolean allowLeashing(Player player, @Nullable Entity leashedEntity) {
        var owner = this.owner;
        return owner == null || isOwner(player, leashedEntity) || filters.getAllowLeashing().test(player, owner.uuid);
    }

    @Override
    public PlayerFilter getAllowMilking() {
        return filters.getAllowMilking();
    }

    @Override
    public void setAllowMilking(PlayerFilter allowMilking) {
        filters.setAllowMilking(allowMilking);
    }

    public void setAllowMilking(ServerLevel level, PlayerFilter allowMilking) {
        if (filters.getAllowMilking() != allowMilking) {
            filters.setAllowMilking(allowMilking);
            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangePlayerFilter(GlobalChunkPos.of(level.dimension(), chunkPos), PlayerFilterField.allowMilking, filters.getAllowMilking()));
        }
    }

    public void setAllowMilking(Level level, PlayerFilter allowMilking) {
        if (level.isClientSide) {
            setAllowMilking(allowMilking);
        } else {
            setAllowMilking((ServerLevel)level, allowMilking);
        }
    }

    public boolean allowMilking(Player player, @Nullable Entity milkedEntity) {
        var owner = this.owner;
        return owner == null || isOwner(player, milkedEntity) || filters.getAllowMilking().test(player, owner.uuid);
    }

    @Override
    public EntityFilter getAllowSplashPotions() {
        return filters.getAllowSplashPotions();
    }

    @Override
    public void setAllowSplashPotions(EntityFilter allowSplashPotions) {
        filters.setAllowSplashPotions(allowSplashPotions);
    }

    public void setAllowSplashPotions(ServerLevel level, EntityFilter allowSplashPotions) {
        if (filters.getAllowSplashPotions() != allowSplashPotions) {
            filters.setAllowSplashPotions(allowSplashPotions);
            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangeEntityFilter(GlobalChunkPos.of(level.dimension(), chunkPos), EntityFilterField.allowSplashPotions, filters.getAllowSplashPotions()));
        }
    }

    public void setAllowSplashPotions(Level level, EntityFilter allowSplashPotions) {
        if (level.isClientSide) {
            setAllowSplashPotions(allowSplashPotions);
        } else {
            setAllowSplashPotions((ServerLevel)level, allowSplashPotions);
        }
    }

    public boolean allowSplashPotions(Entity entity) {
        var owner = this.owner;
        return owner == null || filters.getAllowSplashPotions().test(entity, owner.uuid);
    }

    @Override
    public EntityFilter getAllowProjectiles() {
        return filters.getAllowProjectiles();
    }

    @Override
    public void setAllowProjectiles(EntityFilter allowProjectiles) {
        filters.setAllowProjectiles(allowProjectiles);
    }

    public void setAllowProjectiles(ServerLevel level, EntityFilter allowProjectiles) {
        if (filters.getAllowProjectiles() != allowProjectiles) {
            filters.setAllowProjectiles(allowProjectiles);
            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangeEntityFilter(GlobalChunkPos.of(level.dimension(), chunkPos), EntityFilterField.allowProjectiles, filters.getAllowProjectiles()));
        }
    }

    public void setAllowProjectiles(Level level, EntityFilter allowProjectiles) {
        if (level.isClientSide) {
            setAllowProjectiles(allowProjectiles);
        } else {
            setAllowProjectiles((ServerLevel)level, allowProjectiles);
        }
    }

    public boolean allowProjectiles(Entity entity) {
        var owner = this.owner;
        return owner == null || filters.getAllowProjectiles().test(entity, owner.uuid);
    }

    @Override
    public PlayerFilter getAllowEnderPearls() {
        return filters.getAllowEnderPearls();
    }

    @Override
    public void setAllowEnderPearls(PlayerFilter allowEnderPearls) {
        filters.setAllowEnderPearls(allowEnderPearls);
    }

    public void setAllowEnderPearls(ServerLevel level, PlayerFilter allowEnderPearls) {
        if (filters.getAllowEnderPearls() != allowEnderPearls) {
            filters.setAllowEnderPearls(allowEnderPearls);
            PacketDistributor.sendToPlayersTrackingChunk(level, chunkPos, new ChunkClaimPacket.ChangePlayerFilter(GlobalChunkPos.of(level.dimension(), chunkPos), PlayerFilterField.allowEnderPearls, filters.getAllowEnderPearls()));
        }
    }

    public void setAllowEnderPearls(Level level, PlayerFilter allowEnderPearls) {
        if (level.isClientSide) {
            setAllowEnderPearls(allowEnderPearls);
        } else {
            setAllowEnderPearls((ServerLevel)level, allowEnderPearls);
        }
    }

    public boolean allowEnderPearls(Player player) {
        var owner = this.owner;
        return owner == null || filters.getAllowEnderPearls().test(player, owner.uuid);
    }

    @Override
    public Map<PlayerFilterField, PlayerFilter> getPlayerFilters() {
        return filters.getPlayerFilters();
    }

    @Override
    public Map<EntityFilterField, EntityFilter> getEntityFilters() {
        return filters.getEntityFilters();
    }

    private static boolean isOwner(Entity owningEntity, @Nullable Entity entity) {
        return entity instanceof OwnableEntity ownableEntity && owningEntity.getUUID().equals(ownableEntity.getOwnerUUID());
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

    public enum BlockInteractionCategory {
        DOORS(OzoneBlockTags.DOORS, OzoneChunkAttachment::allowInteractWithDoors),
        CONTAINERS(OzoneBlockTags.CONTAINERS, OzoneChunkAttachment::allowInteractWithContainers) {
            @Override
            public boolean has(BlockState state) {
                return state.hasBlockEntity() && state.getBlock() != Blocks.ENDER_CHEST && super.has(state);
            }
        },
        WORKBENCHES(OzoneBlockTags.WORKBENCHES, OzoneChunkAttachment::allowInteractWithWorkbenches),
        SPECIAL_INTERACTABLES(OzoneBlockTags.SPECIAL_INTERACTABLES, OzoneChunkAttachment::allowInteractWithSpecial),
        CONFIGURABLE(OzoneBlockTags.CONFIGURABLE, OzoneChunkAttachment::allowConfiguration),
        REDSTONE(OzoneBlockTags.REDSTONE, OzoneChunkAttachment::allowInteractWithRedstone),
        SIGNS(BlockTags.ALL_SIGNS, OzoneChunkAttachment::allowInteractWithSigns),
        PLANTS(OzoneBlockTags.PLANTS, OzoneChunkAttachment::allowInteractWithPlants),
        OTHER(OzoneBlockTags.OTHER, OzoneChunkAttachment::allowInteractWithOther);
        
        private final TagKey<Block> tag;
        private final TriPredicate<OzoneChunkAttachment, BlockState, Entity> predicate;

        private <E extends Entity> BlockInteractionCategory(TagKey<Block> tag, BiPredicate<OzoneChunkAttachment, E> predicate) {
            this(tag, (OzoneChunkAttachment claim, @Nullable BlockState state, E entity) -> predicate.test(claim, entity));
        }

        private <E extends Entity> BlockInteractionCategory(TagKey<Block> tag, TriPredicate<OzoneChunkAttachment, BlockState, E> predicate) {
            this.tag = tag;
            this.predicate = (TriPredicate<OzoneChunkAttachment, BlockState, Entity>) predicate;
        }

        public boolean isInteractAllowed(@Nullable BlockState blockState, Entity entity, OzoneChunkAttachment claim) {
            try {
                return predicate.test(claim, blockState, entity);
            } catch (ClassCastException e) {
                return false;
            }
        }

        public boolean has(BlockState state) {
            return state.is(tag);
        }
        
        public static Set<BlockInteractionCategory> of(BlockState state) {
            EnumSet<BlockInteractionCategory> results = null;
            for (var category : BlockInteractionCategory.values()) {
                if (category.has(state)) {
                    if (results == null) {
                        results = EnumSet.of(category);
                    } else {
                        results.add(category);
                    }
                }
            }
            return results == null? Set.of() : results;
        }
    }

    public enum EntityHurtCategory {
        VILLAGERS(OzoneEntityTypeTags.VILLAGERS, OzoneChunkAttachment::allowHurtVillagers),
        PETS(OzoneEntityTypeTags.PETS, OzoneChunkAttachment::allowHurtPets) {
            @Override
            public boolean has(Entity entity, Entity attacker) {
                return entity instanceof TamableAnimal tamableAnimal && tamableAnimal.isTame() || entity.getType().is(tag);
            }
        },
        PASSIVE_MOBS(OzoneEntityTypeTags.PASSIVE_MOBS, OzoneChunkAttachment::allowHurtPassiveMobs) {
            @Override
            public boolean has(Entity entity, @Nullable Entity attacker) {
                if (!(entity instanceof LivingEntity livingEntity)) {
                    return entity.getType().is(tag);
                }
                if (livingEntity instanceof NeutralMob neutralMob) {
                    return neutralMob.getTarget() == null || entity.getType().is(tag);
                } else {
                    return !(livingEntity instanceof Enemy) || entity.getType().is(tag);
                }
            }
        },
        HOSTILE_MOBS(OzoneEntityTypeTags.HOSTILE_MOBS, OzoneChunkAttachment::allowHurtHostileMobs) {
            @Override
            public boolean has(Entity entity, @Nullable Entity attacker) {
                if (!(entity instanceof LivingEntity livingEntity)) {
                    return entity.getType().is(tag);
                }
                if (livingEntity instanceof NeutralMob neutralMob) {
                    var target = neutralMob.getTarget();
                    if (target == null) return entity.getType().is(tag);
                    return target.is(attacker) || target instanceof Player || attacker instanceof Player && (isOwner(attacker, target) || attacker != null && PASSIVE_MOBS.has(attacker, null)) || entity.getType().is(tag);
                } else {
                    return livingEntity instanceof Enemy || entity.getType().is(tag);
                }
            }
        };

        protected final TagKey<EntityType<?>> tag;
        private final TriPredicate<OzoneChunkAttachment, Entity, Entity> predicate;

        private <E1 extends Entity> EntityHurtCategory(TagKey<EntityType<?>> tag, BiPredicate<OzoneChunkAttachment, E1> predicate) {
            this(tag, (OzoneChunkAttachment claim, E1 entity1, @Nullable Entity entity2) -> predicate.test(claim, entity1));
        }

        private <E1 extends Entity, E2 extends Entity> EntityHurtCategory(TagKey<EntityType<?>> tag, TriPredicate<OzoneChunkAttachment, E1, E2> predicate) {
            this.tag = tag;
            this.predicate = (TriPredicate<OzoneChunkAttachment, Entity, Entity>) predicate;
        }

        public boolean isInteractAllowed(Entity entity, OzoneChunkAttachment claim) {
            return isInteractAllowed(entity, null, claim);
        }

        public boolean isInteractAllowed(Entity entity, @Nullable Entity actedUponEntity, OzoneChunkAttachment claim) {
            try {
                return predicate.test(claim, entity, actedUponEntity);
            } catch (ClassCastException e) {
                return false;
            }
        }

        public boolean has(Entity entity, @Nullable Entity attacker) {
            return entity.getType().is(tag);
        }

        public static Set<EntityHurtCategory> of(Entity entity, @Nullable Entity attacker) {
            EnumSet<EntityHurtCategory> results = null;
            for (var category : EntityHurtCategory.values()) {
                if (category.has(entity, attacker)) {
                    if (results == null) {
                        results = EnumSet.of(category);
                    } else {
                        results.add(category);
                    }
                }
            }
            return results == null? Set.of() : results;
        }
    }
}
