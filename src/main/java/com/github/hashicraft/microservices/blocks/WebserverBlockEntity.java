package com.github.hashicraft.microservices.blocks;

import com.github.hashicraft.microservices.ModBlockEntities;
import com.github.hashicraft.stateful.blocks.StatefulBlockEntity;
import com.github.hashicraft.stateful.blocks.Syncable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

public class WebserverBlockEntity extends StatefulBlockEntity implements WebserverInventory {
  private final DefaultedList<ItemStack> items = DefaultedList.ofSize(1,
      ItemStack.EMPTY);

  @Syncable
  public String result;

  @Syncable
  public String port;

  @Syncable
  public String path;

  @Syncable
  public String method;

  @Syncable
  public String timeout;

  @Syncable
  public String tlsCert;

  @Syncable
  public String tlsKey;

  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

  public String getPort() {
    return port;
  }

  public void setPort(String port) {
    this.port = port;
  }

  public String getPath() {
    return this.path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getMethod() {
    return this.method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public String getTimeout() {
    return this.timeout;
  }

  public void setTimeout(String timeout) {
    this.timeout = timeout;
  }

  public String getTlsCert() {
    return this.tlsCert;
  }

  public void setTlsCert(String cert) {
    this.tlsCert = cert;
  }

  public String getTlsKey() {
    return this.tlsKey;
  }

  public void setTlsKey(String key) {
    this.tlsKey = key;
  }

  public WebserverBlockEntity(BlockPos pos, BlockState state) {
    super(ModBlockEntities.WEBSERVER_BLOCK_ENTITY, pos, state, null);
  }

  public WebserverBlockEntity(BlockPos pos, BlockState state, Block parent) {
    super(ModBlockEntities.WEBSERVER_BLOCK_ENTITY, pos, state, parent);
  }

  @Override
  public DefaultedList<ItemStack> getItems() {
    return items;
  }
}