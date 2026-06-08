package org.pageseeder.berlioz.servlet.fixtures;

import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.content.XmlGenerator;
import org.pageseeder.berlioz.xml.XmlWriter;

public final class RedirectXmlGenerator implements XmlGenerator {

  @Override
  public Response generate(Request req, XmlWriter xml) {
    return Response.redirect(ContentStatus.SEE_OTHER, "/elsewhere");
  }
}
