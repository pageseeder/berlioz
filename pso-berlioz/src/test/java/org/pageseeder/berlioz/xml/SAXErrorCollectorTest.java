package org.pageseeder.berlioz.xml;

import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.util.CollectedError;
import org.pageseeder.berlioz.util.CollectedError.Level;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXParseException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SAXErrorCollectorTest {

  private SAXErrorCollector newCollector() {
    return new SAXErrorCollector(LoggerFactory.getLogger(SAXErrorCollectorTest.class));
  }

  @Test
  void testWarning_collectedAtWarningLevel() throws Exception {
    SAXErrorCollector collector = newCollector();
    SAXParseException ex = new SAXParseException("warn", null, null, 1, 1);
    collector.warning(ex);
    List<CollectedError<SAXParseException>> errors = collector.getErrors();
    assertEquals(1, errors.size());
    assertEquals(Level.WARNING, errors.get(0).level());
    assertFalse(collector.hasError(), "Warning should not set error flag");
  }

  @Test
  void testError_collectedAtErrorLevelAndSetsFlag() throws Exception {
    SAXErrorCollector collector = newCollector();
    SAXParseException ex = new SAXParseException("error", null, null, 5, 3);
    collector.error(ex);
    assertTrue(collector.hasError(), "Error should set error flag");
    assertEquals(1, collector.getErrors().size());
    assertEquals(Level.ERROR, collector.getErrors().get(0).level());
  }

  @Test
  void testFatalError_throwsAndSetsFlag() {
    SAXErrorCollector collector = newCollector();
    SAXParseException ex = new SAXParseException("fatal", null, null, 10, 1);
    assertThrows(SAXParseException.class, () -> collector.fatalError(ex));
    assertTrue(collector.hasError());
  }

  @Test
  void testMultipleErrors_allCollected() throws Exception {
    SAXErrorCollector collector = newCollector();
    collector.warning(new SAXParseException("w1", null, null, 1, 1));
    collector.warning(new SAXParseException("w2", null, null, 2, 1));
    collector.error(new SAXParseException("e1", null, null, 3, 1));
    assertEquals(3, collector.getErrors().size());
  }

  @Test
  void testInitialState() {
    SAXErrorCollector collector = newCollector();
    assertFalse(collector.hasError());
    assertTrue(collector.getErrors().isEmpty());
  }
}
