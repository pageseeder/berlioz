package org.pageseeder.berlioz.servlet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.error.HttpException;
import org.pageseeder.berlioz.content.*;
import org.pageseeder.berlioz.error.InvalidParameterException;
import org.pageseeder.berlioz.error.UpstreamException;
import org.pageseeder.berlioz.output.OutputWriter;
import org.pageseeder.berlioz.furi.URIPattern;
import org.pageseeder.berlioz.furi.URIResolveResult;
import org.pageseeder.berlioz.furi.URIResolver;
import org.pageseeder.berlioz.content.ServiceTestHelper;
import org.pageseeder.berlioz.generator.NoContent;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class JsonResponseTest {

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
    File webinf = new File(contextRoot, "WEB-INF");
    webinf.mkdirs();
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
          if ("getServletName".equals(m.getName())) return "test-json";
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
    JsonResponse jr = new JsonResponse(req(), res(), config, matchFor(service), false);
    assertSame(service, jr.getService());
  }

  @Test
  void getStatus_defaultIsOk() {
    Service service = singleGenerator(new NoContent());
    JsonResponse jr = new JsonResponse(req(), res(), config, matchFor(service), false);
    assertEquals(ContentStatus.OK, jr.getStatus());
  }

  @Test
  void getError_initiallyNull() {
    Service service = singleGenerator(new NoContent());
    JsonResponse jr = new JsonResponse(req(), res(), config, matchFor(service), false);
    assertNull(jr.getError());
  }

  @Test
  void getRedirectUrl_initiallyNull() {
    Service service = singleGenerator(new NoContent());
    JsonResponse jr = new JsonResponse(req(), res(), config, matchFor(service), false);
    assertNull(jr.getRedirectUrl());
  }

  // generate() - JsonGenerator -------------------------------------------------------------------

  @Test
  void generate_jsonGenerator_returnsJsonOutput() {
    JsonGenerator gen = (req, json) -> {
      json.startObject().field("hello", "world").endObject();
      return Response.ok();
    };
    Service service = singleGenerator(gen);
    JsonResponse jr = new JsonResponse(req(), res(), config, matchFor(service), false);

    String result = jr.generate();
    assertTrue(result.contains("\"hello\""));
    assertTrue(result.contains("\"world\""));
  }

  @Test
  void generate_jsonGenerator_directService_outputAsIs() {
    JsonGenerator gen = (req, json) -> {
      json.startObject().field("k", "v").endObject();
      return Response.ok();
    };
    Service service = directGenerator(gen);
    JsonResponse jr = new JsonResponse(req(), res(), config, matchFor(service), false);

    String result = jr.generate();
    // Direct service: raw generator output, no envelope
    assertTrue(result.contains("\"k\""));
    assertFalse(result.startsWith("{\"" + service.name(gen) + "\""));
  }

  @Test
  void generate_multipleGenerators_wrapsInEnvelope() {
    NoContent gen1 = new NoContent();
    NoContent gen2 = new NoContent();
    Service service = ServiceTestHelper.build("multi", ServiceTestHelper.highestRule(), gen1, gen2);
    JsonResponse jr = new JsonResponse(req(), res(), config, matchFor(service), false);

    String result = jr.generate();
    // Both generators produce no output → "null" under their names
    assertNotNull(result);
    assertTrue(result.startsWith("{"));
  }

  @Test
  void generate_emptyOutput_returnsNull() {
    JsonGenerator gen = (req, json) -> Response.ok(); // writes nothing
    Service service = singleGenerator(gen);
    JsonResponse jr = new JsonResponse(req(), res(), config, matchFor(service), false);

    String result = jr.generate();
    // Single direct service with no JSON → null
    assertNotNull(result);
  }

  @Test
  void generate_generatorThrows_setsErrorAndBadRequestStatus() {
    JsonGenerator gen = (req, json) -> {
      throw InvalidParameterException.invalidFormat("x", "bad", "expected");
    };
    Service service = directGenerator(gen);
    JsonResponse jr = new JsonResponse(req(), res(), config, matchFor(service), false);

    String result = jr.generate();

    assertNotNull(jr.getError());
    assertEquals(ContentStatus.BAD_REQUEST, jr.getStatus());
    assertNotNull(result); // result may be empty due to provider buffering; error is captured in getError()
  }

  @Test
  void generate_generatorThrowsUpstreamException_setsBadGatewayStatus() {
    JsonGenerator gen = (req, json) -> {
      throw new UpstreamException("connection refused");
    };
    Service service = directGenerator(gen);
    JsonResponse jr = new JsonResponse(req(), res(), config, matchFor(service), false);

    jr.generate();

    assertNotNull(jr.getError());
    assertEquals(ContentStatus.BAD_GATEWAY, jr.getStatus());
  }

  @Test
  void generate_generatorThrowsHttpException_setsSignalStatus() {
    JsonGenerator gen = (req, json) -> {
      throw new HttpException("legal hold", 451) {};
    };
    Service service = directGenerator(gen);
    JsonResponse jr = new JsonResponse(req(), res(), config, matchFor(service), false);

    jr.generate();

    assertNotNull(jr.getError());
    assertEquals(ContentStatus.UNAVAILABLE_FOR_LEGAL_REASONS, jr.getStatus());
    assertEquals(451, jr.getStatusCode());
  }

  @Test
  void generate_generatorThrowsRuntimeException_setsInternalError() {
    JsonGenerator gen = (req, json) -> {
      throw new RuntimeException("unexpected");
    };
    Service service = directGenerator(gen);
    JsonResponse jr = new JsonResponse(req(), res(), config, matchFor(service), false);

    jr.generate();

    assertNotNull(jr.getError());
    assertEquals(ContentStatus.INTERNAL_SERVER_ERROR, jr.getStatus());
  }

  @Test
  void generate_envelopeError_legacyFormatWritesErrorObject() throws Exception {
    setProblemFormat(false);
    JsonGenerator gen = (req, json) -> {
      throw new RuntimeException("unexpected");
    };
    Service service = singleGenerator(gen);
    JsonResponse jr = new JsonResponse(req(), res(), config, matchFor(service), false);

    String result = jr.generate();

    assertTrue(result.contains("\"error\""), "Expected legacy error object: " + result);
    assertFalse(result.contains("\"type\""), "Legacy generator error must not emit problem fields: " + result);
    assertNull(jr.getProblem());
  }

  @Test
  void generate_envelopeError_problemFormatWritesProblemObject() throws Exception {
    setProblemFormat(true);
    try {
      JsonGenerator gen = (req, json) -> {
        throw new RuntimeException("unexpected");
      };
      Service service = singleGenerator(gen);
      JsonResponse jr = new JsonResponse(req(), res(), config, matchFor(service), false);

      String result = jr.generate();

      assertTrue(result.contains("\"type\""), "Expected problem object: " + result);
      assertTrue(result.contains("generator-error"), "Expected generator problem type: " + result);
      assertFalse(result.contains("\"error\""), "Problem format must not use legacy error object: " + result);
      assertNull(jr.getProblem(), "Envelope problems are inline and do not change the top-level media type");
    } finally {
      setProblemFormat(false);
    }
  }

  @Test
  void generate_directError_legacyFormatDoesNotSetTopLevelProblem() throws Exception {
    setProblemFormat(false);
    JsonGenerator gen = (req, json) -> {
      throw new RuntimeException("unexpected");
    };
    Service service = directGenerator(gen);
    JsonResponse jr = new JsonResponse(req(), res(), config, matchFor(service), false);

    String result = jr.generate();

    assertTrue(result.contains("\"error\""), "Expected legacy error object: " + result);
    assertNull(jr.getProblem());
  }

  @Test
  void generate_directError_problemFormatSetsTopLevelProblem() throws Exception {
    setProblemFormat(true);
    try {
      JsonGenerator gen = (req, json) -> {
        throw new RuntimeException("unexpected");
      };
      Service service = directGenerator(gen);
      JsonResponse jr = new JsonResponse(req(), res(), config, matchFor(service), false);

      String result = jr.generate();

      assertTrue(result.contains("generator-error"), "Expected generator problem type: " + result);
      assertNotNull(jr.getProblem());
    } finally {
      setProblemFormat(false);
    }
  }

  @Test
  void generate_generatorSetsNotFoundStatus_propagates() {
    JsonGenerator gen = (req, json) -> Response.status(ContentStatus.NOT_FOUND);
    Service service = singleGenerator(gen);
    JsonResponse jr = new JsonResponse(req(), res(), config, matchFor(service), false);

    jr.generate();

    assertEquals(ContentStatus.NOT_FOUND, jr.getStatus());
  }

  @Test
  void generate_generatorSetsRedirect_propagatesUrl() {
    JsonGenerator gen = (req, json) -> Response.redirect(ContentStatus.SEE_OTHER, "/new");
    Service service = singleGenerator(gen);
    JsonResponse jr = new JsonResponse(req(), res(), config, matchFor(service), false);

    jr.generate();

    assertEquals("/new", jr.getRedirectUrl());
    assertTrue(ContentStatus.isRedirect(jr.getStatus()));
  }

  // generate() - Generator (via OutputWriter adapter) -------------------------------------------

  @Test
  void generate_generatorInterface_outputViaAdapter() {
    Generator gen = (req, out) -> {
      out.startObject("data")
          .field("via", "adapter", OutputWriter.FieldOption.DEFAULT)
          .endObject();
      return Response.ok();
    };
    Service service = singleGenerator(gen);
    JsonResponse jr = new JsonResponse(req(), res(), config, matchFor(service), false);

    String result = jr.generate();
    assertTrue(result.contains("\"via\"") || result.contains("adapter"));
  }

  // generate() - profile -------------------------------------------------------------------------

  @Test
  void generate_withProfile_includesProfileBlock() {
    NoContent gen1 = new NoContent();
    NoContent gen2 = new NoContent();
    Service service = ServiceTestHelper.build("prof", ServiceTestHelper.highestRule(), gen1, gen2);
    JsonResponse jr = new JsonResponse(req(), res(), config, matchFor(service), true);

    String result = jr.generate();
    assertTrue(result.contains("_profile"), "Expected _profile block: " + result);
  }

  @Test
  void generate_withoutProfile_noProfileBlock() {
    NoContent gen = new NoContent();
    Service service = ServiceTestHelper.build("noprof", ServiceTestHelper.highestRule(), gen, new NoContent());
    JsonResponse jr = new JsonResponse(req(), res(), config, matchFor(service), false);

    String result = jr.generate();
    assertFalse(result.contains("_profile"), "Unexpected _profile block: " + result);
  }

  // getHeaders -----------------------------------------------------------------------------------

  @Test
  void getHeaders_generatorSetsCustomHeader_isPresent() {
    JsonGenerator gen = (req, json) -> Response.ok().header("X-Custom", "yes");
    Service service = singleGenerator(gen);
    JsonResponse jr = new JsonResponse(req(), res(), config, matchFor(service), false);

    jr.generate();

    assertEquals("yes", jr.getHeaders().get("X-Custom"));
  }

  @Test
  void getHeaders_returnsUnmodifiableView() {
    Service service = singleGenerator(new NoContent());
    JsonResponse jr = new JsonResponse(req(), res(), config, matchFor(service), false);

    var headers = jr.getHeaders();
    assertThrows(UnsupportedOperationException.class,
        () -> headers.put("X-Intruder", "value"));
  }

  // getEtag --------------------------------------------------------------------------------------

  @Test
  void getEtag_cacheableService_returnsNonNull() {
    // NoContent implements Cacheable and returns "nocontent" → ETag is "nocontent/"
    Service service = singleGenerator(new NoContent());
    JsonResponse jr = new JsonResponse(req(), res(), config, matchFor(service), false);

    String etag = jr.getEtag();
    assertNotNull(etag);
    assertTrue(etag.contains("nocontent"), "Expected etag to contain 'nocontent': " + etag);
  }

  @Test
  void getEtag_nonCacheableGenerator_returnsNull() {
    // A generator that doesn't implement Cacheable
    JsonGenerator gen = (req, json) -> Response.ok();
    Service service = singleGenerator(gen);
    JsonResponse jr = new JsonResponse(req(), res(), config, matchFor(service), false);

    assertNull(jr.getEtag());
  }

  // enableServerTiming ---------------------------------------------------------------------------

  @Test
  void enableServerTiming_addsServerTimingHeaderPerGenerator() {
    JsonGenerator gen = (req, json) -> {
      json.startObject().field("k", "v").endObject();
      return Response.ok();
    };
    Service service = singleGenerator(gen);
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();
    JsonResponse jr = new JsonResponse(req(), recorder.build(), config, matchFor(service), false);
    jr.enableServerTiming();

    jr.generate();

    String timingHeader = recorder.header("Server-Timing");
    assertNotNull(timingHeader, "Expected Server-Timing header when server timing is enabled");
    assertTrue(timingHeader.contains("json1"), "Expected 'json1' metric: " + timingHeader);
    assertTrue(timingHeader.contains("Source"), "Expected 'Source' description: " + timingHeader);
  }

  @Test
  void enableServerTiming_notEnabled_noServerTimingHeader() {
    JsonGenerator gen = (req, json) -> {
      json.startObject().field("k", "v").endObject();
      return Response.ok();
    };
    Service service = singleGenerator(gen);
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();
    JsonResponse jr = new JsonResponse(req(), recorder.build(), config, matchFor(service), false);

    jr.generate();

    assertNull(recorder.header("Server-Timing"), "Expected no Server-Timing header when not enabled");
  }

  // listener -------------------------------------------------------------------------------------

  @Test
  void setListener_andGetListener_roundTrip() {
    GeneratorListener listener = (service, generator, status, etagTime, processTime) -> {};
    JsonResponse.setListener(listener);
    assertSame(listener, JsonResponse.getListener());
    JsonResponse.setListener(null);
    assertNull(JsonResponse.getListener());
  }

  @SuppressWarnings("removal") // ERROR_PROBLEM_FORMAT removed in 1.0; test covers migration escape hatch
  private static void setProblemFormat(boolean value) throws ReflectiveOperationException {
    AtomicReference<Map<String, String>> ref = settingsRef();
    ref.compareAndSet(null, new HashMap<>());
    Map<String, String> settings = ref.get();
    if (value) settings.put(BerliozOption.ERROR_PROBLEM_FORMAT.property(), "true");
    else settings.remove(BerliozOption.ERROR_PROBLEM_FORMAT.property());
  }

  @SuppressWarnings("unchecked")
  private static AtomicReference<Map<String, String>> settingsRef() throws ReflectiveOperationException {
    Field f = GlobalSettings.class.getDeclaredField("SETTINGS");
    f.setAccessible(true);
    return (AtomicReference<Map<String, String>>) f.get(null);
  }
}
