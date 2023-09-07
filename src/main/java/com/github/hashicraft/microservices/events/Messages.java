package com.github.hashicraft.microservices.events;

import com.github.hashicraft.microservices.MicroservicesMod;

import net.minecraft.util.Identifier;

public class Messages {
  public static Identifier DATABASE_BLOCK_REGISTER = new Identifier(MicroservicesMod.MODID, "register_database");
  public static Identifier DATABASE_BLOCK_REMOVE = new Identifier(MicroservicesMod.MODID, "remove_database");
}