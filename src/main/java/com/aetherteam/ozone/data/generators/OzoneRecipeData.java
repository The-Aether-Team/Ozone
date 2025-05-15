package com.aetherteam.ozone.data.generators;

import com.aetherteam.ozone.Ozone;
import com.aetherteam.ozone.block.OzoneBlocks;
import com.aetherteam.ozone.item.OzoneItems;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.Tags;

import java.util.concurrent.CompletableFuture;

public class OzoneRecipeData  extends RecipeProvider {
    protected final String id;
    private final HolderGetter<Item> getter;

    public OzoneRecipeData(HolderLookup.Provider provider, RecipeOutput output) {
        super(provider, output);
        this.id = Ozone.MODID;
        this.getter = provider.lookupOrThrow(Registries.ITEM);
    }

    @Override
    protected void buildRecipes() {
        this.shaped(RecipeCategory.DECORATIONS, OzoneBlocks.SURVEYOR_TABLE)
                .define('#', ItemTags.PLANKS)
                .define('@', Tags.Items.INGOTS_COPPER)
                .pattern("@@")
                .pattern("##")
                .pattern("##")
                .unlockedBy("has_copper_ingot", this.has(Tags.Items.INGOTS_COPPER))
                .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, OzoneItems.CONTAINER_KEY)
                .define('@', Tags.Items.INGOTS_COPPER)
                .define('#', Tags.Items.NUGGETS_IRON)
                .pattern("@")
                .pattern("#")
                .unlockedBy("has_copper_ingot", this.has(Tags.Items.INGOTS_COPPER))
                .save(this.output);
    }

    public static class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> completableFuture) {
            super(packOutput, completableFuture);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider provider, RecipeOutput output) {
            return new OzoneRecipeData(provider, output);
        }

        public String getName() {
            return "Ozone Recipes";
        }
    }
}
