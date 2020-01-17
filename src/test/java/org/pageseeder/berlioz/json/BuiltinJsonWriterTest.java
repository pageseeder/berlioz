package org.pageseeder.berlioz.json;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class BuiltinJsonWriterTest extends JsonWriterTestBase {

  BuiltinJsonWriter newJsonWriter(StringWriter json) {
    return new BuiltinJsonWriter(new PrintWriter(json));
  }

}
