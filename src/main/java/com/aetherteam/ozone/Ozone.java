package com.aetherteam.ozone;

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
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(Ozone.MODID)
public class Ozone {
    public static final String MODID = "ozone_utilities";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Ozone(ModContainer mod, IEventBus bus, Dist dist) {
        bus.addListener(OzoneData::data);
        bus.addListener(this::commonSetup);
        bus.addListener(this::registerPackets);

        DeferredRegister<?>[] registers = {
                OzoneBlocks.BLOCKS,
                OzoneItems.ITEMS,
                OzoneDataAttachments.ATTACHMENTS,
                OzoneArgumentTypes.ARGUMENT_TYPES
        };

        for (DeferredRegister<?> register : registers) {
            register.register(bus);
        }

        this.eventSetup(bus);

        mod.registerConfig(ModConfig.Type.COMMON, OzoneConfig.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {

    }

    public void eventSetup(IEventBus neoBus) {
        IEventBus bus = NeoForge.EVENT_BUS;

        neoBus.addListener(OzoneCreativeTabs::addToCreativeTabs);

        OzoneEventListeners.listen(bus);
    }

    public void registerPackets(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MODID).versioned("1.0.0").optional();

        // CLIENTBOUND
        registrar.playToClient(HomeSuggestionPacket.TYPE, HomeSuggestionPacket.STREAM_CODEC, HomeSuggestionPacket::execute);
        registrar.playToClient(WarpSuggestionPacket.TYPE, WarpSuggestionPacket.STREAM_CODEC, WarpSuggestionPacket::execute);
    }
}
