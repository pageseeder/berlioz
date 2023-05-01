package org.pageseeder.berlioz.security;

import org.junit.Assert;
import org.junit.Test;

public class ContentSecurityPolicyTest {

  @Test
  public void testEmpty() {
    ContentSecurityPolicy empty1 = new ContentSecurityPolicy.Builder().build();
    ContentSecurityPolicy empty2 = new ContentSecurityPolicy.Builder().build();
    Assert.assertTrue(empty1.isEmpty());
    Assert.assertEquals(empty1, empty2);
  }

  @Test
  public void testDefaultSelf() {
    ContentSecurityPolicy empty = new ContentSecurityPolicy.Builder().build();
    ContentSecurityPolicy self1 = new ContentSecurityPolicy.Builder()
        .set(Directive.DEFAULT_SRC, "'self'")
        .build();
    ContentSecurityPolicy self2 = empty.withValue(Directive.DEFAULT_SRC, "'self'");
    Assert.assertNotSame(empty, self2);
    Assert.assertTrue(empty.isEmpty());
    Assert.assertEquals("'self'", self1.get(Directive.DEFAULT_SRC));
    Assert.assertEquals("'self'", self2.get(Directive.DEFAULT_SRC));
    Assert.assertEquals(self1, self2);
  }

  @Test
  public void testAdd1() {
    ContentSecurityPolicy empty = new ContentSecurityPolicy.Builder().build();
    ContentSecurityPolicy self1 = new ContentSecurityPolicy.Builder()
        .add(Directive.DEFAULT_SRC, "'self'")
        .build();
    ContentSecurityPolicy self2 = empty.withSource(Directive.DEFAULT_SRC, "'self'");
    Assert.assertNotSame(empty, self2);
    Assert.assertTrue(empty.isEmpty());
    Assert.assertEquals("'self'", self1.get(Directive.DEFAULT_SRC));
    Assert.assertEquals("'self'", self2.get(Directive.DEFAULT_SRC));
    Assert.assertEquals(self1, self2);
  }

  @Test
  public void testAdd2() {
    ContentSecurityPolicy empty = new ContentSecurityPolicy.Builder().build();
    ContentSecurityPolicy csp1 = new ContentSecurityPolicy.Builder()
        .add(Directive.DEFAULT_SRC, "'self'")
        .add(Directive.DEFAULT_SRC, "https:")
        .build();
    ContentSecurityPolicy csp2 = empty.withSource(Directive.DEFAULT_SRC, "'self'");
    ContentSecurityPolicy csp3 = csp2.withSource(Directive.DEFAULT_SRC, "https:");
    Assert.assertTrue(empty.isEmpty());
    Assert.assertNotSame(empty, csp2);
    Assert.assertNotSame(csp2, csp3);
    Assert.assertEquals("'self' https:", csp1.get(Directive.DEFAULT_SRC));
    Assert.assertEquals("'self' https:", csp3.get(Directive.DEFAULT_SRC));
    Assert.assertEquals(csp1, csp3);
  }

  @Test
  public void testRemove() {
    ContentSecurityPolicy empty = new ContentSecurityPolicy.Builder().build();
    ContentSecurityPolicy self = new ContentSecurityPolicy.Builder()
        .set(Directive.DEFAULT_SRC, "'self'")
        .build();
    ContentSecurityPolicy remove1 = self.builder().remove(Directive.DEFAULT_SRC).build();
    ContentSecurityPolicy remove2 = self.without(Directive.DEFAULT_SRC);
    Assert.assertNull(remove1.get(Directive.DEFAULT_SRC));
    Assert.assertNull(remove2.get(Directive.DEFAULT_SRC));
    Assert.assertEquals(empty, remove1);
    Assert.assertEquals(empty, remove2);
  }
}
