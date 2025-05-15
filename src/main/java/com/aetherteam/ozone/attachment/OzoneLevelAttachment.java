package com.aetherteam.ozone.attachment;

import com.aetherteam.ozone.network.packet.clientbound.HomeSuggestionPacket;
import com.aetherteam.ozone.network.packet.clientbound.WarpSuggestionPacket;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

public class OzoneLevelAttachment {
    private Optional<BlockPos> serverSpawn;
    private Map<String, BlockPos> warps;
    private List<String> warpCommandSuggestions;

    public static final Codec<OzoneLevelAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.optionalFieldOf("server_spawn").forGetter(OzoneLevelAttachment::getServerSpawn),
            Codec.unboundedMap(Codec.STRING, BlockPos.CODEC).fieldOf("warps").forGetter(OzoneLevelAttachment::getWarps)
    ).apply(instance, OzoneLevelAttachment::new));

    public OzoneLevelAttachment() {
        this.serverSpawn = Optional.empty();
        this.warps = new HashMap<>();
        this.warpCommandSuggestions = new ArrayList<>();
    }

    public OzoneLevelAttachment(Optional<BlockPos> serverSpawn, Map<String, BlockPos> warps) {
        this.serverSpawn = serverSpawn;
        this.warps = new HashMap<>(warps);
        this.warpCommandSuggestions = new ArrayList<>(this.warps.keySet().stream().toList());
    }

    public Optional<BlockPos> getServerSpawn() {
        return this.serverSpawn;
    }

    public void setServerSpawn(BlockPos serverSpawn) {
        this.serverSpawn = Optional.of(serverSpawn);
    }

    public void clearServerSpawn() {
        this.serverSpawn = Optional.empty();
    }

    public Map<String, BlockPos> getWarps() {
        return this.warps;
    }

    public void setWarps(Map<String, BlockPos> warps) {
        this.warps = warps;
        this.warpCommandSuggestions = warps.keySet().stream().toList();
        PacketDistributor.sendToAllPlayers(new WarpSuggestionPacket(this.warpCommandSuggestions));
    }

    public void addWarp(String title, BlockPos warp) {
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
}
