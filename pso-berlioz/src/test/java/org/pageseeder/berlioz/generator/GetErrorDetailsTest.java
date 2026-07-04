package org.pageseeder.berlioz.generator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.servlet.ErrorHandlerServlet;
import org.pageseeder.berlioz.xml.XmlStringBuilder;

import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

class GetErrorDetailsTest {

  @BeforeAll
  @SuppressWarnings("unchecked")
  static void initSettings() throws ReflectiveOperationException {
    Field f = GlobalSettings.class.getDeclaredField("SETTINGS");
    f.setAccessible(true);
    ((AtomicReference<Map<String, String>>) f.get(null)).compareAndSet(null, new HashMap<>());
  }

  // http-code and http-class
  // ---------------------------------------------------------------------------

  @Test
  void testNoAttributesDefaultsToStatusCode200() {
    GeneratorTestSupport.RequestBuilder builder = GeneratorTestSupport.request();
    String out = process(builder);
    Assertions.assertTrue(out.contains("http-code=\"200\""));
    Assertions.assertTrue(out.contains("http-class=\"successful\""));
  }

  @Test
  void test404ProducesClientErrorClass() {
    GeneratorTestSupport.RequestBuilder builder = GeneratorTestSupport.request()
        .attribute(ErrorHandlerServlet.ERROR_STATUS_CODE, HttpServletResponse.SC_NOT_FOUND);
    String out = process(builder);
    Assertions.assertTrue(out.contains("http-code=\"404\""));
    Assertions.assertTrue(out.contains("http-class=\"client-error\""));
  }

  @Test
  void test500ProducesServerErrorClass() {
    GeneratorTestSupport.RequestBuilder builder = GeneratorTestSupport.request()
        .attribute(ErrorHandlerServlet.ERROR_STATUS_CODE, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    String out = process(builder);
    Assertions.assertTrue(out.contains("http-code=\"500\""));
    Assertions.assertTrue(out.contains("http-class=\"server-error\""));
  }

  // Optional elements
  // ---------------------------------------------------------------------------

  @Test
  void testMessageAttributeWritesMessageElement() {
    GeneratorTestSupport.RequestBuilder builder = GeneratorTestSupport.request()
        .attribute(ErrorHandlerServlet.ERROR_STATUS_CODE, 404)
        .attribute(ErrorHandlerServlet.ERROR_MESSAGE, "The page was not found");
    String out = process(builder);
    Assertions.assertTrue(out.contains("<message>The page was not found</message>"));
  }

  @Test
  void testRequestUriAttributeWritesRequestUriElement() {
    GeneratorTestSupport.RequestBuilder builder = GeneratorTestSupport.request()
        .attribute(ErrorHandlerServlet.ERROR_STATUS_CODE, 404)
        .attribute(ErrorHandlerServlet.ERROR_REQUEST_URI, "/missing/page");
    String out = process(builder);
    Assertions.assertTrue(out.contains("<request-uri>/missing/page</request-uri>"));
  }

  @Test
  void testExceptionNotIncludedAtMinimalDetailLevel() {
    GeneratorTestSupport.RequestBuilder builder = GeneratorTestSupport.request()
        .attribute(ErrorHandlerServlet.ERROR_STATUS_CODE, 500)
        .attribute(ErrorHandlerServlet.ERROR_EXCEPTION, new IllegalStateException("boom"));
    String out = process(builder);
    Assertions.assertFalse(out.contains("IllegalStateException"), "Exception class should not appear at minimal detail level");
    Assertions.assertFalse(out.contains("<exception"), "Exception element should not appear at minimal detail level");
  }

  @Test
  void testOutputIncludesBerliozElement() {
    String out = process(GeneratorTestSupport.request());
    Assertions.assertTrue(out.contains("<berlioz"), "Berlioz version element should be present");
  }

  // Status propagation
  // ---------------------------------------------------------------------------

  @Test
  void testStatusReturnedInResponse() {
    GeneratorTestSupport.RequestBuilder builder = GeneratorTestSupport.request()
        .attribute(ErrorHandlerServlet.ERROR_STATUS_CODE, HttpServletResponse.SC_NOT_FOUND);
    ContentRequest req = builder.build();
    XmlStringBuilder xml = new XmlStringBuilder();
    Response response = new GetErrorDetails().generate(req, xml);
    Assertions.assertEquals(ContentStatus.NOT_FOUND, response.status());
  }

  // helpers
  // ---------------------------------------------------------------------------

  private static String process(GeneratorTestSupport.RequestBuilder builder) {
    GetErrorDetails gen = new GetErrorDetails();
    ContentRequest req = builder.build();
    XmlStringBuilder xml = new XmlStringBuilder();
    gen.generate(req, xml);
    return xml.toString();
  }
}
