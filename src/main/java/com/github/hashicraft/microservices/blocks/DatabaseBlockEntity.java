package com.github.nicholasjackson.wasmcraft.blocks;

import java.util.ArrayList;

import com.github.nicholasjackson.wasmcraft.WasmcraftMod;
import com.github.hashicraft.stateful.blocks.StatefulBlockEntity;
import com.github.hashicraft.stateful.blocks.Syncable;

import net.minecraft.state.property.BooleanProperty;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class DatabaseBlockEntity extends StatefulBlockEntity {

  @Syncable
  public ArrayList<String> modules = new ArrayList<String>();
  public ArrayList<String> names = new ArrayList<String>();

  @Syncable
  public String function;

  @Syncable
  public ArrayList<String> parameters;

  @Syncable
  public String result;

  @Syncable
  public Integer redstonePower = 0;

  @Syncable(property = "powered", type = BooleanProperty.class)
  public boolean powered = false;

  public DatabaseBlockEntity(BlockPos pos, BlockState state) {
    super(MicroserviesMod.DATABASE_BLOCK_ENTITY, pos, state, null);
  }

  public DatabaseBlockEntity(BlockPos pos, BlockState state, Block parent) {
    super(WasmcraftMod.WASM_BLOCK_ENTITY, pos, state, parent);
  }

}