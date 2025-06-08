package com.aetherteam.ozone.claims;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SequencedSet;
import java.util.Set;
import java.util.Spliterator;
import java.util.stream.Stream;

public interface IFilters {
    EntityFilter getAllowExplosions();
    void setAllowExplosions(EntityFilter allowExplosions);

    EntityFilter getAllowPlace();
    void setAllowPlace(EntityFilter allowPlace);

    EntityFilter getAllowBreak();
    void setAllowBreak(EntityFilter allowBreak);

    PlayerFilter getAllowInteractWithContainers();
    void setAllowInteractWithContainers(PlayerFilter allowInteractWithContainers);

    EntityFilter getAllowInteractWithDoors();
    void setAllowInteractWithDoors(EntityFilter allowInteractWithDoors);

    EntityFilter getAllowInteractWithRedstone();
    void setAllowInteractWithRedstone(EntityFilter allowInteractWithRedstone);

    EntityFilter getAllowConfiguration();
    void setAllowConfiguration(EntityFilter allowConfiguration);

    PlayerFilter getAllowInteractWithSigns();
    void setAllowInteractWithSigns(PlayerFilter allowInteractWithSigns);

    EntityFilter getAllowInteractWithOther();
    void setAllowInteractWithOther(EntityFilter allowInteractWithOther);

    EntityFilter getAllowDrop();
    void setAllowDrop(EntityFilter allowDrop);

    PlayerFilter getAllowInteractWithWorkbenches();
    void setAllowInteractWithWorkbenches(PlayerFilter allowInteractWithWorkbenches);

    PlayerFilter getAllowInteractWithSpecial();
    void setAllowInteractWithSpecial(PlayerFilter allowInteractWithSpecial);

    PlayerFilter getAllowInteractWithPlants();
    void setAllowInteractWithPlants(PlayerFilter allowInteractWithPlants);

    PlayerFilter getAllowInteractWithVillagers();
    void setAllowInteractWithVillagers(PlayerFilter allowInteractWithVillagers);

    EntityFilter getAllowHurtVillagers();
    void setAllowHurtVillagers(EntityFilter allowHurtVillagers);

    EntityFilter getAllowHurtPets();
    void setAllowHurtPets(EntityFilter allowHurtPets);

    EntityFilter getAllowHurtPassiveMobs();
    void setAllowHurtPassiveMobs(EntityFilter allowHurtPassiveMobs);

    EntityFilter getAllowHurtHostileMobs();
    void setAllowHurtHostileMobs(EntityFilter allowHurtHostileMobs);

    PlayerFilter getAllowBreeding();
    void setAllowBreeding(PlayerFilter allowBreeding);

    PlayerFilter getAllowTaming();
    void setAllowTaming(PlayerFilter allowTaming);

    PlayerFilter getAllowLeashing();
    void setAllowLeashing(PlayerFilter allowLeashing);

    PlayerFilter getAllowMilking();
    void setAllowMilking(PlayerFilter allowMilking);

    EntityFilter getAllowSplashPotions();
    void setAllowSplashPotions(EntityFilter allowSplashPotions);

    EntityFilter getAllowProjectiles();
    void setAllowProjectiles(EntityFilter allowProjectiles);

    PlayerFilter getAllowEnderPearls();
    void setAllowEnderPearls(PlayerFilter allowEnderPearls);

    default Map<PlayerFilterField, PlayerFilter> getPlayerFilters() {
        return getFiltersMap(PlayerFilterField.class);
    }

    default Map<EntityFilterField, EntityFilter> getEntityFilters() {
        return getFiltersMap(EntityFilterField.class);
    }

    private <Field extends Enum<Field> & IFilterField<?, Filter>, Filter extends Enum<Filter> & IFilter<Filter>>
    Map<Field, Filter> getFiltersMap(final Class<Field> fieldClass) {
        
        final Field[] fieldValues = fieldClass.getEnumConstants();

        class FiltersMapEntry implements Map.Entry<Field, Filter> {
            final Field key;

            FiltersMapEntry(Field key) {
                this.key = key;
            }

            @Override
            public Field getKey() {
                return key;
            }

            @Override
            public Filter getValue() {
                return key.get(IFilters.this);
            }

            @Override
            public Filter setValue(Filter value) {
                var prevValue = getValue();
                key.set(IFilters.this, value);
                return prevValue;
            }

            @Override
            public int hashCode() {
                return key.hashCode() ^ getValue().hashCode();
            }
            
            @Override
            public String toString() {
                return key + "=" + getValue();
            }
        }

        class FiltersMap extends AbstractMap<Field, Filter> {
            @Override
            public int size() {
                return fieldValues.length;
            }

            transient Set<Entry<Field, Filter>> entrySet;

            @Override
            public Set<Entry<Field, Filter>> entrySet() {
                var es = entrySet;
                return es == null? entrySet = new EntrySet() : es;
            }

            class EntrySet extends AbstractSet<Entry<Field, Filter>> implements SequencedSet<Entry<Field, Filter>> {
                final List<? extends FiltersMapEntry> entries;
                
                EntrySet() {
                    final int length = fieldValues.length;
                    var entries = new FiltersMapEntry[length];
                    for (int i = 0; i < length; i++) {
                        entries[i] = new FiltersMapEntry(fieldValues[i]);
                    }
                    this.entries = Arrays.asList(entries);
                }

                EntrySet(List<? extends FiltersMapEntry> entries) {
                    this.entries = entries;
                }

                @Override
                public int size() {
                    return entries.size();
                }

                @Override
                public Iterator<Entry<Field, Filter>> iterator() {
                    return (Iterator<Entry<Field, Filter>>) entries.iterator();
                }

                @Override
                public Spliterator<Entry<Field, Filter>> spliterator() {
                    return (Spliterator<Entry<Field, Filter>>) entries.spliterator();
                }

                @Override
                public Stream<Entry<Field, Filter>> stream() {
                    return (Stream<Entry<Field, Filter>>) entries.stream();
                }

                @Override
                public Stream<Entry<Field, Filter>> parallelStream() {
                    return (Stream<Entry<Field, Filter>>) entries.parallelStream();
                }

                @Override
                public boolean contains(Object o) {
                    if (o instanceof Map.Entry<?, ?> entry && fieldClass.isInstance(entry.getKey())) {
                        return ((Field)entry.getKey()).get(IFilters.this) == entry.getValue();
                    }
                    return false;
                }

                @Override
                public String toString() {
                    return entries.toString();
                }

                @Override
                public Entry<Field, Filter> getFirst() {
                    return entries.getFirst();
                }

                @Override
                public Entry<Field, Filter> getLast() {
                    return entries.getLast();
                }

                transient SequencedSet<Entry<Field, Filter>> descendingSet;

                @Override
                public SequencedSet<Entry<Field, Filter>> reversed() {
                    var ds = descendingSet;
                    if (ds != null) return ds;
                    return descendingSet = new EntrySet(entries.reversed());
                }
            }
        
            transient Set<Field> keySet;

            @Override
            public Set<Field> keySet() {
                var ks = keySet;
                return ks == null? keySet = Collections.unmodifiableSet(EnumSet.allOf(fieldClass)) : ks;
            }
        }

        return new FiltersMap();
    }
}