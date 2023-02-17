package org.pageseeder.berlioz.http;

import org.junit.Test;
import org.pageseeder.berlioz.content.ParameterTemplate;
import org.junit.Assert;
import org.junit.Test;
public class PerformanceServerTimingTest {

  @Test(expected = NullPointerException.class)
  public void testContructor_NullName() {
    new PerformanceServerTiming(null, 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testContructor_EmptyName() {
    new PerformanceServerTiming("", 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testContructor_InvalidName() {
    // Illegal names are (),/:;<=>?@[\]{} and "
    new PerformanceServerTiming("{", 0);
  }

  @Test
  public void testContructor_ValidName() {
    PerformanceServerTiming a =new PerformanceServerTiming("a.b!C$D", 0);
    Assert.assertEquals("a.b!C$D", a.name());
  }

  @Test
  public void testContructor_EmptyDescription() {
    PerformanceServerTiming a = new PerformanceServerTiming("x", null, 1.2);
    PerformanceServerTiming b = new PerformanceServerTiming("x", "", 1.2);
    PerformanceServerTiming c = new PerformanceServerTiming("x", 1.2);
    Assert.assertEquals("", a.description());
    Assert.assertEquals("", b.description());
    Assert.assertEquals("", c.description());
  }

  @Test
  public void testContructor_ValidDescription() {
    PerformanceServerTiming t = new PerformanceServerTiming("x", "Test", 1.2);
    Assert.assertEquals("Test", t.description());
  }

  @Test
  public void testContructor_InvalidDescription1() {
    PerformanceServerTiming t = new PerformanceServerTiming("x", "\u00a0", 1.2);
    Assert.assertEquals("_", t.description());
  }

  @Test
  public void testContructor_InvalidDescription2() {
    PerformanceServerTiming t = new PerformanceServerTiming("x", "A\u00A0\u000CB\nC", 1.2);
    Assert.assertEquals("A__B_C", t.description());
  }

  @Test
  public void testContructor_InvalidDescription3() {
    PerformanceServerTiming t = new PerformanceServerTiming("x", "A\n\rB", 1.2);
    Assert.assertEquals("A__B", t.description());
  }

  @Test
  public void testToHeader1() {
    PerformanceServerTiming timing = new PerformanceServerTiming("abc", "xyz", 1.2);
    Assert.assertEquals("abc;desc=xyz;dur=1.2", timing.toHeaderString());
  }

  @Test
  public void testToHeader2() {
    PerformanceServerTiming timing = new PerformanceServerTiming("abc", 1.2);
    Assert.assertEquals("abc;dur=1.2", timing.toHeaderString());
  }

  @Test
  public void testToHeader3() {
    PerformanceServerTiming timing = new PerformanceServerTiming("abc", "Requires quotes",1.2);
    Assert.assertEquals("abc;desc=\"Requires quotes\";dur=1.2", timing.toHeaderString());
  }

  @Test
  public void testToHeader4() {
    PerformanceServerTiming timing = new PerformanceServerTiming("abc", "()",1.2);
    Assert.assertEquals("abc;desc=\"()\";dur=1.2", timing.toHeaderString());
  }

  @Test
  public void testToHeader5() {
    PerformanceServerTiming timing = new PerformanceServerTiming("abc", "\"a\\b\"",1.2);
    Assert.assertEquals("abc;desc=\"\\\"a\\\\b\\\"\";dur=1.2", timing.toHeaderString());
  }

  @Test
  public void testToHeader6() {
    PerformanceServerTiming timing = new PerformanceServerTiming("abc",Math.PI);
    Assert.assertEquals("abc;dur=3.142", timing.toHeaderString());
  }

  @Test
  public void testToHeader7() {
    PerformanceServerTiming timing = new PerformanceServerTiming("abc", 2);
    Assert.assertEquals("abc;dur=2", timing.toHeaderString());
  }

  @Test
  public void testToHeader8() {
    PerformanceServerTiming timing = new PerformanceServerTiming("abc", -1);
    Assert.assertEquals("abc", timing.toHeaderString());
  }

  @Test
  public void testToHeader9() {
    PerformanceServerTiming timing = new PerformanceServerTiming("abc", 0);
    Assert.assertEquals("abc;dur=0", timing.toHeaderString());
  }

  @Test
  public void testToHeader10() {
    PerformanceServerTiming timing = new PerformanceServerTiming("abc", 0.0005);
    Assert.assertEquals("abc;dur=0.001", timing.toHeaderString());
  }

}
