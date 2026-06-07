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
package org.pageseeder.berlioz.bundler;

import java.io.File;
import java.nio.file.Path;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for the web bundle tool.
 */
final class WebBundleToolTest {

  @TempDir
  Path temporary;

  @Test
  void testConstructorRequiresDirectory() throws IOException {
    File missing = new File(this.temporary.toFile(), "missing");
    File file = Files.createFile(this.temporary.resolve("not-a-directory")).toFile();

    Assertions.assertThrows(NullPointerException.class, () -> new WebBundleTool(null));
    Assertions.assertThrows(IllegalArgumentException.class, () -> new WebBundleTool(missing));
    Assertions.assertThrows(IllegalArgumentException.class, () -> new WebBundleTool(file));
  }

  @Test
  void testGetBundlesDir() throws IOException {
    File bundles = Files.createDirectory(this.temporary.resolve("bundles")).toFile();

    WebBundleTool tool = new WebBundleTool(bundles);

    Assertions.assertEquals(bundles, tool.getBundlesDir());
  }

  @Test
  void testEmptyFileListsReturnNull() throws IOException {
    WebBundleTool tool = new WebBundleTool(Files.createDirectory(this.temporary.resolve("empty")).toFile());

    Assertions.assertNull(tool.getBundle(List.of(), "empty", false));
    Assertions.assertNull(tool.bundle(List.of(), "empty", false));
    Assertions.assertNull(tool.bundle(List.of(), "empty", BundleType.JS, false));
    Assertions.assertNull(tool.bundleScripts(List.of(), "empty", false));
    Assertions.assertNull(tool.bundleStyles(List.of(), "empty", false));
  }

  @Test
  void testGetBundleDoesNotCreateFile() throws IOException {
    File bundles = Files.createDirectory(this.temporary.resolve("get-bundle")).toFile();
    File script = writeFile("scripts/app.js", "var answer = 42;");
    WebBundleTool tool = new WebBundleTool(bundles);

    File bundle = tool.getBundle(List.of(script), "app", false);

    Assertions.assertNotNull(bundle);
    Assertions.assertFalse(bundle.exists());
    Assertions.assertTrue(bundle.getName().startsWith("app-"));
    Assertions.assertTrue(bundle.getName().endsWith(".js"));
  }

  @Test
  void testBundleScripts() throws IOException {
    WebBundleTool tool = new WebBundleTool(Files.createDirectory(this.temporary.resolve("scripts")).toFile());
    File first = writeFile("js/first.js", "var first = 1;");
    File second = writeFile("js/second.js", "var second = 2;");

    File bundle = tool.bundleScripts(List.of(first, second), "app", false);

    Assertions.assertNotNull(bundle);
    Assertions.assertTrue(bundle.exists());
    Assertions.assertEquals("var first = 1;\nvar second = 2;\n", read(bundle));
  }

  @Test
  void testBundleDetectsJavaScriptType() throws IOException {
    WebBundleTool tool = new WebBundleTool(Files.createDirectory(this.temporary.resolve("detected-js")).toFile());
    File script = writeFile("detected/app.js", "var detected = true;");

    File bundle = tool.bundle(List.of(script), "detected", false);

    Assertions.assertNotNull(bundle);
    Assertions.assertTrue(bundle.exists());
    Assertions.assertTrue(bundle.getName().endsWith(".js"));
  }

  @Test
  void testBundleWithExplicitCssTypeExpandsImportsAndUrls() throws IOException {
    File root = Files.createDirectory(this.temporary.resolve("web")).toFile();
    File bundles = mkdir(root, "style/_");
    WebBundleTool tool = new WebBundleTool(bundles);
    tool.setRoot(root);
    tool.setVirtual(bundles);
    tool.setDataURIThreshold(0);

    File image = writeFile(root, "img/logo.png", "image");
    File imported = writeFile(root, "style/parts/extra.css", ".extra { background: url('../../img/logo.png'); }");
    File style = writeFile(root, "style/main.css",
        "@import url('parts/extra.css');\n"
      + ".main { background: url('../img/logo.png?rev=1'); }");

    File bundle = tool.bundle(List.of(style), "theme", BundleType.CSS, false);
    String css = read(bundle);

    Assertions.assertTrue(image.exists());
    Assertions.assertTrue(imported.exists());
    Assertions.assertTrue(css.contains("/* START import parts/extra.css */"));
    Assertions.assertTrue(css.contains(".extra { background: url(../../img/logo.png); }"));
    Assertions.assertTrue(css.contains("/* END import parts/extra.css */"));
    Assertions.assertTrue(css.contains(".main { background: url(../../img/logo.png?rev=1); }"));
  }

  @Test
  void testBundleStylesCanEmbedSmallImagesAsDataUris() throws IOException {
    File root = Files.createDirectory(this.temporary.resolve("data-uri-web")).toFile();
    File bundles = mkdir(root, "style/_");
    WebBundleTool tool = new WebBundleTool(bundles);
    tool.setRoot(root);
    tool.setDataURIThreshold(1024);
    writeFile(root, "img/logo.png", "image");
    File style = writeFile(root, "style/main.css", ".main { background: url('../img/logo.png'); }");

    File bundle = tool.bundleStyles(List.of(style), "theme", false);

    Assertions.assertTrue(read(bundle).contains("url(data:image/png;base64,"));
  }

  @Test
  void testBundleStylesDoesNotExpandImportsOutsideRoot() throws IOException {
    File root = Files.createDirectory(this.temporary.resolve("import-root")).toFile();
    File bundles = mkdir(root, "style/_");
    WebBundleTool tool = new WebBundleTool(bundles);
    tool.setRoot(root);
    File outside = writeFile(this.temporary.toFile(), "outside.css", ".secret { color: red; }");
    File style = writeFile(root, "style/main.css", "@import url('../../outside.css');\n.main { color: black; }");

    File bundle = tool.bundleStyles(List.of(style), "theme", false);
    String css = read(bundle);

    Assertions.assertTrue(outside.exists());
    Assertions.assertFalse(css.contains(".secret { color: red; }"));
    Assertions.assertFalse(css.contains("/* START import ../../outside.css */"));
    Assertions.assertTrue(css.contains("@import url('../../outside.css');"));
    Assertions.assertTrue(css.contains(".main { color: black; }"));
  }

  @Test
  void testBundleStylesDoesNotEmbedImagesOutsideRoot() throws IOException {
    File root = Files.createDirectory(this.temporary.resolve("url-root")).toFile();
    File bundles = mkdir(root, "style/_");
    WebBundleTool tool = new WebBundleTool(bundles);
    tool.setRoot(root);
    tool.setDataURIThreshold(1024);
    File outside = writeFile(this.temporary.toFile(), "outside.png", "image");
    File style = writeFile(root, "style/main.css", ".main { background: url('../../outside.png'); }");

    File bundle = tool.bundleStyles(List.of(style), "theme", false);
    String css = read(bundle);

    Assertions.assertTrue(outside.exists());
    Assertions.assertFalse(css.contains("url(data:image/png;base64,"));
    Assertions.assertTrue(css.contains(".main { background: url(../../outside.png); }"));
  }

  @Test
  void testBundleStylesCacheIncludesNameAndMinimizeFlag() throws IOException {
    WebBundleTool tool = new WebBundleTool(Files.createDirectory(this.temporary.resolve("cache")).toFile());
    File style = writeFile("cache/main.css", ".main { color: #123456; }");

    File plain = tool.bundleStyles(List.of(style), "plain", false);
    File minimized = tool.bundleStyles(List.of(style), "minimized", true);

    Assertions.assertNotNull(plain);
    Assertions.assertNotNull(minimized);
    Assertions.assertTrue(plain.getName().startsWith("plain-"));
    Assertions.assertFalse(plain.getName().contains(".min.css"));
    Assertions.assertTrue(minimized.getName().startsWith("minimized-"));
    Assertions.assertTrue(minimized.getName().contains(".min.css"));
  }

  @Test
  void testBundleStylesCanBeCalledConcurrently() throws Exception {
    WebBundleTool tool = new WebBundleTool(Files.createDirectory(this.temporary.resolve("concurrent")).toFile());
    File imported = writeFile("concurrent-css/imported.css", ".imported { color: green; }");
    File style = writeFile("concurrent-css/main.css", "@import url('imported.css');\n.main { color: black; }");
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(8);
    List<Future<File>> futures = new ArrayList<>();
    for (int i = 0; i < 8; i++) {
      futures.add(executor.submit(() -> {
        start.await();
        return tool.bundleStyles(List.of(style), "screen", false);
      }));
    }

    start.countDown();
    File first = futures.get(0).get();
    for (Future<File> future : futures) {
      File bundle = future.get();
      Assertions.assertEquals(first, bundle);
      Assertions.assertTrue(bundle.exists());
      Assertions.assertTrue(read(bundle).contains(".imported { color: green; }"));
    }
    Assertions.assertTrue(imported.exists());
    executor.shutdownNow();
  }

  private File writeFile(String path, String value) throws IOException {
    return writeFile(this.temporary.toFile(), path, value);
  }

  private static File writeFile(File root, String path, String value) throws IOException {
    File file = new File(root, path);
    Files.createDirectories(file.getParentFile().toPath());
    Files.writeString(file.toPath(), value, StandardCharsets.UTF_8);
    return file;
  }

  private static File mkdir(File root, String path) throws IOException {
    File folder = new File(root, path);
    Files.createDirectories(folder.toPath());
    return folder;
  }

  private static String read(File file) throws IOException {
    return Files.readString(file.toPath(), StandardCharsets.UTF_8);
  }
}
