package org.pageseeder.berlioz.servlet.fixtures;

import java.io.IOException;

import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.xmlwriter.XMLWriter;

public final class ParameterEchoXmlGenerator implements ContentGenerator {

  @Override
  public void process(ContentRequest req, XMLWriter xml) throws IOException {
    xml.openElement("query");
    xml.attribute("q", req.getParameter("q", ""));
    xml.closeElement();
  }
}
