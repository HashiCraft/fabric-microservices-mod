package com.github.hashicraft.microservices.interpolation;

import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class File {
  // This function attempts to replace the given value with a value from a
  // file, the substitution syntax allows the user to specify the key as
  // "${{file("myfile.txt")}}" in
  // their strings.
  // If no substitution is found, the original value is returned.
  public static String getValue(String value) {
    return replaceInString(value);
  }

  private static String replaceInString(String in) {
    if (in == null || in.isEmpty()) {
      return in;
    }

    Pattern pattern = Pattern.compile("(\\$\\{\\{file\\([\"'](.*)[\"']\\)\\}\\})");
    Matcher matcher = pattern.matcher(in);

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
        // check if the file exists
        var file = new java.io.File(name);
        if (!file.exists()) {
          return out;
        }

        // read contents of file
        String contents = Files.readString(file.toPath());

        if (contents != null && !contents.isEmpty()) {
          out = out.replace(expr, contents);
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }

    return out;
  }
}