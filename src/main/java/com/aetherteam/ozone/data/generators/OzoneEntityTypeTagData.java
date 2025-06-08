package com.aetherteam.ozone.data.generators;

import java.util.concurrent.CompletableFuture;

import com.aetherteam.ozone.Ozone;
import com.aetherteam.ozone.tags.OzoneEntityTypeTags;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;

public class OzoneEntityTypeTagData extends EntityTypeTagsProvider {
    public OzoneEntityTypeTagData(PackOutput output, CompletableFuture<HolderLookup.Provider> provider) {
        super(output, provider, Ozone.MODID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(OzoneEntityTypeTags.VILLAGERS)
            .add(EntityType.VILLAGER);
        tag(OzoneEntityTypeTags.PETS)
            .add(
                EntityType.WOLF,
                EntityType.PARROT,
                EntityType.HORSE,
                EntityType.DONKEY,
                EntityType.ZOMBIE_HORSE,
                EntityType.SKELETON_HORSE,
                EntityType.MULE,
                EntityType.CAT,
                EntityType.AXOLOTL,
                EntityType.TROPICAL_FISH,
                EntityType.ALLAY,
                EntityType.TRADER_LLAMA,
                EntityType.LLAMA
            );
        tag(OzoneEntityTypeTags.PASSIVE_MOBS)
            .add(
                EntityType.COW,
                EntityType.MOOSHROOM,
                EntityType.GOAT,
                EntityType.SHEEP,
                EntityType.CHICKEN,
                EntityType.PIG,
                EntityType.SALMON,
                EntityType.COD,
                EntityType.BAT,
                EntityType.BEE,
                EntityType.ARMADILLO
            );
        tag(OzoneEntityTypeTags.HOSTILE_MOBS)
            .add(
                EntityType.PIGLIN_BRUTE,
                EntityType.BLAZE,
                EntityType.GHAST,
                EntityType.WITHER,
                EntityType.ENDERMAN,
                EntityType.ENDERMITE,
                EntityType.SILVERFISH,
                EntityType.ENDER_DRAGON,
                EntityType.GUARDIAN,
                EntityType.ELDER_GUARDIAN
            )
            .addTags(
                EntityTypeTags.ILLAGER,
                EntityTypeTags.ILLAGER_FRIENDS,
                EntityTypeTags.RAIDERS,
                EntityTypeTags.UNDEAD,
                EntityTypeTags.ARTHROPOD
            );
    }
}
