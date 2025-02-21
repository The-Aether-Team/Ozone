package com.aetherteam.ozone.command;

import com.aetherteam.ozone.Ozone;
import com.aetherteam.ozone.command.teleport.BackCommand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = Ozone.MODID, bus = EventBusSubscriber.Bus.GAME)
public class OzoneCommands {
    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        BackCommand.register(event.getDispatcher());
    }
}
