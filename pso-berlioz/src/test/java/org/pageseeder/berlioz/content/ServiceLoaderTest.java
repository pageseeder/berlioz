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
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.InitEnvironment;
import org.pageseeder.berlioz.generator.GetServices;
import org.pageseeder.berlioz.generator.NoContent;
import org.pageseeder.berlioz.http.HttpMethod;
import org.pageseeder.berlioz.util.CollectedError;
import org.xml.sax.SAXParseException;

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

  @Test
  void testLoad_duplicatePatternWithinFile_onlyFirstServiceRegistered() throws BerliozException {
    // Non-strict mode: the duplicate-pattern condition is a warning, not a fatal error.
    GlobalSettings.setup((InitEnvironment) null);
    ServiceLoader loader = ServiceLoader.getInstance();
    loader.load(new File(WEB_INF, "config/services-duplicate-pattern.xml"));

    MatchingService match = loader.getDefaultRegistry().get("/dup", HttpMethod.GET);
    Assertions.assertNotNull(match, "The first service should still be registered for the pattern");
    Assertions.assertEquals("first", match.service().id(),
        "The duplicate pattern should be dropped from the second service, leaving the first registered");
  }

  @Test
  void testLoad_invalidDirectService_notRegisteredButWarningRetained() throws BerliozException {
    // Non-strict mode: an invalid direct service is a warning, not a fatal error.
    GlobalSettings.setup((InitEnvironment) null);
    ServiceLoader loader = ServiceLoader.getInstance();
    loader.load(new File(WEB_INF, "config/services-invalid-direct.xml"));

    MatchingService match = loader.getDefaultRegistry().get("/no-output", HttpMethod.GET);
    Assertions.assertNull(match, "A direct service whose generator supports no output format must not be registered");

    List<CollectedError<SAXParseException>> warnings = loader.getLastLoadWarnings();
    Assertions.assertFalse(warnings.isEmpty(), "The rejection must be discoverable via getLastLoadWarnings(), not just the logs");
    boolean hasExpectedWarning = warnings.stream()
        .anyMatch(w -> w.error().getMessage() != null && w.error().getMessage().contains("no-output")
            && w.error().getMessage().contains("no output format"));
    Assertions.assertTrue(hasExpectedWarning, "Expected a warning explaining why 'no-output' was not registered");
  }

  @Test
  void testLoad_malformedModule_preservesWarningsFromLastSuccessfulLoad(@TempDir Path webInf)
      throws BerliozException, IOException {
    GlobalSettings.setup((InitEnvironment) null);
    ServiceLoader loader = ServiceLoader.getInstance();
    loader.load(new File(WEB_INF, "config/services-invalid-direct.xml"));
    List<CollectedError<SAXParseException>> previousWarnings = loader.getLastLoadWarnings();
    Assertions.assertFalse(previousWarnings.isEmpty(), "The initial successful load must report a warning");

    Path config = Files.createDirectories(webInf.resolve("config"));
    Files.copy(new File(WEB_INF, "config/services.xml").toPath(), config.resolve("services.xml"));
    Files.writeString(config.resolve("services!malformed.xml"), "<service-config>");
    GlobalSettings.setup(webInf.toFile());

    Assertions.assertThrows(BerliozException.class, loader::load);
    Assertions.assertSame(previousWarnings, loader.getLastLoadWarnings(),
        "A failed aggregate load must not replace warnings from the last successful load");
  }

  // Namespace tests

  static Stream<Arguments> namespaceTestCases() {
    return Stream.of(
        Arguments.of("/ns/simple",           NoContent.class,   "Simple name 'NoContent' should resolve via default namespace"),
        Arguments.of("/ns/prefixed",          GetServices.class, "'gen:GetServices' should resolve via 'gen' prefix namespace"),
        Arguments.of("/ns/qualified",         NoContent.class,   "Fully-qualified class name should pass through unchanged"),
        Arguments.of("/ns/override/simple",   NoContent.class,   "Services-level namespace override should resolve 'NoContent'"),
        Arguments.of("/ns/override/prefixed", GetServices.class, "Services-level 'gen' prefix should resolve 'gen:GetServices'")
    );
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("namespaceTestCases")
  void testNamespace_generatorClass(String url, Class<?> expectedClass, String message) throws BerliozException {
    ServiceLoader loader = ServiceLoader.getInstance();
    loader.load(new File(WEB_INF, "config/services-namespaces.xml"));

    MatchingService match = loader.getDefaultRegistry().get(url, HttpMethod.GET);
    Assertions.assertNotNull(match, "Expected service to match " + url);
    List<BerliozGenerator> generators = match.service().generators();
    Assertions.assertEquals(1, generators.size());
    Assertions.assertInstanceOf(expectedClass, generators.get(0), message);
  }

  // Classpath discovery ------------------------------------------------------------------------

  @Test
  void testDiscoverClasspathSources_dedupesIdenticalUrl(@TempDir Path dir) throws IOException {
    writeMinimalServicesXml(dir);
    URL dirUrl = dir.toUri().toURL();
    try (URLClassLoader classLoader = new URLClassLoader(new URL[] {dirUrl, dirUrl}, null)) {
      List<ServiceSource> sources = ServiceLoader.getInstance().discoverClasspathSources(classLoader);
      Assertions.assertEquals(1, sources.size(), "Identical classpath resource URLs must be deduplicated");
      Assertions.assertEquals(ServiceSourceKind.CLASSPATH, sources.get(0).kind());
    }
  }

  @Test
  void testDiscoverClasspathSources_orderingIsStableAcrossRepeatedCalls(@TempDir Path root) throws IOException {
    Path dirA = Files.createDirectories(root.resolve("a"));
    Path dirB = Files.createDirectories(root.resolve("b"));
    writeMinimalServicesXml(dirA);
    writeMinimalServicesXml(dirB);
    URL[] urls = {dirA.toUri().toURL(), dirB.toUri().toURL()};
    try (URLClassLoader classLoader = new URLClassLoader(urls, null)) {
      List<ServiceSource> first = ServiceLoader.getInstance().discoverClasspathSources(classLoader);
      List<ServiceSource> second = ServiceLoader.getInstance().discoverClasspathSources(classLoader);
      Assertions.assertEquals(2, first.size());
      Assertions.assertEquals(first, second, "Discovery order must be stable across repeated calls");
    }
  }

  @Test
  void testDiscoverSources_classpathSourcesComeBeforeFilesystemSources(@TempDir Path dir) throws IOException {
    writeMinimalServicesXml(dir);
    ClassLoader original = Thread.currentThread().getContextClassLoader();
    try (URLClassLoader classpathLoader = new URLClassLoader(new URL[] {dir.toUri().toURL()}, original)) {
      Thread.currentThread().setContextClassLoader(classpathLoader);
      List<ServiceSource> sources = ServiceLoader.getInstance().discoverSources();
      Assertions.assertFalse(sources.isEmpty());
      Assertions.assertEquals(ServiceSourceKind.CLASSPATH, sources.get(0).kind(),
          "Classpath sources must be discovered before filesystem sources");
      boolean hasFilesystemSource = sources.stream().anyMatch(s -> s.kind() == ServiceSourceKind.FILESYSTEM);
      Assertions.assertTrue(hasFilesystemSource, "Filesystem sources should still be discovered");
    } finally {
      Thread.currentThread().setContextClassLoader(original);
    }
  }

  private static void writeMinimalServicesXml(Path dir) throws IOException {
    Path metaInf = Files.createDirectories(dir.resolve("META-INF/berlioz"));
    Files.writeString(metaInf.resolve("services.xml"), "<service-config version=\"1.0\"/>");
  }

  // Filesystem module ordering -------------------------------------------------------------------

  @Test
  void testListServiceFiles_modulesAreLexicallyOrdered(@TempDir Path webInf) throws IOException {
    Path config = Files.createDirectories(webInf.resolve("config"));
    Files.writeString(config.resolve("services!bbb.xml"), "<services group=\"bbb\"/>");
    Files.writeString(config.resolve("services!aaa.xml"), "<services group=\"aaa\"/>");
    Files.writeString(config.resolve("services!ccc.xml"), "<services group=\"ccc\"/>");
    GlobalSettings.setup(webInf.toFile());

    List<File> files = ServiceLoader.getInstance().listServiceFiles();
    List<String> names = files.stream().map(File::getName).collect(Collectors.toList());
    Assertions.assertEquals(List.of("services!aaa.xml", "services!bbb.xml", "services!ccc.xml"), names);
  }

  // Generator classloader correction --------------------------------------------------------------

  @Test
  void testLoad_generatorsAreLoadedThroughContextClassLoader() throws BerliozException {
    ClassLoader original = Thread.currentThread().getContextClassLoader();
    RecordingClassLoader recording = new RecordingClassLoader(original);
    Thread.currentThread().setContextClassLoader(recording);
    try {
      ServiceLoader loader = ServiceLoader.getInstance();
      loader.load(new File(WEB_INF, "config/services.xml"));
    } finally {
      Thread.currentThread().setContextClassLoader(original);
    }
    Assertions.assertTrue(recording.requested.contains(NoContent.class.getName()),
        "Expected the generator class to be resolved through the context classloader");
  }

  @Test
  void testLoad_generatorClassloaderFallback_stillResolvesBuiltInGenerator() throws BerliozException {
    ClassLoader original = Thread.currentThread().getContextClassLoader();
    DenyingClassLoader denying = new DenyingClassLoader(original, NoContent.class.getName());
    Thread.currentThread().setContextClassLoader(denying);
    try {
      ServiceLoader loader = ServiceLoader.getInstance();
      loader.load(new File(WEB_INF, "config/services.xml"));
      MatchingService match = loader.getDefaultRegistry().get("/home", HttpMethod.GET);
      Assertions.assertNotNull(match, "Built-in generator should still resolve via the fallback classloader");
    } finally {
      Thread.currentThread().setContextClassLoader(original);
    }
  }

  private static final class RecordingClassLoader extends ClassLoader {

    private final List<String> requested = new ArrayList<>();

    RecordingClassLoader(ClassLoader parent) {
      super(parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      this.requested.add(name);
      return super.loadClass(name, resolve);
    }
  }

  private static final class DenyingClassLoader extends ClassLoader {

    private final String denied;

    DenyingClassLoader(ClassLoader parent, String denied) {
      super(parent);
      this.denied = denied;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      if (this.denied.equals(name)) throw new ClassNotFoundException(name);
      return super.loadClass(name, resolve);
    }
  }

}
