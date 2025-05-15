package com.aetherteam.ozone.network.packet.clientbound;

import com.aetherteam.ozone.Ozone;
import com.aetherteam.ozone.attachment.OzoneDataAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record WarpSuggestionPacket(List<String> warps) implements CustomPacketPayload {
    public static final Type<WarpSuggestionPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Ozone.MODID, "warp_suggestions"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WarpSuggestionPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
            WarpSuggestionPacket::warps,
            WarpSuggestionPacket::new);

    @Override
    public Type<WarpSuggestionPacket> type() {
        return TYPE;
    }

    public static void execute(WarpSuggestionPacket payload, IPayloadContext context) {
        if (Minecraft.getInstance().level != null) {
            Minecraft.getInstance().level.getData(OzoneDataAttachments.LEVEL).setWarpCommandSuggestions(payload.warps());
        }
    }
}
