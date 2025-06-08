package com.github.hashicraft.microservices.interpolation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.nbt.NbtCompound;

public class Request {
  public static String getPath(String value, NbtCompound data) throws InterpolationNotFoundError {
    if (data == null || !data.contains("request_path")) {
      throw new InterpolationNotFoundError("Request data is null or does not contain 'request_path'");
    }

    return replaceSimpleInString("request_path", "request_path", value, data);
  }

  public static String getMethod(String value, NbtCompound data) throws InterpolationNotFoundError {
    if (data == null || !data.contains("request_method")) {
      throw new InterpolationNotFoundError("Request data is null or does not contain 'request_method'");
    }

    return replaceSimpleInString("request_method", "request_method", value, data);
  }

  public static String getData(String value, NbtCompound data) throws InterpolationNotFoundError {
    if (data == null || !data.contains("data")) {
      throw new InterpolationNotFoundError("Request data is null or does not contain 'data'");
    }

    return replaceSimpleInString("request_data", "data", value, data);
  }

  public static String getQuery(String value, NbtCompound data) throws InterpolationNotFoundError {
    if (data == null || !data.contains("request_query")) {
      throw new InterpolationNotFoundError("Request data is null or does not contain 'request_query'");
    }

    return replaceQueryInString(value, data);
  }

  private static String replaceSimpleInString(String match, String key, String in, NbtCompound data)
      throws InterpolationNotFoundError {
    if (in == null || in.isEmpty()) {
      return in;
    }

    Pattern pattern = Pattern.compile("(\\$\\{\\{" + match + "\\(\\)\\}\\})");
    Matcher matcher = pattern.matcher(in);

    String out = in;

    String path = data.getString(key, "");
    if (path.isEmpty()) {
      return out;
    }

    // check all occurances
    while (matcher.find()) {
      // expr is the full match ${{request_path}}
      String expr = matcher.group(1);
      out = out.replace(expr, path);
    }

    return out;
  }

  private static String replaceQueryInString(String in, NbtCompound data) throws InterpolationNotFoundError {
    if (in == null || in.isEmpty()) {
      return in;
    }

    Pattern pattern = Pattern.compile("(\\$\\{\\{request_query\\([\"'](.*)[\"']\\)\\}\\})");
    Matcher matcher = pattern.matcher(in);

    NbtCompound queryData = data.getCompound("request_query").get();

    String out = in;

    // check all occurances
    while (matcher.find()) {
      // expr is the full match ${{env("[name]"}}
      // and the text to be replaced with the env var
      String expr = matcher.group(1);

      // name is the name part of ${{env("[name]"}}
      // and should match an item in projectorEnv
      String name = matcher.group(2);

      // name could be an interpolated function
      name = Interpolate.getValue(name);

      String replacement = queryData.getString(name, "");
      if (replacement != null && !replacement.isEmpty()) {
        out = out.replace(expr, replacement);
      } else {
        throw new InterpolationNotFoundError("Query variable not set " + name);
      }
    }

    return out;
  }

}
