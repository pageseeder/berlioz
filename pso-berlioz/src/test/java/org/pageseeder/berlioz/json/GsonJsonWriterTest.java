package org.pageseeder.berlioz.json;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class GsonJsonWriterTest extends JsonWriterTestBase {

  GsonJsonWriter newJsonWriter(StringWriter json) {
    return GsonJsonWriter.newInstance(new PrintWriter(json));
  }

}
