package org.pageseeder.berlioz.servlet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.pageseeder.berlioz.content.Service;
import org.pageseeder.berlioz.content.ServiceTestHelper;
import org.pageseeder.berlioz.xslt.StylesheetSourceKind;
import org.pageseeder.berlioz.xslt.XsltTemplateCache;
import org.pageseeder.berlioz.xslt.XsltTransformException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class BerliozConfigTest {

  // Listener static methods (just verify they don't throw)

  @Test
  void testGetListener_initiallyNull() {
    // Reset to null before checking
    BerliozConfig.setListener(null);
    assertNull(BerliozConfig.getListener());
  }

  @Test
  void resetETagSeed_generatesAndPersistsSeed(@TempDir Path contextRoot) throws Exception {
    Files.createDirectories(contextRoot.resolve("WEB-INF"));
    BerliozConfig config = BerliozConfig.newConfig(servletConfig(contextRoot));

    config.resetETagSeed();

    assertAll(
        () -> assertNotEquals(0L, config.getETagSeed()),
        () -> assertTrue(Files.exists(contextRoot.resolve("WEB-INF/berlioz.etag")))
    );
  }

  @Test
  void contentType_usesNamedCharsetParameter(@TempDir Path contextRoot) throws Exception {
    Files.createDirectories(contextRoot.resolve("WEB-INF"));
    ServletConfig servletConfig = servletConfig(contextRoot, Map.of(
        "content-type", "text/plain;note=\"charset=ISO-8859-1\"; charset = \"UTF-16\""));

    BerliozConfig config = BerliozConfig.newConfig(servletConfig);

    assertAll(
        () -> assertEquals("text/plain", config.getMediaType()),
        () -> assertEquals(StandardCharsets.UTF_16, config.getCharset()),
        () -> assertEquals("text/plain;charset=UTF-16", config.getContentType())
    );
  }

  @Test
  void contentType_charsetTextInAnotherParameterDefaultsToUtf8(@TempDir Path contextRoot)
      throws Exception {
    Files.createDirectories(contextRoot.resolve("WEB-INF"));
    ServletConfig servletConfig = servletConfig(contextRoot,
        Map.of("content-type", "text/plain;note=\"charset=ISO-8859-1\""));

    BerliozConfig config = BerliozConfig.newConfig(servletConfig);

    assertEquals(StandardCharsets.UTF_8, config.getCharset());
  }

  // ---------------------------------------------------------------------------
  // Stylesheet resolution — classpath:/resource: primary and fallback stylesheets
  // ---------------------------------------------------------------------------

  private static final String CLASSPATH_STYLE =
      "org/pageseeder/berlioz/servlet/berliozconfigtest/style.xsl";

  @AfterEach
  void clearXsltCache() {
    XsltTemplateCache.clearAllCache();
  }

  @Test
  void getTransformer_classpathStylesheet_resolvesToClasspathLocation(@TempDir Path contextRoot) throws Exception {
    Files.createDirectories(contextRoot.resolve("WEB-INF"));
    ServletConfig servletConfig = servletConfig(contextRoot, Map.of("stylesheet", "classpath:" + CLASSPATH_STYLE));
    BerliozConfig config = BerliozConfig.newConfig(servletConfig);
    Service service = ServiceTestHelper.build("home", ServiceTestHelper.highestRule());

    XsltTransformer transformer = config.getTransformer(service);

    assertNotNull(transformer);
    assertEquals(StylesheetSourceKind.CLASSPATH, transformer.location().kind());
    String result = transformer.transformOrThrow("<root/>", ServletTestSupport.request().build(), service)
        .content().toString();
    assertTrue(result.contains("classpath-ok"), result);
  }

  @Test
  void getTransformer_classpathStylesheetWithLeadingSlash_resolvesSameResource(@TempDir Path contextRoot) throws Exception {
    Files.createDirectories(contextRoot.resolve("WEB-INF"));
    ServletConfig servletConfig = servletConfig(contextRoot, Map.of("stylesheet", "classpath:/" + CLASSPATH_STYLE));
    BerliozConfig config = BerliozConfig.newConfig(servletConfig);
    Service service = ServiceTestHelper.build("home", ServiceTestHelper.highestRule());

    XsltTransformer transformer = config.getTransformer(service);

    assertNotNull(transformer);
    assertEquals(StylesheetSourceKind.CLASSPATH, transformer.location().kind());
    assertNotNull(transformer.getEtag());
  }

  @Test
  void getTransformer_resourcePrefix_stillResolvesAsClasspathAlias(@TempDir Path contextRoot) throws Exception {
    Files.createDirectories(contextRoot.resolve("WEB-INF"));
    ServletConfig servletConfig = servletConfig(contextRoot, Map.of("stylesheet", "resource:" + CLASSPATH_STYLE));
    BerliozConfig config = BerliozConfig.newConfig(servletConfig);
    Service service = ServiceTestHelper.build("home", ServiceTestHelper.highestRule());

    XsltTransformer transformer = config.getTransformer(service);

    assertNotNull(transformer);
    assertEquals(StylesheetSourceKind.CLASSPATH, transformer.location().kind());
    assertNotNull(transformer.getEtag());
  }

  @Test
  void getTransformer_missingFilesystemStylesheetWithClasspathFallback_usesFallback(@TempDir Path contextRoot) throws Exception {
    Files.createDirectories(contextRoot.resolve("WEB-INF"));
    ServletConfig servletConfig = servletConfig(contextRoot, Map.of(
        "stylesheet", "xslt/does-not-exist.xsl",
        "fallback-stylesheet", "classpath:" + CLASSPATH_STYLE));
    BerliozConfig config = BerliozConfig.newConfig(servletConfig);
    Service service = ServiceTestHelper.build("home", ServiceTestHelper.highestRule());

    XsltTransformer transformer = config.getTransformer(service);

    assertNotNull(transformer);
    String result = transformer.transformOrThrow("<root/>", ServletTestSupport.request().build(), service)
        .content().toString();
    assertTrue(result.contains("classpath-ok"), result);
  }

  @ParameterizedTest
  @ValueSource(strings = {"classpath:", "resource:"})
  void getTransformer_invalidClasspathPrimary_doesNotSelectFilesystemStylesheet(
      String prefix, @TempDir Path contextRoot) throws Exception {
    createTraversalTarget(contextRoot, prefix);
    BerliozConfig config = BerliozConfig.newConfig(servletConfig(contextRoot,
        Map.of("stylesheet", prefix + "a/../local.xsl")));
    Service service = ServiceTestHelper.build("home", ServiceTestHelper.highestRule());

    XsltTransformer transformer = config.getTransformer(service);

    // Rejected paths use the existing invalid-primary error path, never the local stylesheet.
    assertEquals(contextRoot.resolve("WEB-INF"), transformer.location().path());
  }

  @ParameterizedTest
  @ValueSource(strings = {"classpath:", "resource:"})
  void getTransformer_invalidClasspathFallback_doesNotLoadFilesystemStylesheet(
      String prefix, @TempDir Path contextRoot) throws Exception {
    createTraversalTarget(contextRoot, prefix);
    BerliozConfig config = BerliozConfig.newConfig(servletConfig(contextRoot, Map.of(
        "stylesheet", "xslt/does-not-exist.xsl",
        "fallback-stylesheet", prefix + "a/../local.xsl")));
    Service service = ServiceTestHelper.build("home", ServiceTestHelper.highestRule());

    XsltTransformer transformer = config.getTransformer(service);

    assertThrows(XsltTransformException.class, () -> transformer.transformOrThrow(
        "<root/>", ServletTestSupport.request().build(), service));
  }

  private static final String OVERLAY = "META-INF/berlioz/xslt/html/g.xsl";

  private static String stylesheet(String body) {
    return "<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>"
        + body + "</xsl:stylesheet>";
  }

  private static String output(XsltTransformer transformer, Service service) throws Exception {
    return transformer.transformOrThrow("<root/>", ServletTestSupport.request().build(), service)
        .content().toString();
  }

  private static Path overlayJar(Path jar, String marker) throws IOException {
    try (java.util.jar.JarOutputStream out = new java.util.jar.JarOutputStream(Files.newOutputStream(jar))) {
      Map<String, String> entries = Map.of(
          OVERLAY, stylesheet("<xsl:import href='nested/first.xsl'/>"),
          "META-INF/berlioz/xslt/html/nested/first.xsl", stylesheet("<xsl:import href='second.xsl'/>"),
          "META-INF/berlioz/xslt/html/nested/second.xsl",
          stylesheet("<xsl:template match='/'><" + marker + "/></xsl:template>"));
      for (Map.Entry<String, String> entry : entries.entrySet()) {
        out.putNextEntry(new java.util.jar.JarEntry(entry.getKey()));
        out.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
        out.closeEntry();
      }
    }
    return jar;
  }

  @ParameterizedTest
  @ValueSource(strings = {"no", "auto", "manual"})
  void groupOverlay_selectionAndCacheTransitions(String mode, @TempDir Path root) throws Exception {
    java.lang.reflect.Field field = org.pageseeder.berlioz.GlobalSettings.class.getDeclaredField("SETTINGS");
    field.setAccessible(true);
    @SuppressWarnings("unchecked")
    java.util.concurrent.atomic.AtomicReference<Map<String, String>> settings =
        (java.util.concurrent.atomic.AtomicReference<Map<String, String>>) field.get(null);
    Map<String, String> original = settings.get();
    Map<String, String> options = new java.util.HashMap<>(original == null ? Map.of() : original);
    options.put(org.pageseeder.berlioz.BerliozOption.XSLT_CACHE.property(), mode);
    settings.set(options);
    ClassLoader originalLoader = Thread.currentThread().getContextClassLoader();
    Path first = overlayJar(root.resolve("first.jar"), "first-overlay");
    Path second = overlayJar(root.resolve("second.jar"), "second-overlay");
    try (java.net.URLClassLoader loader = new java.net.URLClassLoader(
        new java.net.URL[]{first.toUri().toURL(), second.toUri().toURL()}, originalLoader)) {
      Thread.currentThread().setContextClassLoader(loader);
      Path file = root.resolve("WEB-INF/xslt/html/g.xsl");
      Files.createDirectories(file.getParent());
      BerliozConfig config = BerliozConfig.newConfig(servletConfig(root,
          Map.of("stylesheet", "xslt/html/{GROUP}.xsl")));
      Service service = ServiceTestHelper.build("home", ServiceTestHelper.highestRule());
      XsltTransformer overlay = config.getTransformer(service);
      assertEquals(StylesheetSourceKind.CLASSPATH, overlay.location().kind());
      assertEquals("classpath:" + OVERLAY, overlay.location().logicalPath());
      assertTrue(output(overlay, service).contains("first-overlay"));
      assertSame(overlay, config.getTransformer(service));
      String overlayEtag = overlay.getEtag();

      Files.writeString(file, stylesheet("<xsl:template match='/'><filesystem/></xsl:template>"));
      XsltTransformer override = config.getTransformer(service);
      assertEquals(StylesheetSourceKind.FILESYSTEM, override.location().kind());
      assertTrue(output(override, service).contains("filesystem"));
      assertNotEquals(overlayEtag, override.getEtag());

      Files.delete(file);
      assertTrue(output(config.getTransformer(service), service).contains("first-overlay"));
      Files.writeString(file, "invalid XML");
      assertThrows(XsltTransformException.class, () -> output(config.getTransformer(service), service));
      Files.writeString(file, stylesheet("<xsl:template match='/'><replacement/></xsl:template>"));
      config.getTransformer(service).clearCache();
      assertTrue(output(config.getTransformer(service), service).contains("replacement"));
      Files.writeString(file, stylesheet("<xsl:import href='missing-import.xsl'/>"));
      config.getTransformer(service).clearCache();
      assertThrows(XsltTransformException.class, () -> output(config.getTransformer(service), service));

      // A directory is present but unreadable as stylesheet XML: it must never select the overlay.
      Files.delete(file);
      Files.createDirectory(file);
      config.getTransformer(service).clearCache();
      assertEquals(StylesheetSourceKind.FILESYSTEM, config.getTransformer(service).location().kind());
      assertThrows(XsltTransformException.class, () -> output(config.getTransformer(service), service));
      Files.delete(file);

      // Reverse URL order: the application classloader's first match wins deterministically.
      try (java.net.URLClassLoader reversed = new java.net.URLClassLoader(
          new java.net.URL[]{second.toUri().toURL(), first.toUri().toURL()}, originalLoader)) {
        Thread.currentThread().setContextClassLoader(reversed);
        assertTrue(output(config.getTransformer(service), service).contains("second-overlay"));
        BerliozConfig reloaded = BerliozConfig.newConfig(servletConfig(root,
            Map.of("stylesheet", "xslt/html/{GROUP}.xsl")));
        assertTrue(output(reloaded.getTransformer(service), service).contains("second-overlay"));
      }
    } finally {
      Thread.currentThread().setContextClassLoader(originalLoader);
      settings.set(original);
    }
  }

  @Test
  void groupOverlay_missingBothLocationsFails(@TempDir Path root) throws Exception {
    Files.createDirectories(root.resolve("WEB-INF"));
    BerliozConfig config = BerliozConfig.newConfig(servletConfig(root,
        Map.of("stylesheet", "xslt/missing/{GROUP}.xsl")));
    Service service = ServiceTestHelper.build("home", ServiceTestHelper.highestRule());
    assertThrows(XsltTransformException.class, () -> output(config.getTransformer(service), service));
  }

  @Test
  void groupOverlay_permissionFailureDoesNotFallBack(@TempDir Path root) throws Exception {
    assumeTrue(Files.getFileStore(root).supportsFileAttributeView("posix"));
    ClassLoader original = Thread.currentThread().getContextClassLoader();
    Path jar = overlayJar(root.resolve("overlay.jar"), "overlay");
    Path file = root.resolve("WEB-INF/xslt/html/g.xsl");
    Files.createDirectories(file.getParent());
    Files.writeString(file, stylesheet("<xsl:template match='/'/>"));
    java.util.Set<java.nio.file.attribute.PosixFilePermission> permissions = Files.getPosixFilePermissions(file);
    try (java.net.URLClassLoader loader = new java.net.URLClassLoader(
        new java.net.URL[]{jar.toUri().toURL()}, original)) {
      Thread.currentThread().setContextClassLoader(loader);
      Files.setPosixFilePermissions(file, java.util.Set.of());
      assumeTrue(!Files.isReadable(file), "Process can bypass POSIX permissions");
      BerliozConfig config = BerliozConfig.newConfig(servletConfig(root,
          Map.of("stylesheet", "xslt/html/{GROUP}.xsl")));
      Service service = ServiceTestHelper.build("home", ServiceTestHelper.highestRule());
      assertEquals(StylesheetSourceKind.FILESYSTEM, config.getTransformer(service).location().kind());
      assertThrows(XsltTransformException.class, () -> output(config.getTransformer(service), service));
    } finally {
      Files.setPosixFilePermissions(file, permissions);
      Thread.currentThread().setContextClassLoader(original);
    }
  }

  private static void createTraversalTarget(Path contextRoot, String prefix) throws IOException {
    // Windows forbids ':' in directory names, so this filesystem reinterpretation cannot occur there.
    assumeTrue(File.separatorChar == '/');
    Files.createDirectories(contextRoot.resolve("WEB-INF").resolve(prefix + "a"));
    Files.writeString(contextRoot.resolve("WEB-INF/local.xsl"),
        "<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>"
            + "<xsl:template match='/'><unintended-local-stylesheet/></xsl:template>"
            + "</xsl:stylesheet>");
  }

  private static ServletConfig servletConfig(Path contextRoot) {
    return servletConfig(contextRoot, Map.of());
  }

  private static ServletConfig servletConfig(Path contextRoot, Map<String, String> initParameters) {
    ServletContext context = (ServletContext) Proxy.newProxyInstance(
        ServletContext.class.getClassLoader(),
        new Class<?>[]{ServletContext.class},
        (proxy, m, args) -> {
          if ("getRealPath".equals(m.getName())) return contextRoot.toString();
          return ServletTestSupport.defaultValue(m.getReturnType());
        });
    return (ServletConfig) Proxy.newProxyInstance(
        ServletConfig.class.getClassLoader(),
        new Class<?>[]{ServletConfig.class},
        (proxy, m, args) -> {
          if ("getServletContext".equals(m.getName())) return context;
          if ("getServletName".equals(m.getName())) return "test-config";
          if ("getInitParameter".equals(m.getName())) return initParameters.get(args[0]);
          return ServletTestSupport.defaultValue(m.getReturnType());
        });
  }
}
