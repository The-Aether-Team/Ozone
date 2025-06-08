package com.aetherteam.ozone.claims;

import static com.aetherteam.ozone.claims.PlayerFilter.*;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.mojang.serialization.Codec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum PlayerFilterField implements IFilterField<PlayerFilterField, PlayerFilter> {
    allowInteractWithContainers(OWNER_ONLY, IFilters::getAllowInteractWithContainers, IFilters::setAllowInteractWithContainers),
    allowInteractWithSigns(OWNER_ONLY, IFilters::getAllowInteractWithSigns, IFilters::setAllowInteractWithSigns),
    allowInteractWithWorkbenches(ANYONE, IFilters::getAllowInteractWithWorkbenches, IFilters::setAllowInteractWithWorkbenches),
    allowInteractWithSpecial(FRIENDS, IFilters::getAllowInteractWithSpecial, IFilters::setAllowInteractWithSpecial),
    allowInteractWithPlants(FRIENDS, IFilters::getAllowInteractWithPlants, IFilters::setAllowInteractWithPlants),
    allowInteractWithVillagers(ANYONE, IFilters::getAllowInteractWithVillagers, IFilters::setAllowInteractWithVillagers),
    allowBreeding(ANYONE, IFilters::getAllowBreeding, IFilters::setAllowBreeding),
    allowTaming(FRIENDS, IFilters::getAllowTaming, IFilters::setAllowTaming),
    allowLeashing(FRIENDS, IFilters::getAllowLeashing, IFilters::setAllowLeashing),
    allowMilking(ANYONE, IFilters::getAllowMilking, IFilters::setAllowMilking),
    allowEnderPearls(ANYONE, IFilters::getAllowEnderPearls, IFilters::setAllowEnderPearls);
    
    public static final Codec<PlayerFilterField> CODEC = StringRepresentable.fromEnum(PlayerFilterField::values);
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerFilterField> STREAM_CODEC = new StreamCodec<>() {
        public void encode(RegistryFriendlyByteBuf buffer, PlayerFilterField value) {
            buffer.writeEnum(value);
        }

        public PlayerFilterField decode(RegistryFriendlyByteBuf buffer) {
            return buffer.readEnum(PlayerFilterField.class);
        }
    };

    private final Function<IFilters, PlayerFilter> getter;
    private final BiConsumer<IFilters, PlayerFilter> setter;
    private final PlayerFilter defaultValue, min;

    private PlayerFilterField(PlayerFilter defaultValue, Function<IFilters, PlayerFilter> getter, BiConsumer<IFilters, PlayerFilter> setter) {
        this(defaultValue, getter, setter, PlayerFilter.OWNER_ONLY);
    }

    private PlayerFilterField(PlayerFilter defaultValue, Function<IFilters, PlayerFilter> getter, BiConsumer<IFilters, PlayerFilter> setter, PlayerFilter min) {
        this.defaultValue = defaultValue;
        this.getter = getter;
        this.setter = setter;
        this.min = min;
    }

    @Override
    public PlayerFilter getMinValue() {
        return min;
    }

    @Override
    public PlayerFilter getDefaultValue() {
        return defaultValue;
    }

    @Override
    public PlayerFilter get(IFilters claim) {
        return getter.apply(claim);
    }

    @Override
    public void set(IFilters claim, PlayerFilter value) {
        setter.accept(claim, value);
    }

    @Override
    public Class<PlayerFilter> getFilterClass() {
        return PlayerFilter.class;
    }

    @Override
    public String getSerializedName() {
        return name();
    }

    static final PlayerFilterField[] VALUES = PlayerFilterField.values();
}
