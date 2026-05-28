package org.pageseeder.berlioz.generator;

import org.junit.Assert;
import org.junit.Test;
import org.pageseeder.berlioz.BerliozErrorID;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.servlet.ErrorHandlerServlet;
import org.pageseeder.xmlwriter.XML.NamespaceAware;
import org.pageseeder.xmlwriter.XMLStringWriter;

import javax.servlet.http.HttpServletResponse;

public class GetErrorDetailsTest {

  // http-code and http-class
  // ---------------------------------------------------------------------------

  @Test
  public void testNoAttributesDefaultsToStatusCode200() throws Exception {
    GeneratorTestSupport.RequestBuilder builder = GeneratorTestSupport.request();
    String out = process(builder);
    Assert.assertTrue(out.contains("http-code=\"200\""));
    Assert.assertTrue(out.contains("http-class=\"successful\""));
  }

  @Test
  public void test404ProducesClientErrorClass() throws Exception {
    GeneratorTestSupport.RequestBuilder builder = GeneratorTestSupport.request()
        .attribute(ErrorHandlerServlet.ERROR_STATUS_CODE, HttpServletResponse.SC_NOT_FOUND);
    String out = process(builder);
    Assert.assertTrue(out.contains("http-code=\"404\""));
    Assert.assertTrue(out.contains("http-class=\"client-error\""));
  }

  @Test
  public void test500ProducesServerErrorClass() throws Exception {
    GeneratorTestSupport.RequestBuilder builder = GeneratorTestSupport.request()
        .attribute(ErrorHandlerServlet.ERROR_STATUS_CODE, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    String out = process(builder);
    Assert.assertTrue(out.contains("http-code=\"500\""));
    Assert.assertTrue(out.contains("http-class=\"server-error\""));
  }

  // Optional elements
  // ---------------------------------------------------------------------------

  @Test
  public void testMessageAttributeWritesMessageElement() throws Exception {
    GeneratorTestSupport.RequestBuilder builder = GeneratorTestSupport.request()
        .attribute(ErrorHandlerServlet.ERROR_STATUS_CODE, 404)
        .attribute(ErrorHandlerServlet.ERROR_MESSAGE, "The page was not found");
    String out = process(builder);
    Assert.assertTrue(out.contains("<message>The page was not found</message>"));
  }

  @Test
  public void testRequestUriAttributeWritesRequestUriElement() throws Exception {
    GeneratorTestSupport.RequestBuilder builder = GeneratorTestSupport.request()
        .attribute(ErrorHandlerServlet.ERROR_STATUS_CODE, 404)
        .attribute(ErrorHandlerServlet.ERROR_REQUEST_URI, "/missing/page");
    String out = process(builder);
    Assert.assertTrue(out.contains("<request-uri>/missing/page</request-uri>"));
  }

  @Test
  public void testExceptionIsIncludedInOutput() throws Exception {
    GeneratorTestSupport.RequestBuilder builder = GeneratorTestSupport.request()
        .attribute(ErrorHandlerServlet.ERROR_STATUS_CODE, 500)
        .attribute(ErrorHandlerServlet.ERROR_EXCEPTION, new IllegalStateException("boom"));
    String out = process(builder);
    Assert.assertTrue("Exception class should appear in output",
        out.contains("IllegalStateException") || out.contains("exception"));
  }

  // Status propagation
  // ---------------------------------------------------------------------------

  @Test
  public void testStatusSetOnRequest() throws Exception {
    GeneratorTestSupport.RequestBuilder builder = GeneratorTestSupport.request()
        .attribute(ErrorHandlerServlet.ERROR_STATUS_CODE, HttpServletResponse.SC_NOT_FOUND);
    process(builder);
    Assert.assertEquals(ContentStatus.NOT_FOUND, builder.capturedStatus);
  }

  // helpers
  // ---------------------------------------------------------------------------

  private static String process(GeneratorTestSupport.RequestBuilder builder) throws Exception {
    GetErrorDetails gen = new GetErrorDetails();
    ContentRequest req = builder.build();
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    gen.process(req, xml);
    xml.flush();
    return xml.toString();
  }
}
