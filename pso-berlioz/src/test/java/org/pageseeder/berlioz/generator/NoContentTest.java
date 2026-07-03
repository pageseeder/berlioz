package org.pageseeder.berlioz.generator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.XmlGenerator;
import org.pageseeder.berlioz.xml.XmlStringBuilder;

class NoContentTest {

  @Test
  void testImplementsBothInterfaces() {
    NoContent gen = new NoContent();
    Assertions.assertTrue(gen instanceof XmlGenerator);
    Assertions.assertTrue(gen instanceof Cacheable);
  }

  @Test
  void testETagIsAlwaysNocontent() {
    NoContent gen = new NoContent();
    ContentRequest req = GeneratorTestSupport.request().build();
    Assertions.assertEquals("nocontent", gen.getETag((Request) req));
  }

  @Test
  void testProcessWritesNothing() {
    NoContent gen = new NoContent();
    ContentRequest req = GeneratorTestSupport.request().build();
    XmlStringBuilder xml = new XmlStringBuilder();
    gen.generate(req, xml);
    Assertions.assertEquals("", xml.toString());
  }
}
