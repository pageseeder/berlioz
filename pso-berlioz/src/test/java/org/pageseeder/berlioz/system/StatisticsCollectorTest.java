package org.pageseeder.berlioz.system;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.generator.NoContent;
import org.pageseeder.xmlwriter.XML.NamespaceAware;
import org.pageseeder.xmlwriter.XMLStringWriter;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class StatisticsCollectorTest {

  private StatisticsCollector collector;

  @BeforeEach
  void setup() {
    collector = StatisticsCollector.getInstance();
    collector.clear();
  }

  @Test
  void testGetInstance_returnsSingleton() {
    assertSame(StatisticsCollector.getInstance(), StatisticsCollector.getInstance());
  }

  @Test
  void testInitialToXML_emptyStatistics() throws IOException {
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    collector.toXML(xml);
    String out = xml.toString();
    assertTrue(out.contains("<statistics"), "Should write <statistics> element");
    assertTrue(out.contains("since="), "Should include 'since' attribute");
    assertFalse(out.contains("<statistic "), "Initially should have no statistic entries");
  }

  @Test
  void testGenerate_recordsEntry() throws IOException {
    NoContent gen = new NoContent();
    collector.generate(null, gen, ContentStatus.OK, 1000L, 5000L);

    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    collector.toXML(xml);
    String out = xml.toString();
    assertTrue(out.contains("<statistic"), "Should contain at least one statistic");
    assertTrue(out.contains("generator=\"org.pageseeder.berlioz.generator.NoContent\""));
    assertTrue(out.contains("count=\"1\""));
  }

  @Test
  void testGenerate_accumulatesCount() throws IOException {
    NoContent gen = new NoContent();
    collector.generate(null, gen, ContentStatus.OK, 1000L, 5000L);
    collector.generate(null, gen, ContentStatus.OK, 2000L, 6000L);
    collector.generate(null, gen, ContentStatus.NOT_FOUND, 500L, 3000L);

    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    collector.toXML(xml);
    assertTrue(xml.toString().contains("count=\"3\""));
  }

  @Test
  void testGenerate_tracksMinMaxTimes() throws IOException {
    NoContent gen = new NoContent();
    collector.generate(null, gen, ContentStatus.OK, 1_000_000L, 10_000_000L);
    collector.generate(null, gen, ContentStatus.OK, 5_000_000L, 2_000_000L);

    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    collector.toXML(xml);
    String out = xml.toString();
    // Times are stored in microseconds (nanoseconds / 1000)
    assertTrue(out.contains("min-process=\"2000\""), "Min process time should be 2000 µs, got: " + out);
    assertTrue(out.contains("max-process=\"10000\""), "Max process time should be 10000 µs, got: " + out);
  }

  @Test
  void testClear_resetsState() throws IOException {
    NoContent gen = new NoContent();
    collector.generate(null, gen, ContentStatus.OK, 1000L, 5000L);
    collector.clear();

    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    collector.toXML(xml);
    assertFalse(xml.toString().contains("<statistic "), "After clear, should have no statistics");
  }

  @Test
  void testGenerate_differentGenerators_separateEntries() throws IOException {
    NoContent gen1 = new NoContent();
    NoContent gen2 = new NoContent(); // same class → same entry
    collector.generate(null, gen1, ContentStatus.OK, 1000L, 5000L);
    collector.generate(null, gen2, ContentStatus.OK, 1000L, 5000L);

    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    collector.toXML(xml);
    // Same class → same bucket → count=2
    assertTrue(xml.toString().contains("count=\"2\""));
  }
}
