package org.pageseeder.berlioz.xml;

import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.util.WriteFailureException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class XmlExceptionsTest {

  @Test
  void testIllegalCloseElementException_message() {
    IllegalCloseElementException ex = new IllegalCloseElementException();
    assertNotNull(ex.getMessage());
    assertTrue(ex.getMessage().contains("close"), "Message should mention closing");
    assertInstanceOf(IllegalStateException.class, ex);
  }

  @Test
  void testUnclosedElementException_message() {
    UnclosedElementException ex = new UnclosedElementException("div");
    assertNotNull(ex.getMessage());
    assertTrue(ex.getMessage().contains("div"), "Message should include element name");
    assertInstanceOf(IllegalStateException.class, ex);
  }

  @Test
  void testXmlWriteFailureException_wrapsIOException() {
    IOException cause = new IOException("disk full");
    XmlWriteFailureException ex = new XmlWriteFailureException(cause);
    assertSame(cause, ex.getCause());
    assertNotNull(ex.getMessage());
    assertInstanceOf(WriteFailureException.class, ex);
  }
}
