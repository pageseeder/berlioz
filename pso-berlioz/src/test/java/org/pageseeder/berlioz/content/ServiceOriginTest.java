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
package org.pageseeder.berlioz.content;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ServiceOriginTest {

  @Test
  void testForClasspathResource_jarUrl_displayNameIsJarNamePlusResourcePath() throws MalformedURLException {
    URL url = new URL("jar:file:/opt/apps/my-overlay.jar!/META-INF/berlioz/services.xml");
    ServiceOrigin origin = ServiceOrigin.forClasspathResource(url);

    Assertions.assertEquals(ServiceSourceKind.CLASSPATH, origin.kind());
    Assertions.assertEquals("my-overlay.jar!META-INF/berlioz/services.xml", origin.displayName());
    Assertions.assertEquals("META-INF/berlioz/services.xml", origin.resourcePath());
    Assertions.assertEquals(url, origin.url());
  }

  @Test
  void testForClasspathResource_explodedDirectory_displayNameIsResourcePathOnly() throws MalformedURLException {
    URL url = new URL("file:/opt/apps/target/classes/META-INF/berlioz/services.xml");
    ServiceOrigin origin = ServiceOrigin.forClasspathResource(url);

    Assertions.assertEquals("META-INF/berlioz/services.xml", origin.displayName());
    Assertions.assertFalse(origin.displayName().contains("/opt/apps"),
        "Display name must not leak the absolute deployment path");
  }

  @Test
  void testForFile_relativeToConfigDir() {
    File configDir = new File("/app/WEB-INF/config");
    File file = new File(configDir, "services!admin.xml");
    ServiceOrigin origin = ServiceOrigin.forFile(file, configDir);

    Assertions.assertEquals(ServiceSourceKind.FILESYSTEM, origin.kind());
    Assertions.assertEquals("services!admin.xml", origin.displayName());
    Assertions.assertFalse(origin.displayName().contains("/app/WEB-INF"),
        "Display name must not leak the absolute deployment path");
  }

  @Test
  void testForFile_noConfigDir_fallsBackToFileName() {
    File file = new File("/app/WEB-INF/config/services.xml");
    ServiceOrigin origin = ServiceOrigin.forFile(file, null);

    Assertions.assertEquals("services.xml", origin.displayName());
  }

  @Test
  void testEqualsAndHashCode_basedOnUrl() throws MalformedURLException {
    URL url = new URL("jar:file:/opt/apps/my-overlay.jar!/META-INF/berlioz/services.xml");
    ServiceOrigin a = ServiceOrigin.forClasspathResource(url);
    ServiceOrigin b = ServiceOrigin.forClasspathResource(url);

    Assertions.assertEquals(a, b);
    Assertions.assertEquals(a.hashCode(), b.hashCode());
  }

}
