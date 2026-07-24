package org.pageseeder.berlioz.servlet.fixtures;

import java.io.IOException;

import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.error.HttpException;
import org.pageseeder.xmlwriter.XMLWriter;

/** Throws an HTTP signal from the cache callback, outside generator invocation. */
public final class FailingEtagXmlGenerator implements ContentGenerator, Cacheable {

  @Override
  public String getETag(Request req) {
    throw new HttpException(null, 503) {}.header("Retry-After", "30");
  }

  @Override
  public void process(ContentRequest req, XMLWriter xml) throws IOException {
    xml.element("unused", "unused");
  }
}
