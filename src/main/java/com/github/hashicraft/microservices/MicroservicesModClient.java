package com.github.nicholasjackson.wasmcraft;

import java.io.File;
import java.nio.file.Paths;

import com.github.nicholasjackson.wasmcraft.events.WasmBlockClicked;
import com.github.nicholasjackson.wasmcraft.gui.WasmBlockGui;
import com.github.nicholasjackson.wasmcraft.gui.WasmBlockScreen;
import com.github.nicholasjackson.wasmcraft.wasm.WasmRuntime;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.ActionResult;

public class MicroservicesModClient implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    // Here we will put client-only registration code

  }
}