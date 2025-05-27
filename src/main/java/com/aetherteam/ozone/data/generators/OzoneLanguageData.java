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
        addBlock(OzoneBlocks.SURVEYOR_TABLE, "Surveyor Table");
        addItem(OzoneItems.CONTAINER_KEY, "Container Key");

        addCommand("back", "Returned to previous position");
        addCommand("back.error", "No valid previous position");


        addCommand("tpa.prompt", ". %s / %s");
        addCommand("tpa.prompt.accept", "[Accept]");
        addCommand("tpa.prompt.deny", "[Deny]");

        addCommand("tpa.error.self", "You cannot request to teleport to yourself");
        addCommand("tpa.error.exists", "Request already exists");

        addCommand("tpa.request.source", "Requested teleportation to %s");
        addCommand("tpa.request.target", "%s requested to teleport to you");

        addCommand("tpa.here.source", "Requested %s teleport to you");
        addCommand("tpa.here.target", "%s requested you teleport to them");

        addCommand("tpa.accept.source", "Teleported to %s");
        addCommand("tpa.accept.target", "%s teleported to you");
        addCommand("tpa.accept.error", "No requests found");
        addCommand("tpa.accept.error.not_found", "Could not find target or source");

        addCommand("tpa.deny", "Denied teleportation request");
        addCommand("tpa.deny.source", "Denied teleport request from %s");
        addCommand("tpa.deny.target", "%s denied your teleport request");
        addCommand("tpa.deny.error", "No incoming teleport requests found");

        addCommand("tpa.cancel", "Cancelled teleportation request");
        addCommand("tpa.cancel.source", "Teleportation to %s cancelled");
        addCommand("tpa.cancel.target", "%s cancelled teleportation to you");
        addCommand("tpa.cancel.error", "No outgoing teleport requests found");


        addCommand("rtp", "Teleported to random location");
        addCommand("rtp.search", "Searching for random location");

        addCommand("spawn", "Teleported %s to the server spawn");
        addCommand("spawn.set", "Set the server spawn to %s, %s, %s in %s");
        addCommand("spawn.delete", "Deleted the server spawn");
        addCommand("spawn.error", "Failed to teleport to server spawn");
        addCommand("spawn.error.doesnt_exist", "No server spawn exists");
        addCommand("spawn.set.error", "Failed to set server spawn");
        addCommand("spawn.delete.error", "Failed to delete server spawn");

        addCommand("home", "Teleported %s to home \"%s\"");
        addCommand("home.set", "Created home \"%s\" at %s, %s, %s in %s");
        addCommand("home.delete", "Deleted home \"%s\"");
        addCommand("home.error", "Failed to teleport to home");
        addCommand("home.error.doesnt_exist", "Home doesn't exist");
        addCommand("home.error.already_exists", "Home already exists");
        addCommand("home.set.error", "Failed to set home");
        addCommand("home.delete.error", "Failed to delete home");

        addCommand("warp", "Teleported %s to warp \"%s\"");
        addCommand("warp.set", "Created warp \"%s\" at %s, %s, %s in %s");
        addCommand("warp.delete", "Deleted warp \"%s\"");
        addCommand("warp.error", "Failed to teleport to warp");
        addCommand("warp.error.doesnt_exist", "Warp doesn't exist");
        addCommand("warp.error.already_exists", "Warp already exists");
        addCommand("warp.set.error", "Failed to set warp");
        addCommand("warp.delete.error", "Failed to delete warp");

        addCommand("hat", "Equipped %s %s as a hat");
        addCommand("hat.error", "Hand is empty");

        addCommand("claim.error.noClaim", "There is no claim at that location");
        addCommand("claim.error.alreadyOwner", "Claim owner was not changed");
        addCommand("claim.info.location", "Info for claim at chunk %s in %s:");
        addCommand("claim.info.owner", "Owner: %s");
        addCommand("claim.info.surveyorTableLocation", "Surveyor Table Location: (%d, %d, %d)");
        addCommand("claim.info.surveyorTableLocation.unknown", "Surveyor Table Location: N/A");
        addCommand("claim.info.allowExplosions", "Allow Explosions: %s");
        addCommand("claim.info.allowPlace", "Allow Block Placement: %s");
        addCommand("claim.info.allowBreak", "Allow Block Destruction: %s");
        addCommand("claim.info.allowInteractWithContainers", "Allow Opening Containers: %s");
        addCommand("claim.info.allowInteractWithDoors", "Allow Opening Doors: %s");
        addCommand("claim.info.allowInteractWithRedstone", "Allow Redstone Manipulation: %s");
        addCommand("claim.info.allowInteractWithRedstoneActivators", "Allow Buttons/Levers: %s");
        addCommand("claim.info.allowInteractWithSigns", "Allow Sign Interaction: %s");
        addCommand("claim.info.allowInteractWithOther", "Allow Other Block Interactions: %s");
        addCommand("claim.info.allowDrop", "Allow Item Dropping: %s");
        addCommand("claim.changeOwner.success", "Successfully changed the owner of the claim containing (%d, %d, %d)");
        addCommand("claim.remove.success", "Successfully removed the claim containing (%d, %d, %d)");
        addCommand("claim.filter.success", "Successfully modified claim filter");

        addArgument("entity_filter.no_one", "No one");
        addArgument("entity_filter.owner_only", "Owner only");
        addArgument("entity_filter.owner_and_mobs", "Owner and non-players");
        addArgument("entity_filter.friends", "Friends");
        addArgument("entity_filter.friends_and_mobs", "Friends and non-players");
        addArgument("entity_filter.players", "Players");
        addArgument("entity_filter.mobs", "Non-players");
        addArgument("entity_filter.anyone", "Anyone");
        
        addArgument("entity_filter.invalid", "Invalid entity filter: %s");
        addArgument("player_filter.invalid", "Invalid player filter: %s");

        addGeneric("claim.action_blocked", "This chunk is owned by %s");
        addGeneric("claim.destroyed", "Your chunk claim at (%d, %d, %d) was destroyed!");
        addGeneric("claim.created", "New chunk claim created at (%d, %d, %d)");
        addGeneric("claim.surveyor_table_already_exists", "A claim already exists within this chunk!");
        addGeneric("claim.owner.no_one", "no one");
        addGeneric("claim.owner.someone_else", "someone else");
        addGeneric("claim.limit_reached", "You have reached your claim limit of %s");
        addGeneric("claim.limit.plural", "%s blocks");
        addGeneric("claim.limit.singular", "1 block");

        addCommonConfig("max_chunk_claims", "Max Chunk Claims");

        addPackDescription("mod", "Ozone Resources");
    }

    public void addArgument(String key, String name) {
        add("argument." + this.id + "." + key, name);
    }
}
