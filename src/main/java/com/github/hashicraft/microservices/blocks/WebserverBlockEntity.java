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
  public int serverPort;

  @Syncable
  public String serverPath;

  @Syncable
  public String serverMethod;

  // tracks the number of ticks a block has been
  // powered for
  public int onCount = 0;

  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

  public int getServerPort() {
    return serverPort;
  }

  public void setServerPort(String port) {
    try {
      var p = Integer.parseInt(port);
      this.serverPort = p;
    } catch (NumberFormatException e) {
      MicroservicesMod.LOGGER.error("Invalid port number: " + port);
      return;
    }
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

  public WebserverBlockEntity(BlockPos pos, BlockState state) {
    super(MicroservicesMod.WEBSERVER_BLOCK_ENTITY, pos, state, null);
  }

  public WebserverBlockEntity(BlockPos pos, BlockState state, Block parent) {
    super(MicroservicesMod.WEBSERVER_BLOCK_ENTITY, pos, state, parent);
  }
}