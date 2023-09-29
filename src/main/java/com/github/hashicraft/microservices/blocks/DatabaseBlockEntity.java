package com.github.hashicraft.microservices.blocks;

import com.github.hashicraft.microservices.MicroservicesMod;
import com.github.hashicraft.stateful.blocks.StatefulBlockEntity;
import com.github.hashicraft.stateful.blocks.Syncable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class DatabaseBlockEntity extends StatefulBlockEntity {

  @Syncable
  public String result;

  @Syncable
  public String dbAddress;

  @Syncable
  public String dbUsername;

  @Syncable
  public String dbPassword;

  @Syncable
  public String dbDatabase;

  @Syncable
  public String sqlStatement;

  // tracks the number of ticks a block has been
  // powered for
  public int onCount = 0;

  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

  public String getDbAddress() {
    return dbAddress;
  }

  public void setDbAddress(String address) {
    this.dbAddress = address;
  }

  public String getUsername() {
    return this.dbUsername;
  }

  public void setUsername(String name) {
    this.dbUsername = name;
  }

  public String getPassword() {
    return this.dbPassword;
  }

  public void setPassword(String password) {
    this.dbPassword = password;
  }

  public String getDatabase() {
    return this.dbDatabase;
  }

  public void setDatabase(String database) {
    this.dbDatabase = database;
  }

  public String getSQLStatement() {
    return this.sqlStatement;
  }

  public void setSQLStatement(String sql) {
    this.sqlStatement = sql;
  }

  public DatabaseBlockEntity(BlockPos pos, BlockState state) {
    super(MicroservicesMod.DATABASE_BLOCK_ENTITY, pos, state, null);
  }

  public DatabaseBlockEntity(BlockPos pos, BlockState state, Block parent) {
    super(MicroservicesMod.DATABASE_BLOCK_ENTITY, pos, state, parent);
  }
}