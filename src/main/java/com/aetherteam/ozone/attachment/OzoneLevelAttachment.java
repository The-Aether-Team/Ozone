package com.aetherteam.ozone.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

import java.util.*;

public class OzoneLevelAttachment {
    private Optional<BlockPos> serverSpawn;
    private Map<String, BlockPos> warps;

    public static final Codec<OzoneLevelAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.optionalFieldOf("server_spawn").forGetter(OzoneLevelAttachment::getServerSpawn),
            Codec.unboundedMap(Codec.STRING, BlockPos.CODEC).fieldOf("warps").forGetter(OzoneLevelAttachment::getWarps)
    ).apply(instance, OzoneLevelAttachment::new));

    public OzoneLevelAttachment() {
        this.serverSpawn = Optional.empty();
        this.warps = new HashMap<>();
    }

    public OzoneLevelAttachment(Optional<BlockPos> serverSpawn, Map<String, BlockPos> warps) {
        this.serverSpawn = serverSpawn;
        this.warps = new HashMap<>(warps);
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
    }

    public void addWarp(String title, BlockPos warp) {
        this.warps.put(title, warp);
    }

    public void removeWarp(String title) {
        this.warps.remove(title);
    }
}
