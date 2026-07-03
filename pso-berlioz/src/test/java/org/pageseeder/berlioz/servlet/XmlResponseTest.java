package org.pageseeder.berlioz.servlet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.error.DetailLevel;
import org.pageseeder.berlioz.error.HttpException;
import org.pageseeder.berlioz.content.*;
import org.pageseeder.berlioz.error.InvalidParameterException;
import org.pageseeder.berlioz.error.UpstreamException;
import org.pageseeder.berlioz.furi.URIPattern;
import org.pageseeder.berlioz.furi.URIResolveResult;
import org.pageseeder.berlioz.furi.URIResolver;
import org.pageseeder.berlioz.generator.NoContent;
import org.pageseeder.berlioz.output.OutputWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class XmlResponseTest {

  private static final File WEB_INF = new File("./src/test/resources/org/pageseeder/berlioz");

  @TempDir
  static Path tmp;

  private static BerliozConfig config;

  @BeforeAll
  static void setup() {
    GlobalSettings.setup(WEB_INF);
    config = buildConfig(tmp.toFile());
  }

  // Helpers --------------------------------------------------------------------------------------

  private static BerliozConfig buildConfig(File contextRoot) {
    new File(contextRoot, "WEB-INF").mkdirs();
    ServletContext ctx = (ServletContext) Proxy.newProxyInstance(
        ServletContext.class.getClassLoader(),
        new Class<?>[]{ServletContext.class},
        (proxy, m, args) -> {
          if ("getRealPath".equals(m.getName())) return contextRoot.getAbsolutePath();
          return ServletTestSupport.defaultValue(m.getReturnType());
        });
    ServletConfig cfg = (ServletConfig) Proxy.newProxyInstance(
        ServletConfig.class.getClassLoader(),
        new Class<?>[]{ServletConfig.class},
        (proxy, m, args) -> {
          if ("getServletContext".equals(m.getName())) return ctx;
          if ("getServletName".equals(m.getName())) return "test-xml";
          if ("getInitParameter".equals(m.getName())) return null;
          return ServletTestSupport.defaultValue(m.getReturnType());
        });
    return BerliozConfig.newConfig(cfg);
  }

  private static MatchingService matchFor(Service service) {
    URIPattern pattern = new URIPattern("/test");
    URIResolveResult result = new URIResolver("/test").resolve(pattern);
    return new MatchingService(service, pattern, result);
  }

  private static Service singleGenerator(BerliozGenerator gen) {
    return ServiceTestHelper.build("test", ServiceTestHelper.highestRule(), gen);
  }

  private static Service directGenerator(BerliozGenerator gen) {
    return ServiceTestHelper.buildDirect("test-direct", ServiceTestHelper.highestRule(), gen);
  }

  private static HttpServletRequest req() {
    return ServletTestSupport.request()
        .scheme("http").host("localhost").port(80)
        .uri("/test").servletPath("/test").pathInfo(null)
        .build();
  }

  private static HttpServletResponse res() {
    return ServletTestSupport.response().build();
  }

  // getService / status / error / redirect -------------------------------------------------------

  @Test
  void getService_returnsMatchedService() {
    Service service = singleGenerator(new NoContent());
    XmlResponse xr = new XmlResponse(req(), res(), config, matchFor(service), false);
    assertSame(service, xr.getService());
  }

  @Test
  void getStatus_defaultIsOk() {
    Service service = singleGenerator(new NoContent());
    XmlResponse xr = new XmlResponse(req(), res(), config, matchFor(service), false);
    assertEquals(ContentStatus.OK, xr.getStatus());
  }

  @Test
  void getError_initiallyNull() {
    Service service = singleGenerator(new NoContent());
    XmlResponse xr = new XmlResponse(req(), res(), config, matchFor(service), false);
    assertNull(xr.getError());
  }

  @Test
  void getRedirectURL_initiallyNull() {
    Service service = singleGenerator(new NoContent());
    XmlResponse xr = new XmlResponse(req(), res(), config, matchFor(service), false);
    assertNull(xr.getRedirectURL());
  }

  // generate() - envelope (non-direct) -----------------------------------------------------------

  @Test
  void generate_xmlGenerator_wrapsInRootEnvelope() throws IOException {
    XmlGenerator gen = (req, xml) -> {
      xml.openElement("data").text("hello").closeElement();
      return Response.ok();
    };
    Service service = singleGenerator(gen);
    XmlResponse xr = new XmlResponse(req(), res(), config, matchFor(service), false);

    String result = xr.generate();

    assertTrue(result.contains("<root"), "Expected <root> envelope: " + result);
    assertTrue(result.contains("<content"), "Expected <content> element: " + result);
    assertTrue(result.contains("data"), "Expected generator output in result: " + result);
  }

  @Test
  void generate_xmlGenerator_contentElementHasGeneratorAttribute() throws IOException {
    XmlGenerator gen = (req, xml) -> Response.ok();
    Service service = singleGenerator(gen);
    XmlResponse xr = new XmlResponse(req(), res(), config, matchFor(service), false);

    String result = xr.generate();

    assertTrue(result.contains("generator="), "Expected generator attribute: " + result);
    assertTrue(result.contains("status="), "Expected status attribute: " + result);
  }

  @Test
  void generate_multipleGenerators_bothAppearInOutput() throws IOException {
    XmlGenerator gen1 = (req, xml) -> {
      xml.openElement("first").closeElement();
      return Response.ok();
    };
    XmlGenerator gen2 = (req, xml) -> {
      xml.openElement("second").closeElement();
      return Response.ok();
    };
    Service service = ServiceTestHelper.build("multi", ServiceTestHelper.highestRule(), gen1, gen2);
    XmlResponse xr = new XmlResponse(req(), res(), config, matchFor(service), false);

    String result = xr.generate();

    assertTrue(result.contains("first"), "Expected first generator output: " + result);
    assertTrue(result.contains("second"), "Expected second generator output: " + result);
  }

  @Test
  void generate_headerSkipsLongParameterNamesAndTruncatesLongValues() throws IOException {
    String longName = "n".repeat(101);
    String longValue = "v".repeat(2_001);
    HttpServletRequest req = ServletTestSupport.request()
        .scheme("http").host("localhost").port(80)
        .uri("/test").servletPath("/test").pathInfo(null)
        .parameter(longName, "hidden")
        .parameter("long-value", longValue)
        .parameter("short-value", "visible")
        .build();
    Service service = singleGenerator(new NoContent());
    XmlResponse xr = new XmlResponse(req, res(), config, matchFor(service), false);

    String result = xr.generate();

    assertAll(
        () -> assertFalse(result.contains("hidden"), "parameter with long name must be skipped"),
        () -> assertTrue(result.contains("truncated=\"true\""), "long value should be marked as truncated"),
        () -> assertFalse(result.contains(longValue), "full long value must not be emitted"),
        () -> assertTrue(result.contains("visible"), "ordinary parameter should still be emitted")
    );
  }

  @Test
  void generate_headerWritesGeneratedSecurityNonce() throws Exception {
    setOption(BerliozOption.NONCE_ENABLE, "true");
    setOption(BerliozOption.NONCE_ATTRIBUTE, "cspNonce");
    try {
      Service service = singleGenerator(new NoContent());
      XmlResponse xr = new XmlResponse(req(), res(), config, matchFor(service), false);

      String result = xr.generate();

      assertAll(
          () -> assertTrue(result.contains("<security"), "security element should be emitted"),
          () -> assertTrue(result.contains("nonce="), "nonce attribute should be emitted"),
          () -> assertTrue(result.contains("source=\"berlioz\""), "generated nonce should name Berlioz as source")
      );
    } finally {
      clearOption(BerliozOption.NONCE_ENABLE);
      clearOption(BerliozOption.NONCE_ATTRIBUTE);
    }
  }

  @Test
  void generate_headerSuppressesInvalidAttributeNonce() throws Exception {
    setOption(BerliozOption.NONCE_ENABLE, "true");
    setOption(BerliozOption.NONCE_ATTRIBUTE, "cspNonce");
    try {
      HttpServletRequest req = ServletTestSupport.request()
          .scheme("http").host("localhost").port(80)
          .uri("/test").servletPath("/test").pathInfo(null)
          .attribute("cspNonce", "not a nonce!")
          .build();
      Service service = singleGenerator(new NoContent());
      XmlResponse xr = new XmlResponse(req, res(), config, matchFor(service), false);

      String result = xr.generate();

      assertAll(
          () -> assertTrue(result.contains("invalid nonce"), "invalid nonce should be commented"),
          () -> assertFalse(result.contains("<security"), "invalid nonce should not be emitted")
      );
    } finally {
      clearOption(BerliozOption.NONCE_ENABLE);
      clearOption(BerliozOption.NONCE_ATTRIBUTE);
    }
  }

  @Test
  void generate_generatorInterface_adaptsToXml() throws IOException {
    Generator gen = (req, out) -> {
      out.startObject("data").field("key", "value", OutputWriter.FieldOption.DEFAULT).endObject();
      return Response.ok();
    };
    Service service = singleGenerator(gen);
    XmlResponse xr = new XmlResponse(req(), res(), config, matchFor(service), false);

    String result = xr.generate();

    assertNotNull(result);
    assertTrue(result.contains("<content"), "Expected content element: " + result);
  }

  @Test
  void generate_legacyContentGenerator_writesXml() throws IOException {
    ContentGenerator gen = (req, xml) -> {
      xml.openElement("legacy");
      xml.attribute("key", "value");
      xml.closeElement();
    };
    Service service = singleGenerator(gen);
    XmlResponse xr = new XmlResponse(req(), res(), config, matchFor(service), false);

    String result = xr.generate();

    assertTrue(result.contains("legacy"), "Expected legacy generator output: " + result);
  }

  // generate() - direct service ------------------------------------------------------------------

  @Test
  void generate_directService_noEnvelope() throws IOException {
    XmlGenerator gen = (req, xml) -> {
      xml.openElement("raw").closeElement();
      return Response.ok();
    };
    Service service = directGenerator(gen);
    XmlResponse xr = new XmlResponse(req(), res(), config, matchFor(service), false);

    String result = xr.generate();

    assertFalse(result.contains("<root"), "Direct service must not have <root> envelope: " + result);
    assertTrue(result.contains("raw"), "Expected raw generator output: " + result);
  }

  @Test
  void generate_directService_invalidParameter_badRequestStatus() throws IOException {
    XmlGenerator gen = (req, xml) -> {
      throw InvalidParameterException.invalidFormat("p", "bad", "expected");
    };
    Service service = directGenerator(gen);
    XmlResponse xr = new XmlResponse(req(), res(), config, matchFor(service), false);

    xr.generate();

    assertNotNull(xr.getError());
    assertEquals(ContentStatus.BAD_REQUEST, xr.getStatus());
  }

  @Test
  void generate_directService_upstreamException_badGatewayStatus() throws IOException {
    XmlGenerator gen = (req, xml) -> {
      throw new UpstreamException("connection refused");
    };
    Service service = directGenerator(gen);
    XmlResponse xr = new XmlResponse(req(), res(), config, matchFor(service), false);

    xr.generate();

    assertNotNull(xr.getError());
    assertEquals(ContentStatus.BAD_GATEWAY, xr.getStatus());
  }

  @Test
  void generate_envelopeService_upstreamException_badGatewayStatus() throws IOException {
    XmlGenerator gen = (req, xml) -> {
      throw new UpstreamException("timeout", "search-api");
    };
    Service service = singleGenerator(gen);
    XmlResponse xr = new XmlResponse(req(), res(), config, matchFor(service), false);

    xr.generate();

    assertNotNull(xr.getError());
    assertEquals(ContentStatus.BAD_GATEWAY, xr.getStatus());
  }

  @Test
  void generate_directService_httpException_signalStatus() throws IOException {
    XmlGenerator gen = (req, xml) -> {
      throw new HttpException("legal hold", 451) {};
    };
    Service service = directGenerator(gen);
    XmlResponse xr = new XmlResponse(req(), res(), config, matchFor(service), false);

    xr.generate();

    assertNotNull(xr.getError());
    assertEquals(ContentStatus.UNAVAILABLE_FOR_LEGAL_REASONS, xr.getStatus());
    assertEquals(451, xr.getStatusCode());
  }

  @Test
  void generate_directService_runtimeException_internalServerError() throws IOException {
    XmlGenerator gen = (req, xml) -> {
      throw new RuntimeException("boom");
    };
    Service service = directGenerator(gen);
    XmlResponse xr = new XmlResponse(req(), res(), config, matchFor(service), false);

    xr.generate();

    assertNotNull(xr.getError());
    assertEquals(ContentStatus.INTERNAL_SERVER_ERROR, xr.getStatus());
  }

  // generate() - status propagation -------------------------------------------------------------

  @Test
  void generate_generatorSetsNotFoundStatus_propagates() throws IOException {
    XmlGenerator gen = (req, xml) -> Response.status(ContentStatus.NOT_FOUND);
    Service service = singleGenerator(gen);
    XmlResponse xr = new XmlResponse(req(), res(), config, matchFor(service), false);

    xr.generate();

    assertEquals(ContentStatus.NOT_FOUND, xr.getStatus());
  }

  @Test
  void generate_generatorSetsRedirect_propagatesUrl() throws IOException {
    XmlGenerator gen = (req, xml) -> Response.redirect(ContentStatus.SEE_OTHER, "/new");
    Service service = singleGenerator(gen);
    XmlResponse xr = new XmlResponse(req(), res(), config, matchFor(service), false);

    xr.generate();

    assertEquals("/new", xr.getRedirectURL());
    assertTrue(ContentStatus.isRedirect(xr.getStatus()));
  }

  @Test
  void generate_envelopeError_legacyFormatWritesBerliozException() throws Exception {
    setProblemFormat(false);
    setDetailLevel(DetailLevel.FULL);
    try {
      XmlGenerator gen = (req, xml) -> {
        throw new RuntimeException("gen-error");
      };
      Service service = singleGenerator(gen);
      XmlResponse xr = new XmlResponse(req(), res(), config, matchFor(service), false);

      String result = xr.generate();

      assertTrue(result.contains("<berlioz-exception"), "Expected legacy exception element: " + result);
      assertTrue(result.contains("Unexpected exception caught"), "Expected legacy exception details: " + result);
      assertTrue(result.contains("<cause"), "Expected legacy exception cause: " + result);
      assertFalse(result.contains("<problem>"), "Legacy format must not emit problem element: " + result);
    } finally {
      setDetailLevel(DetailLevel.FULL);
    }
  }

  @Test
  void generate_envelopeError_legacyFormatMinimalLevelWritesEmptyElement() throws Exception {
    setProblemFormat(false);
    setDetailLevel(DetailLevel.MINIMAL);
    try {
      XmlGenerator gen = (req, xml) -> {
        throw new RuntimeException("gen-error");
      };
      Service service = singleGenerator(gen);
      XmlResponse xr = new XmlResponse(req(), res(), config, matchFor(service), false);

      String result = xr.generate();

      assertTrue(result.contains("<berlioz-exception"), "Expected legacy exception element: " + result);
      assertFalse(result.contains("gen-error"), "MINIMAL must not include exception message: " + result);
      assertFalse(result.contains("<cause"), "MINIMAL must not include cause chain: " + result);
    } finally {
      setDetailLevel(DetailLevel.FULL);
    }
  }

  @Test
  void generate_envelopeError_problemFormatWritesProblemElement() throws Exception {
    setProblemFormat(true);
    try {
      XmlGenerator gen = (req, xml) -> {
        throw new RuntimeException("gen-error");
      };
      Service service = singleGenerator(gen);
      XmlResponse xr = new XmlResponse(req(), res(), config, matchFor(service), false);

      String result = xr.generate();

      assertTrue(result.contains("<problem>"), "Expected inline <problem> element: " + result);
      assertTrue(result.contains("generator-error"), "Expected problem type in element: " + result);
      assertFalse(result.contains("<berlioz-exception>"), "Problem format must not emit legacy exception: " + result);
      assertNull(xr.getProblem(), "Envelope problems are inline and do not change the top-level media type");
    } finally {
      setProblemFormat(false);
    }
  }

  @Test
  void generate_directError_problemFormatSetsTopLevelProblem() throws Exception {
    setProblemFormat(true);
    try {
      XmlGenerator gen = (req, xml) -> {
        throw new RuntimeException("gen-error");
      };
      Service service = directGenerator(gen);
      XmlResponse xr = new XmlResponse(req(), res(), config, matchFor(service), false);

      String result = xr.generate();

      assertTrue(result.contains("<problem>"), "Expected top-level problem element: " + result);
      assertTrue(result.contains("generator-error"), "Expected problem type in element: " + result);
      assertNotNull(xr.getProblem());
    } finally {
      setProblemFormat(false);
    }
  }

  @Test
  void generate_directError_legacyFormatWritesBerliozException() throws Exception {
    setProblemFormat(false);
    setDetailLevel(DetailLevel.FULL);
    try {
      XmlGenerator gen = (req, xml) -> {
        throw new RuntimeException("gen-error");
      };
      Service service = directGenerator(gen);
      XmlResponse xr = new XmlResponse(req(), res(), config, matchFor(service), false);

      String result = xr.generate();

      assertTrue(result.contains("<berlioz-exception"), "Expected legacy exception element: " + result);
      assertTrue(result.contains("Unexpected exception caught"), "Expected legacy exception message: " + result);
      assertFalse(result.contains("<problem>"), "Legacy format must not emit problem element: " + result);
      assertNull(xr.getProblem());
    } finally {
      setDetailLevel(DetailLevel.FULL);
    }
  }

  // generate() - profile -------------------------------------------------------------------------

  @Test
  void generate_withProfile_addsProfileAttributes() throws IOException {
    Service service = ServiceTestHelper.build("prof", ServiceTestHelper.highestRule(),
        new NoContent(), new NoContent());
    XmlResponse xr = new XmlResponse(req(), res(), config, matchFor(service), true);

    String result = xr.generate();

    assertTrue(result.contains("profile="), "Expected profile attribute: " + result);
    assertTrue(result.contains("profile-etag="), "Expected profile-etag attribute: " + result);
    assertTrue(result.contains("profile-process="), "Expected profile-process attribute: " + result);
  }

  @Test
  void generate_withoutProfile_noProfileAttributes() throws IOException {
    Service service = singleGenerator(new NoContent());
    XmlResponse xr = new XmlResponse(req(), res(), config, matchFor(service), false);

    String result = xr.generate();

    assertFalse(result.contains("profile="), "Unexpected profile attribute: " + result);
  }

  // enableServerTiming -----------------------------------------------------------------------------

  @Test
  void enableServerTiming_envelopeService_addsServerTimingHeaderPerGenerator() throws IOException {
    Service service = singleGenerator(new NoContent());
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();
    XmlResponse xr = new XmlResponse(req(), recorder.build(), config, matchFor(service), false);
    xr.enableServerTiming();

    xr.generate();

    String timingHeader = recorder.header("Server-Timing");
    assertNotNull(timingHeader, "Expected Server-Timing header when server timing is enabled");
    assertTrue(timingHeader.contains("xml1"), "Expected 'xml1' metric: " + timingHeader);
    assertTrue(timingHeader.contains("Source"), "Expected 'Source' description: " + timingHeader);
  }

  @Test
  void enableServerTiming_directService_addsServerTimingHeaderPerGenerator() throws IOException {
    XmlGenerator gen = (req, xml) -> {
      xml.openElement("raw").closeElement();
      return Response.ok();
    };
    Service service = directGenerator(gen);
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();
    XmlResponse xr = new XmlResponse(req(), recorder.build(), config, matchFor(service), false);
    xr.enableServerTiming();

    xr.generate();

    String timingHeader = recorder.header("Server-Timing");
    assertNotNull(timingHeader, "Expected Server-Timing header when server timing is enabled");
    assertTrue(timingHeader.contains("xml1"), "Expected 'xml1' metric: " + timingHeader);
    assertTrue(timingHeader.contains("Source"), "Expected 'Source' description: " + timingHeader);
  }

  // getHeaders -----------------------------------------------------------------------------------

  @Test
  void getHeaders_generatorSetsCustomHeader_isPresent() throws IOException {
    XmlGenerator gen = (req, xml) -> Response.ok().header("X-Custom", "yes");
    Service service = singleGenerator(gen);
    XmlResponse xr = new XmlResponse(req(), res(), config, matchFor(service), false);

    xr.generate();

    assertEquals("yes", xr.getHeaders().get("X-Custom"));
  }

  @Test
  void getHeaders_returnsUnmodifiableView() {
    Service service = singleGenerator(new NoContent());
    XmlResponse xr = new XmlResponse(req(), res(), config, matchFor(service), false);

    var headers = xr.getHeaders();
    assertThrows(UnsupportedOperationException.class,
        () -> headers.put("X-Intruder", "value"));
  }

  // getEtag --------------------------------------------------------------------------------------

  @Test
  void getEtag_cacheableService_returnsNonNull() {
    Service service = singleGenerator(new NoContent());
    XmlResponse xr = new XmlResponse(req(), res(), config, matchFor(service), false);

    String etag = xr.getEtag();
    assertNotNull(etag);
    assertTrue(etag.contains("nocontent"), "Expected etag to contain 'nocontent': " + etag);
  }

  @Test
  void getEtag_nonCacheableGenerator_returnsNull() {
    XmlGenerator gen = (req, xml) -> Response.ok();
    Service service = singleGenerator(gen);
    XmlResponse xr = new XmlResponse(req(), res(), config, matchFor(service), false);

    assertNull(xr.getEtag());
  }

  @Test
  void getEtag_multipleCacheableGenerators_combinesEtags() {
    Service service = ServiceTestHelper.build("multi-cache", ServiceTestHelper.highestRule(),
        new NoContent(), new NoContent());
    XmlResponse xr = new XmlResponse(req(), res(), config, matchFor(service), false);

    String etag = xr.getEtag();
    assertNotNull(etag);
    // Two NoContent generators each contribute "nocontent/" → combined tag has two segments
    assertEquals(2, etag.chars().filter(c -> c == '/').count(),
        "Expected two '/' separators in combined etag: " + etag);
  }

  // legacy ContentGenerator - legacyResponse edge cases -----------------------------------------

  @Test
  void generate_legacyRedirectWithStatus_propagatesRedirect() throws IOException {
    ContentGenerator gen = (req, xml) -> {
      ((ContentRequest) req).setRedirect("/elsewhere", ContentStatus.SEE_OTHER);
    };
    Service service = singleGenerator(gen);
    XmlResponse xr = new XmlResponse(req(), res(), config, matchFor(service), false);

    xr.generate();

    assertEquals("/elsewhere", xr.getRedirectURL());
    assertTrue(ContentStatus.isRedirect(xr.getStatus()));
  }

  // listener -------------------------------------------------------------------------------------

  @Test
  void setListener_andGetListener_roundTrip() {
    GeneratorListener listener = (service, generator, status, etagTime, processTime) -> {};
    XmlResponse.setListener(listener);
    assertSame(listener, XmlResponse.getListener());
    XmlResponse.setListener(null);
    assertNull(XmlResponse.getListener());
  }

  @SuppressWarnings("removal") // ERROR_PROBLEM_FORMAT removed in 1.0; test covers migration escape hatch
  private static void setProblemFormat(boolean value) throws ReflectiveOperationException {
    AtomicReference<Map<String, String>> ref = settingsRef();
    ref.compareAndSet(null, new HashMap<>());
    Map<String, String> settings = ref.get();
    if (value) settings.put(BerliozOption.ERROR_PROBLEM_FORMAT.property(), "true");
    else settings.remove(BerliozOption.ERROR_PROBLEM_FORMAT.property());
  }

  private static void setDetailLevel(DetailLevel level) throws ReflectiveOperationException {
    AtomicReference<Map<String, String>> ref = settingsRef();
    ref.compareAndSet(null, new HashMap<>());
    Map<String, String> settings = ref.get();
    settings.put(BerliozOption.ERROR_DETAIL.property(), level.name().toLowerCase());
  }

  private static void setOption(BerliozOption option, String value) throws ReflectiveOperationException {
    AtomicReference<Map<String, String>> ref = settingsRef();
    ref.compareAndSet(null, new HashMap<>());
    ref.get().put(option.property(), value);
  }

  private static void clearOption(BerliozOption option) throws ReflectiveOperationException {
    AtomicReference<Map<String, String>> ref = settingsRef();
    ref.compareAndSet(null, new HashMap<>());
    ref.get().remove(option.property());
  }

  @SuppressWarnings("unchecked")
  private static AtomicReference<Map<String, String>> settingsRef() throws ReflectiveOperationException {
    Field f = GlobalSettings.class.getDeclaredField("SETTINGS");
    f.setAccessible(true);
    return (AtomicReference<Map<String, String>>) f.get(null);
  }
}
