package com.aetherteam.ozone.data.generators;

import com.aetherteam.ozone.Ozone;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class OzoneLanguageData extends LanguageProvider {
    protected final String id;

    public OzoneLanguageData(PackOutput output) {
        super(output, Ozone.MODID, "en_us");
        this.id = Ozone.MODID;
    }

    @Override
    protected void addTranslations() {
        this.addBlock(Ozone.SURVEYOR_TABLE, "Surveyor Table");
        this.addItem(Ozone.CONTAINER_KEY, "Container Key");

        this.addPackDescription("mod", "Ozone Resources");
    }

    protected void addPackDescription(String packName, String description) {
        this.add("pack." + this.id + "." + packName + ".description", description);
    }
}
