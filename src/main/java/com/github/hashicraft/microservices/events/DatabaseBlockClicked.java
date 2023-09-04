package com.github.hashicraft.microservices.events;

import com.github.hashicraft.microservices.blocks.DatabaseBlockEntity;

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

  ActionResult interact(DatabaseBlockEntity block, DatabaseGuiCallback callback);
}