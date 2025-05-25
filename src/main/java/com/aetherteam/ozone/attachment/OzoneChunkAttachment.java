package com.aetherteam.ozone.attachment;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiPredicate;

import javax.annotation.Nullable;

import com.aetherteam.ozone.tags.OzoneBlockTags;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class OzoneChunkAttachment {
    @Nullable
    private UUID owner;
    private EntityFilter allowExplosions;
    private EntityFilter allowPlace;
    private EntityFilter allowBreak;
    private PlayerFilter allowInteractWithContainers;
    private EntityFilter allowInteractWithDoors;
    private EntityFilter allowInteractWithRedstone;
    private EntityFilter allowInteractWithRedstoneActivators;
    private PlayerFilter allowInteractWithSigns;
    private EntityFilter allowInteractWithOther;
    private EntityFilter allowDrop;

    public void resetPermissions() {
        allowExplosions = EntityFilter.OWNER_AND_MOBS;
        allowPlace = EntityFilter.FRIENDS_AND_MOBS;
        allowBreak = EntityFilter.FRIENDS_AND_MOBS;
        allowInteractWithContainers = PlayerFilter.OWNER_ONLY;
        allowInteractWithDoors = EntityFilter.ANYONE;
        allowInteractWithRedstone = EntityFilter.ANYONE;
        allowInteractWithRedstoneActivators = EntityFilter.ANYONE;
        allowInteractWithSigns = PlayerFilter.OWNER_ONLY;
        allowInteractWithOther = EntityFilter.FRIENDS_AND_MOBS;
        allowDrop = EntityFilter.ANYONE;
    }

    public static final Codec<OzoneChunkAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(OzoneChunkAttachment::getOwner),
            EntityFilter.CODEC.fieldOf("allowExplosions").forGetter(OzoneChunkAttachment::getAllowExplosions),
            EntityFilter.CODEC.fieldOf("allowPlace").forGetter(OzoneChunkAttachment::getAllowPlace),
            EntityFilter.CODEC.fieldOf("allowBreak").forGetter(OzoneChunkAttachment::getAllowBreak),
            PlayerFilter.CODEC.fieldOf("allowInteractWithContainers").forGetter(OzoneChunkAttachment::getAllowInteractWithContainers),
            EntityFilter.CODEC.fieldOf("allowInteractWithDoors").forGetter(OzoneChunkAttachment::getAllowInteractWithDoors),
            EntityFilter.CODEC.fieldOf("allowInteractWithRedstone").forGetter(OzoneChunkAttachment::getAllowInteractWithRedstone),
            EntityFilter.CODEC.fieldOf("allowInteractWithRedstoneActivators").forGetter(OzoneChunkAttachment::getAllowInteractWithRedstoneActivators),
            PlayerFilter.CODEC.fieldOf("allowInteractWithSigns").forGetter(OzoneChunkAttachment::getAllowInteractWithSigns),
            EntityFilter.CODEC.fieldOf("allowInteractWithOther").forGetter(OzoneChunkAttachment::getAllowInteractWithOther),
            EntityFilter.CODEC.fieldOf("allowDrop").forGetter(OzoneChunkAttachment::getAllowDrop)
    ).apply(instance, OzoneChunkAttachment::new));

    public OzoneChunkAttachment() {
        resetPermissions();
    }

    public OzoneChunkAttachment(Optional<UUID> owner, EntityFilter allowExplosions, EntityFilter allowPlace,
            EntityFilter allowBreak, PlayerFilter allowInteractWithContainers, EntityFilter allowInteractWithDoors,
            EntityFilter allowInteractWithRedstone, EntityFilter allowInteractWithRedstoneActivators,
            PlayerFilter allowInteractWithSigns, EntityFilter allowInteractWithOther,
            EntityFilter allowDrop) {
        this.owner = owner.orElse(null);
        this.allowExplosions = allowExplosions;
        this.allowPlace = allowPlace.atLeast(EntityFilter.OWNER_ONLY);
        this.allowBreak = allowBreak.atLeast(EntityFilter.OWNER_ONLY);
        this.allowInteractWithContainers = allowInteractWithContainers.atLeast(PlayerFilter.OWNER_ONLY);
        this.allowInteractWithDoors = allowInteractWithDoors.atLeast(EntityFilter.OWNER_ONLY);
        this.allowInteractWithRedstone = allowInteractWithRedstone.atLeast(EntityFilter.OWNER_ONLY);
        this.allowInteractWithRedstoneActivators = allowInteractWithRedstoneActivators.atLeast(EntityFilter.OWNER_ONLY);
        this.allowInteractWithSigns = allowInteractWithSigns.atLeast(PlayerFilter.OWNER_ONLY);
        this.allowInteractWithOther = allowInteractWithOther.atLeast(EntityFilter.OWNER_ONLY);
        this.allowDrop = allowDrop.atLeast(EntityFilter.OWNER_ONLY);
    }

    public Optional<UUID> getOwner() {
        return Optional.ofNullable(owner);
    }

    public void setOwner(UUID owner) {
        if (!this.owner.equals(owner)) {
            this.owner = owner;
            if (owner == null) {
                // reset permissions to default values
                resetPermissions();
            }
        }
    }

    public boolean hasOwner() {
        return owner != null;
    }

    public EntityFilter getAllowExplosions() {
        return allowExplosions;
    }

    public void setAllowExplosions(EntityFilter allowExplosions) {
        this.allowExplosions = allowExplosions;
    }

    public boolean allowExplosions(Entity entity) {
        var owner = this.owner;
        return owner == null || allowExplosions.test(entity, owner);
    }

    public EntityFilter getAllowPlace() {
        return allowPlace;
    }

    public void setAllowPlace(EntityFilter allowPlace) {
        this.allowPlace = allowPlace.atLeast(EntityFilter.OWNER_ONLY);
    }

    public boolean allowPlace(Entity entity) {
        var owner = this.owner;
        return owner == null || allowPlace.test(entity, owner);
    }

    public EntityFilter getAllowBreak() {
        return allowBreak;
    }

    public void setAllowBreak(EntityFilter allowBreak) {
        this.allowBreak = allowBreak.atLeast(EntityFilter.OWNER_ONLY);
    }

    public boolean allowBreak(Entity entity) {
        var owner = this.owner;
        return owner == null || allowBreak.test(entity, owner);
    }

    public PlayerFilter getAllowInteractWithContainers() {
        return allowInteractWithContainers;
    }

    public void setAllowInteractWithContainers(PlayerFilter allowInteractWithContainers) {
        this.allowInteractWithContainers = allowInteractWithContainers.atLeast(PlayerFilter.OWNER_ONLY);
    }

    public boolean allowInteractWithContainers(Player player) {
        var owner = this.owner;
        return owner == null || allowInteractWithContainers.test(player, owner);
    }

    protected boolean allowInteractWithContainers(Entity entity) {
        var owner = this.owner;
        return owner == null || entity instanceof Player player && allowInteractWithContainers.test(player, owner);
    }

    public EntityFilter getAllowInteractWithDoors() {
        return allowInteractWithDoors;
    }

    public void setAllowInteractWithDoors(EntityFilter allowInteractWithDoors) {
        this.allowInteractWithDoors = allowInteractWithDoors.atLeast(EntityFilter.OWNER_ONLY);
    }

    public boolean allowInteractWithDoors(Entity entity) {
        var owner = this.owner;
        return owner == null || allowInteractWithDoors.test(entity, owner);
    }
    
    public EntityFilter getAllowInteractWithRedstone() {
        return allowInteractWithRedstone;
    }

    public void setAllowInteractWithRedstone(EntityFilter allowInteractWithRedstone) {
        this.allowInteractWithRedstone = allowInteractWithRedstone.atLeast(EntityFilter.OWNER_ONLY);
    }

    public boolean allowInteractWithRedstone(Entity entity) {
        var owner = this.owner;
        return owner == null || allowInteractWithRedstone.test(entity, owner);
    }

    public EntityFilter getAllowInteractWithRedstoneActivators() {
        return allowInteractWithRedstoneActivators;
    }

    public void setAllowInteractWithRedstoneActivators(EntityFilter allowInteractWithRedstoneActivators) {
        this.allowInteractWithRedstoneActivators = allowInteractWithRedstoneActivators.atLeast(EntityFilter.OWNER_ONLY);
    }

    public boolean allowInteractWithRedstoneActivators(Entity entity) {
        var owner = this.owner;
        return owner == null || allowInteractWithRedstoneActivators.test(entity, owner);
    }

    public PlayerFilter getAllowInteractWithSigns() {
        return allowInteractWithSigns;
    }

    public void setAllowInteractWithSigns(PlayerFilter allowInteractWithSigns) {
        this.allowInteractWithSigns = allowInteractWithSigns.atLeast(PlayerFilter.OWNER_ONLY);
    }

    public boolean allowInteractWithSigns(Player player) {
        var owner = this.owner;
        return owner == null || allowInteractWithSigns.test(player, owner);
    }

    protected boolean allowInteractWithSigns(Entity entity) {
        var owner = this.owner;
        return owner == null || entity instanceof Player player && allowInteractWithSigns.test(player, owner);
    }

    public EntityFilter getAllowInteractWithOther() {
        return allowInteractWithOther;
    }

    public void setAllowInteractWithOther(EntityFilter allowInteractWithOther) {
        this.allowInteractWithOther = allowInteractWithOther.atLeast(EntityFilter.OWNER_ONLY);
    }

    public boolean allowInteractWithOther(Entity entity) {
        var owner = this.owner;
        return owner == null || allowInteractWithOther.test(entity, owner);
    }

    public EntityFilter getAllowDrop() {
        return allowDrop;
    }

    public void setAllowDrop(EntityFilter allowDrop) {
        this.allowDrop = allowDrop.atLeast(EntityFilter.OWNER_ONLY);
    }

    public boolean allowDrop(Entity entity) {
        var owner = this.owner;
        return owner == null || allowDrop.test(entity, owner);
    }
    
    public enum PlayerFilter implements StringRepresentable, BiPredicate<Player, UUID> {
        NO_ONE((player, ownerUuid) -> false),
        OWNER_ONLY((player, ownerUuid) -> player.getUUID().equals(ownerUuid)),
        FRIENDS((player, ownerUuid) -> player.getUUID().equals(ownerUuid)),
        ANYONE((player, ownerUuid) -> true);

        public static final Codec<PlayerFilter> CODEC = StringRepresentable.fromEnum(PlayerFilter::values);

        private final String serializedName = name().toLowerCase(Locale.ENGLISH);
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
            return serializedName;
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

        private final String serializedName = name().toLowerCase(Locale.ENGLISH);
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
            return serializedName;
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
}
