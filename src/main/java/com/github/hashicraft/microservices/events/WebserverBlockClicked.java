package com.github.hashicraft.microservices.events;

import com.github.hashicraft.microservices.blocks.WebserverBlockEntity;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.util.ActionResult;

public interface WebserverBlockClicked {

  Event<WebserverBlockClicked> EVENT = EventFactory.createArrayBacked(WebserverBlockClicked.class,
      (listeners) -> (block, callback) -> {
        for (WebserverBlockClicked listener : listeners) {
          ActionResult result = listener.interact(block, callback);

          if (result != ActionResult.PASS) {
            return result;
          }
        }

        return ActionResult.PASS;
      });

  ActionResult interact(WebserverBlockEntity block, WebserverGuiCallback callback);
}