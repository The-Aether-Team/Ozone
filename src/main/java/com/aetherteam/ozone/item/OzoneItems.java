package com.aetherteam.ozone.item;

import com.aetherteam.ozone.Ozone;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class OzoneItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Ozone.MODID);

    public static final DeferredItem<Item> CONTAINER_KEY = ITEMS.registerItem("container_key", ContainerKeyItem::new, new Item.Properties().stacksTo(1));
}
