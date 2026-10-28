package org.pageseeder.berlioz.system;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.content.GeneratorListener;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.generator.NoContent;
import org.pageseeder.berlioz.servlet.BerliozConfig;
import org.pageseeder.berlioz.output.JsonOutputAdapter;
import org.pageseeder.berlioz.output.XmlOutputAdapter;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

class GetGeneratorStatisticsTest {

  @BeforeEach
  void setup() {
    reset();
  }

  @AfterEach
  void teardown() {
    reset();
  }

  private void reset() {
    BerliozConfig.setListener(null);
    StatisticsCollector.getInstance().clear();
  }

  @Test
  void testConstructor_registersCollectorAsListener() {
    assertNull(BerliozConfig.getListener(), "Listener should be null before construction");
    new GetGeneratorStatistics();
    GeneratorListener listener = BerliozConfig.getListener();
    assertSame(StatisticsCollector.getInstance(), listener,
        "Constructor should register StatisticsCollector as listener");
  }

  @Test
  void testConstructor_alreadyRegistered_doesNotReplace() {
    new GetGeneratorStatistics(); // registers collector
    GeneratorListener first = BerliozConfig.getListener();
    new GetGeneratorStatistics(); // should leave it as-is
    assertSame(first, BerliozConfig.getListener());
  }

  @Test
  void testProcess_writesStatisticsXml() {
    GetGeneratorStatistics gen = new GetGeneratorStatistics();
    XmlOutputAdapter xml = new XmlOutputAdapter();
    gen.generate(request("false"), xml);
    assertTrue(xml.toString().contains("<statistics"), "Should output <statistics> element");
  }

  @Test
  void testProcess_resetTrue_clearsStats() {
    GetGeneratorStatistics gen = new GetGeneratorStatistics();

    // Populate some data
    StatisticsCollector.getInstance().generate(null, new NoContent(),
        ContentStatus.OK, 1000L, 5000L);

    XmlOutputAdapter xml = new XmlOutputAdapter();
    gen.generate(request("true"), xml);

    assertFalse(xml.toString().contains("<statistic generator="),
        "After reset, statistics should be cleared before output");
  }

  @Test
  void testProcess_resetFalse_retainsStats() {
    GetGeneratorStatistics gen = new GetGeneratorStatistics();
    StatisticsCollector.getInstance().generate(null, new NoContent(),
        ContentStatus.OK, 1000L, 5000L);

    XmlOutputAdapter xml = new XmlOutputAdapter();
    gen.generate(request("false"), xml);

    assertTrue(xml.toString().contains("<statistic generator="), "Without reset, stats should be retained");
  }

  @Test
  void testProcess_xmlFormatUnchanged() {
    GetGeneratorStatistics gen = new GetGeneratorStatistics();
    StatisticsCollector collector = StatisticsCollector.getInstance();
    collector.generate(null, new NoContent(), ContentStatus.OK, 1000L, 5000L);
    collector.generate(null, new NoContent(), ContentStatus.NO_CONTENT, 3000L, 0L);

    XmlOutputAdapter xml = new XmlOutputAdapter();
    gen.generate(request("false"), xml);

    String out = xml.toString();
    assertTrue(out.matches("<statistics since=\"[^\"]+\"><statistic .*</statistic></statistics>"), out);
    assertTrue(out.contains("<statistic generator=\"" + NoContent.class.getName() + "\" count=\"2\""
        + " min-etag=\"1\" min-process=\"0\" max-etag=\"3\" max-process=\"5\""
        + " total-etag=\"4\" total-process=\"5\" avg-etag=\"2\" avg-process=\"2\""
        + " avg-last-etag=\"3\" avg-last-process=\"0\">"
        + "<status ok=\"1\" no_content=\"1\"/></statistic>"), out);
  }

  @Test
  void testProcess_writesStatisticsJson() {
    GetGeneratorStatistics gen = new GetGeneratorStatistics();
    StatisticsCollector collector = StatisticsCollector.getInstance();
    collector.generate(null, new NoContent(), ContentStatus.OK, 1000L, 5000L);
    collector.generate(null, new NoContent(), ContentStatus.NO_CONTENT, 3000L, 0L);

    JsonOutputAdapter json = new JsonOutputAdapter();
    gen.generate(request("false"), json);

    String out = json.toString();
    assertTrue(out.matches("\\{\"since\":\"[^\"]+\",\"statistics\":\\[\\{.*\\}\\]\\}"), out);
    assertTrue(out.contains("{\"generator\":\"" + NoContent.class.getName() + "\",\"count\":2,"
        + "\"minEtag\":1,\"minProcess\":0,\"maxEtag\":3,\"maxProcess\":5,"
        + "\"totalEtag\":4,\"totalProcess\":5,\"avgEtag\":2,\"avgProcess\":2,"
        + "\"avgLastEtag\":3,\"avgLastProcess\":0,"
        + "\"status\":{\"ok\":1,\"noContent\":1}}"), out);
  }

  @Test
  void testProcess_emptyStatisticsJson() {
    GetGeneratorStatistics gen = new GetGeneratorStatistics();
    JsonOutputAdapter json = new JsonOutputAdapter();
    gen.generate(request("false"), json);
    assertTrue(json.toString().endsWith(",\"statistics\":[]}"), json.toString());
  }

  private static Request request(String resetValue) {
    return (Request) Proxy.newProxyInstance(
        Request.class.getClassLoader(),
        new Class<?>[]{Request.class},
        (proxy, m, args) -> {
          if ("getParameter".equals(m.getName())) {
            return "reset".equals(args[0]) ? resetValue : (args.length > 1 ? (String) args[1] : null);
          }
          return null;
        });
  }
}
