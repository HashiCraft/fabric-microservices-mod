package com.github.hashicraft.microservices.block;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.github.hashicraft.microservices.blocks.WebserverContext;
import com.github.hashicraft.microservices.blocks.WebserverHandler;
import com.github.hashicraft.microservices.blocks.Webservers;
import net.minecraft.util.math.BlockPos;

public class WebServersTests {

  @Test
  public void serializesDataToJSON() throws IOException {
    Webservers servers = new Webservers();

    WebserverContext context = new WebserverContext();
    context.setPort("8080");
    context.setTlsCert("./cert.pem");
    context.setTlsKey("./key.pem");

    // add a handler to the context
    context.updateHandler(new WebserverHandler(new BlockPos(1, 2, 3), "/test", "GET", "5000"));

    // add the context to the servers
    servers.addOrUpdate(context);

    String json = servers.toJSON();

    System.out.println("Serialized Webservers: " + json);
  }

  @Test
  public void deserializesDataFromJSON() {
    String json = """
        [
          {
            "port": "8080",
            "tlsCert": "./cert.pem",
            "tlsKey": "./key.pem",
            "webserverHandlers": [
              {
                "blockPos": "1_2_3",
                "path": "/test",
                "method": "GET",
                "timeout": "5000"
              }
            ]
          }
        ]""";

    Webservers servers = Webservers.fromJSON(json);
    System.out.println("Deserialized Webservers: " + servers);

    WebserverContext context = servers.getOrDefault("8080");
    assertNotNull(context.getTlsCert());
    assertNotNull(context.getTlsKey());
    assertTrue(context.getTlsCert().contains("./cert.pem"));
    assertTrue(context.getTlsKey().contains("./key.pem"));

    WebserverHandler handler = context.getHandlerForPos(new BlockPos(1, 2, 3));
    assertNotNull(handler.getPath());
    assertNotNull(handler.getMethod());
    assertNotNull(handler.getTimeout());
    assertTrue(handler.getPath().contains("/test"));
    assertTrue(handler.getMethod().contains("GET"));
    assertTrue(handler.getTimeout().contains("5000"));
  }
}