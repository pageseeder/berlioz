package org.pageseeder.berlioz.xslt;

import javax.xml.transform.TransformerException;

import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.util.CollectedError.Level;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

class XsltErrorCollectorTest {

  @Test
  void fatalSensitivity_allowsWarningAndErrorButThrowsFatal() {
    XsltErrorCollector collector = collector(XsltErrorSensitivity.FATAL);
    assertDoesNotThrow(() -> collector.warning(new TransformerException("warning")));
    assertDoesNotThrow(() -> collector.error(new TransformerException("error")));
    assertThrows(TransformerException.class,
        () -> collector.fatalError(new TransformerException("fatal")));
  }

  @Test
  void errorSensitivity_allowsWarningButThrowsError() {
    XsltErrorCollector collector = collector(XsltErrorSensitivity.ERROR);
    assertDoesNotThrow(() -> collector.warning(new TransformerException("warning")));
    assertThrows(TransformerException.class,
        () -> collector.error(new TransformerException("error")));
  }

  @Test
  void warningSensitivity_throwsWarning() {
    XsltErrorCollector collector = collector(XsltErrorSensitivity.WARNING);
    assertThrows(TransformerException.class,
        () -> collector.warning(new TransformerException("warning")));
  }

  @Test
  void throwIfThresholdReached_enforcesPostcondition() {
    XsltErrorCollector collector = collector(XsltErrorSensitivity.ERROR);
    collector.collectQuietly(Level.WARNING, new TransformerException("warning"));
    assertDoesNotThrow(collector::throwIfThresholdReached);
    collector.collectQuietly(Level.ERROR, new TransformerException("error"));
    assertThrows(TransformerException.class, collector::throwIfThresholdReached);
  }

  private static XsltErrorCollector collector(XsltErrorSensitivity sensitivity) {
    return new XsltErrorCollector(LoggerFactory.getLogger(XsltErrorCollectorTest.class), sensitivity);
  }
}
