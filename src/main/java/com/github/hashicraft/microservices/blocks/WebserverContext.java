package com.github.hashicraft.microservices.blocks;

import java.util.ArrayList;

import com.github.hashicraft.microservices.http.NanoHTTP;
import com.google.gson.annotations.Expose;

import io.undertow.Undertow;
import net.minecraft.util.math.BlockPos;

public class WebserverContext {
  // reference to the server, this will not be serialized
  private NanoHTTP server;

  @Expose
  private String port;

  @Expose
  private String tlsCert;

  @Expose
  private String tlsKey;

  @Expose
  private ArrayList<WebserverHandler> webserverHandlers;

  public WebserverContext() {
    this.webserverHandlers = new ArrayList<>();
  }

  public WebserverContext(String port) {
    this.port = port;
    this.webserverHandlers = new ArrayList<>();
  }

  // getters and setters for private methods
  public NanoHTTP getServer() {
    return this.server;
  }

  public void setServer(NanoHTTP server) {
    this.server = server;
  }

  public String getPort() {
    return port;
  }

  public void setPort(String port) {
    this.port = port;
  }

  public String getTlsCert() {
    return this.tlsCert;
  }

  public void setTlsCert(String cert) {
    this.tlsCert = cert;
  }

  public String getTlsKey() {
    return this.tlsKey;
  }

  public void setTlsKey(String key) {
    this.tlsKey = key;
  }

  public ArrayList<WebserverHandler> getHandlers() {
    return this.webserverHandlers;
  }

  public void setHandlers(ArrayList<WebserverHandler> handlers) {
    this.webserverHandlers = handlers;
  }

  public boolean handlerExists(String path, String method) {
    return this.webserverHandlers.stream()
        .anyMatch(handler -> handler.getPath().equals(path) && handler.getMethod().equals(method));
  }

  public WebserverHandler getHandlerForPos(BlockPos pos) {
    return this.webserverHandlers.stream()
        .filter(handler -> handler.getBlockPos().equals(pos))
        .findFirst()
        .orElse(new WebserverHandler(pos));
  }

  public void removeHandler(BlockPos pos) {
    this.webserverHandlers
        .removeIf(h -> h.getBlockPos().equals(pos));
  }

  public void updateHandler(WebserverHandler handler) {
    // remove or update the handler in the list
    this.webserverHandlers.removeIf(h -> h.getBlockPos().equals(handler.getBlockPos()));
    this.webserverHandlers.add(handler);
  }
}