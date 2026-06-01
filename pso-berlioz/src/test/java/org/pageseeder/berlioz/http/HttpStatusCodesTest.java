package org.pageseeder.berlioz.http;

import org.junit.Assert;
import org.junit.Test;

public class HttpStatusCodesTest {

  @Test
  public void testGetTitle_KnownCodes() {
    Assert.assertEquals("OK", HttpStatusCodes.getTitle(200));
    Assert.assertEquals("Moved Permanently", HttpStatusCodes.getTitle(301));
    Assert.assertEquals("Not Found", HttpStatusCodes.getTitle(404));
    Assert.assertEquals("Internal Server Error", HttpStatusCodes.getTitle(500));
  }

  @Test
  public void testGetTitle_UnknownCode() {
    Assert.assertNull(HttpStatusCodes.getTitle(199));
    Assert.assertNull(HttpStatusCodes.getTitle(599));
  }

  @Test
  public void testGetClassOfStatus() {
    Assert.assertEquals("Informational", HttpStatusCodes.getClassOfStatus(100));
    Assert.assertEquals("Successful", HttpStatusCodes.getClassOfStatus(204));
    Assert.assertEquals("Redirection", HttpStatusCodes.getClassOfStatus(304));
    Assert.assertEquals("Client Error", HttpStatusCodes.getClassOfStatus(404));
    Assert.assertEquals("Server Error", HttpStatusCodes.getClassOfStatus(503));
  }

  @Test
  public void testGetClassOfStatus_OutOfRange() {
    Assert.assertNull(HttpStatusCodes.getClassOfStatus(99));
    Assert.assertNull(HttpStatusCodes.getClassOfStatus(600));
  }

}
