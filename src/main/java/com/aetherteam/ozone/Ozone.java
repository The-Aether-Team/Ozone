package com.aetherteam.ozone;

import com.aetherteam.ozone.attachment.OzoneDataAttachments;
import com.aetherteam.ozone.block.SurveyorTableBlock;
import com.aetherteam.ozone.data.OzoneData;
import com.aetherteam.ozone.item.ContainerKeyItem;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(Ozone.MODID)
public class Ozone {
    public static final String MODID = "ozone_utilities";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public static final DeferredBlock<Block> SURVEYOR_TABLE = BLOCKS.registerBlock("surveyor_table", SurveyorTableBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).instrument(NoteBlockInstrument.BASS).strength(2.5F).sound(SoundType.WOOD).ignitedByLava());
    public static final DeferredItem<BlockItem> SURVEYOR_TABLE_ITEM = ITEMS.registerSimpleBlockItem("surveyor_table", SURVEYOR_TABLE);

    public static final DeferredItem<Item> CONTAINER_KEY = ITEMS.registerItem("container_key", ContainerKeyItem::new, new Item.Properties().stacksTo(1));

    public Ozone(ModContainer mod, IEventBus bus, Dist dist) {
        bus.addListener(OzoneData::data);
        bus.addListener(this::commonSetup);
        bus.addListener(this::addCreative);

        BLOCKS.register(bus);
        ITEMS.register(bus);
        OzoneDataAttachments.ATTACHMENTS.register(bus);

        NeoForge.EVENT_BUS.register(this);

        this.eventSetup(bus);

        mod.registerConfig(ModConfig.Type.COMMON, OzoneConfig.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {

    }

    public void eventSetup(IEventBus neoBus) {
        IEventBus bus = NeoForge.EVENT_BUS;

        OzoneEventListeners.listen(bus);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(SURVEYOR_TABLE_ITEM);
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(CONTAINER_KEY);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {

        }
    }
}
