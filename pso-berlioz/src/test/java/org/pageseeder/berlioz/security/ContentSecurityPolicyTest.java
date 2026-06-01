package org.pageseeder.berlioz.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ContentSecurityPolicyTest {

  @Test
  void testEmpty() {
    ContentSecurityPolicy empty1 = new ContentSecurityPolicy.Builder().build();
    ContentSecurityPolicy empty2 = new ContentSecurityPolicy.Builder().build();
    Assertions.assertTrue(empty1.isEmpty());
    Assertions.assertEquals(empty1, empty2);
  }

  @Test
  void testDefaultSelf() {
    ContentSecurityPolicy empty = new ContentSecurityPolicy.Builder().build();
    ContentSecurityPolicy self1 = new ContentSecurityPolicy.Builder()
        .set(Directive.DEFAULT_SRC, "'self'")
        .build();
    ContentSecurityPolicy self2 = empty.withValue(Directive.DEFAULT_SRC, "'self'");
    Assertions.assertNotSame(empty, self2);
    Assertions.assertTrue(empty.isEmpty());
    Assertions.assertEquals("'self'", self1.get(Directive.DEFAULT_SRC));
    Assertions.assertEquals("'self'", self2.get(Directive.DEFAULT_SRC));
    Assertions.assertEquals(self1, self2);
  }

  @Test
  void testAdd1() {
    ContentSecurityPolicy empty = new ContentSecurityPolicy.Builder().build();
    ContentSecurityPolicy self1 = new ContentSecurityPolicy.Builder()
        .add(Directive.DEFAULT_SRC, "'self'")
        .build();
    ContentSecurityPolicy self2 = empty.withSource(Directive.DEFAULT_SRC, "'self'");
    Assertions.assertNotSame(empty, self2);
    Assertions.assertTrue(empty.isEmpty());
    Assertions.assertEquals("'self'", self1.get(Directive.DEFAULT_SRC));
    Assertions.assertEquals("'self'", self2.get(Directive.DEFAULT_SRC));
    Assertions.assertEquals(self1, self2);
  }

  @Test
  void testAdd2() {
    ContentSecurityPolicy empty = new ContentSecurityPolicy.Builder().build();
    ContentSecurityPolicy csp1 = new ContentSecurityPolicy.Builder()
        .add(Directive.DEFAULT_SRC, "'self'")
        .add(Directive.DEFAULT_SRC, "https:")
        .build();
    ContentSecurityPolicy csp2 = empty.withSource(Directive.DEFAULT_SRC, "'self'");
    ContentSecurityPolicy csp3 = csp2.withSource(Directive.DEFAULT_SRC, "https:");
    Assertions.assertTrue(empty.isEmpty());
    Assertions.assertNotSame(empty, csp2);
    Assertions.assertNotSame(csp2, csp3);
    Assertions.assertEquals("'self' https:", csp1.get(Directive.DEFAULT_SRC));
    Assertions.assertEquals("'self' https:", csp3.get(Directive.DEFAULT_SRC));
    Assertions.assertEquals(csp1, csp3);
  }

  @Test
  void testRemove() {
    ContentSecurityPolicy empty = new ContentSecurityPolicy.Builder().build();
    ContentSecurityPolicy self = new ContentSecurityPolicy.Builder()
        .set(Directive.DEFAULT_SRC, "'self'")
        .build();
    ContentSecurityPolicy remove1 = self.builder().remove(Directive.DEFAULT_SRC).build();
    ContentSecurityPolicy remove2 = self.without(Directive.DEFAULT_SRC);
    Assertions.assertNull(remove1.get(Directive.DEFAULT_SRC));
    Assertions.assertNull(remove2.get(Directive.DEFAULT_SRC));
    Assertions.assertEquals(empty, remove1);
    Assertions.assertEquals(empty, remove2);
  }
}
