package com.aetherteam.ozone.attachment;

import com.aetherteam.ozone.network.packet.clientbound.HomeSuggestionPacket;
import com.aetherteam.ozone.network.packet.clientbound.WarpSuggestionPacket;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

public class OzonePlayerAttachment {
    private Optional<GlobalPos> previousPosition;
    private Map<String, GlobalPos> homes;
    private List<String> homeCommandSuggestions;

    public static final Codec<OzonePlayerAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GlobalPos.CODEC.optionalFieldOf("previous_position").forGetter(OzonePlayerAttachment::getPreviousPosition),
            Codec.unboundedMap(Codec.STRING, GlobalPos.CODEC).fieldOf("homes").forGetter(OzonePlayerAttachment::getHomes)
    ).apply(instance, OzonePlayerAttachment::new));

    private boolean shouldSyncAfterJoin;

    public OzonePlayerAttachment() {
        this.previousPosition = Optional.empty();
        this.homes = new HashMap<>();
        this.homeCommandSuggestions = new ArrayList<>();
    }

    public OzonePlayerAttachment(Optional<GlobalPos> previousPosition, Map<String, GlobalPos> homes) {
        this.previousPosition = previousPosition;
        this.homes = new HashMap<>(homes);
        this.homeCommandSuggestions = new ArrayList<>(this.homes.keySet().stream().toList());
    }

    public void login(Player player) {
        this.shouldSyncAfterJoin = true;
    }

    public void changeDimension(Player player) {
        this.shouldSyncAfterJoin = true;
    }

    public void postTickUpdate(Player player) {
        this.syncAfterJoin(player);
    }

    private void syncAfterJoin(Player player) {
        if (this.shouldSyncAfterJoin) {
            if (player instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(serverPlayer, new HomeSuggestionPacket(this.homeCommandSuggestions));
                if (serverPlayer.getServer() != null) {
                    ServerLevel overworld = OzoneLevelAttachment.getOverworld(serverPlayer.getServer());
                    if (overworld != null) {
                        PacketDistributor.sendToPlayer(serverPlayer, new WarpSuggestionPacket(overworld.getData(OzoneDataAttachments.LEVEL).getWarpCommandSuggestions()));
                    }
                }
                
            }
            this.shouldSyncAfterJoin = false;
        }
    }

    public Optional<GlobalPos> getPreviousPosition() {
        return this.previousPosition;
    }

    public void setPreviousPosition(GlobalPos previousPosition) {
        this.previousPosition = Optional.of(previousPosition);
    }

    public void clearPreviousPosition() {
        this.previousPosition = Optional.empty();
    }

    public Map<String, GlobalPos> getHomes() {
        return this.homes;
    }

    public void setHomes(ServerPlayer serverPlayer, Map<String, GlobalPos> warps) {
        this.homes = warps;
        this.homeCommandSuggestions = warps.keySet().stream().toList();
        PacketDistributor.sendToPlayer(serverPlayer, new HomeSuggestionPacket(this.homeCommandSuggestions));
    }

    public void addHome(ServerPlayer serverPlayer, String title, GlobalPos warp) {
        this.homes.put(title, warp);
        this.homeCommandSuggestions.add(title);
        PacketDistributor.sendToPlayer(serverPlayer, new HomeSuggestionPacket(this.homeCommandSuggestions));
    }

    public void removeHome(ServerPlayer serverPlayer, String title) {
        this.homes.remove(title);
        this.homeCommandSuggestions.remove(title);
        PacketDistributor.sendToPlayer(serverPlayer, new HomeSuggestionPacket(this.homeCommandSuggestions));
    }

    public List<String> getHomeCommandSuggestions() {
        return this.homeCommandSuggestions;
    }

    public void setHomeCommandSuggestions(List<String> homeCommandSuggestions) {
        this.homeCommandSuggestions = homeCommandSuggestions;
    }
}
