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
  void testRequestReload_malformedCandidateRetainsLastSuccessfulRegistry(@TempDir Path webInf)
      throws BerliozException, IOException {
    Path config = Files.createDirectories(webInf.resolve("config"));
    Files.copy(new File(WEB_INF, "config/services.xml").toPath(), config.resolve("services.xml"));
    GlobalSettings.setup(webInf.toFile());
    ServiceLoader loader = ServiceLoader.getInstance();
    loader.loadIfRequired();

    MatchingService previous = loader.getDefaultRegistry().get("/home", HttpMethod.GET);
    Assertions.assertNotNull(previous);
    long previousVersion = loader.getDefaultRegistry().version();
    List<CollectedError<SAXParseException>> previousWarnings = loader.getLastLoadWarnings();
    List<ServiceRegistration> previousRegistrations = loader.getDefaultRegistry().registrations();

    Files.writeString(config.resolve("services!malformed.xml"), "<service-config>");
    loader.requestReload();

    Assertions.assertNotNull(loader.getDefaultRegistry().get("/home", HttpMethod.GET),
        "Requesting a reload must not clear the live registry before a candidate is available");
    Assertions.assertFalse(loader.loadIfRequired());
    MatchingService retained = loader.getDefaultRegistry().get("/home", HttpMethod.GET);
    Assertions.assertNotNull(retained, "A failed requested reload must retain the last successful registry");
    Assertions.assertEquals(previous.service().id(), retained.service().id());
    Assertions.assertEquals(previousVersion, loader.getDefaultRegistry().version(),
        "A failed requested reload must retain the last successful registry version");
    Assertions.assertSame(previousWarnings, loader.getLastLoadWarnings(),
        "A failed requested reload must retain warnings from the last successful load");
    Assertions.assertSame(previousRegistrations, loader.getDefaultRegistry().registrations(),
        "A failed requested reload must retain origins from the last successful load");
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
    long previousVersion = loader.getDefaultRegistry().version();

    Path config = Files.createDirectories(webInf.resolve("config"));
    Files.copy(new File(WEB_INF, "config/services.xml").toPath(), config.resolve("services.xml"));
    Files.writeString(config.resolve("services!malformed.xml"), "<service-config>");
    GlobalSettings.setup(webInf.toFile());

    Assertions.assertThrows(BerliozException.class, loader::load);
    Assertions.assertSame(previousWarnings, loader.getLastLoadWarnings(),
        "A failed aggregate load must not replace warnings from the last successful load");
    Assertions.assertEquals(previousVersion, loader.getDefaultRegistry().version(),
        "A failed aggregate load must not change the registry version");
    Assertions.assertNull(loader.getDefaultRegistry().get("/home", HttpMethod.GET),
        "A failed aggregate load must not publish any part of the failed candidate ('/home' "
            + "belongs to the malformed candidate's main services.xml, not the previous load)");
  }

  // Transactional merge across sources -------------------------------------------------------------

  private static final String SERVICE_CONFIG_HEADER =
      "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
      + "<!DOCTYPE service-config PUBLIC \"-//Berlioz//DTD::Services 1.0//EN\"\n"
      + "    \"https://pageseeder.org/schema/berlioz/services-1.0.dtd\">\n";

  private static String mainFile(String id, String pattern, String method) {
    return serviceConfigFile("default", id, pattern, method);
  }

  /**
   * Builds a complete {@code <service-config>} document, the shape every classpath JAR overlay is
   * expected to use for its own {@code META-INF/berlioz/services.xml} (see the class javadoc of
   * {@link ServiceLoader#parseSource}). Used here for filesystem modules too, since per-source
   * isolation means a module using this root no longer erases another source's contributions.
   */
  private static String moduleFile(String group, String id, String pattern, String method) {
    return serviceConfigFile(group, id, pattern, method);
  }

  private static String serviceConfigFile(String group, String id, String pattern, String method) {
    return SERVICE_CONFIG_HEADER
        + "<service-config version=\"1.0\">\n"
        + "  <services group=\"" + group + "\">\n"
        + "    <service id=\"" + id + "\" method=\"" + method + "\">\n"
        + "      <url pattern=\"" + pattern + "\"/>\n"
        + "      <generator class=\"org.pageseeder.berlioz.generator.NoContent\"/>\n"
        + "    </service>\n"
        + "  </services>\n"
        + "</service-config>\n";
  }

  @Test
  void testLoad_mergesMainFileAndModuleWithoutLosingEitherSource(@TempDir Path webInf) throws BerliozException, IOException {
    GlobalSettings.setup((InitEnvironment) null);
    ServiceLoader loader = ServiceLoader.getInstance();
    Path config = Files.createDirectories(webInf.resolve("config"));
    Files.writeString(config.resolve("services.xml"), mainFile("home", "/home", "get"));
    Files.writeString(config.resolve("services!extra.xml"), moduleFile("extra", "extra", "/extra", "get"));
    GlobalSettings.setup(webInf.toFile());

    loader.load();

    Assertions.assertNotNull(loader.getDefaultRegistry().get("/home", HttpMethod.GET),
        "The main file's service must survive merging with a module (per-source isolation must not erase it)");
    Assertions.assertNotNull(loader.getDefaultRegistry().get("/extra", HttpMethod.GET),
        "The module's own service must be registered");
  }

  @Test
  void testLoad_conflictingPattern_laterSourceWinsAndWarnsWithBothOrigins(@TempDir Path webInf)
      throws BerliozException, IOException {
    GlobalSettings.setup((InitEnvironment) null);
    ServiceLoader loader = ServiceLoader.getInstance();
    Path config = Files.createDirectories(webInf.resolve("config"));
    Files.writeString(config.resolve("services.xml"), mainFile("home", "/home", "get"));
    Files.writeString(config.resolve("services!override.xml"), moduleFile("override", "home-override", "/home", "get"));
    GlobalSettings.setup(webInf.toFile());

    loader.load();

    MatchingService match = loader.getDefaultRegistry().get("/home", HttpMethod.GET);
    Assertions.assertNotNull(match);
    Assertions.assertEquals("home-override", match.service().id(),
        "The module loaded after the main file must win the conflict");

    List<CollectedError<SAXParseException>> warnings = loader.getLastLoadWarnings();
    Assertions.assertSame(warnings, loader.getDefaultRegistry().warnings(),
        "The loader must expose warnings from the current registry publication");
    boolean hasConflictWarning = warnings.stream().anyMatch(w -> {
      String message = w.error().getMessage();
      return message != null && message.contains("home-override") && message.contains("home")
          && message.contains("services.xml") && message.contains("services!override.xml");
    });
    Assertions.assertTrue(hasConflictWarning,
        "Expected a warning naming both the replaced and replacing service and origin: " + warnings);

    ServiceRegistration published = loader.getDefaultRegistry().registrations().stream()
        .filter(registration -> registration.method() == HttpMethod.GET
            && "/home".equals(registration.pattern().toString()))
        .findFirst()
        .orElseThrow();
    Assertions.assertEquals("home-override", published.service().id());
    Assertions.assertEquals("services!override.xml", published.origin().displayName(),
        "The published registration must retain the winning declaration's origin");
  }

  @Test
  void testLoad_samePatternDifferentMethod_doesNotConflict(@TempDir Path webInf) throws BerliozException, IOException {
    GlobalSettings.setup((InitEnvironment) null);
    ServiceLoader loader = ServiceLoader.getInstance();
    Path config = Files.createDirectories(webInf.resolve("config"));
    Files.writeString(config.resolve("services.xml"), mainFile("home-get", "/home", "get"));
    Files.writeString(config.resolve("services!post.xml"), moduleFile("post", "home-post", "/home", "post"));
    GlobalSettings.setup(webInf.toFile());

    loader.load();

    MatchingService get = loader.getDefaultRegistry().get("/home", HttpMethod.GET);
    MatchingService post = loader.getDefaultRegistry().get("/home", HttpMethod.POST);
    Assertions.assertNotNull(get);
    Assertions.assertEquals("home-get", get.service().id());
    Assertions.assertNotNull(post);
    Assertions.assertEquals("home-post", post.service().id());

    boolean hasOverrideWarning = loader.getLastLoadWarnings().stream()
        .anyMatch(w -> w.error().getMessage() != null && w.error().getMessage().contains("overrides"));
    Assertions.assertFalse(hasOverrideWarning,
        "Different HTTP methods sharing the same pattern must not be reported as a conflict");
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

  @ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
  void uncheckedDiscoveryFailureIsThrottledAndRecovers(boolean reload) throws Exception {
    ServiceLoader loader = ServiceLoader.getInstance();
    if (reload) loader.loadIfRequired();
    ServiceRegistry registry = loader.getDefaultRegistry();
    long version = registry.version();
    List<ServiceRegistration> registrations = registry.registrations();
    List<CollectedError<SAXParseException>> warnings = loader.getLastLoadWarnings();
    ClassLoader original = Thread.currentThread().getContextClassLoader();
    IllegalStateException cause = new IllegalStateException("broken resource enumeration");
    int[] attempts = {0};
    try {
      Thread.currentThread().setContextClassLoader(new ClassLoader(original) {
        @Override
        public java.util.Enumeration<URL> getResources(String name) {
          attempts[0]++;
          throw cause;
        }
      });
      loader.requestReload();
      long now = System.nanoTime();
      if (reload) {
        Assertions.assertFalse(loader.loadIfRequired(now));
        Assertions.assertFalse(loader.loadIfRequired(now + ServiceLoader.RETRY_DELAY_NANOS - 1));
        Assertions.assertNotNull(registry.get("/home", HttpMethod.GET));
      } else {
        BerliozException failure = Assertions.assertThrows(BerliozException.class,
            () -> loader.loadIfRequired(now));
        Assertions.assertSame(cause, failure.getCause());
        Assertions.assertSame(failure, Assertions.assertThrows(BerliozException.class,
            () -> loader.loadIfRequired(now + ServiceLoader.RETRY_DELAY_NANOS - 1)));
      }
      Assertions.assertEquals(1, attempts[0]);
      Assertions.assertEquals(reload ? "reload-failed" : "startup-failed", loader.getLoadStatus());
      Assertions.assertEquals(version, registry.version());
      Assertions.assertSame(registrations, registry.registrations());
      Assertions.assertSame(warnings, loader.getLastLoadWarnings());
      Thread.currentThread().setContextClassLoader(original);
      Assertions.assertTrue(loader.loadIfRequired(now + ServiceLoader.RETRY_DELAY_NANOS));
      Assertions.assertEquals("ready", loader.getLoadStatus());
    } finally {
      Thread.currentThread().setContextClassLoader(original);
    }
  }

  @Test
  void jvmErrorsAreNotConvertedToLoadFailures() {
    ServiceLoader loader = ServiceLoader.getInstance();
    ClassLoader original = Thread.currentThread().getContextClassLoader();
    LinkageError failure = new LinkageError("broken classloader");
    try {
      Thread.currentThread().setContextClassLoader(new ClassLoader(original) {
        @Override
        public java.util.Enumeration<URL> getResources(String name) {
          throw failure;
        }
      });
      Assertions.assertSame(failure, Assertions.assertThrows(LinkageError.class, loader::loadIfRequired));
    } finally {
      Thread.currentThread().setContextClassLoader(original);
    }
  }

  @Test
  void discoveryFailureKeepsRegistryAndRetriesAfterDelay() throws Exception {
    ServiceLoader loader = ServiceLoader.getInstance();
    loader.loadIfRequired();
    ServiceRegistry registry = loader.getDefaultRegistry();
    long version = registry.version();
    List<ServiceRegistration> registrations = registry.registrations();
    List<CollectedError<SAXParseException>> warnings = loader.getLastLoadWarnings();
    ClassLoader original = Thread.currentThread().getContextClassLoader();
    int[] attempts = {0};
    ClassLoader broken = new ClassLoader(original) {
      @Override
      public java.util.Enumeration<URL> getResources(String name) throws IOException {
        attempts[0]++;
        throw new IOException("sensitive deployment path");
      }
    };
    try {
      Thread.currentThread().setContextClassLoader(broken);
      loader.requestReload();
      long now = System.nanoTime();
      Assertions.assertFalse(loader.loadIfRequired(now));
      Assertions.assertEquals("reload-failed", loader.getLoadStatus());
      Assertions.assertNotNull(registry.get("/home", HttpMethod.GET));
      Assertions.assertEquals(version, registry.version());
      Assertions.assertSame(registrations, registry.registrations());
      Assertions.assertSame(warnings, loader.getLastLoadWarnings());
      Assertions.assertFalse(loader.loadIfRequired(now + ServiceLoader.RETRY_DELAY_NANOS - 1));
      Assertions.assertEquals(1, attempts[0]);
      Assertions.assertFalse(loader.loadIfRequired(now + ServiceLoader.RETRY_DELAY_NANOS));
      Assertions.assertEquals(2, attempts[0]);
      Thread.currentThread().setContextClassLoader(original);
      Assertions.assertTrue(loader.loadIfRequired(now + 2 * ServiceLoader.RETRY_DELAY_NANOS));
      Assertions.assertEquals("ready", loader.getLoadStatus());
      Assertions.assertNotSame(registrations, registry.registrations());
    } finally {
      Thread.currentThread().setContextClassLoader(original);
    }
  }

  @Test
  void startupDiscoveryFailureIsNotPublishedAndExplicitRetryRecovers() throws Exception {
    ServiceLoader loader = ServiceLoader.getInstance();
    long version = loader.getDefaultRegistry().version();
    ClassLoader original = Thread.currentThread().getContextClassLoader();
    IOException cause = new IOException("discovery failed");
    int[] attempts = {0};
    try {
      Thread.currentThread().setContextClassLoader(new ClassLoader(original) {
        @Override
        public java.util.Enumeration<URL> getResources(String name) throws IOException {
          attempts[0]++;
          throw cause;
        }
      });
      BerliozException failure = Assertions.assertThrows(BerliozException.class, loader::loadIfRequired);
      Assertions.assertSame(cause, failure.getCause());
      Assertions.assertSame(failure, Assertions.assertThrows(BerliozException.class, loader::loadIfRequired));
      Assertions.assertEquals(1, attempts[0]);
      Assertions.assertEquals(version, loader.getDefaultRegistry().version());
      Assertions.assertEquals("startup-failed", loader.getLoadStatus());
      Thread.currentThread().setContextClassLoader(original);
      loader.requestReload();
      Assertions.assertTrue(loader.loadIfRequired());
      Assertions.assertEquals("ready", loader.getLoadStatus());
      loader.clear();
      Assertions.assertEquals("pending", loader.getLoadStatus());
    } finally {
      Thread.currentThread().setContextClassLoader(original);
    }
  }

  @Test
  void filesystemDiscoveryDistinguishesAbsentDirectoryFromInvalidDirectory(@TempDir Path webInf)
      throws Exception {
    GlobalSettings.setup(webInf.toFile());
    ServiceLoader loader = ServiceLoader.getInstance();
    Assertions.assertTrue(loader.discoverSources().isEmpty());
    // A regular file in place of a directory deterministically fails enumeration on all platforms.
    Files.writeString(webInf.resolve("config"), "not a directory");
    Assertions.assertThrows(BerliozException.class, loader::discoverSources);
    Assertions.assertThrows(BerliozException.class, loader::loadIfRequired);
    Assertions.assertEquals("startup-failed", loader.getLoadStatus());
  }

  @Test
  void filesystemDiscoveryFailureRetainsSuccessfulEmptyRegistry(@TempDir Path webInf) throws Exception {
    GlobalSettings.setup(webInf.toFile());
    ServiceLoader loader = ServiceLoader.getInstance();
    Assertions.assertTrue(loader.loadIfRequired());
    long version = loader.getDefaultRegistry().version();
    Files.writeString(webInf.resolve("config"), "not a directory");
    loader.requestReload();
    Assertions.assertFalse(loader.loadIfRequired());
    Assertions.assertEquals("reload-failed", loader.getLoadStatus());
    Assertions.assertEquals(version, loader.getDefaultRegistry().version());
    Files.delete(webInf.resolve("config"));
    loader.requestReload();
    Assertions.assertTrue(loader.loadIfRequired());
    Assertions.assertEquals("ready", loader.getLoadStatus());
  }

  // Classpath discovery ------------------------------------------------------------------------

  @Test
  void testDiscoverClasspathSources_dedupesIdenticalUrl(@TempDir Path dir) throws Exception {
    writeMinimalServicesXml(dir);
    URL dirUrl = dir.toUri().toURL();
    try (URLClassLoader classLoader = new URLClassLoader(new URL[] {dirUrl, dirUrl}, null)) {
      List<ServiceSource> sources = ServiceLoader.getInstance().discoverClasspathSources(classLoader);
      Assertions.assertEquals(1, sources.size(), "Identical classpath resource URLs must be deduplicated");
      Assertions.assertEquals(ServiceSourceKind.CLASSPATH, sources.get(0).kind());
    }
  }

  @Test
  void testDiscoverClasspathSources_orderingIsIndependentOfEnumerationOrder(@TempDir Path root) throws Exception {
    Path dirA = Files.createDirectories(root.resolve("a"));
    Path dirB = Files.createDirectories(root.resolve("b"));
    writeMinimalServicesXml(dirA);
    writeMinimalServicesXml(dirB);
    URL urlA = dirA.toUri().toURL();
    URL urlB = dirB.toUri().toURL();
    try (URLClassLoader forward = new URLClassLoader(new URL[] {urlA, urlB}, null);
         URLClassLoader reverse = new URLClassLoader(new URL[] {urlB, urlA}, null)) {
      List<ServiceSource> first = ServiceLoader.getInstance().discoverClasspathSources(forward);
      List<ServiceSource> second = ServiceLoader.getInstance().discoverClasspathSources(reverse);
      Assertions.assertEquals(2, first.size());
      Assertions.assertEquals(first, second, "Discovery order must not depend on classloader enumeration order");
    }
  }

  @Test
  void testDiscoverSources_classpathSourcesComeBeforeFilesystemSources(@TempDir Path dir) throws Exception {
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

  // Response code origin --------------------------------------------------------------------------

  @Test
  void testLoad_recordsResponseCodeOrigin(@TempDir Path dir) throws BerliozException, IOException {
    Path services = dir.resolve("services.xml");
    Files.writeString(services, String.join("\n",
        "<!DOCTYPE service-config PUBLIC \"-//Berlioz//DTD::Services 1.0//EN\"",
        "    \"https://pageseeder.org/schema/berlioz/services-1.0.dtd\">",
        "<service-config version=\"1.0\">",
        "  <services group=\"declared\">",
        "    <response-code use=\"name:*\" rule=\"lowest\"/>",
        "    <service id=\"own\" method=\"get\">",
        "      <url pattern=\"/own\"/>",
        "      <response-code use=\"name:*\" rule=\"first\"/>",
        "      <generator class=\"org.pageseeder.berlioz.generator.NoContent\"/>",
        "    </service>",
        "    <service id=\"inherited\" method=\"get\">",
        "      <url pattern=\"/inherited\"/>",
        "      <generator class=\"org.pageseeder.berlioz.generator.NoContent\"/>",
        "    </service>",
        "  </services>",
        "  <services group=\"undeclared\">",
        "    <service id=\"none\" method=\"get\">",
        "      <url pattern=\"/none\"/>",
        "      <generator class=\"org.pageseeder.berlioz.generator.NoContent\"/>",
        "    </service>",
        "  </services>",
        "</service-config>"));
    ServiceLoader loader = ServiceLoader.getInstance();
    loader.load(services.toFile());
    ServiceRegistry registry = loader.getDefaultRegistry();

    Service own = registry.get("/own", HttpMethod.GET).service();
    Assertions.assertEquals(ServiceStatusRule.CodeRule.FIRST, own.rule().rule());
    Assertions.assertEquals(ServiceStatusRule.Origin.SERVICE, own.ruleOrigin());

    Service inherited = registry.get("/inherited", HttpMethod.GET).service();
    Assertions.assertEquals(ServiceStatusRule.CodeRule.LOWEST, inherited.rule().rule());
    Assertions.assertEquals(ServiceStatusRule.Origin.GROUP, inherited.ruleOrigin());

    Service none = registry.get("/none", HttpMethod.GET).service();
    Assertions.assertSame(ServiceStatusRule.DEFAULT_RULE, none.rule());
    Assertions.assertEquals(ServiceStatusRule.Origin.DEFAULT, none.ruleOrigin());
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
