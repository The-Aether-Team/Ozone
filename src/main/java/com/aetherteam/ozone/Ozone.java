package com.aetherteam.ozone;

import org.slf4j.Logger;

import com.aetherteam.ozone.attachment.OzoneDataAttachments;
import com.aetherteam.ozone.block.OzoneBlocks;
import com.aetherteam.ozone.block.entity.OzoneBlockEntities;
import com.aetherteam.ozone.command.OzoneCommands;
import com.aetherteam.ozone.command.argument.OzoneArgumentTypes;
import com.aetherteam.ozone.data.OzoneData;
import com.aetherteam.ozone.item.OzoneCreativeTabs;
import com.aetherteam.ozone.item.OzoneItems;
import com.aetherteam.ozone.network.packet.clientbound.ChunkClaimPacket;
import com.aetherteam.ozone.network.packet.clientbound.HomeSuggestionPacket;
import com.aetherteam.ozone.network.packet.clientbound.WarpSuggestionPacket;
import com.mojang.logging.LogUtils;

import net.minecraft.server.Services;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(Ozone.MODID)
public class Ozone {
    public static final String MODID = "ozone_utilities";
    public static final Logger LOGGER = LogUtils.getLogger();
    
    static Services services;

    public Ozone(ModContainer mod, IEventBus modBus, Dist dist) {
        modBus.addListener(OzoneData::data);
        modBus.addListener(OzoneClientSetup::setup);
        modBus.addListener(this::registerPackets);
        modBus.addListener(OzoneCreativeTabs::addToCreativeTabs);

        OzoneBlocks.BLOCKS.register(modBus);
        OzoneItems.ITEMS.register(modBus);
        OzoneBlockEntities.BLOCK_ENTITIES.register(modBus);
        OzoneDataAttachments.ATTACHMENTS.register(modBus);
        OzoneArgumentTypes.ARGUMENT_TYPES.register(modBus);
        
        OzoneEventListeners.register(NeoForge.EVENT_BUS);
        NeoForge.EVENT_BUS.addListener(OzoneCommands::registerCommands);

        mod.registerConfig(ModConfig.Type.COMMON, OzoneConfig.SPEC);
        
    }

    private void registerPackets(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MODID).versioned("1.0.0").optional();

        // CLIENTBOUND
        registrar.playToClient(HomeSuggestionPacket.TYPE, HomeSuggestionPacket.STREAM_CODEC, HomeSuggestionPacket::execute);
        registrar.playToClient(WarpSuggestionPacket.TYPE, WarpSuggestionPacket.STREAM_CODEC, WarpSuggestionPacket::execute);
        registrar.playToClient(ChunkClaimPacket.ChangeOwner.TYPE, ChunkClaimPacket.ChangeOwner.STREAM_CODEC, ChunkClaimPacket.ChangeOwner::execute);
        registrar.playToClient(ChunkClaimPacket.Delete.TYPE, ChunkClaimPacket.Delete.STREAM_CODEC, ChunkClaimPacket.Delete::execute);
        registrar.playToClient(ChunkClaimPacket.ChangeSurveyorTableLocation.TYPE, ChunkClaimPacket.ChangeSurveyorTableLocation.STREAM_CODEC, ChunkClaimPacket.ChangeSurveyorTableLocation::execute);
        registrar.playToClient(ChunkClaimPacket.ChangePlayerFilter.TYPE, ChunkClaimPacket.ChangePlayerFilter.STREAM_CODEC, ChunkClaimPacket.ChangePlayerFilter::execute);
        registrar.playToClient(ChunkClaimPacket.ChangeEntityFilter.TYPE, ChunkClaimPacket.ChangeEntityFilter.STREAM_CODEC, ChunkClaimPacket.ChangeEntityFilter::execute);
        registrar.playToClient(ChunkClaimPacket.ChangeOwnerAndSurveyorTableLocation.TYPE, ChunkClaimPacket.ChangeOwnerAndSurveyorTableLocation.STREAM_CODEC, ChunkClaimPacket.ChangeOwnerAndSurveyorTableLocation::execute);
        registrar.playToClient(ChunkClaimPacket.ChangeFilters.TYPE, ChunkClaimPacket.ChangeFilters.STREAM_CODEC, ChunkClaimPacket.ChangeFilters::execute);
    }
}
