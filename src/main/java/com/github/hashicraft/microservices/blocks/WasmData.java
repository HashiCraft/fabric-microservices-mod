package com.github.hashicraft.microservices.blocks;

public class WasmData {
  private String request_id;
  private String data;
  private String request_method;
  private String request_path;
  private java.util.Map<String, String> request_query;
  private int error_code;

  public String getRequestID() {
    return request_id;
  }

  public void setRequestID(String request_id) {
    this.request_id = request_id;
  }

  public String getData() {
    return data;
  }

  public void setData(String data) {
    this.data = data;
  }

  public String getRequest_method() {
    return request_method;
  }

  public void setRequest_method(String method) {
    this.request_method = method;
  }

  public String getRequest_path() {
    return request_path;
  }

  public void setRequest_path(String path) {
    this.request_path = path;
  }

  public java.util.Map<String, String> getRequest_query() {
    return request_query;
  }

  public void setRequest_query(java.util.Map<String, String> query) {
    this.request_query = query;
  }

  public int getErrorCode() {
    return error_code;
  }

  public void setErrorCode(int error_code) {
    this.error_code = error_code;
  }

  public String toJson() {
    com.google.gson.Gson gson = new com.google.gson.Gson();
    return gson.toJson(this);
  }

  public static WasmData fromJson(String json) {
    com.google.gson.Gson gson = new com.google.gson.Gson();
    return gson.fromJson(json, WasmData.class);
  }
}
