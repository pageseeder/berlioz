package org.pageseeder.berlioz;

import org.junit.jupiter.api.Test;
import org.pageseeder.xmlwriter.XML.NamespaceAware;
import org.pageseeder.xmlwriter.XMLStringWriter;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class BerliozExceptionTest {

  @Test
  void testConstructor_messageOnly() {
    BerliozException ex = new BerliozException("test message");
    assertEquals("test message", ex.getMessage());
    assertNull(ex.getCause());
    assertNull(ex.id());
  }

  @Test
  void testConstructor_messageAndCause() {
    Exception cause = new RuntimeException("root cause");
    BerliozException ex = new BerliozException("test message", cause);
    assertEquals("test message", ex.getMessage());
    assertSame(cause, ex.getCause());
    assertNull(ex.id());
  }

  @Test
  void testConstructor_messageAndId() {
    BerliozException ex = new BerliozException("test message", BerliozErrorID.SERVICES_NOT_FOUND);
    assertEquals("test message", ex.getMessage());
    assertNull(ex.getCause());
    assertSame(BerliozErrorID.SERVICES_NOT_FOUND, ex.id());
  }

  @Test
  void testConstructor_allArgs() {
    Exception cause = new RuntimeException("root cause");
    BerliozException ex = new BerliozException("test message", cause, BerliozErrorID.SERVICES_NOT_FOUND);
    assertEquals("test message", ex.getMessage());
    assertSame(cause, ex.getCause());
    assertSame(BerliozErrorID.SERVICES_NOT_FOUND, ex.id());
  }

  @Test
  void testId_nullWhenNotSet() {
    BerliozException ex = new BerliozException("no id");
    assertNull(ex.id());
  }

  @Test
  @SuppressWarnings("deprecation")
  void testSetId_updatesId() {
    BerliozException ex = new BerliozException("test");
    assertNull(ex.id());
    ex.setId(BerliozErrorID.SERVICES_NOT_FOUND);
    assertSame(BerliozErrorID.SERVICES_NOT_FOUND, ex.id());
  }

  @Test
  @SuppressWarnings("deprecation")
  void testSetId_overwritesConstructorId() {
    BerliozException ex = new BerliozException("test", BerliozErrorID.SERVICES_NOT_FOUND);
    ex.setId(BerliozErrorID.UNEXPECTED);
    assertSame(BerliozErrorID.UNEXPECTED, ex.id());
  }

  @Test
  void testIsExceptionSubtype() {
    assertInstanceOf(Exception.class, new BerliozException("test"));
  }

  @Test
  @SuppressWarnings("deprecation")
  void testToXML_messageOnly() throws IOException {
    BerliozException ex = new BerliozException("error message");
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    ex.toXML(xml);
    xml.flush();
    String out = xml.toString();
    assertTrue(out.contains("<berlioz-exception>"));
    assertTrue(out.contains("<message>error message</message>"));
    assertTrue(out.contains("<stack-trace>"));
    assertFalse(out.contains("<cause>"));
    assertTrue(out.contains("</berlioz-exception>"));
  }

  @Test
  @SuppressWarnings("deprecation")
  void testToXML_withCause() throws IOException {
    Exception cause = new RuntimeException("root cause");
    BerliozException ex = new BerliozException("error message", cause);
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    ex.toXML(xml);
    xml.flush();
    String out = xml.toString();
    assertTrue(out.contains("<cause>"));
    assertTrue(out.contains("root cause"));
  }

  @Test
  @SuppressWarnings("deprecation")
  void testToXML_stackTracePresent() throws IOException {
    BerliozException ex = new BerliozException("trace test");
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    ex.toXML(xml);
    xml.flush();
    String out = xml.toString();
    assertTrue(out.contains("BerliozException"));
  }
}
