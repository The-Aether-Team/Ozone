package com.aetherteam.ozone.data.generators;

import com.aetherteam.nitrogen.data.providers.NitrogenLanguageProvider;
import com.aetherteam.ozone.Ozone;
import com.aetherteam.ozone.block.OzoneBlocks;
import com.aetherteam.ozone.item.OzoneItems;
import net.minecraft.data.PackOutput;
public class OzoneLanguageData extends NitrogenLanguageProvider {
    public OzoneLanguageData(PackOutput output) {
        super(output, Ozone.MODID);
    }

    @Override
    protected void addTranslations() {
        this.addBlock(OzoneBlocks.SURVEYOR_TABLE, "Surveyor Table");
        this.addItem(OzoneItems.CONTAINER_KEY, "Container Key");

        this.addCommand("back.error", "No valid previous position");
        this.addCommand("tpa.request", "Requested teleportation to %s");
//        this.addCommand("tpa.here", "%s requested you teleport to them");
        this.addCommand("tpa.cancel", "Cancelled teleportation to %s");
        this.addCommand("tpa.cancel.error", "Failed to cancel teleport request");
        this.addCommand("tpa.accept.error", "Failed to cancel teleport request");
        this.addCommand("rtp", "Teleported %s to random location");
        this.addCommand("spawn", "Teleported %s to the server spawn warp");
        this.addCommand("spawn.set", "Set the server spawn warp to %s, %s, %s");
        this.addCommand("spawn.delete", "Deleted the server spawn warp");
        this.addCommand("home", "Teleported %s to home \"%s\"");
        this.addCommand("home.set", "Created home \"%s\" at %s, %s, %s");
        this.addCommand("home.delete", "Deleted home \"%s\"");
        this.addCommand("warp", "Teleported %s to warp \"%s\"");
        this.addCommand("warp.set", "Created warp \"%s\" at %s, %s, %s");
        this.addCommand("warp.delete", "Deleted warp \"%s\"");

        this.addPackDescription("mod", "Ozone Resources");
    }
}
