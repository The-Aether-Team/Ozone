package com.aetherteam.ozone.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public record GlobalChunkPos(ResourceKey<Level> dimension, ChunkPos pos) {
    public static final MapCodec<GlobalChunkPos> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(GlobalChunkPos::dimension),
        ChunkPos.CODEC.fieldOf("pos").forGetter(GlobalChunkPos::pos)
    ).apply(instance, GlobalChunkPos::of));
    public static final Codec<GlobalChunkPos> CODEC = MAP_CODEC.codec();
    public static final StreamCodec<ByteBuf, GlobalChunkPos> STREAM_CODEC = StreamCodec.composite(
        ResourceKey.streamCodec(Registries.DIMENSION), GlobalChunkPos::dimension,
        ChunkPos.STREAM_CODEC, GlobalChunkPos::pos,
        GlobalChunkPos::of
    );

    public static GlobalChunkPos of(ResourceKey<Level> dimension, ChunkPos pos) {
        return new GlobalChunkPos(dimension, pos);
    }

    public int getX() {
        return pos.x;
    }

    public int getZ() {
        return pos.z;
    }

    @Override
    public final String toString() {
        return dimension + " " + pos;
    }

    @Override
    public final int hashCode() {
        return 31 * dimension.location().hashCode() + pos.hashCode();
    }

    @Override
    public final boolean equals(Object obj) {
        return this == obj || obj instanceof GlobalChunkPos other && this.pos.equals(other.pos) && this.dimension.registry().equals(other.dimension.registry()) && this.dimension.location().equals(other.dimension.location());
    }
}
