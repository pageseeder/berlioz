package org.pageseeder.berlioz.servlet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.xml.transform.Templates;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

class XsltTransformResultTest {

  private static Templates htmlTemplates;
  private static Templates xmlTemplates;

  @BeforeAll
  static void compileTemplates() throws Exception {
    TransformerFactory factory = TransformerFactory.newInstance();
    htmlTemplates = factory.newTemplates(new StreamSource(new StringReader(
        "<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>"
        + "<xsl:output method='html' media-type='text/html' encoding='UTF-8'/>"
        + "</xsl:stylesheet>")));
    xmlTemplates = factory.newTemplates(new StreamSource(new StringReader(
        "<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>"
        + "<xsl:output method='xml' media-type='application/xml' encoding='UTF-8'/>"
        + "</xsl:stylesheet>")));
  }

  // Success constructor

  @Test
  void testSuccessStatus() {
    XsltTransformResult r = new XsltTransformResult("<html/>", 1000L, htmlTemplates);
    assertEquals(XsltTransformResult.Status.OK, r.status());
  }

  @Test
  void testSuccessContent() {
    XsltTransformResult r = new XsltTransformResult("<html/>", 1000L, htmlTemplates);
    assertEquals("<html/>", r.content().toString());
  }

  @Test
  void testSuccessTime() {
    XsltTransformResult r = new XsltTransformResult("<html/>", 42_000L, htmlTemplates);
    assertEquals(42_000L, r.time());
  }

  @Test
  void testSuccessNoException() {
    XsltTransformResult r = new XsltTransformResult("<html/>", 1000L, htmlTemplates);
    assertNull(r.getException());
    assertNull(r.getErrorMessage());
  }

  @Test
  void testSuccessMediaTypeFromTemplates() {
    XsltTransformResult html = new XsltTransformResult("", 0L, htmlTemplates);
    assertEquals("text/html", html.getMediaType());

    XsltTransformResult xml = new XsltTransformResult("", 0L, xmlTemplates);
    assertEquals("application/xml", xml.getMediaType());
  }

  // Error constructor

  @Test
  void testErrorStatus() {
    Exception ex = new RuntimeException("xslt failed");
    XsltTransformResult r = new XsltTransformResult("<error/>", ex, null);
    assertEquals(XsltTransformResult.Status.ERROR, r.status());
  }

  @Test
  void testErrorTimeIsZero() {
    XsltTransformResult r = new XsltTransformResult("", new RuntimeException(), null);
    assertEquals(0L, r.time());
  }

  @Test
  void testErrorExceptionIsPreserved() {
    RuntimeException ex = new RuntimeException("bad transform");
    XsltTransformResult r = new XsltTransformResult("", ex, null);
    assertSame(ex, r.getException());
  }

  @Test
  void testErrorWithNullTemplates_usesDefaults() {
    XsltTransformResult r = new XsltTransformResult("", new RuntimeException(), null);
    assertEquals("text/html", r.getMediaType());
    assertEquals("utf-8", r.getEncoding());
  }

  @Test
  void testErrorWithTemplates_usesTemplateOutputProps() {
    XsltTransformResult r = new XsltTransformResult("", new RuntimeException(), xmlTemplates);
    assertEquals("application/xml", r.getMediaType());
  }

  @Test
  void testImplementsBerliozOutput() {
    assertInstanceOf(BerliozOutput.class, new XsltTransformResult("", 0L, htmlTemplates));
  }
}
