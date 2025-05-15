package com.aetherteam.ozone.item;

import com.aetherteam.ozone.block.OzoneBlocks;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

public class OzoneCreativeTabs {
    public static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(OzoneBlocks.SURVEYOR_TABLE);
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(OzoneItems.CONTAINER_KEY);
        }
    }
}
