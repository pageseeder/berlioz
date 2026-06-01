package org.pageseeder.berlioz.xml;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXParseException;

import static org.junit.jupiter.api.Assertions.*;

class BerliozErrorHandlerTest {

  @Test
  void testGetInstance_returnsSingleton() {
    assertSame(BerliozErrorHandler.INSTANCE, BerliozErrorHandler.getInstance());
  }

  @Test
  void testError_throwsException() {
    SAXParseException ex = new SAXParseException("parse error", null, null, 1, 1);
    assertThrows(SAXParseException.class, () -> BerliozErrorHandler.INSTANCE.error(ex));
  }

  @Test
  void testFatalError_throwsException() {
    SAXParseException ex = new SAXParseException("fatal error", null, null, 2, 3);
    assertThrows(SAXParseException.class, () -> BerliozErrorHandler.INSTANCE.fatalError(ex));
  }

  @Test
  void testWarning_throwsException() {
    SAXParseException ex = new SAXParseException("warning", null, null, 0, 0);
    assertThrows(SAXParseException.class, () -> BerliozErrorHandler.INSTANCE.warning(ex));
  }

  @Test
  void testError_throwsSameException() {
    SAXParseException ex = new SAXParseException("same", null, null, 1, 1);
    SAXParseException thrown = assertThrows(SAXParseException.class,
        () -> BerliozErrorHandler.INSTANCE.error(ex));
    assertSame(ex, thrown);
  }
}
