package org.pageseeder.berlioz.json;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class JakartaJsonWriterTest extends JsonWriterTestBase {

  JakartaJsonWriter newJsonWriter(StringWriter json) {
    return JakartaJsonWriter.newInstance(new PrintWriter(json));
  }

}
