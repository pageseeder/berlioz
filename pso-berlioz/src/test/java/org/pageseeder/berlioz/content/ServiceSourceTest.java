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

class ServiceSourceTest {

  @Test
  void testClasspath_kindAndOrderingKey() throws MalformedURLException {
    URL url = new URL("jar:file:/opt/apps/my-overlay.jar!/META-INF/berlioz/services.xml");
    ServiceSource source = ServiceSource.classpath(url);

    Assertions.assertEquals(ServiceSourceKind.CLASSPATH, source.kind());
    Assertions.assertEquals("my-overlay.jar!META-INF/berlioz/services.xml", source.orderingKey());
    Assertions.assertEquals(url, source.url());
  }

  @Test
  void testFilesystem_kindAndOrderingKey() {
    File configDir = new File("/app/WEB-INF/config");
    File file = new File(configDir, "services.xml");
    ServiceSource source = ServiceSource.filesystem(file, configDir);

    Assertions.assertEquals(ServiceSourceKind.FILESYSTEM, source.kind());
    Assertions.assertEquals("services.xml", source.orderingKey());
  }

  @Test
  void testEqualsAndHashCode_basedOnUrl() throws MalformedURLException {
    URL url = new URL("jar:file:/opt/apps/my-overlay.jar!/META-INF/berlioz/services.xml");
    ServiceSource a = ServiceSource.classpath(url);
    ServiceSource b = ServiceSource.classpath(url);

    Assertions.assertEquals(a, b);
    Assertions.assertEquals(a.hashCode(), b.hashCode());
  }

}
