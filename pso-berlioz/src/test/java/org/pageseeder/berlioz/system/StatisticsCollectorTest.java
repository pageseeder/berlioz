package org.pageseeder.berlioz.system;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.generator.NoContent;
import org.pageseeder.berlioz.xml.XmlStringBuilder;

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
  void testInitialToXML_emptyStatistics() {
    XmlStringBuilder xml = new XmlStringBuilder();
    collector.toXml(xml);
    String out = xml.toString();
    assertTrue(out.contains("<statistics"), "Should write <statistics> element");
    assertTrue(out.contains("since="), "Should include 'since' attribute");
    assertFalse(out.contains("<statistic "), "Initially should have no statistic entries");
  }

  @Test
  void testGenerate_recordsEntry() {
    NoContent gen = new NoContent();
    collector.generate(null, gen, ContentStatus.OK, 1000L, 5000L);

    XmlStringBuilder xml = new XmlStringBuilder();
    collector.toXml(xml);
    String out = xml.toString();
    assertTrue(out.contains("<statistic"), "Should contain at least one statistic");
    assertTrue(out.contains("generator=\"org.pageseeder.berlioz.generator.NoContent\""));
    assertTrue(out.contains("count=\"1\""));
  }

  @Test
  void testGenerate_accumulatesCount() {
    NoContent gen = new NoContent();
    collector.generate(null, gen, ContentStatus.OK, 1000L, 5000L);
    collector.generate(null, gen, ContentStatus.OK, 2000L, 6000L);
    collector.generate(null, gen, ContentStatus.NOT_FOUND, 500L, 3000L);

    XmlStringBuilder xml = new XmlStringBuilder();
    collector.toXml(xml);
    assertTrue(xml.toString().contains("count=\"3\""));
  }

  @Test
  void testGenerate_tracksMinMaxTimes() {
    NoContent gen = new NoContent();
    collector.generate(null, gen, ContentStatus.OK, 1_000_000L, 10_000_000L);
    collector.generate(null, gen, ContentStatus.OK, 5_000_000L, 2_000_000L);

    XmlStringBuilder xml = new XmlStringBuilder();
    collector.toXml(xml);
    String out = xml.toString();
    // Times are stored in microseconds (nanoseconds / 1000)
    assertTrue(out.contains("min-process=\"2000\""), "Min process time should be 2000 µs, got: " + out);
    assertTrue(out.contains("max-process=\"10000\""), "Max process time should be 10000 µs, got: " + out);
  }

  @Test
  void testClear_resetsState() {
    NoContent gen = new NoContent();
    collector.generate(null, gen, ContentStatus.OK, 1000L, 5000L);
    collector.clear();

    XmlStringBuilder xml = new XmlStringBuilder();
    collector.toXml(xml);
    assertFalse(xml.toString().contains("<statistic "), "After clear, should have no statistics");
  }

  @Test
  void testGenerate_differentGenerators_separateEntries() {
    NoContent gen1 = new NoContent();
    NoContent gen2 = new NoContent(); // same class → same entry
    collector.generate(null, gen1, ContentStatus.OK, 1000L, 5000L);
    collector.generate(null, gen2, ContentStatus.OK, 1000L, 5000L);

    XmlStringBuilder xml = new XmlStringBuilder();
    collector.toXml(xml);
    // Same class → same bucket → count=2
    assertTrue(xml.toString().contains("count=\"2\""));
  }
}
