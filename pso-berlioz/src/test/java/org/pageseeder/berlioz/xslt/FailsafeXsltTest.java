package org.pageseeder.berlioz.xslt;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.pageseeder.berlioz.servlet.XsltTransformer;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

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
        // Legacy format — known error IDs (contextual help blocks)
        "legacy-transform-not-found.xml",
        "legacy-transform-dynamic-error.xml",
        "legacy-services-not-found.xml",
        "legacy-services-malformed.xml",
        // Problem format (RFC 9457) — no exception extension
        "problem-not-found-404.xml",
        "problem-method-not-allowed-405.xml",
        "problem-server-error-500.xml",
        "problem-invalid-parameter-400.xml",
        "problem-upstream-error-502.xml",
        // Problem format — with exception extension
        "problem-server-error-500-standard.xml",
        "problem-server-error-500-full.xml"
    );
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
