package org.pageseeder.berlioz.http;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class HttpMethodTest {

  @Test
  void testIsMappable() {
    Assertions.assertTrue(HttpMethod.GET.isMappable());
    Assertions.assertTrue(HttpMethod.POST.isMappable());
    Assertions.assertTrue(HttpMethod.PUT.isMappable());
    Assertions.assertTrue(HttpMethod.PATCH.isMappable());
    Assertions.assertTrue(HttpMethod.DELETE.isMappable());
    Assertions.assertTrue(HttpMethod.QUERY.isMappable());
    Assertions.assertFalse(HttpMethod.HEAD.isMappable());
    Assertions.assertFalse(HttpMethod.OPTIONS.isMappable());
  }

  @Test
  void testMappable() {
    Assertions.assertTrue(HttpMethod.mappable().contains(HttpMethod.GET));
    Assertions.assertTrue(HttpMethod.mappable().contains(HttpMethod.POST));
    Assertions.assertTrue(HttpMethod.mappable().contains(HttpMethod.PUT));
    Assertions.assertTrue(HttpMethod.mappable().contains(HttpMethod.PATCH));
    Assertions.assertTrue(HttpMethod.mappable().contains(HttpMethod.DELETE));
    Assertions.assertTrue(HttpMethod.mappable().contains(HttpMethod.QUERY));
    Assertions.assertFalse(HttpMethod.mappable().contains(HttpMethod.HEAD));
    Assertions.assertFalse(HttpMethod.mappable().contains(HttpMethod.OPTIONS));
  }

  @Test
  void testValueOf_query() {
    Assertions.assertEquals(HttpMethod.QUERY, HttpMethod.valueOf("QUERY"));
  }

}
