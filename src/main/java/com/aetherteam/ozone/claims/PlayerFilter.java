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
import net.minecraft.world.entity.player.Player;

public enum PlayerFilter implements IFilter<PlayerFilter>, BiPredicate<Player, UUID> {
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
        .collect(Collectors.toMap(e -> Filters.cleanName(e.name), e -> e));

    private final String name = name().toLowerCase(Locale.ROOT);
    private final BiPredicate<Player, UUID> predicate;

    private PlayerFilter(BiPredicate<Player, UUID> predicate) {
        this.predicate = predicate;
    }

    public PlayerFilter atLeastOwner() {
        return this == NO_ONE? OWNER_ONLY : this;
    }

    @Override
    public boolean test(Player player, UUID ownerUuid) {
        return predicate.test(player, ownerUuid);
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
    public static PlayerFilter getByName(@Nullable String friendlyName) {
        return friendlyName == null? null : BY_NAME.get(Filters.cleanName(friendlyName));
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