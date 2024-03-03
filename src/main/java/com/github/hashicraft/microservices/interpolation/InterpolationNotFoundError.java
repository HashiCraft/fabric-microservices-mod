package com.github.hashicraft.microservices.interpolation;

public class InterpolationNotFoundError extends Exception {
  public InterpolationNotFoundError(String message) {
    super(message);
  }
}
