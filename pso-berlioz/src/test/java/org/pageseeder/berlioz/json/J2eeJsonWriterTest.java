package org.pageseeder.berlioz.json;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class J2eeJsonWriterTest extends JsonWriterTestBase {

  J2eeJsonWriter newJsonWriter(StringWriter json) {
    return J2eeJsonWriter.newInstance(new PrintWriter(json));
  }

}
