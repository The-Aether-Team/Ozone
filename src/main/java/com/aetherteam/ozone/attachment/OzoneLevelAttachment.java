package com.aetherteam.ozone.attachment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;

import com.aetherteam.ozone.network.packet.clientbound.WarpSuggestionPacket;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.neoforged.neoforge.network.PacketDistributor;

public class OzoneLevelAttachment {
    private GlobalPos serverSpawn;
    private final Map<String, GlobalPos> warps;
    private final HashSet<String> warpCommandSuggestions;
    private final HashMap<UUID, List<GlobalChunkPos>> playerClaims;
    private final Set<TeleportRequest> tpaQueue = new HashSet<>();

    public static final Codec<OzoneLevelAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GlobalPos.CODEC.optionalFieldOf("server_spawn").forGetter(OzoneLevelAttachment::getServerSpawn),
            Codec.unboundedMap(Codec.STRING, GlobalPos.CODEC).fieldOf("warps").forGetter(OzoneLevelAttachment::getWarps),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.list(GlobalChunkPos.CODEC)).fieldOf("playerClaims").forGetter(OzoneLevelAttachment::getPlayerClaims)
    ).apply(instance, OzoneLevelAttachment::new));

    public OzoneLevelAttachment() {
        this.serverSpawn = null;
        this.warps = new HashMap<>();
        this.warpCommandSuggestions = new HashSet<>();
        this.playerClaims = new HashMap<>();
    }

    public OzoneLevelAttachment(Optional<GlobalPos> serverSpawn, Map<String, GlobalPos> warps) {
        this(serverSpawn, warps, Map.of());
    }

    public OzoneLevelAttachment(Optional<GlobalPos> serverSpawn, Map<String, GlobalPos> warps, Optional<Map<UUID, List<GlobalChunkPos>>> playerClaims) {
        this(serverSpawn, warps, playerClaims.orElse(Map.of()));
    }

    public OzoneLevelAttachment(Optional<GlobalPos> serverSpawn, Map<String, GlobalPos> warps, List<Pair<UUID, List<GlobalChunkPos>>> playerClaims) {
        this.serverSpawn = serverSpawn.orElse(null);
        this.warps = new HashMap<>(warps);
        this.warpCommandSuggestions = new HashSet<>(this.warps.keySet());
        this.playerClaims = playerClaims.isEmpty()? new HashMap<>() : playerClaims.stream().collect(Collectors.toMap(
            Pair::getFirst,
            entry -> new ArrayList<>(entry.getSecond()),
            (list1, list2) -> {
                for (var elem : list2) {
                    if (!list1.contains(elem)) {
                        list1.add(elem);
                    }
                }
                return list1;
            },
            HashMap::new
        ));
    }
    
    public OzoneLevelAttachment(Optional<GlobalPos> serverSpawn, Map<String, GlobalPos> warps, Map<UUID, List<GlobalChunkPos>> playerClaims) {
        this.serverSpawn = serverSpawn.orElse(null);
        this.warps = new HashMap<>(warps);
        this.warpCommandSuggestions = new HashSet<>(this.warps.keySet());
        this.playerClaims = playerClaims.isEmpty()? new HashMap<>() : playerClaims.entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> new ArrayList<>(entry.getValue()),
            (list1, list2) -> {
                for (var elem : list2) {
                    if (!list1.contains(elem)) {
                        list1.add(elem);
                    }
                }
                return list1;
            },
            HashMap::new
        ));
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
        if (this.warps != warps) {
            this.warps.clear();
            this.warps.putAll(warps);
            this.warpCommandSuggestions.clear();
            if (this.warpCommandSuggestions.addAll(warps.keySet())) {
                PacketDistributor.sendToAllPlayers(new WarpSuggestionPacket(this.warpCommandSuggestions));
            }
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

    public HashMap<UUID, List<GlobalChunkPos>> getPlayerClaims() {
        return playerClaims;
    }

    private Optional<Map<UUID, List<GlobalChunkPos>>> getPlayerClaimsOpt() {
        return playerClaims.isEmpty()? Optional.empty() : Optional.of(playerClaims);
    }

    private Optional<List<Pair<UUID, List<GlobalChunkPos>>>> getPlayerClaimsAsCompoundListOpt() {
        var list = getPlayerClaimsAsCompoundList();
        return list.isEmpty()? Optional.empty() : Optional.of(list);
    }

    private List<Pair<UUID, List<GlobalChunkPos>>> getPlayerClaimsAsCompoundList() {
        if (playerClaims.isEmpty()) return List.of();
        return playerClaims.entrySet().stream()
            .filter(entry -> !entry.getValue().isEmpty())
            .map(entry -> Pair.of(entry.getKey(), entry.getValue()))
            .toList();
    }

    public List<GlobalChunkPos> getPlayerClaims(UUID playerUUID) {
        return playerClaims.computeIfAbsent(Validate.notNull(playerUUID, "playerUUID was null"), uuid -> new ArrayList<>());
    }

    public boolean addPlayerClaim(UUID playerUUID, GlobalChunkPos claim) {
        var claims = getPlayerClaims(playerUUID);
        if (!claims.contains(Validate.notNull(claim, "claim was null"))) {
            return claims.add(claim);
        }
        return false;
    }

    public boolean removePlayerClaim(UUID playerUUID, GlobalChunkPos claim) {
        var claims = playerClaims.get(Validate.notNull(playerUUID, "playerUUID was null"));
        Validate.notNull(claim, "claim was null");
        if (claims != null) {
            return claims.remove(claim);
        }
        return false;
    }

    public int getPlayerClaimCount(UUID playerUUID) {
        var claims = playerClaims.get(Validate.notNull(playerUUID, "playerUUID was null"));
        return claims == null? 0 : claims.size();
    }

    public record TeleportRequest(UUID teleportSubject, UUID teleportTarget) { }
}
