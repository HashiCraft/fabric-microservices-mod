package com.github.hashicraft.microservices.blocks;

import com.google.gson.annotations.Expose;

public class DatabaseContext {
  private boolean active;

  @Expose
  private String address;

  @Expose
  private String username;

  @Expose
  private String password;

  @Expose
  private String database;

  @Expose
  private String sql;

  // public getters and setters
  public boolean getActive() {
    return this.active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getUsername() {
    return this.username;
  }

  public void setUsername(String user) {
    this.username = user;
  }

  public String getPassword() {
    return this.password;
  }

  public void setPassword(String pass) {
    this.password = pass;
  }

  public String getDatabase() {
    return this.database;
  }

  public void setDatabase(String db) {
    this.database = db;
  }

  public String getSQL() {
    return this.sql;
  }

  public void setSQL(String sql) {
    this.sql = sql;
  }

  public DatabaseContext() {
  }
}
