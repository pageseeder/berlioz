package org.pageseeder.berlioz.sample;

import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.xmlwriter.XMLWriter;

import java.io.IOException;

/**
 * Minimal generator using Berlioz's legacy XML API.
 *
 * <p>This class is included as a comparison point for applications that still implement
 * {@link ContentGenerator}. New code should normally use {@link HelloGenerator}'s
 * {@code Generator}/{@code OutputWriter} style, but many existing Berlioz applications still have
 * generators that write XML directly through {@link XMLWriter}.</p>
 */
public final class LegacyGenerator implements ContentGenerator {

  @Override
  public void process(ContentRequest req, XMLWriter xml) throws IOException {
    String name = req.getParameter("name", "Berlioz developer");

    xml.openElement("hello");
    xml.attribute("path", req.getBerliozPath());
    xml.attribute("name", name);
    xml.openElement("message");
    xml.writeText("Hello " + name);
    xml.closeElement();
    xml.closeElement();
  }
}
