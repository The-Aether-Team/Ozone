package com.aetherteam.ozone.command.fun;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class HatCommand {
    private static final SimpleCommandExceptionType ERROR_EMPTY_HAND = new SimpleCommandExceptionType(Component.translatable("commands.ozone_utilities.hat.error"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("hat").executes((context) -> hat(context.getSource())).requires((source) -> source.hasPermission(3)));
    }

    private static int hat(CommandSourceStack source) throws CommandSyntaxException {
        if (source.getEntityOrException() instanceof ServerPlayer sender) {
            ItemStack stack = sender.getMainHandItem();
            if (!stack.isEmpty()) {
                ItemStack head = sender.getItemBySlot(EquipmentSlot.HEAD);
                if (head.isEmpty()) {
                    sender.setItemSlot(EquipmentSlot.HEAD, stack);
                    sender.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                } else {
                    sender.setItemSlot(EquipmentSlot.HEAD, stack);
                    sender.setItemSlot(EquipmentSlot.MAINHAND, head);
                }
                source.sendSuccess(() -> Component.translatable("commands.ozone_utilities.hat", stack.getCount(), stack.getDisplayName()), false);
            } else {
                throw ERROR_EMPTY_HAND.create();
            }
        }
        return 1;
    }
}
