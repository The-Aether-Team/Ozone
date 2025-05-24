package com.aetherteam.ozone.attachment;

import com.aetherteam.ozone.network.packet.clientbound.WarpSuggestionPacket;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class OzoneLevelAttachment {
    private GlobalPos serverSpawn;
    private Map<String, GlobalPos> warps;
    private final HashSet<String> warpCommandSuggestions;
    private final Set<TeleportRequest> tpaQueue = new HashSet<>();

    public static final Codec<OzoneLevelAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GlobalPos.CODEC.optionalFieldOf("server_spawn").forGetter(OzoneLevelAttachment::getServerSpawn),
            Codec.unboundedMap(Codec.STRING, GlobalPos.CODEC).fieldOf("warps").forGetter(OzoneLevelAttachment::getWarps)
    ).apply(instance, OzoneLevelAttachment::new));

    public OzoneLevelAttachment() {
        this.serverSpawn = null;
        this.warps = new HashMap<>();
        this.warpCommandSuggestions = new HashSet<>();
    }

    public OzoneLevelAttachment(Optional<GlobalPos> serverSpawn, Map<String, GlobalPos> warps) {
        this.serverSpawn = serverSpawn.orElse(null);
        this.warps = new HashMap<>(warps);
        this.warpCommandSuggestions = new HashSet<>(this.warps.keySet());
    }

    public Optional<GlobalPos> getServerSpawn() {
        return Optional.ofNullable(this.serverSpawn);
    }

    public void setServerSpawn(GlobalPos serverSpawn) {
        this.serverSpawn = serverSpawn;
    }

    public void clearServerSpawn() {
        this.serverSpawn = null;
    }

    public Map<String, GlobalPos> getWarps() {
        return this.warps;
    }

    public void setWarps(Map<String, GlobalPos> warps) {
        this.warps = Validate.notNull(warps, "warps was null");
        this.warpCommandSuggestions.clear();
        if (this.warpCommandSuggestions.addAll(warps.keySet())) {
            PacketDistributor.sendToAllPlayers(new WarpSuggestionPacket(this.warpCommandSuggestions));
        }
    }

    public void addWarp(String title, GlobalPos warp) {
        this.warps.put(title, warp);
        if (this.warpCommandSuggestions.add(title)) {
            PacketDistributor.sendToAllPlayers(new WarpSuggestionPacket(this.warpCommandSuggestions));
        }
    }

    public void removeWarp(String title) {
        this.warps.remove(title);
        if (this.warpCommandSuggestions.remove(title)) {
            PacketDistributor.sendToAllPlayers(new WarpSuggestionPacket(this.warpCommandSuggestions));
        }
    }

    public Set<String> getWarpCommandSuggestions() {
        return this.warpCommandSuggestions;
    }

    public void setWarpCommandSuggestions(Collection<String> warpCommandSuggestions) {
        Validate.notNull(warpCommandSuggestions, "warpCommandSuggestions was null");
        if (warpCommandSuggestions != this.warpCommandSuggestions) {
            this.warpCommandSuggestions.clear();
            this.warpCommandSuggestions.addAll(warpCommandSuggestions);
        }
    }

    public Set<TeleportRequest> getTpaQueue() {
        return this.tpaQueue;
    }

    public void addTpaRequest(TeleportRequest request) {
        this.tpaQueue.add(request);
    }

    @Nullable
    public static ServerLevel getOverworld(MinecraftServer server) {
        return server.getLevel(Level.OVERWORLD);
    }

    public record TeleportRequest(UUID teleportSubject, UUID teleportTarget) { }
}
