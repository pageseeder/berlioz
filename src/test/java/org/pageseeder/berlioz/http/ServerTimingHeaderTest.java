package org.pageseeder.berlioz.http;

import org.junit.Assert;
import org.junit.Test;
import org.pageseeder.berlioz.content.ParameterTemplate;

public class ServerTimingHeaderTest {

  @Test
  public void testToValue1() {
    ServerTimingHeader header = new ServerTimingHeader();
    header.add(new PerformanceServerTiming("miss", -1));
    header.add(new PerformanceServerTiming("db", 53));
    header.add(new PerformanceServerTiming("app", 47.2));
    Assert.assertEquals("miss, db;dur=53, app;dur=47.2", header.toValue());
  }

  @Test
  public void testToValue2() {
    ServerTimingHeader header = new ServerTimingHeader();
    header.add(new PerformanceServerTiming("customView", -1));
    header.add(new PerformanceServerTiming("dc", "atl", -1));
    Assert.assertEquals("customView, dc;desc=atl", header.toValue());
  }

  @Test
  public void testToValue3() {
    ServerTimingHeader header = new ServerTimingHeader();
    header.add(new PerformanceServerTiming("cache", "Cache Read",23.2));
    Assert.assertEquals("cache;desc=\"Cache Read\";dur=23.2", header.toValue());
  }

}
