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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests for the web bundle tool.
 */
public final class WebBundleToolTest {

  @Rule
  public TemporaryFolder temporary = new TemporaryFolder();

  @Test
  public void testConstructorRequiresDirectory() throws IOException {
    File missing = new File(this.temporary.getRoot(), "missing");
    File file = this.temporary.newFile("not-a-directory");

    Assert.assertThrows(IllegalArgumentException.class, () -> new WebBundleTool(null));
    Assert.assertThrows(IllegalArgumentException.class, () -> new WebBundleTool(missing));
    Assert.assertThrows(IllegalArgumentException.class, () -> new WebBundleTool(file));
  }

  @Test
  public void testGetBundlesDir() throws IOException {
    File bundles = this.temporary.newFolder("bundles");

    WebBundleTool tool = new WebBundleTool(bundles);

    Assert.assertEquals(bundles, tool.getBundlesDir());
  }

  @Test
  public void testEmptyFileListsReturnNull() throws IOException {
    WebBundleTool tool = new WebBundleTool(this.temporary.newFolder("empty"));

    Assert.assertNull(tool.getBundle(List.of(), "empty", false));
    Assert.assertNull(tool.bundle(List.of(), "empty", false));
    Assert.assertNull(tool.bundle(List.of(), "empty", BundleType.JS, false));
    Assert.assertNull(tool.bundleScripts(List.of(), "empty", false));
    Assert.assertNull(tool.bundleStyles(List.of(), "empty", false));
  }

  @Test
  public void testGetBundleDoesNotCreateFile() throws IOException {
    File bundles = this.temporary.newFolder("get-bundle");
    File script = writeFile("scripts/app.js", "var answer = 42;");
    WebBundleTool tool = new WebBundleTool(bundles);

    File bundle = tool.getBundle(List.of(script), "app", false);

    Assert.assertNotNull(bundle);
    Assert.assertFalse(bundle.exists());
    Assert.assertTrue(bundle.getName().startsWith("app-"));
    Assert.assertTrue(bundle.getName().endsWith(".js"));
  }

  @Test
  public void testBundleScripts() throws IOException {
    WebBundleTool tool = new WebBundleTool(this.temporary.newFolder("scripts"));
    File first = writeFile("js/first.js", "var first = 1;");
    File second = writeFile("js/second.js", "var second = 2;");

    File bundle = tool.bundleScripts(List.of(first, second), "app", false);

    Assert.assertNotNull(bundle);
    Assert.assertTrue(bundle.exists());
    Assert.assertEquals("var first = 1;\nvar second = 2;\n", read(bundle));
  }

  @Test
  public void testBundleDetectsJavaScriptType() throws IOException {
    WebBundleTool tool = new WebBundleTool(this.temporary.newFolder("detected-js"));
    File script = writeFile("detected/app.js", "var detected = true;");

    File bundle = tool.bundle(List.of(script), "detected", false);

    Assert.assertNotNull(bundle);
    Assert.assertTrue(bundle.exists());
    Assert.assertTrue(bundle.getName().endsWith(".js"));
  }

  @Test
  public void testBundleWithExplicitCssTypeExpandsImportsAndUrls() throws IOException {
    File root = this.temporary.newFolder("web");
    File bundles = mkdir(root, "style/_");
    WebBundleTool tool = new WebBundleTool(bundles);
    tool.setVirtual(bundles);
    tool.setDataURIThreshold(0);

    File image = writeFile(root, "img/logo.png", "image");
    File imported = writeFile(root, "style/parts/extra.css", ".extra { background: url('../../img/logo.png'); }");
    File style = writeFile(root, "style/main.css",
        "@import url('parts/extra.css');\n"
      + ".main { background: url('../img/logo.png?rev=1'); }");

    File bundle = tool.bundle(List.of(style), "theme", BundleType.CSS, false);
    String css = read(bundle);

    Assert.assertTrue(image.exists());
    Assert.assertTrue(imported.exists());
    Assert.assertTrue(css.contains("/* START import parts/extra.css */"));
    Assert.assertTrue(css.contains(".extra { background: url(../../img/logo.png); }"));
    Assert.assertTrue(css.contains("/* END import parts/extra.css */"));
    Assert.assertTrue(css.contains(".main { background: url(../../img/logo.png?rev=1); }"));
  }

  @Test
  public void testBundleStylesCanEmbedSmallImagesAsDataUris() throws IOException {
    File root = this.temporary.newFolder("data-uri-web");
    File bundles = mkdir(root, "style/_");
    WebBundleTool tool = new WebBundleTool(bundles);
    tool.setDataURIThreshold(1024);
    writeFile(root, "img/logo.png", "image");
    File style = writeFile(root, "style/main.css", ".main { background: url('../img/logo.png'); }");

    File bundle = tool.bundleStyles(List.of(style), "theme", false);

    Assert.assertTrue(read(bundle).contains("url(data:image/png;base64,"));
  }

  @Test
  public void testBundleStylesCacheIncludesNameAndMinimizeFlag() throws IOException {
    WebBundleTool tool = new WebBundleTool(this.temporary.newFolder("cache"));
    File style = writeFile("cache/main.css", ".main { color: #123456; }");

    File plain = tool.bundleStyles(List.of(style), "plain", false);
    File minimized = tool.bundleStyles(List.of(style), "minimized", true);

    Assert.assertNotNull(plain);
    Assert.assertNotNull(minimized);
    Assert.assertTrue(plain.getName().startsWith("plain-"));
    Assert.assertFalse(plain.getName().contains(".min.css"));
    Assert.assertTrue(minimized.getName().startsWith("minimized-"));
    Assert.assertTrue(minimized.getName().contains(".min.css"));
  }

  @Test
  public void testBundleStylesCanBeCalledConcurrently() throws Exception {
    WebBundleTool tool = new WebBundleTool(this.temporary.newFolder("concurrent"));
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
      Assert.assertEquals(first, bundle);
      Assert.assertTrue(bundle.exists());
      Assert.assertTrue(read(bundle).contains(".imported { color: green; }"));
    }
    Assert.assertTrue(imported.exists());
    executor.shutdownNow();
  }

  private File writeFile(String path, String value) throws IOException {
    return writeFile(this.temporary.getRoot(), path, value);
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
