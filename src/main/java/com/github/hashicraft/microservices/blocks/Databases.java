package com.github.hashicraft.microservices.blocks;

import java.util.HashMap;
import java.util.Set;
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;

import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.minecraft.util.math.BlockPos;

public class Databases {
  private HashMap<String, DatabaseContext> DATABASES = new HashMap<String, DatabaseContext>();

  public Set<Entry<BlockPos, DatabaseContext>> entrySet() {
    Set<Entry<BlockPos, DatabaseContext>> entrySet = new HashSet<Entry<BlockPos, DatabaseContext>>();

    for (Entry<String, DatabaseContext> entry : DATABASES.entrySet()) {
      BlockPos pos = deseriaBlockPos(entry.getKey());
      entrySet.add(new AbstractMap.SimpleEntry<BlockPos, DatabaseContext>(pos, entry.getValue()));
    }

    return entrySet;
  }

  public void add(BlockPos pos, DatabaseContext context) {
    DATABASES.put(serializeBlockPos(pos), context);
  }

  public DatabaseContext get(BlockPos pos) {
    return DATABASES.get(serializeBlockPos(pos));
  }

  public void remove(BlockPos pos) {
    DATABASES.remove(serializeBlockPos(pos));
  }

  public boolean exists(BlockPos pos) {
    return DATABASES.containsKey(serializeBlockPos(pos));
  }

  public String toJSON() {
    Type typeObject = new TypeToken<HashMap<String, DatabaseContext>>() {
    }.getType();

    Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .excludeFieldsWithoutExposeAnnotation()
        .create();

    return gson.toJson(this.DATABASES, typeObject);
  }

  public void writeToConfig() throws IOException {
    Path path = Paths.get("config", "databases.json");
    FileWriter myWriter = new FileWriter(path.toString());
    myWriter.write(toJSON());
    myWriter.close();
  }

  public static Databases loadFromConfig() {
    Path path = Paths.get("config", "databases.json");
    try (FileReader myReader = new FileReader(path.toString())) {
      String json = IOUtils.toString(myReader);
      myReader.close();

      return fromJSON(json);
    } catch (IOException e) {
      return new Databases();
    }
  }

  public static Databases fromJSON(String json) {
    Type typeObject = new TypeToken<HashMap<String, DatabaseContext>>() {
    }.getType();

    Gson gson = new GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .create();

    HashMap<String, DatabaseContext> map = gson.fromJson(json, typeObject);
    var ws = new Databases();
    ws.DATABASES = map;

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
