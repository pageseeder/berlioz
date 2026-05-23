package org.pageseeder.berlioz.http;

import org.junit.Assert;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;

import java.util.Collection;

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

  @Test
  public void testAddMetricMethods() {
    ServerTimingHeader header = new ServerTimingHeader();
    header.addMetric("miss");
    header.addMetric("db", 53);
    header.addMetric("dc", "atl");
    header.addMetric("app", "App Render", 47.2);
    Assert.assertEquals("miss, db;dur=53, dc;desc=atl, app;desc=\"App Render\";dur=47.2", header.toValue());
  }

  @Test
  public void testAddMetricNano() {
    ServerTimingHeader header = new ServerTimingHeader();
    header.addMetricNano("db", 1234567);
    header.addMetricNano("app", "App Render", 2000000);
    Assert.assertEquals("db;dur=1.235, app;desc=\"App Render\";dur=2", header.toValue());
  }

  @Test
  public void testAddHeaderTo() {
    ServerTimingHeader header = new ServerTimingHeader();
    header.addMetric("db", 53);
    HttpTestSupport.ResponseRecorder recorder = HttpTestSupport.response();
    HttpServletResponse response = recorder.build();
    header.addHeaderTo(response);
    Assert.assertEquals("db;dur=53", recorder.header(HttpHeaders.SERVER_TIMING));
  }

  @Test
  public void testStaticAddMetricNano() {
    HttpTestSupport.ResponseRecorder recorder = HttpTestSupport.response();
    HttpServletResponse response = recorder.build();
    ServerTimingHeader.addMetricNano(response, "app", "App Render", 2000000);

    Collection<String> values = recorder.headers(HttpHeaders.SERVER_TIMING);
    Assert.assertEquals(1, values.size());
    Assert.assertEquals("app;desc=\"App Render\";dur=2", values.iterator().next());
  }

}
