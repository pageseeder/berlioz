/*
 * Copyright 2015 Allette Systems (Australia)
 * http://www.allette.com.au
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pageseeder.berlioz.aeson;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.XMLConstants;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.junit.Assert;
import org.junit.Test;

public class JSONResultTest {

  // ---------------------------------------------------------------------------
  // supports()
  // ---------------------------------------------------------------------------

  @Test
  public void testSupportsJsonMediaType() throws Exception {
    Transformer t = jsonTransformer();
    Assert.assertTrue("xml method + application/json media type must be supported", JSONResult.supports(t));
  }

  @Test
  public void testSupportsRejectsHtmlMethod() throws Exception {
    Transformer t = TransformerFactory.newInstance().newTransformer();
    t.setOutputProperty("method", "html");
    t.setOutputProperty("media-type", "application/json");
    Assert.assertFalse("html method must not be supported", JSONResult.supports(t));
  }

  @Test
  public void testSupportsRejectsWrongMediaType() throws Exception {
    Transformer t = TransformerFactory.newInstance().newTransformer();
    t.setOutputProperty("method", "xml");
    t.setOutputProperty("media-type", "text/xml");
    Assert.assertFalse("text/xml media type must not be supported", JSONResult.supports(t));
  }

  @Test
  public void testSupportsRejectsDefaultTransformer() throws Exception {
    Transformer t = TransformerFactory.newInstance().newTransformer();
    Assert.assertFalse("Default identity transformer must not be supported", JSONResult.supports(t));
  }

  // ---------------------------------------------------------------------------
  // newInstanceIfSupported()
  // ---------------------------------------------------------------------------

  @Test
  public void testNewInstanceIfSupportedReturnsJSONResultWhenSupported() throws Exception {
    Transformer t = jsonTransformer();
    StreamResult sr = new StreamResult(new StringWriter());
    Result r = JSONResult.newInstanceIfSupported(t, sr);
    Assert.assertTrue("Must return a JSONResult when transformer is supported", r instanceof JSONResult);
  }

  @Test
  public void testNewInstanceIfSupportedReturnsStreamResultWhenNotSupported() throws Exception {
    Transformer t = TransformerFactory.newInstance().newTransformer();
    StreamResult sr = new StreamResult(new StringWriter());
    Result r = JSONResult.newInstanceIfSupported(t, sr);
    Assert.assertSame("Must return the original StreamResult when transformer is not supported", sr, r);
  }

  // ---------------------------------------------------------------------------
  // newInstance(StreamResult) — various stream types
  // ---------------------------------------------------------------------------

  @Test
  public void testNewInstanceFromOutputStream() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    StreamResult sr = new StreamResult(out);
    JSONResult r = JSONResult.newInstance(sr);
    Assert.assertNotNull(r);
  }

  @Test
  public void testNewInstanceFromWriter() {
    StringWriter writer = new StringWriter();
    StreamResult sr = new StreamResult(writer);
    JSONResult r = JSONResult.newInstance(sr);
    Assert.assertNotNull(r);
  }

  @Test
  public void testNewInstanceFallsBackToSystemOut() {
    StreamResult sr = new StreamResult();
    JSONResult r = JSONResult.newInstance(sr);
    Assert.assertNotNull(r);
  }

  // ---------------------------------------------------------------------------
  // End-to-end: transform XML through a JSONResult
  // ---------------------------------------------------------------------------

  @Test
  public void testTransformProducesValidJSON() throws Exception {
    String xml = "<root description=\"hello\"><item id=\"1\"/></root>";
    StringWriter out = new StringWriter();
    Transformer t = jsonTransformer();
    StreamResult sr = new StreamResult(out);
    Result result = JSONResult.newInstanceIfSupported(t, sr);
    t.transform(new StreamSource(new StringReader(xml)), result);
    String json = out.toString();
    Assert.assertTrue("JSON must start with {", json.startsWith("{"));
    Assert.assertTrue("JSON must contain 'description' attribute", json.contains("\"description\""));
    Assert.assertTrue("JSON must contain attribute value", json.contains("\"hello\""));
  }

  @Test
  public void testTransformToOutputStream() throws Exception {
    String xml = "<root><item name=\"x\"/></root>";
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Transformer t = jsonTransformer();
    StreamResult sr = new StreamResult(out);
    Result result = JSONResult.newInstanceIfSupported(t, sr);
    t.transform(new StreamSource(new StringReader(xml)), result);
    String json = out.toString("UTF-8");
    Assert.assertFalse("Output must not be empty", json.isEmpty());
    Assert.assertTrue("JSON must contain 'name'", json.contains("\"name\""));
  }

  // ---------------------------------------------------------------------------
  // Helper
  // ---------------------------------------------------------------------------

  private static Transformer jsonTransformer() throws Exception {
    TransformerFactory factory = TransformerFactory.newInstance();
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
    Transformer t = factory.newTransformer();
    t.setOutputProperty("method", "xml");
    t.setOutputProperty("media-type", "application/json");
    return t;
  }

}
