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
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.http.HttpMethod;

class ServiceLoaderTest {

  private static final File WEB_INF = new File("./src/test/resources/org/pageseeder/berlioz");

  @BeforeEach
  void setUp() {
    GlobalSettings.setup(WEB_INF);
    ServiceLoader.getInstance().clear();
  }

  @AfterEach
  void tearDown() {
    ServiceLoader.getInstance().clear();
  }

  @Test
  void testLoad_parsesServicesFile() throws BerliozException {
    ServiceLoader loader = ServiceLoader.getInstance();
    loader.load(new File(WEB_INF, "config/services.xml"));

    ServiceRegistry registry = loader.getDefaultRegistry();
    List<Service> services = registry.getServices();
    Assertions.assertFalse(services.isEmpty(), "Registry should contain at least one service");
  }

  @Test
  void testLoad_homeServiceRegistered() throws BerliozException {
    ServiceLoader loader = ServiceLoader.getInstance();
    loader.load(new File(WEB_INF, "config/services.xml"));

    MatchingService match = loader.getDefaultRegistry().get("/home", HttpMethod.GET);
    Assertions.assertNotNull(match, "Expected 'home' service to match /home GET");
    Assertions.assertEquals("home", match.service().id());
    Assertions.assertEquals("default", match.service().group());
  }

  @Test
  void testLoadIfRequired_loadsOnce() throws BerliozException {
    ServiceLoader loader = ServiceLoader.getInstance();
    boolean first = loader.loadIfRequired();
    boolean second = loader.loadIfRequired();
    Assertions.assertTrue(first, "First call should return true (loaded)");
    Assertions.assertFalse(second, "Second call should return false (already loaded)");
  }

  @Test
  void testClear_resetsLoadedFlag() throws BerliozException {
    ServiceLoader loader = ServiceLoader.getInstance();
    loader.loadIfRequired();
    loader.clear();
    boolean reloaded = loader.loadIfRequired();
    Assertions.assertTrue(reloaded, "After clear, loadIfRequired should load again");
  }

  @Test
  void testListServiceFiles_returnsServicesXml() {
    ServiceLoader loader = ServiceLoader.getInstance();
    List<File> files = loader.listServiceFiles();
    Assertions.assertFalse(files.isEmpty(), "Expected at least one services file");
    boolean hasServicesXml = files.stream().anyMatch(f -> f.getName().equals("services.xml"));
    Assertions.assertTrue(hasServicesXml, "Expected services.xml in the list");
  }
}
