package com.aetherteam.ozone.data;

import com.aetherteam.ozone.data.generators.OzoneBlockTagData;
import com.aetherteam.ozone.data.generators.OzoneEntityTypeTagData;
import com.aetherteam.ozone.data.generators.OzoneLanguageData;
import com.aetherteam.ozone.data.generators.OzoneLootTableData;
import com.aetherteam.ozone.data.generators.OzoneModelData;
import com.aetherteam.ozone.data.generators.OzoneRecipeData;
import net.minecraft.DetectedVersion;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.metadata.PackMetadataGenerator;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.util.InclusiveRange;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class OzoneData {
    public static void data(GatherDataEvent.Client event) {
        DataGenerator generator = event.getGenerator();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        PackOutput packOutput = generator.getPackOutput();

        // Client Data
        generator.addProvider(true, new OzoneModelData(packOutput));
        generator.addProvider(true, new OzoneLanguageData(packOutput));

        // Server Data
        generator.addProvider(true, new OzoneRecipeData.Runner(packOutput, lookupProvider));
        generator.addProvider(true, OzoneLootTableData.create(packOutput, lookupProvider));

        // Tags
        generator.addProvider(true, new OzoneBlockTagData(packOutput, lookupProvider));
        generator.addProvider(true, new OzoneEntityTypeTagData(packOutput, lookupProvider));

        // pack.mcmeta
        generator.addProvider(true, new PackMetadataGenerator(packOutput).add(PackMetadataSection.TYPE, new PackMetadataSection(
                Component.translatable("pack.ozone_utilities.mod.description"),
                DetectedVersion.BUILT_IN.getPackVersion(PackType.CLIENT_RESOURCES),
                Optional.of(new InclusiveRange<>(0, Integer.MAX_VALUE)))));
    }
}
