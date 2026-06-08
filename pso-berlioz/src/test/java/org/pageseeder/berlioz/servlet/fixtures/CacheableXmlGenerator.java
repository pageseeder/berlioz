package org.pageseeder.berlioz.servlet.fixtures;

import java.io.IOException;

import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.xmlwriter.XMLWriter;

public final class CacheableXmlGenerator implements ContentGenerator, Cacheable {

  @Override
  public String getETag(Request req) {
    return "cacheable-xml";
  }

  @Override
  public void process(ContentRequest req, XMLWriter xml) throws IOException {
    xml.openElement("cached");
    xml.writeText("cacheable");
    xml.closeElement();
  }
}
