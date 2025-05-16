package com.aetherteam.ozone.attachment;

import com.aetherteam.ozone.network.packet.clientbound.WarpSuggestionPacket;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class OzoneLevelAttachment {
    private Optional<GlobalPos> serverSpawn;
    private Map<String, GlobalPos> warps;
    private List<String> warpCommandSuggestions;
    private final Set<TeleportRequest> tpaQueue = new HashSet<>();

    public static final Codec<OzoneLevelAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GlobalPos.CODEC.optionalFieldOf("server_spawn").forGetter(OzoneLevelAttachment::getServerSpawn),
            Codec.unboundedMap(Codec.STRING, GlobalPos.CODEC).fieldOf("warps").forGetter(OzoneLevelAttachment::getWarps)
    ).apply(instance, OzoneLevelAttachment::new));

    public OzoneLevelAttachment() {
        this.serverSpawn = Optional.empty();
        this.warps = new HashMap<>();
        this.warpCommandSuggestions = new ArrayList<>();
    }

    public OzoneLevelAttachment(Optional<GlobalPos> serverSpawn, Map<String, GlobalPos> warps) {
        this.serverSpawn = serverSpawn;
        this.warps = new HashMap<>(warps);
        this.warpCommandSuggestions = new ArrayList<>(this.warps.keySet().stream().toList());
    }

    public Optional<GlobalPos> getServerSpawn() {
        return this.serverSpawn;
    }

    public void setServerSpawn(GlobalPos serverSpawn) {
        this.serverSpawn = Optional.of(serverSpawn);
    }

    public void clearServerSpawn() {
        this.serverSpawn = Optional.empty();
    }

    public Map<String, GlobalPos> getWarps() {
        return this.warps;
    }

    public void setWarps(Map<String, GlobalPos> warps) {
        this.warps = warps;
        this.warpCommandSuggestions = warps.keySet().stream().toList();
        PacketDistributor.sendToAllPlayers(new WarpSuggestionPacket(this.warpCommandSuggestions));
    }

    public void addWarp(String title, GlobalPos warp) {
        this.warps.put(title, warp);
        this.warpCommandSuggestions.add(title);
        PacketDistributor.sendToAllPlayers(new WarpSuggestionPacket(this.warpCommandSuggestions));
    }

    public void removeWarp(String title) {
        this.warps.remove(title);
        this.warpCommandSuggestions.remove(title);
        PacketDistributor.sendToAllPlayers(new WarpSuggestionPacket(this.warpCommandSuggestions));
    }

    public List<String> getWarpCommandSuggestions() {
        return this.warpCommandSuggestions;
    }

    public void setWarpCommandSuggestions(List<String> warpCommandSuggestions) {
        this.warpCommandSuggestions = warpCommandSuggestions;
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
