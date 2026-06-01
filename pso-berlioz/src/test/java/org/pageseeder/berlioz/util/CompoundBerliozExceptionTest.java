package org.pageseeder.berlioz.util;

import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.BerliozErrorID;

import static org.junit.jupiter.api.Assertions.*;

class CompoundBerliozExceptionTest {

  @Test
  void testConstructor_messageAndCollector() {
    ErrorCollector<Exception> collector = new ErrorCollector<>();
    CompoundBerliozException ex = new CompoundBerliozException("message", collector);
    assertEquals("message", ex.getMessage());
    assertNotNull(ex.getCollector());
  }

  @Test
  void testConstructor_messageExceptionAndCollector() {
    ErrorCollector<Exception> collector = new ErrorCollector<>();
    Exception cause = new Exception("cause");
    CompoundBerliozException ex = new CompoundBerliozException("message", cause, collector);
    assertEquals("message", ex.getMessage());
    assertSame(cause, ex.getCause());
    assertNotNull(ex.getCollector());
  }

  @Test
  void testConstructor_messageIdAndCollector() {
    ErrorCollector<Exception> collector = new ErrorCollector<>();
    CompoundBerliozException ex = new CompoundBerliozException("message", BerliozErrorID.SERVICES_NOT_FOUND, collector);
    assertEquals("message", ex.getMessage());
    assertNotNull(ex.getCollector());
  }

  @Test
  void testConstructor_allArgs() {
    ErrorCollector<Exception> collector = new ErrorCollector<>();
    Exception cause = new Exception("cause");
    CompoundBerliozException ex = new CompoundBerliozException("message", cause, BerliozErrorID.SERVICES_NOT_FOUND, collector);
    assertEquals("message", ex.getMessage());
    assertSame(cause, ex.getCause());
    assertNotNull(ex.getCollector());
  }

  @Test
  void testGetCollector_sameInstance() {
    ErrorCollector<Exception> collector = new ErrorCollector<>();
    CompoundBerliozException ex = new CompoundBerliozException("message", collector);
    assertSame(collector, ex.getCollector());
  }
}
