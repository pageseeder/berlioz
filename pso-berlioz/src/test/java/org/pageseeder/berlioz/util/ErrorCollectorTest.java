package org.pageseeder.berlioz.util;

import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.util.CollectedError.Level;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ErrorCollectorTest {

  @Test
  void testInitialState() {
    ErrorCollector<Exception> collector = new ErrorCollector<>();
    assertFalse(collector.hasError());
    assertTrue(collector.getErrors().isEmpty());
  }

  @Test
  void testCollectWarning_doesNotSetFlag() throws Exception {
    ErrorCollector<Exception> collector = new ErrorCollector<>();
    collector.collect(Level.WARNING, new Exception("warn"));
    assertFalse(collector.hasError());
    assertEquals(1, collector.getErrors().size());
  }

  @Test
  void testCollectError_setsFlag() throws Exception {
    ErrorCollector<Exception> collector = new ErrorCollector<>();
    collector.collect(Level.ERROR, new Exception("err"));
    assertTrue(collector.hasError());
  }

  @Test
  void testCollectFatal_throwsAndSetsFlag() {
    ErrorCollector<Exception> collector = new ErrorCollector<>();
    Exception ex = new Exception("fatal");
    assertThrows(Exception.class, () -> collector.collect(Level.FATAL, ex));
    assertTrue(collector.hasError());
  }

  @Test
  void testCollectQuietly_neverThrows() {
    ErrorCollector<Exception> collector = new ErrorCollector<>();
    assertDoesNotThrow(() -> collector.collectQuietly(Level.FATAL, new Exception("fatal")));
    assertTrue(collector.hasError());
  }

  @Test
  void testCollectQuietly_warningDoesNotSetFlag() {
    ErrorCollector<Exception> collector = new ErrorCollector<>();
    collector.collectQuietly(Level.WARNING, new Exception("warn"));
    assertFalse(collector.hasError());
  }

  @Test
  void testGetErrors_returnsAllCollected() throws Exception {
    ErrorCollector<Exception> collector = new ErrorCollector<>();
    collector.collect(Level.WARNING, new Exception("a"));
    collector.collectQuietly(Level.ERROR, new Exception("b"));
    List<CollectedError<Exception>> errors = collector.getErrors();
    assertEquals(2, errors.size());
    assertEquals(Level.WARNING, errors.get(0).level());
    assertEquals(Level.ERROR, errors.get(1).level());
  }

  @Test
  void testSetException_changesThrowThreshold() throws Exception {
    ErrorCollector<Exception> collector = new ErrorCollector<>();
    collector.setException(Level.WARNING);
    Exception ex = new Exception("warn");
    assertThrows(Exception.class, () -> collector.collect(Level.WARNING, ex));
  }

  @Test
  void testSetErrorFlag_changesFlagThreshold() throws Exception {
    ErrorCollector<Exception> collector = new ErrorCollector<>();
    collector.setErrorFlag(Level.WARNING);
    collector.collect(Level.WARNING, new Exception("warn"));
    assertTrue(collector.hasError());
  }

  @Test
  void testSetException_nullThrows() {
    ErrorCollector<Exception> collector = new ErrorCollector<>();
    assertThrows(NullPointerException.class, () -> collector.setException(null));
  }

  @Test
  void testSetErrorFlag_nullThrows() {
    ErrorCollector<Exception> collector = new ErrorCollector<>();
    assertThrows(NullPointerException.class, () -> collector.setErrorFlag(null));
  }
}
