package org.pageseeder.berlioz.http;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class HttpStatusCodesTest {

  @Test
  void testGetTitle_KnownCodes() {
    Assertions.assertEquals("OK", HttpStatusCodes.getTitle(200));
    Assertions.assertEquals("Moved Permanently", HttpStatusCodes.getTitle(301));
    Assertions.assertEquals("Not Found", HttpStatusCodes.getTitle(404));
    Assertions.assertEquals("Internal Server Error", HttpStatusCodes.getTitle(500));
  }

  @Test
  void testGetTitle_UnknownCode() {
    Assertions.assertNull(HttpStatusCodes.getTitle(199));
    Assertions.assertNull(HttpStatusCodes.getTitle(599));
  }

  @Test
  void testGetClassOfStatus() {
    Assertions.assertEquals("Informational", HttpStatusCodes.getClassOfStatus(100));
    Assertions.assertEquals("Successful", HttpStatusCodes.getClassOfStatus(204));
    Assertions.assertEquals("Redirection", HttpStatusCodes.getClassOfStatus(304));
    Assertions.assertEquals("Client Error", HttpStatusCodes.getClassOfStatus(404));
    Assertions.assertEquals("Server Error", HttpStatusCodes.getClassOfStatus(503));
  }

  @Test
  void testGetClassOfStatus_OutOfRange() {
    Assertions.assertNull(HttpStatusCodes.getClassOfStatus(99));
    Assertions.assertNull(HttpStatusCodes.getClassOfStatus(600));
  }

}
