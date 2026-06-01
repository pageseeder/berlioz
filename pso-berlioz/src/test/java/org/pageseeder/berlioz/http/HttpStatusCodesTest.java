package org.pageseeder.berlioz.http;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class HttpStatusCodesTest {

  @Test
  void testGetTitle_KnownCodes() {
    Assertions.assertEquals(HttpStatusCodes.getTitle(200), "OK");
    Assertions.assertEquals(HttpStatusCodes.getTitle(301), "Moved Permanently");
    Assertions.assertEquals(HttpStatusCodes.getTitle(404), "Not Found");
    Assertions.assertEquals(HttpStatusCodes.getTitle(500), "Internal Server Error");
  }

  @Test
  void testGetTitle_UnknownCode() {
    Assertions.assertNull(HttpStatusCodes.getTitle(199));
    Assertions.assertNull(HttpStatusCodes.getTitle(599));
  }

  @Test
  void testGetClassOfStatus() {
    Assertions.assertEquals(HttpStatusCodes.getClassOfStatus(100), "Informational");
    Assertions.assertEquals(HttpStatusCodes.getClassOfStatus(204), "Successful");
    Assertions.assertEquals(HttpStatusCodes.getClassOfStatus(304), "Redirection");
    Assertions.assertEquals(HttpStatusCodes.getClassOfStatus(404), "Client Error");
    Assertions.assertEquals(HttpStatusCodes.getClassOfStatus(503), "Server Error");
  }

  @Test
  void testGetClassOfStatus_OutOfRange() {
    Assertions.assertNull(HttpStatusCodes.getClassOfStatus(99));
    Assertions.assertNull(HttpStatusCodes.getClassOfStatus(600));
  }

}
