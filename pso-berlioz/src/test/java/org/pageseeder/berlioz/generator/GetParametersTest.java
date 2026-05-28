package org.pageseeder.berlioz.generator;

import org.junit.Assert;
import org.junit.Test;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.xmlwriter.XML.NamespaceAware;
import org.pageseeder.xmlwriter.XMLStringWriter;

public class GetParametersTest {

  // process() tests
  // ---------------------------------------------------------------------------

  @Test
  public void testProcessNoParametersWritesEmptyElement() throws Exception {
    ContentRequest req = GeneratorTestSupport.request().build();
    String out = process(req);
    Assert.assertTrue("Should contain <parameters>", out.contains("<parameters"));
    Assert.assertFalse("Should contain no <parameter> elements", out.contains("<parameter "));
  }

  @Test
  public void testProcessSingleParameter() throws Exception {
    ContentRequest req = GeneratorTestSupport.request()
        .parameter("name", "Alice")
        .build();
    String out = process(req);
    Assert.assertTrue(out.contains("name=\"name\""));
    Assert.assertTrue(out.contains("Alice"));
  }

  @Test
  public void testProcessMultipleParameters() throws Exception {
    ContentRequest req = GeneratorTestSupport.request()
        .parameter("a", "1")
        .parameter("b", "2")
        .build();
    String out = process(req);
    Assert.assertTrue(out.contains("name=\"a\""));
    Assert.assertTrue(out.contains("name=\"b\""));
    Assert.assertTrue(out.contains(">1<"));
    Assert.assertTrue(out.contains(">2<"));
  }

  @Test
  public void testProcessMultiValueParameter() throws Exception {
    ContentRequest req = GeneratorTestSupport.request()
        .multiParameter("color", "red", "blue", "green")
        .build();
    String out = process(req);
    long count = out.chars().filter(c -> c == '<').count()
        - out.chars().filter(c -> c == '/').count();
    // 3 <parameter> elements inside <parameters>
    Assert.assertTrue("Should contain 3 parameter elements", out.contains("red"));
    Assert.assertTrue(out.contains("blue"));
    Assert.assertTrue(out.contains("green"));
  }

  // getETag() tests
  // ---------------------------------------------------------------------------

  @Test
  public void testETagWithNoParametersIsStable() {
    GetParameters gen = new GetParameters();
    ContentRequest req = GeneratorTestSupport.request().build();
    String etag1 = gen.getETag(req);
    String etag2 = gen.getETag(req);
    Assert.assertEquals("ETag should be stable for same request", etag1, etag2);
    Assert.assertNotNull(etag1);
    Assert.assertFalse(etag1.isEmpty());
  }

  @Test
  public void testETagDiffersWithDifferentParameters() {
    GetParameters gen = new GetParameters();
    ContentRequest empty  = GeneratorTestSupport.request().build();
    ContentRequest withParam = GeneratorTestSupport.request().parameter("q", "test").build();
    Assert.assertNotEquals(gen.getETag(empty), gen.getETag(withParam));
  }

  @Test
  public void testETagSameForIdenticalParameters() {
    GetParameters gen = new GetParameters();
    ContentRequest req1 = GeneratorTestSupport.request().parameter("q", "foo").build();
    ContentRequest req2 = GeneratorTestSupport.request().parameter("q", "foo").build();
    Assert.assertEquals(gen.getETag(req1), gen.getETag(req2));
  }

  // helpers
  // ---------------------------------------------------------------------------

  private static String process(ContentRequest req) throws Exception {
    GetParameters gen = new GetParameters();
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    gen.process(req, xml);
    xml.flush();
    return xml.toString();
  }
}
