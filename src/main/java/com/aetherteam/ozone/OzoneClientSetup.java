package com.aetherteam.ozone;

import net.minecraft.client.Minecraft;
import net.minecraft.server.Services;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public final class OzoneClientSetup {
    public static void setup(FMLClientSetupEvent event) {
        ModContainer mod = ModLoadingContext.get().getActiveContainer();
        mod.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        
    }

    private OzoneClientSetup() {}
}
