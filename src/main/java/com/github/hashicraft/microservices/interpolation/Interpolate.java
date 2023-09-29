package com.github.hashicraft.microservices.interpolation;

public class Interpolate {
  // This function calls the other interpolation functions like env and file in
  // sequence to interpolate a string.
  public static String getValue(String value) {

    // first process environment variables
    var out = Env.getValue(value);

    // then files
    out = File.getValue(out);

    return out;
  }
}
