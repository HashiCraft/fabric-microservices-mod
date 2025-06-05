package com.github.hashicraft.microservices;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.util.function.Function;

import com.github.hashicraft.microservices.blocks.DatabaseBlock;
import com.github.hashicraft.microservices.blocks.WasmBlock;
import com.github.hashicraft.microservices.blocks.WebserverBlock;

public class ModBlocks {
  public static final String MODID = "microservices";

  public static final RegistryKey<ItemGroup> ITEM_GROUP = RegistryKey.of(RegistryKeys.ITEM_GROUP,
      Identifier.of(MODID, "microservices"));

  public static final Block DATABASE_BLOCK = ModBlocks.register(
      MODID,
      "database_block",
      DatabaseBlock::new,
      AbstractBlock.Settings.create().strength(4.0f).nonOpaque().solid(),
      true);

  public static final Block WEBSERVER_BLOCK = ModBlocks.register(
      MODID,
      "webserver_block",
      WebserverBlock::new,
      AbstractBlock.Settings.create().strength(4.0f).nonOpaque().solid(),
      true);

  public static final Block WASM_BLOCK = ModBlocks.register(
      MODID,
      "wasm_block",
      WasmBlock::new,
      AbstractBlock.Settings.create().strength(4.0f).nonOpaque().solid(),
      true);

  public static void initialize() {
    Registry.register(Registries.ITEM_GROUP, ITEM_GROUP, FabricItemGroup.builder()
        .icon(() -> new ItemStack(DATABASE_BLOCK))
        .displayName(Text.translatable("microservices"))
        .build());

    WebserverBlock.registerEvents();
    DatabaseBlock.registerEvents();
    WasmBlock.registerEvents();
    WasmBlock.startRuntime();
  }

  private static Block register(String modid, String name, Function<AbstractBlock.Settings, Block> blockFactory,
      AbstractBlock.Settings settings, boolean shouldRegisterItem) {
    // Create a registry key for the block
    RegistryKey<Block> blockKey = keyOfBlock(modid, name);
    // Create the block instance
    Block block = blockFactory.apply(settings.registryKey(blockKey));

    // Sometimes, you may not want to register an item for the block.
    // Eg: if it's a technical block like `minecraft:moving_piston` or
    // `minecraft:end_gateway`
    if (shouldRegisterItem) {
      // Items need to be registered with a different type of registry key, but the ID
      // can be the same.
      RegistryKey<Item> itemKey = keyOfItem(modid, name);

      BlockItem blockItem = new BlockItem(block, new Item.Settings().registryKey(itemKey));
      Registry.register(Registries.ITEM, itemKey, blockItem);

      ItemGroupEvents.modifyEntriesEvent(ITEM_GROUP).register(content -> {
        content.add(blockItem);
      });
    }

    return Registry.register(Registries.BLOCK, blockKey, block);
  }

  private static RegistryKey<Block> keyOfBlock(String modid, String name) {
    return RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(modid, name));
  }

  private static RegistryKey<Item> keyOfItem(String modid, String name) {
    return RegistryKey.of(RegistryKeys.ITEM, Identifier.of(modid, name));
  }

}