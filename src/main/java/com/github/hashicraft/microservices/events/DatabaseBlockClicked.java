package com.github.nicholasjackson.wasmcraft.events;

import com.github.nicholasjackson.wasmcraft.blocks.WasmBlockEntity;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.util.ActionResult;

public interface DatabaseBlockClicked {

  Event<DatabaseBlockClicked> EVENT = EventFactory.createArrayBacked(DatabaseBlockClicked.class,
      (listeners) -> (block, callback) -> {
        for (DatabaseBlockClicked listener : listeners) {
          ActionResult result = listener.interact(block, callback);

          if (result != ActionResult.PASS) {
            return result;
          }
        }

        return ActionResult.PASS;
      });

  ActionResult interact(WasmBlockEntity block, DatabaseGuiCallback callback);
}