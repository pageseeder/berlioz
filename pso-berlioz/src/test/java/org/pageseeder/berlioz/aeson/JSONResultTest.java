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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class JSONResultTest {

  // ---------------------------------------------------------------------------
  // supports()
  // ---------------------------------------------------------------------------

  @Test
  void testSupportsJsonMediaType() throws Exception {
    Transformer t = jsonTransformer();
    Assertions.assertTrue(JSONResult.supports(t), "xml method + application/json media type must be supported");
  }

  @Test
  void testSupportsRejectsHtmlMethod() throws Exception {
    Transformer t = TransformerFactory.newInstance().newTransformer();
    t.setOutputProperty("method", "html");
    t.setOutputProperty("media-type", "application/json");
    Assertions.assertFalse(JSONResult.supports(t), "html method must not be supported");
  }

  @Test
  void testSupportsRejectsWrongMediaType() throws Exception {
    Transformer t = TransformerFactory.newInstance().newTransformer();
    t.setOutputProperty("method", "xml");
    t.setOutputProperty("media-type", "text/xml");
    Assertions.assertFalse(JSONResult.supports(t), "text/xml media type must not be supported");
  }

  @Test
  void testSupportsRejectsDefaultTransformer() throws Exception {
    Transformer t = TransformerFactory.newInstance().newTransformer();
    Assertions.assertFalse(JSONResult.supports(t), "Default identity transformer must not be supported");
  }

  // ---------------------------------------------------------------------------
  // newInstanceIfSupported()
  // ---------------------------------------------------------------------------

  @Test
  void testNewInstanceIfSupportedReturnsJSONResultWhenSupported() throws Exception {
    Transformer t = jsonTransformer();
    StreamResult sr = new StreamResult(new StringWriter());
    Result r = JSONResult.newInstanceIfSupported(t, sr);
    Assertions.assertTrue(r instanceof JSONResult, "Must return a JSONResult when transformer is supported");
  }

  @Test
  void testNewInstanceIfSupportedReturnsStreamResultWhenNotSupported() throws Exception {
    Transformer t = TransformerFactory.newInstance().newTransformer();
    StreamResult sr = new StreamResult(new StringWriter());
    Result r = JSONResult.newInstanceIfSupported(t, sr);
    Assertions.assertSame(sr, r, "Must return the original StreamResult when transformer is not supported");
  }

  // ---------------------------------------------------------------------------
  // newInstance(StreamResult) — various stream types
  // ---------------------------------------------------------------------------

  @Test
  void testNewInstanceFromOutputStream() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    StreamResult sr = new StreamResult(out);
    JSONResult r = JSONResult.newInstance(sr);
    Assertions.assertNotNull(r);
  }

  @Test
  void testNewInstanceFromWriter() {
    StringWriter writer = new StringWriter();
    StreamResult sr = new StreamResult(writer);
    JSONResult r = JSONResult.newInstance(sr);
    Assertions.assertNotNull(r);
  }

  @Test
  void testNewInstanceFallsBackToSystemOut() {
    StreamResult sr = new StreamResult();
    JSONResult r = JSONResult.newInstance(sr);
    Assertions.assertNotNull(r);
  }

  // ---------------------------------------------------------------------------
  // End-to-end: transform XML through a JSONResult
  // ---------------------------------------------------------------------------

  @Test
  void testTransformProducesValidJSON() throws Exception {
    String xml = "<root description=\"hello\"><item id=\"1\"/></root>";
    StringWriter out = new StringWriter();
    Transformer t = jsonTransformer();
    StreamResult sr = new StreamResult(out);
    Result result = JSONResult.newInstanceIfSupported(t, sr);
    t.transform(new StreamSource(new StringReader(xml)), result);
    String json = out.toString();
    Assertions.assertTrue(json.startsWith("{"), "JSON must start with {");
    Assertions.assertTrue(json.contains("\"description\""), "JSON must contain 'description' attribute");
    Assertions.assertTrue(json.contains("\"hello\""), "JSON must contain attribute value");
  }

  @Test
  void testTransformToOutputStream() throws Exception {
    String xml = "<root><item name=\"x\"/></root>";
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Transformer t = jsonTransformer();
    StreamResult sr = new StreamResult(out);
    Result result = JSONResult.newInstanceIfSupported(t, sr);
    t.transform(new StreamSource(new StringReader(xml)), result);
    String json = out.toString("UTF-8");
    Assertions.assertFalse(json.isEmpty(), "Output must not be empty");
    Assertions.assertTrue(json.contains("\"name\""), "JSON must contain 'name'");
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
