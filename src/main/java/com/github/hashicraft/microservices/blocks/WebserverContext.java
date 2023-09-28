package com.github.hashicraft.microservices.blocks;

import io.javalin.Javalin;

public class WebserverContext {
  public Boolean powered;
  public Javalin server;

  public WebserverContext(Javalin server, Boolean powered) {
    this.powered = powered;
    this.server = server;
  }
}