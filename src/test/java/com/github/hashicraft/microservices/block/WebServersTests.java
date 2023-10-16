package com.github.hashicraft.microservices.block;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileWriter;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.github.hashicraft.microservices.blocks.WebserverContext;
import com.github.hashicraft.microservices.blocks.Webservers;
import com.github.hashicraft.microservices.interpolation.File;

import net.minecraft.util.math.BlockPos;

public class WebServersTests {

  @Test
  public void serializesDataToJSON() throws IOException {
    Webservers servers = new Webservers();

    WebserverContext context = new WebserverContext();
    context.setServerPort("8080");
    context.setServerPath("/test");
    context.setServerMethod("GET");

    servers.add(new BlockPos(1, 2, 3), context);

    String json = servers.toJSON();


    assertContains(json, "\"serverPort\": \"8080\"");
  }

  @Test
  public void deserializesDataFromJSON() {
    String json = """
        {
          "BlockPos{x\u003d1, y\u003d2, z\u003d3}": {
            "serverPort": "8080",
            "serverPath": "/test",
            "serverMethod": "GET"
          }
        }""";

    Webservers servers = Webservers.fromJSON(json);

    WebserverContext context = servers.get(new BlockPos(1, 2, 3));

    assertContains(context.getServerPort(), "8080");
  }

  public void assertContains(String string, String subString) {
    assertTrue(string.contains(subString));
  }
}