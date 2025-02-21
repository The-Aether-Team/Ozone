package com.aetherteam.ozone.data.generators.models;

import com.aetherteam.ozone.Ozone;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ItemModelOutput;
import net.minecraft.client.data.models.model.ModelInstance;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BiConsumer;

public class OzoneItemModels extends ItemModelGenerators {
    public OzoneItemModels(ItemModelOutput itemModelOutput, BiConsumer<ResourceLocation, ModelInstance> modelOutput) {
        super(itemModelOutput, modelOutput);
    }

    @Override
    public void run() {
        this.generateFlatItem(Ozone.CONTAINER_KEY.get(), ModelTemplates.FLAT_ITEM);
    }
}
