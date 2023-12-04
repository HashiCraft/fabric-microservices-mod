package com.github.hashicraft.microservices.interpolation;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.hashicraft.microservices.MicroservicesMod;

public class Env {
  // This function attempts to replace the given value with a value from the
  // environment. Environment variables must be named "HASHICRAFT_key", the
  // substitution syntax allows the user to specify the key as "${{env.key}}" in
  // their strings.
  //
  // If no environment substitution is found, the original value is returned.
  public static String getValue(String value) {
    return replaceInString(value);
  }

  private static String replaceInString(String in) {
    if (in == null || in.isEmpty()) {
      return in;
    }

    Pattern pattern = Pattern.compile("(\\$\\{\\{env\\.(.+?)\\}\\})");
    Matcher matcher = pattern.matcher(in);
    Map<String, String> env = System.getenv();

    String out = in;
    // check all occurance
    while (matcher.find()) {
      // expr is the full match ${{env.[name]}}
      // and the text to be replaced with the env var
      String expr = matcher.group(1);

      // name is the name part of ${{env.[name]}}
      // and should match an item in projectorEnv
      String name = matcher.group(2);

      try {
        String replacement = env.get(MicroservicesMod.MODID.toUpperCase(Locale.ENGLISH) + "_" + name);
        if (replacement != null && !replacement.isEmpty()) {
          out = out.replace(expr, replacement);
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }

    return "";
  }
}