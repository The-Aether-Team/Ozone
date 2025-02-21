package com.aetherteam.ozone.data.generators.models;

import com.aetherteam.ozone.Ozone;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelOutput;
import net.minecraft.client.data.models.blockstates.BlockStateGenerator;
import net.minecraft.client.data.models.model.ModelInstance;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class OzoneBlockModels extends BlockModelGenerators {
    public OzoneBlockModels(Consumer<BlockStateGenerator> blockStateOutput, ItemModelOutput itemModelOutput, BiConsumer<ResourceLocation, ModelInstance> modelOutput) {
        super(blockStateOutput, itemModelOutput, modelOutput);
    }

    @Override
    public void run() {
        this.createSurveyorTable();
    }

    public void createSurveyorTable() {
        TextureMapping textureMapping = new TextureMapping()
                .put(TextureSlot.PARTICLE, TextureMapping.getBlockTexture(Ozone.SURVEYOR_TABLE.get(), "_side3"))
                .put(TextureSlot.DOWN, TextureMapping.getBlockTexture(Blocks.PALE_OAK_PLANKS))
                .put(TextureSlot.UP, TextureMapping.getBlockTexture(Ozone.SURVEYOR_TABLE.get(), "_top"))
                .put(TextureSlot.NORTH, TextureMapping.getBlockTexture(Ozone.SURVEYOR_TABLE.get(), "_side3"))
                .put(TextureSlot.EAST, TextureMapping.getBlockTexture(Ozone.SURVEYOR_TABLE.get(), "_side3"))
                .put(TextureSlot.SOUTH, TextureMapping.getBlockTexture(Ozone.SURVEYOR_TABLE.get(), "_side1"))
                .put(TextureSlot.WEST, TextureMapping.getBlockTexture(Ozone.SURVEYOR_TABLE.get(), "_side2"));
        this.blockStateOutput.accept(createSimpleBlock(Ozone.SURVEYOR_TABLE.get(), ModelTemplates.CUBE.create(Ozone.SURVEYOR_TABLE.get(), textureMapping, this.modelOutput)));
    }
}
