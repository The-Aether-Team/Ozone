package com.aetherteam.ozone;

import net.minecraft.resources.ResourceLocation;

public final class Locator {
    private Locator() {}

    public static ResourceLocation resource(String str) {
        return ResourceLocation.parse(str);
    }

    public static ResourceLocation ozone(String str) {
        return ResourceLocation.fromNamespaceAndPath(Ozone.MODID, str);
    }

    public static ResourceLocation aether(String str) {
        return ResourceLocation.fromNamespaceAndPath("aether", str);
    }

    public static ResourceLocation aether_ii(String str) {
        return ResourceLocation.fromNamespaceAndPath("aether_ii", str);
    }
}
