package org.pageseeder.berlioz.util;

import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.util.CollectedError.Level;
import org.pageseeder.xmlwriter.XML.NamespaceAware;
import org.pageseeder.xmlwriter.XMLStringWriter;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class CollectedErrorTest {

  @Test
  void testLevel() {
    Exception ex = new Exception("test");
    CollectedError<Exception> c = new CollectedError<>(Level.WARNING, ex);
    assertEquals(Level.WARNING, c.level());
  }

  @Test
  void testError() {
    Exception ex = new Exception("test");
    CollectedError<Exception> c = new CollectedError<>(Level.ERROR, ex);
    assertSame(ex, c.error());
  }

  @Test
  void testNullLevelThrows() {
    Exception ex = new Exception();
    assertThrows(NullPointerException.class, () -> new CollectedError<>(null, ex));
  }

  @Test
  void testNullErrorThrows() {
    assertThrows(NullPointerException.class, () -> new CollectedError<>(Level.ERROR, null));
  }

  @Test
  void testToXML_producesCollectedElement() throws IOException {
    CollectedError<Exception> c = new CollectedError<>(Level.WARNING, new Exception("oops"));
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    c.toXML(xml);
    String out = xml.toString();
    assertTrue(out.contains("<collected"), "Should open <collected> element");
    assertTrue(out.contains("level=\"warning\""), "Should include level attribute");
    assertTrue(out.contains("</collected>"), "Should close element");
  }

  @Test
  void testLevelToString() {
    assertEquals("warning", Level.WARNING.toString());
    assertEquals("error", Level.ERROR.toString());
    assertEquals("fatal", Level.FATAL.toString());
  }
}
