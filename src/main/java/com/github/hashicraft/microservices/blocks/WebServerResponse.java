package com.github.hashicraft.microservices.blocks;

public class WebServerResponse {
  private String data;
  private int status_code;

  public WebServerResponse(String data, int status_code) {
    this.data = data;
    this.status_code = status_code;
  }

  public String getData() {
    return data;
  }

  public void setData(String data) {
    this.data = data;
  }

  public int getStatusCode() {
    return status_code;
  }

  public void setStatusCode(int status_code) {
    this.status_code = status_code;
  }
}
