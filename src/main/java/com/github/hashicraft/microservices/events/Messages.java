package com.github.hashicraft.microservices.events;

import com.github.hashicraft.microservices.MicroservicesMod;

import net.minecraft.util.Identifier;

public class Messages {
  public static Identifier DATABASE_BLOCK_REGISTER = new Identifier(MicroservicesMod.MODID, "register_database");
  public static Identifier DATABASE_BLOCK_REMOVE = new Identifier(MicroservicesMod.MODID, "remove_database");

  public static Identifier WEBSERVER_BLOCK_REGISTER = new Identifier(MicroservicesMod.MODID, "register_webserver");
  public static Identifier WEBSERVER_BLOCK_REMOVE = new Identifier(MicroservicesMod.MODID, "remove_webserver");
  public static Identifier WEBSERVER_BLOCK_UPDATED = new Identifier(MicroservicesMod.MODID, "update_webserver");
}