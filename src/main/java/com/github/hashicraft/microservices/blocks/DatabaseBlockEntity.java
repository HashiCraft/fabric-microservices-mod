package com.github.hashicraft.microservices.blocks;

import com.github.hashicraft.microservices.MicroservicesMod;
import com.github.hashicraft.stateful.blocks.StatefulBlockEntity;
import com.github.hashicraft.stateful.blocks.Syncable;

import net.minecraft.state.property.BooleanProperty;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class DatabaseBlockEntity extends StatefulBlockEntity {

  @Syncable
  public String result;

  @Syncable
  public Integer redstonePower = 0;

  @Syncable(property = "powered", type = BooleanProperty.class)
  public boolean powered = false;

  public DatabaseBlockEntity(BlockPos pos, BlockState state) {
    super(MicroservicesMod.DATABASE_BLOCK_ENTITY, pos, state, null);
  }

  public DatabaseBlockEntity(BlockPos pos, BlockState state, Block parent) {
    super(MicroservicesMod.DATABASE_BLOCK_ENTITY, pos, state, parent);
  }
}