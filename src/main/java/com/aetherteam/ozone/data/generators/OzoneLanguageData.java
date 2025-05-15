package com.aetherteam.ozone.data.generators;

import com.aetherteam.nitrogen.data.providers.NitrogenLanguageProvider;
import com.aetherteam.ozone.Ozone;
import net.minecraft.data.PackOutput;
public class OzoneLanguageData extends NitrogenLanguageProvider {
    public OzoneLanguageData(PackOutput output) {
        super(output, Ozone.MODID);
    }

    @Override
    protected void addTranslations() {
        this.addBlock(Ozone.SURVEYOR_TABLE, "Surveyor Table");
        this.addItem(Ozone.CONTAINER_KEY, "Container Key");

        this.addCommand("back.error", "No valid previous position");
        this.addCommand("teleport.request", "Requested teleportation to %s");
        this.addCommand("teleport.cancel", "Cancelled teleportation to %s");
        this.addCommand("teleport.cancel.error", "Failed to cancel teleport request");
        this.addCommand("teleport.accept.error", "Failed to cancel teleport request");
        this.addCommand("rtp", "Teleported %s to random location");
        this.addCommand("spawn", "Teleported %s to the server spawn warp");
        this.addCommand("spawn.set", "Set the server spawn warp to %s, %s, %s");
        this.addCommand("spawn.delete", "Deleted the server spawn warp");

        this.addPackDescription("mod", "Ozone Resources");
    }
}
