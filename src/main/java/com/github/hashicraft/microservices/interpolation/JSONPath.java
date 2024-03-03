package com.github.hashicraft.microservices.interpolation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;

public class JSONPath {

  // This function attempts to extract a value from a JSON string using a JSONPath

  // If no substitution is found, the original value is returned.
  public static String getValue(String value) throws InterpolationNotFoundError {
    return replaceInString(value);
  }

  private static String replaceInString(String in) throws InterpolationNotFoundError {
    if (in == null || in.isEmpty()) {
      return in;
    }

    Pattern pattern = Pattern.compile("(\\$\\{\\{json_path\\([\"'](.*?)[\"'],[ ]?[\"'](.*)[\"']\\)\\}\\})");
    Matcher matcher = pattern.matcher(in);

    String out = in;

    // check all occurance
    while (matcher.find()) {
      // expr is the full match ${{json_path("[name]"}}
      // and the text to be replaced with the json value
      String expr = matcher.group(1);

      // name is the name part of ${{json_path("[name]"}}
      // and should match an item in projectorEnv
      String json = matcher.group(2);

      // path is the path part of ${{json_path("[name]"}}
      // and should match an item in projectorEnv
      String path = matcher.group(3);

      // recursively interpolate the json and path
      json = Interpolate.getValue(json);
      path = Interpolate.getValue(path);

      try {
        ReadContext ctx = JsonPath.parse(json);
        if (ctx == null) {
          throw new InterpolationNotFoundError("Unable to parse JSON " + json);
        }

        String value = ctx.read(path);
        if (value == null || value.isEmpty()) {
          throw new InterpolationNotFoundError("No value found for " + path + " in " + json);
        }

        out = out.replace(expr, value);
      } catch (Exception e) {
        throw new InterpolationNotFoundError("Error processing JSON " + json);
      }
    }

    return out;
  }
}