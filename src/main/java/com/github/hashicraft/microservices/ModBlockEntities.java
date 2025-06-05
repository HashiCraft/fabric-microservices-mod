package com.github.hashicraft.microservices;

import com.github.hashicraft.microservices.blocks.DatabaseBlockEntity;
import com.github.hashicraft.microservices.blocks.WasmBlockEntity;
import com.github.hashicraft.microservices.blocks.WebserverBlockEntity;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {
  public static final String MODID = "microservices";

  public static final BlockEntityType<WebserverBlockEntity> WEBSERVER_BLOCK_ENTITY = register(
      "webserver",
      WebserverBlockEntity::new,
      ModBlocks.WEBSERVER_BLOCK);

  public static final BlockEntityType<DatabaseBlockEntity> DATABASE_BLOCK_ENTITY = register(
      "database",
      DatabaseBlockEntity::new,
      ModBlocks.DATABASE_BLOCK);

  public static final BlockEntityType<WasmBlockEntity> WASM_BLOCK_ENTITY = register(
      "wasm",
      WasmBlockEntity::new,
      ModBlocks.WASM_BLOCK);

  public static void initialize() {
  }

  private static <T extends BlockEntity> BlockEntityType<T> register(
      String name,
      FabricBlockEntityTypeBuilder.Factory<? extends T> entityFactory,
      Block... blocks) {

    Identifier id = Identifier.of(MODID, name);

    return Registry.register(Registries.BLOCK_ENTITY_TYPE, id,
        FabricBlockEntityTypeBuilder.<T>create(entityFactory, blocks).build());
  }
}
