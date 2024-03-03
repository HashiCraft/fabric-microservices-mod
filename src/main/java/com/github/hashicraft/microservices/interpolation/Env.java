package com.github.hashicraft.microservices.interpolation;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.hashicraft.microservices.MicroservicesMod;

public class Env {
  // This function attempts to replace the given value with a value from the
  // environment. Environment variables must be named "MICROSERVICES_key", the
  // substitution syntax allows the user to specify the key as "${{env.key}}" in
  // their strings.
  //
  // If no environment substitution is found, an InterpolationNotFoundError is
  // thrown.
  public static String getValue(String value) throws InterpolationNotFoundError {
    return replaceInString(value);
  }

  private static String replaceInString(String in) throws InterpolationNotFoundError {
    if (in == null || in.isEmpty()) {
      return in;
    }

    Pattern pattern = Pattern.compile("(\\$\\{\\{env\\([\"'](.*)[\"']\\)\\}\\})");
    Matcher matcher = pattern.matcher(in);

    Map<String, String> env = System.getenv();

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

      String envName = MicroservicesMod.MODID.toUpperCase(Locale.ENGLISH) + "_" + name;
      String replacement = env.get(envName);
      if (replacement != null && !replacement.isEmpty()) {
        out = out.replace(expr, replacement);
      } else {
        throw new InterpolationNotFoundError("Environment variable not set " + envName);
      }
    }

    return out;
  }
}