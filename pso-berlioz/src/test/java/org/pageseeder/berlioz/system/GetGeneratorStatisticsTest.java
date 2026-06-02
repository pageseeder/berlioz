package org.pageseeder.berlioz.system;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.content.GeneratorListener;
import org.pageseeder.berlioz.generator.NoContent;
import org.pageseeder.berlioz.servlet.BerliozConfig;
import org.pageseeder.xmlwriter.XML.NamespaceAware;
import org.pageseeder.xmlwriter.XMLStringWriter;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GetGeneratorStatisticsTest {

  @BeforeEach
  void setup() {
    BerliozConfig.setListener(null);
    StatisticsCollector.getInstance().clear();
  }

  @AfterEach
  void teardown() {
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
  void testProcess_writesStatisticsXml() throws Exception {
    GetGeneratorStatistics gen = new GetGeneratorStatistics();
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);

    gen.process(request("false"), xml);

    assertTrue(xml.toString().contains("<statistics"), "Should output <statistics> element");
  }

  @Test
  void testProcess_resetTrue_clearsStats() throws Exception {
    GetGeneratorStatistics gen = new GetGeneratorStatistics();

    // Populate some data
    StatisticsCollector.getInstance().generate(null, new NoContent(),
        ContentStatus.OK, 1000L, 5000L);

    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    gen.process(request("true"), xml);

    assertFalse(xml.toString().contains("<statistic generator="),
        "After reset, statistics should be cleared before output");
  }

  @Test
  void testProcess_resetFalse_retainsStats() throws Exception {
    GetGeneratorStatistics gen = new GetGeneratorStatistics();
    StatisticsCollector.getInstance().generate(null, new NoContent(),
        ContentStatus.OK, 1000L, 5000L);

    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    gen.process(request("false"), xml);

    assertTrue(xml.toString().contains("<statistic generator="), "Without reset, stats should be retained");
  }

  private static ContentRequest request(String resetValue) {
    return (ContentRequest) Proxy.newProxyInstance(
        ContentRequest.class.getClassLoader(),
        new Class<?>[]{ContentRequest.class},
        (proxy, m, args) -> {
          if ("getParameter".equals(m.getName())) {
            return "reset".equals(args[0]) ? resetValue : (String) args[1];
          }
          return null;
        });
  }
}
