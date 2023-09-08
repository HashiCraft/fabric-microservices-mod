package com.github.hashicraft.microservices;

public class Client {
  private static boolean client = false;

  public static void setClient(boolean value) {
    client = value;
  }

  public static boolean isClient() {
    return client;
  }
}
