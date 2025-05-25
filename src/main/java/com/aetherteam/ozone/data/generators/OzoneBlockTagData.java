package com.aetherteam.ozone.data.generators;

import java.util.concurrent.CompletableFuture;

import com.aetherteam.ozone.Ozone;
import com.aetherteam.ozone.tags.OzoneBlockTags;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

public class OzoneBlockTagData extends BlockTagsProvider {
    public OzoneBlockTagData(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, Ozone.MODID);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(OzoneBlockTags.DOORS)
            .addTags(BlockTags.DOORS, BlockTags.TRAPDOORS, BlockTags.FENCE_GATES, Tags.Blocks.FENCE_GATES);
        tag(OzoneBlockTags.CONTAINERS)
            .addTags(Tags.Blocks.CHESTS, Tags.Blocks.BARRELS, Tags.Blocks.PLAYER_WORKSTATIONS_FURNACES, Tags.Blocks.BOOKSHELVES)
            .add(Blocks.DISPENSER, Blocks.DROPPER, Blocks.DECORATED_POT, Blocks.HOPPER)
            .addOptional(ResourceLocation.fromNamespaceAndPath("aether", "skyroot_chest"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("aether", "altar"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("aether", "freezer"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("aether", "incubator"))
            .addOptional(ResourceLocation.fromNamespaceAndPath("aetherii", "skyroot_chest"));
        tag(OzoneBlockTags.REDSTONE_ACTIVATORS)
            .addTags(BlockTags.BUTTONS, Tags.Blocks.CHESTS_TRAPPED)
            .add(Blocks.LEVER, Blocks.TARGET);
        tag(OzoneBlockTags.OTHER)
            .add(Blocks.FLOWER_POT, Blocks.COMPOSTER);
    }
}
