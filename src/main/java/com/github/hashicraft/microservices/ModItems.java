package com.github.hashicraft.microservices;

import java.util.function.Function;

import com.github.hashicraft.microservices.items.DataItem;
import com.github.hashicraft.microservices.items.ErrorItem;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class ModItems {
  public static final String MODID = "microservices";

  public static final Item DATA_ITEM = register(
      "data_item",
      DataItem::new,
      new Item.Settings());

  public static final Item ERROR_ITEM = register(
      "error_item",
      ErrorItem::new,
      new Item.Settings());

  public static void initializeClient() {
    ItemTooltipCallback.EVENT.register((itemStack, tooltipContext, tooltipType, list) -> {
      if (itemStack.isOf(DATA_ITEM)) {
        NbtCompound item = itemStack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();

        String id = item.getString("request_id", "");
        String data = item.getString("data", "");

        list.add(Text.literal("Request ID").setStyle(Style.EMPTY.withColor(Formatting.WHITE)));
        list.add(Text.literal(id).setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
        list.add(Text.literal(""));
        list.add(Text.literal("Data").setStyle(Style.EMPTY.withColor(Formatting.WHITE)));
        list.add(Text.literal(data).setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
      }

      if (itemStack.isOf(DATA_ITEM)) {
        NbtCompound item = itemStack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();

        String id = item.getString("request_id", "");
        String data = item.getString("data", "");

        list.add(Text.literal("Request ID").setStyle(Style.EMPTY.withColor(Formatting.WHITE)));
        list.add(Text.literal(id).setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
        list.add(Text.literal(""));
        list.add(Text.literal("Data").setStyle(Style.EMPTY.withColor(Formatting.WHITE)));
        list.add(Text.literal(data).setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
      }
    });
  }

  public static Item register(String name, Function<Item.Settings, Item> itemFactory, Item.Settings settings) {
    // Create the item key.
    RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MODID, name));

    // Create the item instance.
    Item item = itemFactory.apply(settings.registryKey(itemKey));

    // Register the item.
    Registry.register(Registries.ITEM, itemKey, item);

    return item;
  }
}
