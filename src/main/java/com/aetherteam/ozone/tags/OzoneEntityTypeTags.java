package com.aetherteam.ozone.tags;

import com.aetherteam.ozone.Ozone;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public final class OzoneEntityTypeTags {
    public static final TagKey<EntityType<?>> VILLAGERS = create("category/villagers");
    public static final TagKey<EntityType<?>> PETS = create("category/pets");
    public static final TagKey<EntityType<?>> PASSIVE_MOBS = create("category/passive");
    public static final TagKey<EntityType<?>> HOSTILE_MOBS = create("category/hostile");

    private OzoneEntityTypeTags() {}

    private static TagKey<EntityType<?>> create(String name) {

        return TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Ozone.MODID, name));
    }
}
