package com.github.hashicraft.microservices.blocks;

import com.google.gson.annotations.Expose;

import io.javalin.Javalin;

public class WebserverContext {
  @Expose
  private String port;

  @Expose
  private String path;

  @Expose
  private String method;

  @Expose
  private String tlsCert;

  @Expose
  private String tlsKey;

  // reference to the server, this will not be serialized
  private Javalin server;

  // getters and setters for private methods
  public String getPort() {
    return port;
  }

  public void setPort(String port) {
    this.port = port;
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

  public Javalin getServer() {
    return this.server;
  }

  public void setServer(Javalin server) {
    this.server = server;
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

  public WebserverContext() {
  }
}