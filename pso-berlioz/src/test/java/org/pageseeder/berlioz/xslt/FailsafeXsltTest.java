package org.pageseeder.berlioz.xslt;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.pageseeder.berlioz.servlet.XsltTransformer;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Applies the built-in failsafe error XSLT to each sample error XML fixture and asserts that
 * the output is a valid HTML document.
 *
 * <p>Fixtures live in {@code src/test/resources/org/pageseeder/berlioz/xslt/samples/} and cover
 * every axis that the XSLT must handle: legacy vs. RFC 9457 problem format, minimal/standard/full
 * detail level, and the most common error types (404, 405, 500, invalid-parameter, upstream, XSLT
 * transform failures, and services configuration errors).
 *
 * <p>To generate HTML previews for visual inspection, run:
 * <pre>{@code
 * ./gradlew :pso-berlioz:generateErrorSamples
 * }</pre>
 * Output is written to {@code build/error-samples/}.
 *
 * <p>Requires Saxon-HE on the test runtime classpath. Saxon is registered automatically as the
 * JAXP {@code TransformerFactory} when it is present, enabling XSLT 2.0 features such as
 * {@code format-dateTime} that the JDK's built-in Xalan processor does not support.
 */
@Tag("error-samples")
class FailsafeXsltTest {

  private static final String SAMPLES_PATH = "org/pageseeder/berlioz/xslt/samples/";

  /**
   * All fixture file names, ordered for readable output: legacy format first, then problem format.
   */
  static List<String> fixtures() {
    return Arrays.asList(
        // Legacy format — minimal (status, title, message only)
        "legacy-client-error-404-minimal.xml",
        "legacy-server-error-500-minimal.xml",
        // Legacy format — standard (adds exception class + message)
        "legacy-server-error-500-standard.xml",
        // Legacy format — full (adds stack trace, cause chain, HTTP headers, HTTP parameters)
        "legacy-server-error-500-full.xml",
        "legacy-invalid-parameter-full.xml",
        "legacy-upstream-error-full.xml",
        // Legacy format — all known error IDs (contextual help blocks)
        "legacy-unexpected.xml",
        "legacy-lifecycle-error.xml",
        "legacy-services-not-found.xml",
        "legacy-services-malformed.xml",
        "legacy-transform-not-found.xml",
        "legacy-transform-invalid.xml",
        "legacy-transform-dynamic-error.xml",
        "legacy-generator-error-unforced.xml",
        "legacy-generator-error-multiple.xml",
        // Problem format (RFC 9457) — framework-level HTTP errors
        "problem-bad-request-400.xml",
        "problem-not-found-404.xml",
        "problem-method-not-allowed-405.xml",
        "problem-service-unavailable-503.xml",
        "problem-server-error-500.xml",
        // Problem format (RFC 9457) — normalized generator-level problems
        "problem-generator-error.xml",
        "problem-invalid-parameter-400.xml",
        "problem-http-signal-451.xml",
        "problem-upstream-error-502.xml",
        // Problem format — with exception extension
        "problem-server-error-500-standard.xml",
        "problem-server-error-500-full.xml",
        // Problem format — all known type URIs (contextual help blocks)
        "problem-unexpected.xml",
        "problem-lifecycle-error.xml",
        "problem-services-not-found.xml",
        "problem-services-malformed.xml",
        "problem-transform-not-found.xml",
        "problem-transform-invalid.xml",
        "problem-transform-invalid-xslt-error.xml",
        "problem-transform-dynamic-error.xml",
        "problem-generator-error-unchecked.xml",
        "problem-generator-error-unforced.xml",
        "problem-generator-error-multiple.xml"
    );
  }

  @Test
  void fixtureListIncludesAllXmlSamples() throws Exception {
    URL dir = FailsafeXsltTest.class.getClassLoader().getResource(SAMPLES_PATH);
    assertNotNull(dir, "Fixture directory not found on classpath: " + SAMPLES_PATH);
    Path path = Paths.get(dir.toURI());
    List<String> actual;
    try (java.util.stream.Stream<Path> files = Files.list(path)) {
      actual = files
          .filter(Files::isRegularFile)
          .map(p -> p.getFileName().toString())
          .filter(name -> name.endsWith(".xml"))
          .sorted(Comparator.naturalOrder())
          .collect(Collectors.toList());
    }
    List<String> expected = fixtures().stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList());
    assertEquals(expected, actual, "Every XML sample fixture should be exercised by this test");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("fixtures")
  void failsafeXslt_producesHtml(String fixture) throws IOException {
    String xml = loadFixture(fixture);
    URL xsl = failsafeUrl();

    String html = XsltTransformer.transformFailSafe(xml, xsl);

    assertNotNull(html, "transform result must not be null");
    assertNotSame(xml, html, "transform must produce new output, not return the input unchanged");
    assertTrue(
        html.toLowerCase(java.util.Locale.ROOT).startsWith("<!doctype html"),
        () -> "output should start with HTML doctype but was:\n" + html.substring(0, Math.min(200, html.length()))
    );
    assertTrue(html.contains("<html"), "output should contain an <html> element");

    if (Boolean.getBoolean("berlioz.generateSamples")) {
      writePreview(fixture, html);
    }
  }

  @Test
  void transformFailSafe_nullStylesheetReturnsOriginalXml() {
    String xml = "<server-error http-code=\"500\"><title>Error</title></server-error>";

    String result = XsltTransformer.transformFailSafe(xml, (URL) null);

    assertEquals(xml, result);
  }

  @Test
  void transformFailSafe_runtimeFailureReturnsOriginalXml(@TempDir Path tmp) throws IOException {
    String xml = "<server-error http-code=\"500\"><title>Error</title></server-error>";
    Path xsl = tmp.resolve("fail.xsl");
    Files.writeString(xsl,
        "<?xml version=\"1.0\"?>"
        + "<xsl:stylesheet version=\"2.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">"
        + "<xsl:template match=\"/\">"
        + "<xsl:message terminate=\"yes\">stop</xsl:message>"
        + "</xsl:template>"
        + "</xsl:stylesheet>",
        StandardCharsets.UTF_8);

    String result = XsltTransformer.transformFailSafe(xml, xsl.toUri().toURL());

    assertEquals(xml, result);
  }

  @Test
  void loadResource_missingOrUnreadableReturnsEmptyString() throws Exception {
    Method loadResource = XsltTransformer.class.getDeclaredMethod("loadResource", ClassLoader.class, String.class);
    loadResource.setAccessible(true);
    ClassLoader missing = new ClassLoader(null) {
      @Override
      public InputStream getResourceAsStream(String name) {
        return null;
      }
    };
    ClassLoader unreadable = new ClassLoader(null) {
      @Override
      public InputStream getResourceAsStream(String name) {
        return new InputStream() {
          @Override
          public int read() throws IOException {
            throw new IOException("broken");
          }
        };
      }
    };

    assertAll(
        () -> assertEquals("", loadResource.invoke(null, missing, "missing.html")),
        () -> assertEquals("", loadResource.invoke(null, unreadable, "broken.html"))
    );
  }

  @Test
  void detectXslt2ReturnsFalseForMissingFactory() throws Exception {
    Method detectXslt2 = XsltTransformer.class.getDeclaredMethod("detectXslt2");
    detectXslt2.setAccessible(true);
    String property = "javax.xml.transform.TransformerFactory";
    String previous = System.getProperty(property);
    try {
      System.setProperty(property, "org.pageseeder.berlioz.tests.DoesNotExistTransformerFactory");
      assertFalse((Boolean) detectXslt2.invoke(null), "missing factory should be treated as unsupported");
    } finally {
      if (previous == null) System.clearProperty(property);
      else System.setProperty(property, previous);
    }
  }

  // --- Helpers ---------------------------------------------------------------------------------

  private static String loadFixture(String name) throws IOException {
    URL url = FailsafeXsltTest.class.getClassLoader().getResource(SAMPLES_PATH + name);
    assertNotNull(url, "Fixture not found on classpath: " + SAMPLES_PATH + name);
    return new String(url.openStream().readAllBytes(), StandardCharsets.UTF_8);
  }

  private static URL failsafeUrl() {
    URL url = FailsafeXsltTest.class.getClassLoader()
        .getResource("org/pageseeder/berlioz/xslt/failsafe-error-html.xsl");
    assertNotNull(url, "Failsafe XSLT not found on classpath");
    return url;
  }

  private static void writePreview(String fixture, String html) throws IOException {
    Path outDir = Paths.get("build/error-samples");
    Files.createDirectories(outDir);
    String name = fixture.endsWith(".xml") ? fixture.substring(0, fixture.length() - 4) : fixture;
    Path out = outDir.resolve(name + ".html");
    Files.writeString(out, html, StandardCharsets.UTF_8);
  }

}
