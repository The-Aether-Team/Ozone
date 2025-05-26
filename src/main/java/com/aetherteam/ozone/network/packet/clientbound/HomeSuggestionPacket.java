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

public record HomeSuggestionPacket(List<String> homes) implements CustomPacketPayload {
    public static final Type<HomeSuggestionPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Ozone.MODID, "home_suggestions"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HomeSuggestionPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
            HomeSuggestionPacket::homes,
            HomeSuggestionPacket::new);

    @Override
    public Type<HomeSuggestionPacket> type() {
        return TYPE;
    }

    public void execute(IPayloadContext context) {
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().level != null) {
            Minecraft.getInstance().player.getData(OzoneDataAttachments.PLAYER).setHomeCommandSuggestions(homes);
        }
    }
}