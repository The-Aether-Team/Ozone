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

    public static DeferredHolder<ArgumentTypeInfo<?, ?>, ArgumentTypeInfo<?, ?>> HOMES = ARGUMENT_TYPES.register("homes", () -> ArgumentTypeInfos.registerByClass(HomeArgument.class, SingletonArgumentInfo.contextFree(HomeArgument::home)));
    public static DeferredHolder<ArgumentTypeInfo<?, ?>, ArgumentTypeInfo<?, ?>> WARPS = ARGUMENT_TYPES.register("warps", () -> ArgumentTypeInfos.registerByClass(WarpArgument.class, SingletonArgumentInfo.contextFree(WarpArgument::warp)));
}
