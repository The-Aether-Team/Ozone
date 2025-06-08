package com.aetherteam.ozone.claims;

import java.util.AbstractSet;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.google.common.collect.Iterators;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public class Filters implements IFilters {
    private EntityFilter allowExplosions;
    private EntityFilter allowPlace;
    private EntityFilter allowBreak;
    private PlayerFilter allowInteractWithContainers;
    private EntityFilter allowInteractWithDoors;
    private EntityFilter allowInteractWithRedstone;
    private EntityFilter allowConfiguration;
    private PlayerFilter allowInteractWithSigns;
    private EntityFilter allowInteractWithOther;
    private EntityFilter allowDrop;
    private PlayerFilter allowInteractWithWorkbenches;
    private PlayerFilter allowInteractWithSpecial;
    private PlayerFilter allowInteractWithPlants;
    private PlayerFilter allowInteractWithVillagers;
    private EntityFilter allowHurtVillagers;
    private EntityFilter allowHurtPets;
    private EntityFilter allowHurtPassiveMobs;
    private EntityFilter allowHurtHostileMobs;
    private PlayerFilter allowBreeding;
    private PlayerFilter allowTaming;
    private PlayerFilter allowLeashing;
    private PlayerFilter allowMilking;
    private EntityFilter allowSplashPotions;
    private EntityFilter allowProjectiles;
    private PlayerFilter allowEnderPearls;

    public static final Codec<Filters> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.simpleMap(
                PlayerFilterField.CODEC,
                PlayerFilter.CODEC,
                StringRepresentable.keys(PlayerFilterField.values())
            )
            .fieldOf("playerFilters").forGetter(IFilters::getPlayerFilters),
        Codec.simpleMap(
                EntityFilterField.CODEC,
                EntityFilter.CODEC,
                StringRepresentable.keys(EntityFilterField.values())
            )
            .fieldOf("entityFilters").forGetter(IFilters::getEntityFilters)
    ).apply(instance, Filters::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, Filters> STREAM_CODEC = new StreamCodec<>() {
        public void encode(RegistryFriendlyByteBuf buffer, Filters value) {
            for (var field : fields()) {
                buffer.writeEnum(field.get(value));
            }
        }

        public Filters decode(RegistryFriendlyByteBuf buffer) {
            var value = new Filters(0);
            for (var field : fields()) {
                readAndSet(field, value, buffer);
            }
            return value;
        }

        private static <Field extends IFilterField<?, Filter>, Filter extends Enum<Filter> & IFilter<Filter>>
        void readAndSet(Field field, IFilters value, RegistryFriendlyByteBuf buffer) {
            field.set(value, buffer.readEnum(field.getFilterClass()));
        }
    };

    public Filters() {
        reset();
    }

    private Filters(int unused) {}

    public Filters(Map<PlayerFilterField, PlayerFilter> playerFilters, Map<EntityFilterField, EntityFilter> entityFilters) {
        for (var field : PlayerFilterField.values()) {
            var value = playerFilters.get(field);
            if (value == null) value = field.getDefaultValue();
            field.set(this, value);
        }
        for (var field : EntityFilterField.values()) {
            var value = entityFilters.get(field);
            if (value == null) value = field.getDefaultValue();
            field.set(this, value);
        }
    }

    public void reset() {
        for (var field : fields()) {
            reset(field);
        }
    }

    public <Field extends IFilterField<?, Filter>, Filter extends Enum<Filter> & IFilter<Filter>>
    void reset(Field field) {
        field.set(this, field.getDefaultValue());
    }

    public void assignFrom(IFilters filters) {
        if (filters == this) return;
        for (var field : fields()) {
            setFrom(field, filters);
        }
    }

    public <Field extends IFilterField<?, Filter>, Filter extends Enum<Filter> & IFilter<Filter>>
    void setFrom(Field field, IFilters filters) {
        field.set(this, field.get(filters));
    }

    @Override
    public EntityFilter getAllowExplosions() {
        return allowExplosions;
    }

    @Override
    public void setAllowExplosions(EntityFilter allowExplosions) {
        this.allowExplosions = allowExplosions.atLeast(EntityFilterField.allowExplosions.getMinValue());
    }

    @Override
    public EntityFilter getAllowPlace() {
        return allowPlace;
    }

    @Override
    public void setAllowPlace(EntityFilter allowPlace) {
        this.allowPlace = allowPlace.atLeast(EntityFilterField.allowPlace.getMinValue());
    }

    @Override
    public EntityFilter getAllowBreak() {
        return allowBreak;
    }

    @Override
    public void setAllowBreak(EntityFilter allowBreak) {
        this.allowBreak = allowBreak.atLeast(EntityFilterField.allowBreak.getMinValue());
    }

    @Override
    public PlayerFilter getAllowInteractWithContainers() {
        return allowInteractWithContainers;
    }

    @Override
    public void setAllowInteractWithContainers(PlayerFilter allowInteractWithContainers) {
        this.allowInteractWithContainers = allowInteractWithContainers.atLeast(PlayerFilterField.allowInteractWithContainers.getMinValue());
    }

    @Override
    public EntityFilter getAllowInteractWithDoors() {
        return allowInteractWithDoors;
    }

    @Override
    public void setAllowInteractWithDoors(EntityFilter allowInteractWithDoors) {
        this.allowInteractWithDoors = allowInteractWithDoors.atLeast(EntityFilterField.allowInteractWithDoors.getMinValue());
    }

    @Override
    public EntityFilter getAllowInteractWithRedstone() {
        return allowInteractWithRedstone;
    }

    @Override
    public void setAllowInteractWithRedstone(EntityFilter allowInteractWithRedstone) {
        this.allowInteractWithRedstone = allowInteractWithRedstone.atLeast(EntityFilterField.allowInteractWithRedstone.getMinValue());
    }

    @Override
    public EntityFilter getAllowConfiguration() {
        return allowConfiguration;
    }

    @Override
    public void setAllowConfiguration(EntityFilter allowConfiguration) {
        this.allowConfiguration = allowConfiguration.atLeast(EntityFilterField.allowConfiguration.getMinValue());
    }

    @Override
    public PlayerFilter getAllowInteractWithSigns() {
        return allowInteractWithSigns;
    }

    @Override
    public void setAllowInteractWithSigns(PlayerFilter allowInteractWithSigns) {
        this.allowInteractWithSigns = allowInteractWithSigns.atLeast(PlayerFilterField.allowInteractWithSigns.getMinValue());
    }

    @Override
    public EntityFilter getAllowInteractWithOther() {
        return allowInteractWithOther;
    }

    @Override
    public void setAllowInteractWithOther(EntityFilter allowInteractWithOther) {
        this.allowInteractWithOther = allowInteractWithOther.atLeast(EntityFilterField.allowInteractWithOther.getMinValue());
    }

    @Override
    public EntityFilter getAllowDrop() {
        return allowDrop;
    }

    @Override
    public void setAllowDrop(EntityFilter allowDrop) {
        this.allowDrop = allowDrop.atLeast(EntityFilterField.allowDrop.getMinValue());
    }

    @Override
    public PlayerFilter getAllowInteractWithWorkbenches() {
        return allowInteractWithWorkbenches;
    }

    @Override
    public void setAllowInteractWithWorkbenches(PlayerFilter allowInteractWithWorkbenches) {
        this.allowInteractWithWorkbenches = allowInteractWithWorkbenches.atLeast(PlayerFilterField.allowInteractWithWorkbenches.getMinValue());
    }

    @Override
    public PlayerFilter getAllowInteractWithSpecial() {
        return allowInteractWithSpecial;
    }

    @Override
    public void setAllowInteractWithSpecial(PlayerFilter allowInteractWithSpecial) {
        this.allowInteractWithSpecial = allowInteractWithSpecial.atLeast(PlayerFilterField.allowInteractWithSpecial.getMinValue());
    }

    @Override
    public PlayerFilter getAllowInteractWithPlants() {
        return allowInteractWithPlants;
    }

    @Override
    public void setAllowInteractWithPlants(PlayerFilter allowInteractWithPlants) {
        this.allowInteractWithPlants = allowInteractWithPlants.atLeast(PlayerFilterField.allowInteractWithPlants.getMinValue());
    }

    @Override
    public PlayerFilter getAllowInteractWithVillagers() {
        return allowInteractWithVillagers;
    }

    @Override
    public void setAllowInteractWithVillagers(PlayerFilter allowInteractWithVillagers) {
        this.allowInteractWithVillagers = allowInteractWithVillagers.atLeast(PlayerFilterField.allowInteractWithVillagers.getMinValue());
    }

    @Override
    public EntityFilter getAllowHurtVillagers() {
        return allowHurtVillagers;
    }

    @Override
    public void setAllowHurtVillagers(EntityFilter allowHurtVillagers) {
        this.allowHurtVillagers = allowHurtVillagers.atLeast(EntityFilterField.allowHurtVillagers.getMinValue());
    }

    @Override
    public EntityFilter getAllowHurtPets() {
        return allowHurtPets;
    }

    @Override
    public void setAllowHurtPets(EntityFilter allowHurtPets) {
        this.allowHurtPets = allowHurtPets.atLeast(EntityFilterField.allowHurtPets.getMinValue());
    }

    @Override
    public EntityFilter getAllowHurtPassiveMobs() {
        return allowHurtPassiveMobs;
    }

    @Override
    public void setAllowHurtPassiveMobs(EntityFilter allowHurtPassiveMobs) {
        this.allowHurtPassiveMobs = allowHurtPassiveMobs.atLeast(EntityFilterField.allowHurtPassiveMobs.getMinValue());
    }

    @Override
    public EntityFilter getAllowHurtHostileMobs() {
        return allowHurtHostileMobs;
    }

    @Override
    public void setAllowHurtHostileMobs(EntityFilter allowHurtHostileMobs) {
        this.allowHurtHostileMobs = allowHurtHostileMobs.atLeast(EntityFilterField.allowHurtHostileMobs.getMinValue());
    }

    @Override
    public PlayerFilter getAllowBreeding() {
        return allowBreeding;
    }

    @Override
    public void setAllowBreeding(PlayerFilter allowBreeding) {
        this.allowBreeding = allowBreeding.atLeast(PlayerFilterField.allowBreeding.getMinValue());
    }

    @Override
    public PlayerFilter getAllowTaming() {
        return allowTaming;
    }

    @Override
    public void setAllowTaming(PlayerFilter allowTaming) {
        this.allowTaming = allowTaming.atLeast(PlayerFilterField.allowTaming.getMinValue());
    }

    @Override
    public PlayerFilter getAllowLeashing() {
        return allowLeashing;
    }

    @Override
    public void setAllowLeashing(PlayerFilter allowLeashing) {
        this.allowLeashing = allowLeashing.atLeast(PlayerFilterField.allowLeashing.getMinValue());
    }

    @Override
    public PlayerFilter getAllowMilking() {
        return allowMilking;
    }

    @Override
    public void setAllowMilking(PlayerFilter allowMilking) {
        this.allowMilking = allowMilking.atLeast(PlayerFilterField.allowMilking.getMinValue());
    }

    @Override
    public EntityFilter getAllowSplashPotions() {
        return allowSplashPotions;
    }

    @Override
    public void setAllowSplashPotions(EntityFilter allowSplashPotions) {
        this.allowSplashPotions = allowSplashPotions.atLeast(EntityFilterField.allowSplashPotions.getMinValue());
    }

    @Override
    public EntityFilter getAllowProjectiles() {
        return allowProjectiles;
    }

    @Override
    public void setAllowProjectiles(EntityFilter allowProjectiles) {
        this.allowProjectiles = allowProjectiles.atLeast(EntityFilterField.allowProjectiles.getMinValue());
    }

    @Override
    public PlayerFilter getAllowEnderPearls() {
        return allowEnderPearls;
    }

    @Override
    public void setAllowEnderPearls(PlayerFilter allowEnderPearls) {
        this.allowEnderPearls = allowEnderPearls.atLeast(PlayerFilterField.allowEnderPearls.getMinValue());
    }

    @Nullable
    private transient Map<PlayerFilterField, PlayerFilter> playerFilters;

    @Override
    public Map<PlayerFilterField, PlayerFilter> getPlayerFilters() {
        var m = playerFilters;
        return m == null? playerFilters = IFilters.super.getPlayerFilters() : m;
    }

    @Nullable
    private transient Map<EntityFilterField, EntityFilter> entityFilters;

    @Override
    public Map<EntityFilterField, EntityFilter> getEntityFilters() {
        var m = entityFilters;
        return m == null? entityFilters = IFilters.super.getEntityFilters() : m;
    }

    static String cleanName(String string) {
        return string.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
    }

    public static final Set<? extends Enum<? extends IFilterField<?, ?>>> fieldsAsEnums() {
        return (Set<? extends Enum<? extends IFilterField<?, ?>>>) FIELDS;
    }

    public static final Set<? extends IFilterField<?, ?>> fields() {
        return (Set<? extends IFilterField<?, ?>>) FIELDS;
    }

    private static final Set<?> FIELDS = new AbstractSet<Enum<? extends IFilterField<?, ?>>>() {
        private static final EnumSet<EntityFilterField> ENTITY_FILTERS = EnumSet.allOf(EntityFilterField.class);
        private static final EnumSet<PlayerFilterField> PLAYER_FILTERS = EnumSet.allOf(PlayerFilterField.class);

        @Override
        public int size() {
            return PLAYER_FILTERS.size() + ENTITY_FILTERS.size();
        }

        @Override
        public Iterator<Enum<? extends IFilterField<?, ?>>> iterator() {
            return Iterators.concat(ENTITY_FILTERS.iterator(), PLAYER_FILTERS.iterator());
        }

        @Override
        public boolean contains(@Nullable Object o) {
            return o instanceof EntityFilterField || o instanceof PlayerFilterField;
        }

        @Override
        public Stream<Enum<? extends IFilterField<?, ?>>> stream() {
            return Stream.concat(ENTITY_FILTERS.stream(), PLAYER_FILTERS.stream());
        }

        @Override
        public Stream<Enum<? extends IFilterField<?, ?>>> parallelStream() {
            return Stream.<Enum<? extends IFilterField<?, ?>>>concat(ENTITY_FILTERS.parallelStream(), PLAYER_FILTERS.parallelStream()).parallel();
        }
    };
}