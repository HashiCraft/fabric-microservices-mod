package com.github.hashicraft.microservices.interpolation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import net.minecraft.nbt.NbtCompound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;

@ExtendWith(SystemStubsExtension.class)
public class InterpolateTests {

  @SystemStub
  public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Test
  public void environmentVariablesWithoutPrefixAreIgnored() {
    environmentVariables.set("test_var", "Nic");

    Exception ex = assertThrows(InterpolationNotFoundError.class, () -> {
      Env.getValue("Hello, ${{env('test_var')}}!");
    });

    assertEquals("Environment variable not set MICROSERVICES_test_var", ex.getMessage());
  }

  @Test
  public void environmentVariablesAreInterpolated() throws InterpolationNotFoundError {
    environmentVariables.set("MICROSERVICES_test_var", "Nic");

    String result = Env.getValue("Hello, ${{env('test_var')}}!");

    assertEquals("Hello, Nic!", result);
  }

  @Test
  public void filesAreInterpolated() throws IOException, InterpolationNotFoundError {
    Path path = Files.createTempFile("inter", ".txt");
    Files.write(path, "Nic".getBytes());

    String result = File.getValue("Hello, ${{file('" + path.toString() + "')}}!");

    assertEquals("Hello, Nic!", result);
  }

  @Test
  public void jsonPathIsInterpolated() throws InterpolationNotFoundError {
    String result = JSONPath.getValue("Hello, ${{json_path('{\"name\": \"Nic\"}', '$.name')}}!");

    assertEquals("Hello, Nic!", result);
  }

  @Test
  public void environmentVariablesCanBeInterpolatedFromEnvironmentVariables() throws IOException {
    environmentVariables.set("MICROSERVICES_test_var1", "test_var2");
    environmentVariables.set("MICROSERVICES_test_var2", "Nic");

    String result = Interpolate.getValue("Hello, ${{env('${{env('test_var1')}}')}}!");

    assertEquals("Hello, Nic!", result);
  }

  @Test
  public void environmentVariablesAreInterpolatedBeforeFiles() throws IOException {
    Path path = Files.createTempFile("inter", ".txt");
    Files.write(path, "Nic".getBytes());
    environmentVariables.set("MICROSERVICES_test_var", path.toString());

    String result = Interpolate.getValue("Hello, ${{file('${{env('test_var')}}')}}!");

    assertEquals("Hello, Nic!", result);
  }

  @Test
  public void jsongPathAreInterpolatedAfterFiles() throws IOException {
    Path path = Files.createTempFile("inter", ".txt");
    Files.write(path, "{\"name\": \"Nic\"}".getBytes());

    String result = Interpolate.getValue("Hello, ${{json_path('${{file('" + path.toString() + "')}}','$.name')}}!");

    assertEquals("Hello, Nic!", result);
  }

  @Test
  void requestPathIsInterpolated() throws InterpolationNotFoundError {
    NbtCompound data = new NbtCompound();
    data.putString("request_path", "/abc/123");

    String result = Interpolate.getValue("request_path: ${{request_path()}}", data);

    // Assuming the request_path function returns "Nic" for the given URL
    assertEquals("request_path: /abc/123", result);
  }

  @Test
  void requestMethodIsInterpolated() throws InterpolationNotFoundError {
    NbtCompound data = new NbtCompound();
    data.putString("request_method", "GET");

    String result = Interpolate.getValue("request_method: ${{request_method()}}", data);

    // Assuming the request_path function returns "Nic" for the given URL
    assertEquals("request_method: GET", result);
  }

  @Test
  void requestDataIsInterpolated() throws InterpolationNotFoundError {
    NbtCompound data = new NbtCompound();
    data.putString("data", "hello, this is some data");

    String result = Interpolate.getValue("request_data: ${{request_data()}}", data);

    // Assuming the request_path function returns "Nic" for the given URL
    assertEquals("request_data: hello, this is some data", result);
  }

  @Test
  void requestQueryIsInterpolated() throws InterpolationNotFoundError {
    NbtCompound query = new NbtCompound();
    query.putString("testing", "hello, this is some data");

    NbtCompound data = new NbtCompound();
    data.put("request_query", query);

    String result = Interpolate.getValue("request_query: ${{request_query(\"testing\")}}", data);

    // Assuming the request_path function returns "Nic" for the given URL
    assertEquals("request_query: hello, this is some data", result);
  }
}
