package com.github.hashicraft.microservices.interpolation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.nbt.NbtCompound;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Interpolate {
  static Logger LOGGER = LoggerFactory.getLogger(Interpolate.class);

  public static String getValue(String value) {
    return getValue(value, null);
  }

  // This function calls the other interpolation functions like env and file in
  // sequence to interpolate a string.
  //
  // i.e. if the input is "Hello, ${{env('test_var')}}!" and the environment has
  // MICROSERVICES_test_var set to "Nic", the output will be "Hello, Nic!"
  //
  // Interpolation can also be recursive, i.e. if the input is
  // "${{file('${{env('test_file')}}')}}",
  // the function will first interpolate the env function and then the file
  // function.
  public static String getValue(String value, NbtCompound data) {
    if (value == null || value.isEmpty()) {
      return "";
    }

    Pattern pattern = Pattern.compile("(\\$\\{\\{(.*?)\\((.*)\\}\\})");
    Matcher matcher = pattern.matcher(value);

    // not an interpolation function return the value
    if (!matcher.find()) {
      return value;
    }

    // get the name of the interpolation function
    String func = matcher.group(2);

    switch (func) {
      case "env":
        try {
          return Env.getValue(value);
        } catch (InterpolationNotFoundError e) {
          LOGGER.error("Error interpolating value: {}, error: {}", value, e.getMessage());
        }
      case "request_path":
        try {
          return Request.getPath(value, data);
        } catch (InterpolationNotFoundError e) {
          LOGGER.error("Error interpolating value: {}, error: {}", value, e.getMessage());
        }
      case "request_method":
        try {
          return Request.getMethod(value, data);
        } catch (InterpolationNotFoundError e) {
          LOGGER.error("Error interpolating value: {}, error: {}", value, e.getMessage());
        }
      case "request_data":
        try {
          return Request.getData(value, data);
        } catch (InterpolationNotFoundError e) {
          LOGGER.error("Error interpolating value: {}, error: {}", value, e.getMessage());
        }
      case "request_query":
        try {
          return Request.getQuery(value, data);
        } catch (InterpolationNotFoundError e) {
          LOGGER.error("Error interpolating value: {}, error: {}", value, e.getMessage());
        }
      case "file":
        try {
          return File.getValue(value);
        } catch (InterpolationNotFoundError e) {
          LOGGER.error("Error interpolating value: {}, error: {}", value, e.getMessage());
        }
      case "json_path":
        try {
          return JSONPath.getValue(value);
        } catch (InterpolationNotFoundError e) {
          LOGGER.error("Error interpolating value: {}, error: {}", value, e.getMessage());
        }
      default:
        LOGGER.error("Unknown interpolation function: {}", func);
        return value;
    }
  }
}