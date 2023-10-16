package com.github.hashicraft.microservices.blocks;

import com.google.gson.annotations.Expose;

import io.javalin.Javalin;

public class WebServerContext {
  @Expose
  private String serverPort;

  @Expose
  private String serverPath;

  @Expose
  private String serverMethod;

  // reference to the server, this will not be serialized
  private Javalin server;

  // getters and setters for private methods
  public String getServerPort() {
    return serverPort;
  }

  public void setServerPort(String port) {
    this.serverPort = port;
  }

  public String getServerPath() {
    return this.serverPath;
  }

  public void setServerPath(String path) {
    this.serverPath = path;
  }

  public String getServerMethod() {
    return this.serverMethod;
  }

  public void setServerMethod(String method) {
    this.serverMethod = method;
  }

  public Javalin getServer() {
    return this.server;
  }

  public void setServer(Javalin server) {
    this.server = server;
  }

  public WebServerContext() {
  }
}