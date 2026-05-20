package org.pageseeder.berlioz.json;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class JacksonJsonWriterTest extends JsonWriterTestBase {

  JacksonJsonWriter newJsonWriter(StringWriter json) {
    return JacksonJsonWriter.newInstance(new PrintWriter(json));
  }

}
