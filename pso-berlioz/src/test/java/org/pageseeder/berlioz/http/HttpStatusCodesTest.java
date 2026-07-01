package org.pageseeder.berlioz.http;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.content.ContentStatus;

class HttpStatusCodesTest {

  @Test
  void testGetTitle_KnownCodes() {
    Assertions.assertEquals("OK", HttpStatusCodes.getTitle(200));
    Assertions.assertEquals("Non-Authoritative Information", HttpStatusCodes.getTitle(203));
    Assertions.assertEquals("Moved Permanently", HttpStatusCodes.getTitle(301));
    Assertions.assertEquals("Permanent Redirect", HttpStatusCodes.getTitle(308));
    Assertions.assertEquals("Not Found", HttpStatusCodes.getTitle(404));
    Assertions.assertEquals("Payload Too Large", HttpStatusCodes.getTitle(413));
    Assertions.assertEquals("URI Too Long", HttpStatusCodes.getTitle(414));
    Assertions.assertEquals("Unprocessable Entity", HttpStatusCodes.getTitle(422));
    Assertions.assertEquals("Precondition Required", HttpStatusCodes.getTitle(428));
    Assertions.assertEquals("Request Header Fields Too Large", HttpStatusCodes.getTitle(431));
    Assertions.assertEquals("Unavailable For Legal Reasons", HttpStatusCodes.getTitle(451));
    Assertions.assertEquals("Internal Server Error", HttpStatusCodes.getTitle(500));
    Assertions.assertEquals("Insufficient Storage", HttpStatusCodes.getTitle(507));
  }

  @Test
  void testGetTitle_AllContentStatusesHaveTitle() {
    for (ContentStatus status : ContentStatus.values()) {
      Assertions.assertNotNull(HttpStatusCodes.getTitle(status.code()), status.name());
    }
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
