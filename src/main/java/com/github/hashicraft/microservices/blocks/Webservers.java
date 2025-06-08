package com.github.hashicraft.microservices.blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.IOUtils;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.gson.reflect.TypeToken;

import net.minecraft.util.math.BlockPos;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Webservers {

  private List<WebserverContext> webserverContexts;

  public Webservers() {
    this.webserverContexts = new ArrayList<>();
  }

  public List<WebserverContext> getContexts() {
    return webserverContexts;
  }

  public void addOrUpdate(WebserverContext context) {
    // if the context already exists, remove it
    webserverContexts.removeIf(existingContext -> existingContext.getPort().equals(context.getPort()));
    webserverContexts.add(context);
  }

  public boolean exists(String port, String path, String method) {
    var ctx = webserverContexts.stream()
        .filter(context -> context.getPort().equals(port))
        .findFirst();

    if (ctx.isPresent()) {
      return false;
    }

    return ctx.get().handlerExists(path, method);
  }

  public WebserverContext getOrDefault(String port) {
    Optional<WebserverContext> ctx = webserverContexts.stream()
        .filter(context -> context.getPort().equals(port))
        .findFirst();

    if (ctx.isPresent()) {
      return ctx.get();
    } else {
      var wsx = new WebserverContext(port);
      this.addOrUpdate(wsx);
      return wsx;
    }
  }

  public Optional<WebserverContext> getAtLocation(BlockPos pos) {
    return webserverContexts.stream()
        .filter(context -> context.getHandlers().stream()
            .anyMatch(handler -> handler.getBlockPos().equals(pos)))
        .findFirst();
  }

  public void removeAtLocation(BlockPos pos) {
    webserverContexts.forEach(context -> context.removeHandler(pos));
  }

  public void writeToConfig() throws IOException {
    Path path = Paths.get("config", "webservers.json");
    FileWriter myWriter = new FileWriter(path.toString());
    myWriter.write(toJSON());
    myWriter.close();
  }

  public static Webservers loadFromConfig() {
    Path path = Paths.get("config", "webservers.json");
    try (FileReader myReader = new FileReader(path.toString())) {
      String json = IOUtils.toString(myReader);
      myReader.close();

      return fromJSON(json);
    } catch (IOException e) {
      return new Webservers();
    }
  }

  public String toJSON() {
    Type typeObject = new TypeToken<List<WebserverContext>>() {
    }.getType();

    Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .excludeFieldsWithoutExposeAnnotation()
        .create();

    return gson.toJson(this.webserverContexts, typeObject);
  }

  public static Webservers fromJSON(String json) {
    Type typeObject = new TypeToken<List<WebserverContext>>() {
    }.getType();

    Gson gson = new GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .create();

    List<WebserverContext> ar = gson.fromJson(json, typeObject);
    var ws = new Webservers();
    ws.webserverContexts = ar;

    return ws;
  }

}