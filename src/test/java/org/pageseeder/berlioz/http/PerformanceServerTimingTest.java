package org.pageseeder.berlioz.http;

import org.junit.Test;
import org.pageseeder.berlioz.content.ParameterTemplate;
import org.junit.Assert;
import org.junit.Test;
public class PerformanceServerTimingTest {

  @Test(expected = NullPointerException.class)
  public void testContructor_NullName() {
    new PerformanceServerTiming(null, "", 0);
  }

  @Test
  public void testContructor_EmptyDescription() {
    PerformanceServerTiming a = new PerformanceServerTiming("x", null, 1.2);
    PerformanceServerTiming b = new PerformanceServerTiming("x", "", 1.2);
    PerformanceServerTiming c = new PerformanceServerTiming("x", 1.2);
    Assert.assertEquals("", a.description());
    Assert.assertEquals("", b.description());
    Assert.assertEquals("", c.description());
// TODO    Assert.assertEquals(a.duration(), b.duration());
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

}
