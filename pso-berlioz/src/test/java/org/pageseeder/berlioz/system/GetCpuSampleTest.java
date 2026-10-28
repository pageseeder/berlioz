package org.pageseeder.berlioz.system;

import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.output.JsonOutputAdapter;
import org.pageseeder.berlioz.output.OutputWriter;
import org.pageseeder.berlioz.output.XmlOutputAdapter;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GetCpuSampleTest {

  // generate() tests
  // ---------------------------------------------------------------------------

  @Test
  void testGenerateWritesCpuSample() {
    OutputWriter out = new XmlOutputAdapter();
    new GetCpuSample().generate(emptyRequest(), out);
    String xml = out.toString();
    assertTrue(xml.startsWith("<cpu-sample processors=\""), xml);
    assertTrue(xml.contains(" uptime-ms=\""), xml);
  }

  @Test
  void testGenerateDoesNotSleep() {
    long start = System.nanoTime();
    for (int i = 0; i < 10; i++) {
      new GetCpuSample().generate(emptyRequest(), new XmlOutputAdapter());
    }
    long elapsedMs = (System.nanoTime() - start) / 1_000_000;
    assertTrue(elapsedMs < 1000, "10 samples should not wait, took " + elapsedMs + "ms");
  }

  // sample() tests
  // ---------------------------------------------------------------------------

  @Test
  void testSampleAllValuesXml() {
    OptionalMetrics metrics = metrics(Map.of("ProcessCpuTime", 5_230_000_000L, "CpuLoad", 0.15));
    List<GarbageCollectorMXBean> collectors = List.of(collector(true, 100), collector(true, 20));
    OutputWriter out = new XmlOutputAdapter();
    GetCpuSample.sample(8, 123456L, metrics, collectors, 1.25, out);
    assertEquals("<cpu-sample processors=\"8\" uptime-ms=\"123456\" process-time-ns=\"5230000000\""
        + " gc-time-ms=\"120\" system-load=\"0.15\" load-average=\"1.25\"/>", out.toString());
  }

  @Test
  void testSampleAllValuesJson() {
    OptionalMetrics metrics = metrics(Map.of("ProcessCpuTime", 5_230_000_000L, "CpuLoad", 0.15));
    List<GarbageCollectorMXBean> collectors = List.of(collector(true, 120));
    OutputWriter out = new JsonOutputAdapter();
    GetCpuSample.sample(8, 123456L, metrics, collectors, 1.25, out);
    assertEquals("{\"processors\":8,\"uptimeMs\":123456,\"processTimeNs\":5230000000,"
        + "\"gcTimeMs\":120,\"systemLoad\":0.15,\"loadAverage\":1.25}", out.toString());
  }

  @Test
  void testSampleUnavailableValuesAreAbsent() {
    OptionalMetrics metrics = metrics(Map.of("ProcessCpuTime", -1L, "CpuLoad", -1.0));
    List<GarbageCollectorMXBean> collectors = List.of(collector(true, -1), collector(false, 50));
    OutputWriter out = new XmlOutputAdapter();
    GetCpuSample.sample(4, 1000L, metrics, collectors, -1.0, out);
    assertEquals("<cpu-sample processors=\"4\" uptime-ms=\"1000\"/>", out.toString());
  }

  @Test
  void testSampleFallsBackToSystemCpuLoad() {
    OptionalMetrics metrics = metrics(Map.of("SystemCpuLoad", 0.4567));
    OutputWriter out = new XmlOutputAdapter();
    GetCpuSample.sample(2, 10L, metrics, Collections.emptyList(), -1.0, out);
    assertEquals("<cpu-sample processors=\"2\" uptime-ms=\"10\" system-load=\"0.457\"/>", out.toString());
  }

  // collectionTime() tests
  // ---------------------------------------------------------------------------

  @Test
  void testCollectionTimeNullWhenNoneReports() {
    assertNull(GetCpuSample.collectionTime(Collections.emptyList()));
    assertNull(GetCpuSample.collectionTime(List.of(collector(true, -1))));
  }

  @Test
  void testCollectionTimeIncludesZero() {
    assertEquals(0L, GetCpuSample.collectionTime(List.of(collector(true, 0))));
  }

  // OptionalMetrics tests
  // ---------------------------------------------------------------------------

  @Test
  void testOptionalMetricsIgnoresNonNumbersAndOutOfRange() {
    OptionalMetrics metrics = metrics(Map.of("A", "text", "B", 1.5, "C", 0.5));
    assertNull(metrics.count("A"));
    assertNull(metrics.count("Missing"));
    assertEquals(0.5, metrics.fraction("A", "B", "C"));
    assertNull(metrics.fraction("B"));
  }

  @Test
  void testOptionalMetricsPlatformDoesNotFail() {
    OptionalMetrics metrics = OptionalMetrics.platform();
    assertNull(metrics.count("NoSuchAttribute"));
    assertNull(metrics.fraction("NoSuchAttribute"));
  }

  // helpers
  // ---------------------------------------------------------------------------

  private static OptionalMetrics metrics(Map<String, Object> values) {
    return new OptionalMetrics(values::get);
  }

  private static GarbageCollectorMXBean collector(boolean valid, long time) {
    return (GarbageCollectorMXBean) Proxy.newProxyInstance(
        GarbageCollectorMXBean.class.getClassLoader(),
        new Class<?>[]{GarbageCollectorMXBean.class},
        (proxy, m, args) -> {
          switch (m.getName()) {
            case "isValid": return valid;
            case "getCollectionTime": return time;
            default: return null;
          }
        });
  }

  private static Request emptyRequest() {
    return (Request) Proxy.newProxyInstance(
        Request.class.getClassLoader(),
        new Class<?>[]{Request.class},
        (proxy, m, args) -> null);
  }
}
