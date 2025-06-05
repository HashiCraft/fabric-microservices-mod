package com.github.hashicraft.microservices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.hashicraft.stateful.blocks.EntityServerState;

import net.fabricmc.api.ModInitializer;

public class MicroservicesMod implements ModInitializer {
  public static final Logger LOGGER = LoggerFactory.getLogger(MicroservicesMod.class);

  @Override
  public void onInitialize() {
    // This code runs as soon as Minecraft is in a mod-load-ready state.
    System.out.println("Microservices v1.3.0 loading...");

    System.out.println("Registering Microservices blocks");
    ModBlocks.initialize();

    System.out.println("Registering Microservices entities");
    ModBlockEntities.initialize();

    // register the entity server state updates
    EntityServerState.RegisterStateUpdates();
  }
}