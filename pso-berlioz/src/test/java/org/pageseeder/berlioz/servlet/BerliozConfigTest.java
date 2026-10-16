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
