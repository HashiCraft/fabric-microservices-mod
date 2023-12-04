package com.github.hashicraft.microservices.interpolation;

public class Interpolate {
  // This function calls the other interpolation functions like env and file in
  // sequence to interpolate a string.
  public static String getValue(String value) {
    if (value == null || value.isEmpty()) {
      return "";
    }

    // first process environment variables
    var out = Env.getValue(value);
    if (!out.isEmpty()) {
      return out;
    }

    // no environment variable try processing files
    out = File.getValue(out);
    if (!out.isEmpty()) {
      return out;
    }

    // no substitution found return the original value
    return value;
  }
}
