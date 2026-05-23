package org.pageseeder.berlioz.http;

import org.junit.Assert;
import org.junit.Test;

public class HttpMethodTest {

  @Test
  public void testIsMappable() {
    Assert.assertTrue(HttpMethod.GET.isMappable());
    Assert.assertTrue(HttpMethod.POST.isMappable());
    Assert.assertTrue(HttpMethod.PUT.isMappable());
    Assert.assertTrue(HttpMethod.PATCH.isMappable());
    Assert.assertTrue(HttpMethod.DELETE.isMappable());
    Assert.assertFalse(HttpMethod.HEAD.isMappable());
    Assert.assertFalse(HttpMethod.OPTIONS.isMappable());
  }

  @Test
  public void testMappable() {
    Assert.assertTrue(HttpMethod.mappable().contains(HttpMethod.GET));
    Assert.assertTrue(HttpMethod.mappable().contains(HttpMethod.POST));
    Assert.assertTrue(HttpMethod.mappable().contains(HttpMethod.PUT));
    Assert.assertTrue(HttpMethod.mappable().contains(HttpMethod.PATCH));
    Assert.assertTrue(HttpMethod.mappable().contains(HttpMethod.DELETE));
    Assert.assertFalse(HttpMethod.mappable().contains(HttpMethod.HEAD));
    Assert.assertFalse(HttpMethod.mappable().contains(HttpMethod.OPTIONS));
  }

}
