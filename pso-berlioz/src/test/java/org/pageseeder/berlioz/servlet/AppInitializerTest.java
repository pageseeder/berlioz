/*
 * Copyright 2026 Allette Systems (Australia)
 * http://www.allette.com.au
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pageseeder.berlioz.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pageseeder.berlioz.InitEnvironment;

/**
 * Tests for {@link AppInitializer}'s service configuration check, specifically the 0.14.2 rule
 * that a missing filesystem {@code services.xml} is no longer reported as a failure when a
 * classpath-discovered overlay source is available.
 */
class AppInitializerTest {

  @Test
  void testCheckServices_filesystemServicesXml_reportsOk(@TempDir Path webInf) throws Exception {
    Files.createDirectories(webInf.resolve("config"));
    Files.writeString(webInf.resolve("config/services.xml"), "<service-config version=\"1.0\"/>");

    String output = invokeCheckServices(InitEnvironment.create(webInf.toFile()));

    Assertions.assertTrue(output.contains("Services: OK"), output);
    Assertions.assertFalse(output.contains("FAIL"), output);
  }

  @Test
  void testCheckServices_noFilesystemButClasspathSource_reportsOkAsClasspathProvided(@TempDir Path webInf,
      @TempDir Path classpathDir) throws Exception {
    Files.createDirectories(webInf.resolve("config"));
    Path metaInf = Files.createDirectories(classpathDir.resolve("META-INF/berlioz"));
    Files.writeString(metaInf.resolve("services.xml"), "<service-config version=\"1.0\"/>");

    ClassLoader original = Thread.currentThread().getContextClassLoader();
    try (URLClassLoader classpathLoader = new URLClassLoader(new URL[] {classpathDir.toUri().toURL()}, original)) {
      Thread.currentThread().setContextClassLoader(classpathLoader);
      String output = invokeCheckServices(InitEnvironment.create(webInf.toFile()));

      Assertions.assertTrue(output.contains("Services: OK"), output);
      Assertions.assertFalse(output.contains("FAIL"), output);
      Assertions.assertTrue(output.contains("classpath"), output);
    } finally {
      Thread.currentThread().setContextClassLoader(original);
    }
  }

  @Test
  void testCheckServices_neitherFilesystemNorClasspath_reportsFail(@TempDir Path webInf) throws Exception {
    Files.createDirectories(webInf.resolve("config"));

    String output = invokeCheckServices(InitEnvironment.create(webInf.toFile()));

    Assertions.assertTrue(output.contains("FAIL"), output);
  }

  private static String invokeCheckServices(InitEnvironment env)
      throws NoSuchMethodException, IllegalAccessException, IOException {
    Method method = AppInitializer.class.getDeclaredMethod("checkServices", InitEnvironment.class);
    method.setAccessible(true);
    PrintStream original = System.out;
    ByteArrayOutputStream captured = new ByteArrayOutputStream();
    System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
    try {
      method.invoke(null, env);
    } catch (InvocationTargetException ex) {
      throw new AssertionError(ex.getCause());
    } finally {
      System.setOut(original);
    }
    return captured.toString(StandardCharsets.UTF_8);
  }

}
