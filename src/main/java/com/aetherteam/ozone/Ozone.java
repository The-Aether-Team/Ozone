package com.aetherteam.ozone;

import org.slf4j.Logger;

import com.aetherteam.ozone.attachment.OzoneDataAttachments;
import com.aetherteam.ozone.block.OzoneBlocks;
import com.aetherteam.ozone.command.argument.OzoneArgumentTypes;
import com.aetherteam.ozone.data.OzoneData;
import com.aetherteam.ozone.item.OzoneCreativeTabs;
import com.aetherteam.ozone.item.OzoneItems;
import com.aetherteam.ozone.network.packet.clientbound.HomeSuggestionPacket;
import com.aetherteam.ozone.network.packet.clientbound.WarpSuggestionPacket;
import com.mojang.logging.LogUtils;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(Ozone.MODID)
public class Ozone {
    public static final String MODID = "ozone_utilities";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Ozone(ModContainer mod, IEventBus modBus, Dist dist) {
        modBus.addListener(OzoneData::data);
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::registerPackets);
        modBus.addListener(OzoneCreativeTabs::addToCreativeTabs);

        OzoneBlocks.BLOCKS.register(modBus);
        OzoneItems.ITEMS.register(modBus);
        OzoneDataAttachments.ATTACHMENTS.register(modBus);
        OzoneArgumentTypes.ARGUMENT_TYPES.register(modBus);
        
        OzoneEventListeners.register(NeoForge.EVENT_BUS);

        mod.registerConfig(ModConfig.Type.COMMON, OzoneConfig.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {

    }

    private void registerPackets(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MODID).versioned("1.0.0").optional();

        // CLIENTBOUND
        registrar.playToClient(HomeSuggestionPacket.TYPE, HomeSuggestionPacket.STREAM_CODEC, HomeSuggestionPacket::execute);
        registrar.playToClient(WarpSuggestionPacket.TYPE, WarpSuggestionPacket.STREAM_CODEC, WarpSuggestionPacket::execute);
    }
}
