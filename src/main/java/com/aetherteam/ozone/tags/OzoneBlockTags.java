package com.aetherteam.ozone.tags;

import com.aetherteam.ozone.Ozone;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class OzoneBlockTags {
    public static final TagKey<Block> DOORS = create("category/doors");
    public static final TagKey<Block> REDSTONE = create("category/redstone");
    public static final TagKey<Block> CONTAINERS = create("category/containers");
    public static final TagKey<Block> REDSTONE_ACTIVATORS = create("category/redstone_activators");
    public static final TagKey<Block> OTHER = create("category/other");

    private OzoneBlockTags() {}

    private static TagKey<Block> create(String name) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Ozone.MODID, name));
    }
}
