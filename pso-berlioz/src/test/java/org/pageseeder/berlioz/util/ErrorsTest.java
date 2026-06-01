package org.pageseeder.berlioz.util;

import org.junit.jupiter.api.Test;
import org.pageseeder.xmlwriter.XML.NamespaceAware;
import org.pageseeder.xmlwriter.XMLStringWriter;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.LocatorImpl;

import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ErrorsTest {

  @Test
  void testGetStackTrace_unsafe() {
    Exception ex = new RuntimeException("boom");
    String trace = Errors.getStackTrace(ex, false);
    assertTrue(trace.contains("RuntimeException"), "Stack trace should include exception class");
    assertTrue(trace.contains("boom"), "Stack trace should include message");
  }

  @Test
  void testGetStackTrace_safe_stripsServletApi() {
    // Safe mode strips content after javax.servlet.http.HttpServlet.service
    Exception ex = new RuntimeException("test");
    String trace = Errors.getStackTrace(ex, true);
    assertFalse(trace.contains("javax.servlet.http.HttpServlet.service"),
        "Safe stack trace should not include servlet API lines");
  }

  @Test
  void testCleanMessage_noChaining() {
    Exception ex = new RuntimeException("simple message");
    assertEquals("simple message", Errors.cleanMessage(ex));
  }

  @Test
  void testCleanMessage_stripsClassPrefix() {
    Exception cause = new IllegalArgumentException("bad input");
    Exception ex = new RuntimeException(cause.getClass().getName() + ": " + cause.getMessage(), cause);
    assertEquals("bad input", Errors.cleanMessage(ex));
  }

  @Test
  void testCleanMessage_nullMessage() {
    Exception ex = new RuntimeException((String) null);
    assertEquals("No message", Errors.cleanMessage(ex));
  }

  @Test
  void testToXML_exception() throws IOException {
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    Errors.toXML(new RuntimeException("test"), xml);
    String out = xml.toString();
    assertTrue(out.contains("<exception"), "Should wrap in <exception>");
    assertTrue(out.contains("class="), "Should include class attribute");
    assertTrue(out.contains("test"), "Should include message");
  }

  @Test
  void testToXML_saxParseException() throws IOException {
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    SAXParseException ex = new SAXParseException("parse error", "pubId", "sysId", 10, 5);
    Errors.toXML(ex, xml);
    String out = xml.toString();
    assertTrue(out.contains("SAXParseException"), "Should include SAX type");
    assertTrue(out.contains("line=\"10\""), "Should include line number");
  }

  @Test
  void testToXML_transformerException() throws IOException {
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    TransformerException ex = new TransformerException("xslt error");
    Errors.toXML(ex, xml);
    String out = xml.toString();
    assertTrue(out.contains("TransformerException"), "Should include transformer type");
  }

  @Test
  void testToXML_locator_null() throws IOException {
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    // null locator should produce no output
    Errors.toXML((org.xml.sax.Locator) null, xml);
    assertEquals("", xml.toString(), "Null locator should write nothing");
  }

  @Test
  void testToXML_locator() throws IOException {
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    LocatorImpl locator = new LocatorImpl();
    locator.setLineNumber(5);
    locator.setColumnNumber(12);
    locator.setSystemId("file.xml");
    Errors.toXML(locator, xml);
    String out = xml.toString();
    assertTrue(out.contains("<location"), "Should produce <location> element");
    assertTrue(out.contains("line=\"5\""), "Should include line number");
    assertTrue(out.contains("column=\"12\""), "Should include column number");
  }

  @Test
  void testToXML_sourceLocator_null() throws IOException {
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    Errors.toXML((SourceLocator) null, xml);
    assertEquals("", xml.toString(), "Null source locator should write nothing");
  }
}
