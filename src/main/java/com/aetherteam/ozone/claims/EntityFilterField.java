package com.aetherteam.ozone.claims;

import static com.aetherteam.ozone.claims.EntityFilter.ANYONE;
import static com.aetherteam.ozone.claims.EntityFilter.FRIENDS_AND_MOBS;
import static com.aetherteam.ozone.claims.EntityFilter.OWNER_AND_MOBS;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.mojang.serialization.Codec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum EntityFilterField implements IFilterField<EntityFilterField, EntityFilter> {
    allowExplosions(OWNER_AND_MOBS, IFilters::getAllowExplosions, IFilters::setAllowExplosions, EntityFilter.NO_ONE),
    allowPlace(FRIENDS_AND_MOBS, IFilters::getAllowPlace, IFilters::setAllowPlace),
    allowBreak(FRIENDS_AND_MOBS, IFilters::getAllowBreak, IFilters::setAllowBreak),
    allowInteractWithDoors(ANYONE, IFilters::getAllowInteractWithDoors, IFilters::setAllowInteractWithDoors),
    allowInteractWithRedstone(ANYONE, IFilters::getAllowInteractWithRedstone, IFilters::setAllowConfiguration),
    allowConfiguration(ANYONE, IFilters::getAllowConfiguration, IFilters::setAllowConfiguration),
    allowInteractWithOther(FRIENDS_AND_MOBS, IFilters::getAllowInteractWithOther, IFilters::setAllowInteractWithOther),
    allowDrop(ANYONE, IFilters::getAllowDrop, IFilters::setAllowDrop),
    allowHurtVillagers(OWNER_AND_MOBS, IFilters::getAllowHurtVillagers, IFilters::setAllowHurtVillagers),
    allowHurtPets(OWNER_AND_MOBS, IFilters::getAllowHurtPets, IFilters::setAllowHurtPets),
    allowHurtPassiveMobs(FRIENDS_AND_MOBS, IFilters::getAllowHurtPassiveMobs, IFilters::setAllowHurtPassiveMobs),
    allowHurtHostileMobs(ANYONE, IFilters::getAllowHurtHostileMobs, IFilters::setAllowHurtHostileMobs),
    allowSplashPotions(ANYONE, IFilters::getAllowSplashPotions, IFilters::setAllowSplashPotions),
    allowProjectiles(ANYONE, IFilters::getAllowProjectiles, IFilters::setAllowProjectiles);

    public static final Codec<EntityFilterField> CODEC = StringRepresentable.fromEnum(EntityFilterField::values);
    public static final StreamCodec<RegistryFriendlyByteBuf, EntityFilterField> STREAM_CODEC = new StreamCodec<>() {
        public void encode(RegistryFriendlyByteBuf buffer, EntityFilterField value) {
            buffer.writeEnum(value);
        }

        public EntityFilterField decode(RegistryFriendlyByteBuf buffer) {
            return buffer.readEnum(EntityFilterField.class);
        }
    };

    private final Function<IFilters, EntityFilter> getter;
    private final BiConsumer<IFilters, EntityFilter> setter;
    private final EntityFilter defaultValue, min;

    private EntityFilterField(EntityFilter defaultValue, Function<IFilters, EntityFilter> getter, BiConsumer<IFilters, EntityFilter> setter) {
        this(defaultValue, getter, setter, EntityFilter.OWNER_ONLY);
    }

    private EntityFilterField(EntityFilter defaultValue, Function<IFilters, EntityFilter> getter, BiConsumer<IFilters, EntityFilter> setter, EntityFilter min) {
        this.defaultValue = defaultValue;
        this.getter = getter;
        this.setter = setter;
        this.min = min;
    }

    @Override
    public EntityFilter getMinValue() {
        return min;
    }

    @Override
    public EntityFilter getDefaultValue() {
        return defaultValue;
    }

    @Override
    public EntityFilter get(IFilters claim) {
        return getter.apply(claim);
    }

    @Override
    public void set(IFilters claim, EntityFilter value) {
        setter.accept(claim, value);
    }

    @Override
    public Class<EntityFilter> getFilterClass() {
        return EntityFilter.class;
    }

    @Override
    public String getSerializedName() {
        return name();
    }

    static final EntityFilterField[] VALUES = EntityFilterField.values();
}