package org.pageseeder.berlioz.servlet.fixtures;

import java.io.IOException;

import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.xmlwriter.XMLWriter;

public final class EchoXmlGenerator implements ContentGenerator {

  @Override
  public void process(ContentRequest req, XMLWriter xml) throws IOException {
    xml.openElement("message");
    xml.attribute("path", req.getBerliozPath());
    xml.writeText("hello");
    xml.closeElement();
  }
}
