package com.aetherteam.ozone.attachment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import com.aetherteam.ozone.network.packet.clientbound.HomeSuggestionPacket;
import com.aetherteam.ozone.network.packet.clientbound.WarpSuggestionPacket;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

public class OzonePlayerAttachment {
    @Nullable
    private GlobalPos previousPosition;
    private final HashMap<String, GlobalPos> homes;
    private final ArrayList<String> homeCommandSuggestions;

    public static final Codec<OzonePlayerAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GlobalPos.CODEC.optionalFieldOf("previous_position").forGetter(OzonePlayerAttachment::getPreviousPosition),
            Codec.unboundedMap(Codec.STRING, GlobalPos.CODEC).fieldOf("homes").forGetter(OzonePlayerAttachment::getHomes)
    ).apply(instance, OzonePlayerAttachment::new));

    private boolean shouldSyncAfterJoin;

    public OzonePlayerAttachment() {
        this.previousPosition = null;
        this.homes = new HashMap<>();
        this.homeCommandSuggestions = new ArrayList<>();
    }

    public OzonePlayerAttachment(Optional<GlobalPos> previousPosition, Map<String, GlobalPos> homes) {
        this.previousPosition = previousPosition.orElse(null);
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
                    ServerLevel overworld = serverPlayer.getServer().overworld();
                    if (overworld != null) {
                        PacketDistributor.sendToPlayer(serverPlayer, new WarpSuggestionPacket(overworld.getData(OzoneDataAttachments.LEVEL).getWarpCommandSuggestions()));
                    }
                }
                
            }
            this.shouldSyncAfterJoin = false;
        }
    }

    public Optional<GlobalPos> getPreviousPosition() {
        return Optional.ofNullable(previousPosition);
    }

    public void setPreviousPosition(GlobalPos previousPosition) {
        this.previousPosition = previousPosition;
    }

    public void clearPreviousPosition() {
        this.previousPosition = null;
    }

    public Map<String, GlobalPos> getHomes() {
        return this.homes;
    }

    public void setHomes(ServerPlayer serverPlayer, Map<String, GlobalPos> homes) {
        if (this.homes != homes) {
            this.homes.clear();
            this.homes.putAll(homes);
            this.homeCommandSuggestions.clear();
            this.homeCommandSuggestions.addAll(this.homes.keySet());
            PacketDistributor.sendToPlayer(serverPlayer, new HomeSuggestionPacket(this.homeCommandSuggestions));
        }
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
        if (this.homeCommandSuggestions != homeCommandSuggestions) {
            this.homeCommandSuggestions.clear();
            this.homeCommandSuggestions.addAll(homeCommandSuggestions);
        }
    }
}
