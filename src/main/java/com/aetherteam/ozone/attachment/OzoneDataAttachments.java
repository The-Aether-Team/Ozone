package com.aetherteam.ozone.attachment;

import com.aetherteam.ozone.Ozone;
import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Optional;
import java.util.UUID;

public class OzoneDataAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Ozone.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<OzoneLevelAttachment>> LEVEL = ATTACHMENTS.register("level", () -> AttachmentType.builder((holder) -> new OzoneLevelAttachment()).serialize(OzoneLevelAttachment.CODEC).build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<OzonePlayerAttachment>> PLAYER = ATTACHMENTS.register("player", () -> AttachmentType.builder((holder) -> new OzonePlayerAttachment()).serialize(OzonePlayerAttachment.CODEC).build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> LOCKED = ATTACHMENTS.register("locked", () -> AttachmentType.builder((holder) -> false).serialize(Codec.BOOL).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Optional<UUID>>> OWNER = ATTACHMENTS.register("owner", () -> AttachmentType.<Optional<UUID>>builder((holder) -> Optional.empty()).serialize(UUIDUtil.CODEC.optionalFieldOf("uuid").codec()).build());
}
