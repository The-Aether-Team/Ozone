package com.aetherteam.ozone.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class OzonePlayerAttachment {
    private Optional<BlockPos> previousPosition; //todo mixin the teleport method to assign this value before teleporting. or use the event hook
    private Optional<UUID> otherPlayerRequestingTeleport;
    private Optional<UUID> requestingTeleportToOtherPlayer; //todo check two ways that this matches for the other player and the other player has matching of the above field.
    private Map<String, BlockPos> homes;

    public static final Codec<OzonePlayerAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.optionalFieldOf("previous_position").forGetter(OzonePlayerAttachment::getPreviousPosition),
            UUIDUtil.CODEC.optionalFieldOf("other_player_requesting_teleport").forGetter(OzonePlayerAttachment::getOtherPlayerRequestingTeleport),
            UUIDUtil.CODEC.optionalFieldOf("requesting_teleport_to_other_player").forGetter(OzonePlayerAttachment::getRequestingTeleportToOtherPlayer),
            Codec.unboundedMap(Codec.STRING, BlockPos.CODEC).fieldOf("homes").forGetter(OzonePlayerAttachment::getHomes)
    ).apply(instance, OzonePlayerAttachment::new));

    public OzonePlayerAttachment() {
        this.previousPosition = Optional.empty();
        this.otherPlayerRequestingTeleport = Optional.empty();
        this.requestingTeleportToOtherPlayer = Optional.empty();
        this.homes = new HashMap<>();
    }

    public OzonePlayerAttachment(Optional<BlockPos> previousPosition, Optional<UUID> otherPlayerRequestingTeleport, Optional<UUID> requestingTeleportToOtherPlayer, Map<String, BlockPos> homes) {
        this.previousPosition = previousPosition;
        this.otherPlayerRequestingTeleport = otherPlayerRequestingTeleport;
        this.requestingTeleportToOtherPlayer = requestingTeleportToOtherPlayer;
        this.homes = new HashMap<>(homes);
    }

    public Optional<BlockPos> getPreviousPosition() {
        return this.previousPosition;
    }

    public void setPreviousPosition(BlockPos previousPosition) {
        this.previousPosition = Optional.of(previousPosition);
    }

    public void clearPreviousPosition() {
        this.previousPosition = Optional.empty();
    }

    public Optional<UUID> getOtherPlayerRequestingTeleport() {
        return this.otherPlayerRequestingTeleport;
    }

    public void setOtherPlayerRequestingTeleport(UUID otherPlayerRequestingTeleport) {
        this.otherPlayerRequestingTeleport = Optional.of(otherPlayerRequestingTeleport);
    }

    public Optional<UUID> getRequestingTeleportToOtherPlayer() {
        return this.requestingTeleportToOtherPlayer;
    }

    public void setRequestingTeleportToOtherPlayer(UUID requestingTeleportToOtherPlayer) {
        this.requestingTeleportToOtherPlayer = Optional.of(requestingTeleportToOtherPlayer);
    }

    public Map<String, BlockPos> getHomes() {
        return this.homes;
    }

    public void setHomes(Map<String, BlockPos> warps) {
        this.homes = warps;
    }

    public void addHome(String title, BlockPos warp) {
        this.homes.put(title, warp);
    }
}
