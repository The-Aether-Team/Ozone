package com.aetherteam.ozone.claims;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public enum EntityFilter implements IFilter<EntityFilter>, BiPredicate<Entity, UUID> {
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
        .collect(Collectors.toMap(e -> Filters.cleanName(e.name), e -> e));

    private final String name = name().toLowerCase(Locale.ROOT);
    private final BiPredicate<Entity, UUID> predicate;

    private EntityFilter(BiPredicate<Entity, UUID> predicate) {
        this.predicate = predicate;
    }

    public EntityFilter atLeastOwner() {
        return this == NO_ONE? OWNER_ONLY : this;
    }

    @Override
    public boolean test(Entity entity, UUID ownerUuid) {
        return predicate.test(entity, ownerUuid);
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    @Override
    public Component getDescription() {
        return Component.translatable("argument.ozone_utilities.entity_filter." + name);
    }

    @Nullable
    public static EntityFilter getByName(@Nullable String friendlyName) {
        return friendlyName == null? null : BY_NAME.get(Filters.cleanName(friendlyName));
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