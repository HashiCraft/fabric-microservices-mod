package com.github.hashicraft.microservices;

import com.github.hashicraft.microservices.events.DatabaseBlockClicked;
import com.github.hashicraft.microservices.events.WebserverBlockClicked;
import com.github.hashicraft.microservices.gui.DatabaseBlockGui;
import com.github.hashicraft.microservices.gui.DatabaseBlockScreen;
import com.github.hashicraft.microservices.gui.WebserverBlockGui;
import com.github.hashicraft.microservices.gui.WebserverBlockScreen;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.ActionResult;

@Environment(EnvType.CLIENT)
public class MicroservicesModClient implements ClientModInitializer {

  @Override
  public void onInitializeClient() {
    Client.setClient(true);

    // Here we will put client-only registration code
    DatabaseBlockClicked.EVENT.register((block, callback) -> {
      DatabaseBlockGui gui = new DatabaseBlockGui(block, callback);
      DatabaseBlockScreen screen = new DatabaseBlockScreen(gui);
      MinecraftClient.getInstance().setScreen(screen);

      return ActionResult.PASS;
    });

    WebserverBlockClicked.EVENT.register((block, callback) -> {
      WebserverBlockGui gui = new WebserverBlockGui(block, callback);
      WebserverBlockScreen screen = new WebserverBlockScreen(gui);
      MinecraftClient.getInstance().setScreen(screen);

      return ActionResult.PASS;
    });
  }
}