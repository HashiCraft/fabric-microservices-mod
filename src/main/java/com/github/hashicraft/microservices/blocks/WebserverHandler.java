package com.github.hashicraft.microservices.blocks;

import com.google.gson.annotations.Expose;
import net.minecraft.util.math.BlockPos;

public class WebserverHandler {
  @Expose
  private String blockPos;

  @Expose
  private String path;

  @Expose
  private String method;

  @Expose
  private String timeout;

  public WebserverHandler(BlockPos pos) {
    this.setBlockPos(pos);
  }

  public WebserverHandler(BlockPos pos, String path, String method, String timeout) {
    this.setBlockPos(pos);
    this.setPath(path);
    this.setMethod(method);
    this.setTimeout(timeout);
  }

  public String getPath() {
    return this.path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getMethod() {
    return this.method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public String getTimeout() {
    return this.timeout;
  }

  public void setTimeout(String timeout) {
    this.timeout = timeout;
  }

  public void setBlockPos(BlockPos pos) {
    this.blockPos = serializeBlockPos(pos);
  }

  public BlockPos getBlockPos() {
    return deseriaBlockPos(this.blockPos);
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