package com.aetherteam.ozone.data.generators;

import static com.aetherteam.ozone.Locator.aether;
import static com.aetherteam.ozone.Locator.aether_ii;

import java.util.concurrent.CompletableFuture;

import com.aetherteam.ozone.Ozone;
import com.aetherteam.ozone.tags.OzoneBlockTags;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
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
            .addTags(
                BlockTags.DOORS,
                BlockTags.TRAPDOORS,
                BlockTags.FENCE_GATES,
                Tags.Blocks.FENCE_GATES
            );
        tag(OzoneBlockTags.CONTAINERS)
            .addTags(
                Tags.Blocks.CHESTS,
                Tags.Blocks.BARRELS,
                Tags.Blocks.PLAYER_WORKSTATIONS_FURNACES
            )
            .add(
                Blocks.DISPENSER,
                Blocks.DROPPER,
                Blocks.DECORATED_POT,
                Blocks.HOPPER,
                Blocks.BLAST_FURNACE,
                Blocks.SMOKER,
                Blocks.LECTERN,
                Blocks.CHISELED_BOOKSHELF
            )
            .addOptional(aether("skyroot_chest"))
            .addOptional(aether("altar"))
            .addOptional(aether("freezer"))
            .addOptional(aether("incubator"))
            .addOptional(aether("holystone_furnace"))
            .addOptional(aether("treasure_chest"))
            .addOptional(aether("chest_mimic"))
            .addOptional(aether_ii("skyroot_chest"))
            .addOptional(aether_ii("altar"))
            .addOptional(aether_ii("holystone_furnace"))
            .addOptional(aether_ii("arkenium_forge"));
        tag(OzoneBlockTags.WORKBENCHES)
            .addTags(Tags.Blocks.PLAYER_WORKSTATIONS_CRAFTING_TABLES)
            .add(
                Blocks.FLETCHING_TABLE,
                Blocks.SMITHING_TABLE,
                Blocks.STONECUTTER,
                Blocks.GRINDSTONE,
                Blocks.ENCHANTING_TABLE,
                Blocks.LOOM
            )
            .addOptional(aether("skyroot_crafting_table"))
            .addOptional(aether_ii("skyroot_crafting_table"))
            .addOptional(aether_ii("artisans_bench"));
        tag(OzoneBlockTags.SPECIAL_INTERACTABLES)
            .addTag(BlockTags.ANVIL)
            .add(Blocks.CRAFTER)
            .addOptional(aether("sun_altar"));
        tag(OzoneBlockTags.CONFIGURABLE)
            .add(
                Blocks.REPEATER,
                Blocks.COMPARATOR,
                Blocks.DAYLIGHT_DETECTOR,
                Blocks.REDSTONE_WIRE,
                Blocks.COPPER_BULB,
                Blocks.NOTE_BLOCK,
                Blocks.PUMPKIN
            );
        tag(OzoneBlockTags.REDSTONE)
            .addTags(BlockTags.BUTTONS, Tags.Blocks.CHESTS_TRAPPED)
            .add(Blocks.LEVER, Blocks.TARGET);
        tag(OzoneBlockTags.PLANTS)
            .addTags(BlockTags.CROPS, BlockTags.CAVE_VINES)
            .add(
                Blocks.VINE,
                Blocks.TWISTING_VINES,
                Blocks.TWISTING_VINES_PLANT,
                Blocks.WEEPING_VINES,
                Blocks.WEEPING_VINES_PLANT,
                Blocks.SWEET_BERRY_BUSH
            )
            .addOptional(aether_ii("orange_tree"))
            .addOptional(aether_ii("brettl_plant"))
            .addOptional(aether_ii("brettl_plant_tip"));
        tag(OzoneBlockTags.OTHER)
            .addTag(BlockTags.BEDS)
            .add(
                Blocks.FLOWER_POT,
                Blocks.COMPOSTER,
                Blocks.JUKEBOX,
                Blocks.SUSPICIOUS_SAND,
                Blocks.SUSPICIOUS_GRAVEL
            );
    }
}
