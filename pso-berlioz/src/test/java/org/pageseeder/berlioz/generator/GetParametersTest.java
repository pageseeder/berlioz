package org.pageseeder.berlioz.generator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.xml.XmlStringBuilder;

class GetParametersTest {

  // process() tests
  // ---------------------------------------------------------------------------

  @Test
  void testProcessNoParametersWritesEmptyElement() {
    ContentRequest req = GeneratorTestSupport.request().build();
    String out = process(req);
    Assertions.assertTrue(out.contains("<parameters"), "Should contain <parameters>");
    Assertions.assertFalse(out.contains("<parameter "), "Should contain no <parameter> elements");
  }

  @Test
  void testProcessSingleParameter() {
    ContentRequest req = GeneratorTestSupport.request()
        .parameter("name", "Alice")
        .build();
    String out = process(req);
    Assertions.assertTrue(out.contains("name=\"name\""));
    Assertions.assertTrue(out.contains("Alice"));
  }

  @Test
  void testProcessMultipleParameters() {
    ContentRequest req = GeneratorTestSupport.request()
        .parameter("a", "1")
        .parameter("b", "2")
        .build();
    String out = process(req);
    Assertions.assertTrue(out.contains("name=\"a\""));
    Assertions.assertTrue(out.contains("name=\"b\""));
    Assertions.assertTrue(out.contains(">1<"));
    Assertions.assertTrue(out.contains(">2<"));
  }

  @Test
  void testProcessMultiValueParameter() {
    ContentRequest req = GeneratorTestSupport.request()
        .multiParameter("color", "red", "blue", "green")
        .build();
    String out = process(req);
    // 3 <parameter> elements inside <parameters>
    Assertions.assertTrue(out.contains("red"), "Should contain 3 parameter elements");
    Assertions.assertTrue(out.contains("blue"));
    Assertions.assertTrue(out.contains("green"));
  }

  // getETag() tests
  // ---------------------------------------------------------------------------

  @Test
  void testETagWithNoParametersIsStable() {
    GetParameters gen = new GetParameters();
    ContentRequest req = GeneratorTestSupport.request().build();
    String etag1 = gen.getETag((Request) req);
    String etag2 = gen.getETag((Request) req);
    Assertions.assertEquals(etag1, etag2, "ETag should be stable for same request");
    Assertions.assertNotNull(etag1);
    Assertions.assertFalse(etag1.isEmpty());
  }

  @Test
  void testETagDiffersWithDifferentParameters() {
    GetParameters gen = new GetParameters();
    ContentRequest empty  = GeneratorTestSupport.request().build();
    ContentRequest withParam = GeneratorTestSupport.request().parameter("q", "test").build();
    Assertions.assertNotEquals(gen.getETag((Request) empty), gen.getETag((Request) withParam));
  }

  @Test
  void testETagSameForIdenticalParameters() {
    GetParameters gen = new GetParameters();
    ContentRequest req1 = GeneratorTestSupport.request().parameter("q", "foo").build();
    ContentRequest req2 = GeneratorTestSupport.request().parameter("q", "foo").build();
    Assertions.assertEquals(gen.getETag((Request) req1), gen.getETag((Request) req2));
  }

  // helpers
  // ---------------------------------------------------------------------------

  private static String process(ContentRequest req) {
    GetParameters gen = new GetParameters();
    XmlStringBuilder xml = new XmlStringBuilder();
    gen.generate(req, xml);
    return xml.toString();
  }
}
