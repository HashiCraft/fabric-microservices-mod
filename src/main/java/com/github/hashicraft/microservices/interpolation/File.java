package com.github.hashicraft.microservices.interpolation;

import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class File {
  // This function attempts to replace the given value with a value from a
  // file, the substitution syntax allows the user to specify the key as
  // "${{file("myfile.txt")}}" in
  // their strings.
  // If no substitution is found, the original value is returned.
  public static String getValue(String value) throws InterpolationNotFoundError {
    return replaceInString(value);
  }

  private static String replaceInString(String in) throws InterpolationNotFoundError {
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

      // recursively interpolate the name
      name = Interpolate.getValue(name);

      // check if the file exists
      var file = new java.io.File(name);
      if (!file.exists()) {
        throw new InterpolationNotFoundError("File does not exist" + file.toPath());
      }

      // read contents of file
      String replacement;
      try {
        replacement = Files.readString(file.toPath());
      } catch (IOException e) {
        throw new InterpolationNotFoundError("Error reading file " + file.toPath());
      }

      if (replacement != null && !replacement.isEmpty()) {
        out = out.replace(expr, replacement);
      } else {
        throw new InterpolationNotFoundError("File empty " + file.toPath());
      }
    }

    return out;
  }
}