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
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.InitEnvironment;

/**
 * Regression tests for SEC-01: an arbitrary {@code config} request parameter must not be able to
 * grow {@link GetWebBundles#JS_CONFIGS}/{@link GetWebBundles#CSS_CONFIGS} without bound.
 */
final class GetWebBundlesTest {

  @TempDir
  Path temporary;

  @Test
  void testUnconfiguredNamesDoNotGrowCache() throws IOException {
    setupGlobalSettings("<global/>");
    File root = Files.createDirectory(this.temporary.resolve("public-unbounded")).toFile();
    GetWebBundles generator = new GetWebBundles();

    // Seed the "default" entry so the loop below can only reuse it, never grow the cache further.
    generator.getConfig(BundleConfig.DEFAULT_CONFIG, BundleType.JS, root);
    int before = GetWebBundles.JS_CONFIGS.size();
    for (int i = 0; i < 1000; i++) {
      generator.getConfig("attacker-" + i, BundleType.JS, root);
    }

    Assertions.assertEquals(before, GetWebBundles.JS_CONFIGS.size());
    Assertions.assertTrue(GetWebBundles.JS_CONFIGS.containsKey(BundleConfig.DEFAULT_CONFIG));
  }

  @Test
  void testConfiguredNameGetsItsOwnCacheEntry() throws IOException {
    setupGlobalSettings(
        "<global>"
      + "  <berlioz>"
      + "    <jsbundler>"
      + "      <configs custom=\"group\"/>"
      + "    </jsbundler>"
      + "  </berlioz>"
      + "</global>");
    File root = Files.createDirectory(this.temporary.resolve("public-custom")).toFile();
    GetWebBundles generator = new GetWebBundles();

    BundleConfig config = generator.getConfig("custom", BundleType.JS, root);

    Assertions.assertSame(config, GetWebBundles.JS_CONFIGS.get("custom"));
  }

  @Test
  void testUnconfiguredNameResolvesToDefaultBundleDefinitions() throws IOException {
    setupGlobalSettings("<global/>");
    File root = Files.createDirectory(this.temporary.resolve("public-fallback")).toFile();
    GetWebBundles generator = new GetWebBundles();

    BundleConfig fromDefault = generator.getConfig(BundleConfig.DEFAULT_CONFIG, BundleType.JS, root);
    BundleConfig fromUnknown = generator.getConfig("not-declared-anywhere", BundleType.JS, root);

    Assertions.assertSame(fromDefault, fromUnknown);
  }

  private void setupGlobalSettings(String xml) throws IOException {
    File webinf = this.temporary.resolve("WEB-INF").toFile();
    File config = new File(webinf, "config");
    Files.createDirectories(config.toPath());
    Files.writeString(new File(config, "config.xml").toPath(), xml, StandardCharsets.UTF_8);
    GlobalSettings.setup(InitEnvironment.create(webinf).mode("default"));
    GlobalSettings.load();
  }
}
