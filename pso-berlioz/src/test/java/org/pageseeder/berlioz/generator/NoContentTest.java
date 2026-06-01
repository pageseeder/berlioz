package org.pageseeder.berlioz.generator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.xmlwriter.XML.NamespaceAware;
import org.pageseeder.xmlwriter.XMLStringWriter;

public class NoContentTest {

  @Test
  public void testImplementsBothInterfaces() {
    NoContent gen = new NoContent();
    Assertions.assertTrue(gen instanceof ContentGenerator);
    Assertions.assertTrue(gen instanceof Cacheable);
  }

  @Test
  public void testETagIsAlwaysNocontent() {
    NoContent gen = new NoContent();
    ContentRequest req = GeneratorTestSupport.request().build();
    Assertions.assertEquals(gen.getETag(req), "nocontent");
  }

  @Test
  public void testProcessWritesNothing() {
    NoContent gen = new NoContent();
    ContentRequest req = GeneratorTestSupport.request().build();
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    gen.process(req, xml);
    xml.flush();
    Assertions.assertEquals(xml.toString(), "");
  }
}
