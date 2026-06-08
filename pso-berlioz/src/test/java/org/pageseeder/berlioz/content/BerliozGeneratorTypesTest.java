/*
 * Copyright 2026 Allette Systems (Australia)
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
package org.pageseeder.berlioz.content;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.json.JsonWriter;
import org.pageseeder.berlioz.output.OutputType;
import org.pageseeder.berlioz.output.OutputWriter;
import org.pageseeder.berlioz.xml.XmlWriter;

import java.io.IOException;
import java.util.Set;

/**
 * Tests for the four generator interface types: {@link XmlGenerator}, {@link JsonGenerator},
 * {@link Generator}, and {@link RawGenerator}.
 *
 * <p>Each type is an interface — the tests verify the {@code supported()} contract declared by
 * each type's default method, that each type extends {@link BerliozGenerator}, that
 * {@code generate()} delegates to the implementation, and the dual-implementation pattern
 * documented in the Javadoc.
 */
final class BerliozGeneratorTypesTest {

  // ---------------------------------------------------------------------------
  // XmlGenerator
  // ---------------------------------------------------------------------------

  @Test
  void xmlGenerator_supported_returnsXmlOnly() {
    XmlGenerator gen = (req, xml) -> Response.ok();
    Assertions.assertEquals(Set.of(OutputType.XML), gen.supported());
  }

  @Test
  void xmlGenerator_isInstanceOfBerliozGenerator() {
    XmlGenerator gen = (req, xml) -> Response.ok();
    Assertions.assertInstanceOf(BerliozGenerator.class, gen);
  }

  @Test
  void xmlGenerator_generate_returnsResponse() {
    XmlGenerator gen = (req, xml) -> Response.ok();
    Response r = gen.generate(null, (XmlWriter) null);
    Assertions.assertNotNull(r);
    Assertions.assertEquals(ContentStatus.OK, r.status());
  }

  // ---------------------------------------------------------------------------
  // JsonGenerator
  // ---------------------------------------------------------------------------

  @Test
  void jsonGenerator_supported_returnsJsonOnly() {
    JsonGenerator gen = (req, json) -> Response.ok();
    Assertions.assertEquals(Set.of(OutputType.JSON), gen.supported());
  }

  @Test
  void jsonGenerator_isInstanceOfBerliozGenerator() {
    JsonGenerator gen = (req, json) -> Response.ok();
    Assertions.assertInstanceOf(BerliozGenerator.class, gen);
  }

  @Test
  void jsonGenerator_generate_returnsResponse() {
    JsonGenerator gen = (req, json) -> Response.status(ContentStatus.NOT_FOUND);
    Response r = gen.generate(null, (JsonWriter) null);
    Assertions.assertNotNull(r);
    Assertions.assertEquals(ContentStatus.NOT_FOUND, r.status());
  }

  // ---------------------------------------------------------------------------
  // Generator (format-agnostic)
  // ---------------------------------------------------------------------------

  @Test
  void generator_supported_returnsXmlAndJson() {
    Generator gen = (req, out) -> Response.ok();
    Assertions.assertEquals(Set.of(OutputType.XML, OutputType.JSON), gen.supported());
  }

  @Test
  void generator_isInstanceOfBerliozGenerator() {
    Generator gen = (req, out) -> Response.ok();
    Assertions.assertInstanceOf(BerliozGenerator.class, gen);
  }

  @Test
  void generator_generate_returnsResponse() {
    Generator gen = (req, out) -> Response.ok();
    Response r = gen.generate(null, (OutputWriter) null);
    Assertions.assertNotNull(r);
    Assertions.assertEquals(ContentStatus.OK, r.status());
  }

  // ---------------------------------------------------------------------------
  // RawGenerator
  // ---------------------------------------------------------------------------

  @Test
  void rawGenerator_supported_returnsRawOnly() {
    RawGenerator gen = (req, out) -> Response.ok();
    Assertions.assertEquals(Set.of(OutputType.RAW), gen.supported());
  }

  @Test
  void rawGenerator_isInstanceOfBerliozGenerator() {
    RawGenerator gen = (req, out) -> Response.ok();
    Assertions.assertInstanceOf(BerliozGenerator.class, gen);
  }

  @Test
  void rawGenerator_generate_returnsResponse() throws IOException {
    RawGenerator gen = (req, out) -> Response.ok();
    Response r = gen.generate(null, null);
    Assertions.assertNotNull(r);
    Assertions.assertEquals(ContentStatus.OK, r.status());
  }

  // ---------------------------------------------------------------------------
  // Dual-implementation: XmlGenerator + JsonGenerator
  // ---------------------------------------------------------------------------

  @Test
  void dualXmlJson_supported_returnsXmlAndJson() {
    class DualGenerator implements XmlGenerator, JsonGenerator {
      @Override public Set<OutputType> supported() { return Set.of(OutputType.XML, OutputType.JSON); }
      @Override public Response generate(Request req, XmlWriter xml)  { return Response.ok(); }
      @Override public Response generate(Request req, JsonWriter json) { return Response.ok(); }
    }
    DualGenerator gen = new DualGenerator();
    Assertions.assertEquals(Set.of(OutputType.XML, OutputType.JSON), gen.supported());
    Assertions.assertInstanceOf(XmlGenerator.class, gen);
    Assertions.assertInstanceOf(JsonGenerator.class, gen);
    Assertions.assertInstanceOf(BerliozGenerator.class, gen);
  }

  @Test
  void dualXmlJson_xmlGenerate_returnsResponse() {
    class DualGenerator implements XmlGenerator, JsonGenerator {
      @Override public Set<OutputType> supported() { return Set.of(OutputType.XML, OutputType.JSON); }
      @Override public Response generate(Request req, XmlWriter xml)  { return Response.status(ContentStatus.NO_CONTENT); }
      @Override public Response generate(Request req, JsonWriter json) { return Response.ok(); }
    }
    DualGenerator gen = new DualGenerator();
    XmlGenerator asXml = gen;
    Assertions.assertEquals(ContentStatus.NO_CONTENT, asXml.generate(null, (XmlWriter) null).status());
    JsonGenerator asJson = gen;
    Assertions.assertEquals(ContentStatus.OK, asJson.generate(null, (JsonWriter) null).status());
  }

}
