package com.github.nicholasjackson.wasmcraft;

import org.slf4j.LoggerFactory;

import com.github.hashicraft.stateful.blocks.EntityServerState;
import com.github.nicholasjackson.wasmcraft.blocks.WasmBlock;
import com.github.nicholasjackson.wasmcraft.blocks.WasmBlockEntity;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class MicroservicersMod implements ModInitializer {
  public static final String MODID = "microservices";
  public static final Logger LOGGER = LoggerFactory.getLogger(MicroservicersMod.class);

  public static final Identifier DATABASE_BLOCK_ID = new Identifier(MODID, "database_block");
  public static final DatabaseBlock DATABASE_BLOCK = new DatabaseBlock(FabricBlockSettings.create().strength(4.0f)
      .nonOpaque().solid());

  public static BlockEntityType<DatabaseBlockEntity> DATABASE_BLOCK_ENTITY;
  public static final Item DATABASE_ITEM = new BlockItem(DATABASE_BLOCK, new Item.Settings());

  public static final RegistryKey<ItemGroup> ITEM_GROUP = RegistryKey.of(RegistyKeys.ITEM_GROUP,
      new Identifier(MODID, "microservices"));

  @Override
  public void onInitialize() {
    // This code runs as soon as Minecraft is in a mod-load-ready state.
    System.out.println("Microservices v1.0.0 loading...");

    EntityServerState.RegisterStateUpdates();
  }
}
