package com.aetherteam.ozone;

import net.neoforged.neoforge.common.ModConfigSpec;

public class OzoneConfig {
    private static final ModConfigSpec.IntValue MAX_CHUNK_CLAIMS;

    public static int getMaxChunkClaims() {
        return MAX_CHUNK_CLAIMS.getAsInt();
    }

    public static void setMaxChunkClaims(int maxChunkClaims) {
        MAX_CHUNK_CLAIMS.set(maxChunkClaims);
    }

    static final ModConfigSpec SPEC;

    static {
        var builder = new ModConfigSpec.Builder();

        MAX_CHUNK_CLAIMS = builder
            .comment("Maximum number of chunk claims a player can have")
            .translation("config.ozone_utilities.common.max_chunk_claims")
            .defineInRange("maxChunkClaims", 500, 0, Integer.MAX_VALUE);

        SPEC = builder.build();
    }
}
