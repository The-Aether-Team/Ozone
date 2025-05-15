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

        this.addCommand("back", "Returned to previous position");
        this.addCommand("back.error", "No valid previous position");

        this.addCommand("tpa.request.source", "Requested teleportation to %s");
        this.addCommand("tpa.request.target", "%s requested to teleport to you");
        this.addCommand("tpa.here.source", "Requested %s teleport to you");
        this.addCommand("tpa.here.target", "%s requested you teleport to them");
        this.addCommand("tpa.accept.source", "Teleported to %s");
        this.addCommand("tpa.accept.target", "%s teleported to you");
        this.addCommand("tpa.accept.error", "Failed to accept teleport request");
        this.addCommand("tpa.deny.source", "Denied %s teleport to you");
        this.addCommand("tpa.deny.target", "%s denied you teleport to them");
        this.addCommand("tpa.deny.error", "Failed to deny teleport request");
        this.addCommand("tpa.cancel.source", "Cancelled teleportation to %s");
        this.addCommand("tpa.cancel.target", "%s cancelled teleportation to you");
        this.addCommand("tpa.cancel.error", "Failed to cancel teleport request");

        this.addCommand("rtp", "Teleported to random location");
        this.addCommand("rtp.search", "Searching for random location");

        this.addCommand("spawn", "Teleported %s to the server spawn");
        this.addCommand("spawn.set", "Set the server spawn to %s, %s, %s in %s");
        this.addCommand("spawn.delete", "Deleted the server spawn");
        this.addCommand("spawn.error", "Failed to teleport to server spawn");
        this.addCommand("spawn.error.doesnt_exist", "No server spawn exists");
        this.addCommand("spawn.set.error", "Failed to set server spawn");
        this.addCommand("spawn.delete.error", "Failed to delete server spawn");

        this.addCommand("home", "Teleported %s to home \"%s\"");
        this.addCommand("home.set", "Created home \"%s\" at %s, %s, %s in %s");
        this.addCommand("home.delete", "Deleted home \"%s\"");
        this.addCommand("home.error", "Failed to teleport to home");
        this.addCommand("home.error.doesnt_exist", "Home doesn't exist");
        this.addCommand("home.error.already_exists", "Home already exists");
        this.addCommand("home.set.error", "Failed to set home");
        this.addCommand("home.delete.error", "Failed to delete home");

        this.addCommand("warp", "Teleported %s to warp \"%s\"");
        this.addCommand("warp.set", "Created warp \"%s\" at %s, %s, %s in %s");
        this.addCommand("warp.delete", "Deleted warp \"%s\"");
        this.addCommand("warp.error", "Failed to teleport to warp");
        this.addCommand("warp.error.doesnt_exist", "Warp doesn't exist");
        this.addCommand("warp.error.already_exists", "Warp already exists");
        this.addCommand("warp.set.error", "Failed to set warp");
        this.addCommand("warp.delete.error", "Failed to delete warp");

        this.addPackDescription("mod", "Ozone Resources");
    }
}
