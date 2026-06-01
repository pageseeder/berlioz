package org.pageseeder.berlioz.generator;

import org.junit.Assert;
import org.junit.Test;
import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.xmlwriter.XML.NamespaceAware;
import org.pageseeder.xmlwriter.XMLStringWriter;

public class NoContentTest {

  @Test
  public void testImplementsBothInterfaces() {
    NoContent gen = new NoContent();
    Assert.assertTrue(gen instanceof ContentGenerator);
    Assert.assertTrue(gen instanceof Cacheable);
  }

  @Test
  public void testETagIsAlwaysNocontent() {
    NoContent gen = new NoContent();
    ContentRequest req = GeneratorTestSupport.request().build();
    Assert.assertEquals("nocontent", gen.getETag(req));
  }

  @Test
  public void testProcessWritesNothing() {
    NoContent gen = new NoContent();
    ContentRequest req = GeneratorTestSupport.request().build();
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    gen.process(req, xml);
    xml.flush();
    Assert.assertEquals("", xml.toString());
  }
}
