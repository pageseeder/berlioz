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
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.InitEnvironment;

/**
 * Tests for bundle configuration loading.
 */
final class BundleConfigTest {

  @TempDir
  Path temporary;

  @Test
  void testDefaultConfigIncludesServiceBundle() throws IOException {
    setupGlobalSettings("<global/>");
    File root = Files.createDirectory(this.temporary.resolve("public-default")).toFile();

    BundleConfig config = BundleConfig.newInstance("default", BundleType.JS, root);

    Assertions.assertEquals(List.of("global", "group", "service"), names(config.definitions()));
  }

  @Test
  void testConfigNamesAreTrimmed() throws IOException {
    setupGlobalSettings(
        "<global>"
      + "  <berlioz>"
      + "    <jsbundler>"
      + "      <configs spaced=\" global, group , service, \"/>"
      + "    </jsbundler>"
      + "  </berlioz>"
      + "</global>");
    File root = Files.createDirectory(this.temporary.resolve("public-spaced")).toFile();

    BundleConfig config = BundleConfig.newInstance("spaced", BundleType.JS, root);

    Assertions.assertEquals(List.of("global", "group", "service"), names(config.definitions()));
  }

  @Test
  void testIncludeOverrideKeepsDefaultFilename() throws IOException {
    setupGlobalSettings(
        "<global>"
      + "  <berlioz>"
      + "    <jsbundler>"
      + "      <configs custom=\"group\"/>"
      + "      <bundles>"
      + "        <group include=\"/script/{GROUP}/extra.js\"/>"
      + "      </bundles>"
      + "    </jsbundler>"
      + "  </berlioz>"
      + "</global>");
    File root = Files.createDirectory(this.temporary.resolve("public-override")).toFile();

    BundleConfig config = BundleConfig.newInstance("custom", BundleType.JS, root);

    Assertions.assertEquals(1, config.definitions().size());
    BundleDefinition definition = config.definitions().get(0);
    Assertions.assertEquals(definition.name(), "group");
    Assertions.assertEquals(definition.filename(), "{GROUP}");
    Assertions.assertArrayEquals(new String[]{"/script/{GROUP}/extra.js"}, definition.paths());
  }

  private void setupGlobalSettings(String xml) throws IOException {
    File webinf = Files.createDirectory(this.temporary.resolve("WEB-INF")).toFile();
    File config = new File(webinf, "config");
    Files.createDirectories(config.toPath());
    Files.writeString(new File(config, "config.xml").toPath(), xml, StandardCharsets.UTF_8);
    GlobalSettings.setup(InitEnvironment.create(webinf).mode("default"));
    GlobalSettings.load();
  }

  private static List<String> names(List<BundleDefinition> definitions) {
    return definitions.stream().map(BundleDefinition::name).collect(Collectors.toList());
  }
}
