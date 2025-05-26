package com.aetherteam.ozone.command.argument;

import com.aetherteam.ozone.Ozone;

import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class OzoneArgumentTypes {
    public static final DeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENT_TYPES = DeferredRegister.create(BuiltInRegistries.COMMAND_ARGUMENT_TYPE, Ozone.MODID);

    public static final DeferredHolder<ArgumentTypeInfo<?, ?>, ArgumentTypeInfo<?, ?>> HOMES = ARGUMENT_TYPES.register("homes", () -> ArgumentTypeInfos.registerByClass(HomeArgument.class, SingletonArgumentInfo.contextFree(HomeArgument::home)));
    public static final DeferredHolder<ArgumentTypeInfo<?, ?>, ArgumentTypeInfo<?, ?>> WARPS = ARGUMENT_TYPES.register("warps", () -> ArgumentTypeInfos.registerByClass(WarpArgument.class, SingletonArgumentInfo.contextFree(WarpArgument::warp)));
    public static final DeferredHolder<ArgumentTypeInfo<?, ?>, ArgumentTypeInfo<?, ?>> ENTITY_FILTER = ARGUMENT_TYPES.register("entity_filter", () -> ArgumentTypeInfos.registerByClass(EntityFilterArgument.class, SingletonArgumentInfo.contextFree(EntityFilterArgument::entityFilter)));
    public static final DeferredHolder<ArgumentTypeInfo<?, ?>, ArgumentTypeInfo<?, ?>> PLAYER_FILTER = ARGUMENT_TYPES.register("player_filter", () -> ArgumentTypeInfos.registerByClass(PlayerFilterArgument.class, SingletonArgumentInfo.contextFree(PlayerFilterArgument::playerFilter)));
    public static final DeferredHolder<ArgumentTypeInfo<?, ?>, ArgumentTypeInfo<?, ?>> GAME_PROFILE = ARGUMENT_TYPES.register("game_profile", () -> ArgumentTypeInfos.registerByClass(SingleGameProfileArgument.class, SingletonArgumentInfo.contextFree(SingleGameProfileArgument::gameProfile)));
}
