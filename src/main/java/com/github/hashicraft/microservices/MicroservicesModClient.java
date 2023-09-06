package com.github.hashicraft.microservices;

import com.github.hashicraft.microservices.events.DatabaseBlockClicked;
import com.github.hashicraft.microservices.gui.DatabaseBlockGui;
import com.github.hashicraft.microservices.gui.DatabaseBlockScreen;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.ActionResult;

public class MicroservicesModClient implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    // Here we will put client-only registration code
    DatabaseBlockClicked.EVENT.register((block, callback) -> {
      DatabaseBlockGui gui = new DatabaseBlockGui(block, callback);
      DatabaseBlockScreen screen = new DatabaseBlockScreen(gui);
      MinecraftClient.getInstance().setScreen(screen);

      return ActionResult.PASS;
    });
  }
}