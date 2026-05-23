package org.pageseeder.berlioz.json;

import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class BuiltinJsonWriterTest extends JsonWriterTestBase {

  BuiltinJsonWriter newJsonWriter(StringWriter json) {
    return new BuiltinJsonWriter(new PrintWriter(json));
  }

  @Test(expected = NullPointerException.class)
  public void testValueStringNull() {
    newJsonWriter(new StringWriter()).value((String) null);
  }

  @Test(expected = NullPointerException.class)
  public void testFieldStringNull() {
    newJsonWriter(new StringWriter()).startObject().field("k", (String) null);
  }

}
