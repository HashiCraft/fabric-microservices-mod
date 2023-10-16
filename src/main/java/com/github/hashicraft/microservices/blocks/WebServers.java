package com.github.hashicraft.microservices.blocks;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.gson.reflect.TypeToken;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.util.math.BlockPos;

public class WebServers {

  private HashMap<String, WebServerContext> SERVERS = new HashMap<String, WebServerContext>();

  // map iteration methods
  public Set<Entry<BlockPos, WebServerContext>> entrySet() {
    Set<Entry<BlockPos, WebServerContext>> entrySet = new HashSet<Entry<BlockPos, WebServerContext>>();

    for (Entry<String, WebServerContext> entry : SERVERS.entrySet()) {
      BlockPos pos = deseriaBlockPos(entry.getKey());
      entrySet.add(new AbstractMap.SimpleEntry<BlockPos, WebServerContext>(pos, entry.getValue()));
    }

    return entrySet;
  }

  public void add(BlockPos pos, WebServerContext context) {
    SERVERS.put(serializeBlockPos(pos), context);
  }

  public WebServerContext get(BlockPos pos) {
    return SERVERS.get(serializeBlockPos(pos));
  }

  public void remove(BlockPos pos) {
    SERVERS.remove(serializeBlockPos(pos));
  }

  public boolean exists(BlockPos pos) {
    return SERVERS.containsKey(serializeBlockPos(pos));
  }

  public String toJSON() {
    Type typeObject = new TypeToken<HashMap<String, WebServerContext>>() {
    }.getType();

    Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .excludeFieldsWithoutExposeAnnotation()
        .create();

    return gson.toJson(this.SERVERS, typeObject);
  }

  public void writeToConfig() throws IOException {
    Path path = Paths.get("config", "webservers.json");
    FileWriter myWriter = new FileWriter(path.toString());
    myWriter.write(toJSON());
    myWriter.close();
  }

  public static WebServers loadFromConfig() {
    Path path = Paths.get("config", "webservers.json");
    try (FileReader myReader = new FileReader(path.toString())) {
      String json = IOUtils.toString(myReader);
      myReader.close();

      return fromJSON(json);
    } catch (IOException e) {
      return new WebServers();
    }
  }

  public static WebServers fromJSON(String json) {
    Type typeObject = new TypeToken<HashMap<String, WebServerContext>>() {
    }.getType();

    Gson gson = new GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .create();

    HashMap<String, WebServerContext> map = gson.fromJson(json, typeObject);
    var ws = new WebServers();
    ws.SERVERS = map;

    return ws;
  }

  public static String serializeBlockPos(BlockPos pos) {
    return String.format("%s_%s_%s", pos.getX(), pos.getY(), pos.getZ());
  }

  public static BlockPos deseriaBlockPos(String pos) {
    String[] p = pos.split("_", -1);

    return new BlockPos(
        Integer.parseInt(p[0]),
        Integer.parseInt(p[1]),
        Integer.parseInt(p[2]));
  }
}