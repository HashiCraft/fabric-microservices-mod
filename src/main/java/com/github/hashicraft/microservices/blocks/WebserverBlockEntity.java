package com.github.hashicraft.microservices.blocks;

import com.github.hashicraft.microservices.MicroservicesMod;
import com.github.hashicraft.stateful.blocks.StatefulBlockEntity;
import com.github.hashicraft.stateful.blocks.Syncable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class WebserverBlockEntity extends StatefulBlockEntity {

  @Syncable
  public String result;

  @Syncable
  public String serverPort;

  @Syncable
  public String serverPath;

  @Syncable
  public String serverMethod;

  @Syncable
  public String tlsCert;

  @Syncable
  public String tlsKey;

  // tracks the number of ticks a block has been
  // powered for
  public int onCount = 0;

  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

  public String getServerPort() {
    return serverPort;
  }

  public void setServerPort(String port) {
    this.serverPort = port;
  }

  public String getServerPath() {
    return this.serverPath;
  }

  public void setServerPath(String path) {
    this.serverPath = path;
  }

  public String getServerMethod() {
    return this.serverMethod;
  }

  public void setServerMethod(String method) {
    this.serverMethod = method;
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
    super(MicroservicesMod.WEBSERVER_BLOCK_ENTITY, pos, state, null);
  }

  public WebserverBlockEntity(BlockPos pos, BlockState state, Block parent) {
    super(MicroservicesMod.WEBSERVER_BLOCK_ENTITY, pos, state, parent);
  }
}