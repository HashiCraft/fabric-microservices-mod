package com.github.hashicraft.microservices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.hashicraft.microservices.blocks.DatabaseBlock;
import com.github.hashicraft.microservices.blocks.DatabaseBlockEntity;
import com.github.hashicraft.stateful.blocks.EntityServerState;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
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

public class MicroservicesMod implements ModInitializer {
  public static final String MODID = "microservices";
  public static final Logger LOGGER = LoggerFactory.getLogger(MicroservicesMod.class);

  public static final Identifier DATABASE_BLOCK_ID = new Identifier(MODID, "database_block");
  public static final DatabaseBlock DATABASE_BLOCK = new DatabaseBlock(FabricBlockSettings.create().strength(4.0f)
      .nonOpaque().solid());

  public static final Identifier DATABASE_ENTITY_ID = new Identifier(MODID, "database_entity");
  public static BlockEntityType<DatabaseBlockEntity> DATABASE_BLOCK_ENTITY;

  public static final Item DATABASE_ITEM = new BlockItem(DATABASE_BLOCK, new Item.Settings());
  public static final RegistryKey<ItemGroup> ITEM_GROUP = RegistryKey.of(RegistryKeys.ITEM_GROUP,
      new Identifier(MODID, "microservices"));

  @Override
  public void onInitialize() {
    // This code runs as soon as Minecraft is in a mod-load-ready state.
    System.out.println("Microservices v1.0.0 loading...");

    Registry.register(Registries.BLOCK, DATABASE_BLOCK_ID, DATABASE_BLOCK);
    Registry.register(Registries.ITEM, DATABASE_BLOCK_ID, DATABASE_ITEM);
    Registry.register(Registries.ITEM_GROUP, ITEM_GROUP, FabricItemGroup.builder()
        .icon(() -> new ItemStack(DATABASE_BLOCK))
        .displayName(Text.translatable("microservices.database"))
        .build());

    DATABASE_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, DATABASE_ENTITY_ID,
        FabricBlockEntityTypeBuilder.create(DatabaseBlockEntity::new, DATABASE_BLOCK).build());

    ItemGroupEvents.modifyEntriesEvent(ITEM_GROUP).register(content -> {
      content.add(DATABASE_ITEM);
    });

    // register for block events
    DatabaseBlock.registerEvents();

    EntityServerState.RegisterStateUpdates();
  }
}