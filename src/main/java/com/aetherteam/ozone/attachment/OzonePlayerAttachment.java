package com.aetherteam.ozone.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

import java.util.Optional;

public class OzonePlayerAttachment {
    public Optional<BlockPos> previousPosition; //todo mixin the teleport method to assign this value before teleporting.

    public static final Codec<OzonePlayerAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.optionalFieldOf("previous_position").forGetter(OzonePlayerAttachment::getPreviousPosition)
    ).apply(instance, OzonePlayerAttachment::new));

    public OzonePlayerAttachment() {
        this.previousPosition = Optional.empty();
    }

    public OzonePlayerAttachment(Optional<BlockPos> previousPosition) {
        this.previousPosition = previousPosition;
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
}
