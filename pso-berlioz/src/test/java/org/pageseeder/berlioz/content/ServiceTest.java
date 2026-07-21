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

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.generator.NoContent;
import org.pageseeder.berlioz.http.HttpMethod;
import org.pageseeder.berlioz.json.JsonWriter;
import org.pageseeder.berlioz.output.OutputType;
import org.pageseeder.berlioz.output.OutputWriter;
import org.pageseeder.berlioz.xml.XmlStringBuilder;
import org.pageseeder.berlioz.xml.XmlWriter;
import org.pageseeder.xmlwriter.XMLWriter;

final class ServiceTest {

  private static Service.Builder defaultBuilder(String id) {
    return new Service.Builder()
        .id(id)
        .group("test")
        .rule(ServiceStatusRule.DEFAULT_RULE);
  }

  // --- Builder ---

  @Test
  void testBuilder_id() {
    Service s = defaultBuilder("my-service").add(new NoContent()).build();
    Assertions.assertEquals("my-service", s.id());
  }

  @Test
  void testBuilder_group() {
    Service s = defaultBuilder("svc").add(new NoContent()).build();
    Assertions.assertEquals("test", s.group());
  }

  @Test
  void testBuilder_groupDefault() {
    Service s = new Service.Builder()
        .id("svc")
        .group(null)
        .rule(ServiceStatusRule.DEFAULT_RULE)
        .add(new NoContent())
        .build();
    Assertions.assertEquals("default", s.group());
  }

  @Test
  void testBuilder_cache() {
    Service s = defaultBuilder("svc").cache("max-age=3600").add(new NoContent()).build();
    Assertions.assertEquals("max-age=3600", s.cache());
  }

  @Test
  void testBuilder_cacheDefault() {
    Service s = defaultBuilder("svc").add(new NoContent()).build();
    Assertions.assertEquals("", s.cache());
  }

  @Test
  void testBuilder_flags() {
    Service s = defaultBuilder("svc").flags("secure").add(new NoContent()).build();
    Assertions.assertEquals("secure", s.flags());
  }

  @Test
  void testBuilder_flagsDefault() {
    Service s = defaultBuilder("svc").add(new NoContent()).build();
    Assertions.assertEquals("", s.flags());
  }

  @Test
  void testBuilder_idRequired() {
    Service.Builder builder = new Service.Builder()
        .group("test")
        .rule(ServiceStatusRule.DEFAULT_RULE)
        .add(new NoContent());
    Assertions.assertThrows(NullPointerException.class, builder::build);
  }

  @Test
  void testBuilder_reset() {
    Service.Builder builder = defaultBuilder("first");
    builder.add(new NoContent());
    builder.build();
    builder.reset();

    Assertions.assertNull(builder.id());
    Assertions.assertEquals("test", builder.group());

    builder.id("second").add(new NoContent());
    Service second = builder.build();
    Assertions.assertEquals("second", second.id());
  }

  // --- Generators ---

  @Test
  void testGenerators_single() {
    NoContent g = new NoContent();
    Service s = defaultBuilder("svc").add(g).build();
    Assertions.assertEquals(1, s.generators().size());
    Assertions.assertSame(g, s.generators().get(0));
  }

  @Test
  void testGenerators_multiple() {
    NoContent g1 = new NoContent();
    NoContent g2 = new NoContent();
    Service s = defaultBuilder("svc").add(g1).add(g2).build();
    Assertions.assertEquals(2, s.generators().size());
  }

  // --- Cacheability ---

  @Test
  void testIsCacheable_allCacheable() {
    Service s = defaultBuilder("svc").add(new NoContent()).build();
    Assertions.assertTrue(s.isCacheable());
  }

  @Test
  void testIsCacheable_notCacheable() {
    ContentGenerator nonCacheable = (req, xml) -> {};
    Service s = defaultBuilder("svc").add(nonCacheable).build();
    Assertions.assertFalse(s.isCacheable());
  }

  @Test
  void testIsCacheable_mixedGenerators() {
    ContentGenerator nonCacheable2 = (req, xml) -> {};
    Service s = defaultBuilder("svc").add(new NoContent()).add(nonCacheable2).build();
    Assertions.assertFalse(s.isCacheable());
  }

  @Test
  void testIsCacheable_static_emptyList() {
    Assertions.assertTrue(Service.isCacheable(Collections.emptyList()));
  }

  @Test
  void testIsCacheable_static_allCacheable() {
    Assertions.assertTrue(Service.isCacheable(List.of(new NoContent())));
  }

  @Test
  void testIsCacheable_static_notCacheable() {
    ContentGenerator nonCacheable = (req, xml) -> {};
    Assertions.assertFalse(Service.isCacheable(List.of(nonCacheable)));
  }

  @Test
  void testCacheable_contentGenerator_getETagRequestOverrideWorks() {
    CacheableContentByRequest generator = new CacheableContentByRequest();

    Assertions.assertEquals("request", generator.getETag((Request) contentRequest()));
    Assertions.assertNull(Service.cacheableMethodWarning(generator));
  }

  @Test
  void testCacheable_contentGenerator_getETagContentRequestOverrideWorks() {
    CacheableContentByContentRequest generator = new CacheableContentByContentRequest();

    Assertions.assertEquals("content-request", generator.getETag((Request) contentRequest()));
    Assertions.assertNull(Service.cacheableMethodWarning(generator));
  }

  @Test
  void testCacheable_nonContentGenerator_getETagRequestOverrideWorks() {
    CacheableGeneratorByRequest generator = new CacheableGeneratorByRequest();

    Assertions.assertEquals("request", generator.getETag(request()));
    Assertions.assertNull(Service.cacheableMethodWarning(generator));
  }

  @Test
  void testCacheable_nonContentGenerator_getETagContentRequestOverrideWarnsAndIsNotUsed() {
    CacheableGeneratorByContentRequest generator = new CacheableGeneratorByContentRequest();

    Assertions.assertNull(generator.getETag((Request) contentRequest()));
    String warning = Service.cacheableMethodWarning(generator);
    Assertions.assertNotNull(warning);
    Assertions.assertTrue(warning.contains("override getETag(Request)"), warning);
  }

  @Test
  void testCacheable_noGetETagOverrideWarns() {
    CacheableGeneratorWithoutETag generator = new CacheableGeneratorWithoutETag();

    String warning = Service.cacheableMethodWarning(generator);
    Assertions.assertNotNull(warning);
    Assertions.assertTrue(warning.contains("overrides neither getETag method"), warning);
  }

  // --- Name and Target ---

  @Test
  void testName_defaultIsKebabCase() {
    NoContent g = new NoContent();
    Service s = defaultBuilder("svc").add(g).build();
    Assertions.assertEquals("no-content", s.name(g));
  }

  @Test
  void testName_explicit() {
    NoContent g = new NoContent();
    Service s = defaultBuilder("svc").add(g).name("my-generator").build();
    Assertions.assertEquals("my-generator", s.name(g));
  }

  @Test
  void testTarget_absent() {
    NoContent g = new NoContent();
    Service s = defaultBuilder("svc").add(g).build();
    Assertions.assertNull(s.target(g));
  }

  @Test
  void testTarget_set() {
    NoContent g = new NoContent();
    Service s = defaultBuilder("svc").add(g).target("main").build();
    Assertions.assertEquals("main", s.target(g));
  }

  // --- Parameters ---

  @Test
  void testParameters_empty() {
    NoContent g = new NoContent();
    Service s = defaultBuilder("svc").add(g).build();
    Assertions.assertTrue(s.parameters(g).isEmpty());
  }

  @Test
  void testParameters_withParameter() {
    NoContent g = new NoContent();
    Parameter p = new Parameter("key", "value");
    Service s = defaultBuilder("svc").add(g).parameter(p).build();
    List<Parameter> params = s.parameters(g);
    Assertions.assertEquals(1, params.size());
    Assertions.assertEquals("key", params.get(0).name());
    Assertions.assertEquals("value", params.get(0).value());
  }

  @Test
  void testParameters_nullParameterIgnored() {
    NoContent g = new NoContent();
    Service s = defaultBuilder("svc").add(g).parameter(null).build();
    Assertions.assertTrue(s.parameters(g).isEmpty());
  }

  // --- affectStatus ---

  @Test
  void testAffectStatus_appliesToAll() {
    NoContent g = new NoContent();
    Service s = defaultBuilder("svc").add(g).build();
    // DEFAULT_RULE applies to all
    Assertions.assertTrue(s.affectStatus(g));
  }

  @Test
  void testAffectStatus_byName_matching() {
    NoContent g = new NoContent();
    ServiceStatusRule rule = ServiceStatusRule.newInstance("name:my-gen", "HIGHEST");
    Service s = new Service.Builder()
        .id("svc").group("test").rule(rule)
        .add(g).name("my-gen")
        .build();
    Assertions.assertTrue(s.affectStatus(g));
  }

  @Test
  void testAffectStatus_byName_notMatching() {
    NoContent g = new NoContent();
    ServiceStatusRule rule = ServiceStatusRule.newInstance("name:other-gen", "HIGHEST");
    Service s = new Service.Builder()
        .id("svc").group("test").rule(rule)
        .add(g).name("my-gen")
        .build();
    Assertions.assertFalse(s.affectStatus(g));
  }

  // --- rule ---

  @Test
  void testRule_returnsBuilderRule() {
    Service s = defaultBuilder("svc").add(new NoContent()).build();
    Assertions.assertSame(ServiceStatusRule.DEFAULT_RULE, s.rule());
  }

  // --- isDirect ---

  @Test
  void testIsDirect_defaultFalse() {
    Service s = defaultBuilder("svc").add(new NoContent()).build();
    Assertions.assertFalse(s.isDirect());
  }

  @Test
  void testIsDirect_true() {
    Service s = defaultBuilder("svc").direct(true).add(new NoContent()).build();
    Assertions.assertTrue(s.isDirect());
  }

  // --- supported ---

  @Test
  void testSupported_contentGenerator_xmlOnly() {
    ContentGenerator g = (req, xml) -> {};
    Service s = defaultBuilder("svc").add(g).build();
    Assertions.assertEquals(Set.of(OutputType.XML), s.supported());
  }

  @Test
  void testSupported_jsonGenerator_jsonOnly() {
    JsonGenerator g = (req, json) -> Response.ok();
    Service s = defaultBuilder("svc").add(g).build();
    Assertions.assertEquals(Set.of(OutputType.JSON), s.supported());
  }

  @Test
  void testSupported_generator_xmlAndJson() {
    Generator g = (req, out) -> Response.ok();
    Service s = defaultBuilder("svc").add(g).build();
    Assertions.assertEquals(Set.of(OutputType.XML, OutputType.JSON), s.supported());
  }

  @Test
  void testSupported_intersection_contentAndGenerator_xmlOnly() {
    ContentGenerator cg = (req, xml) -> {};
    Generator g = (req, out) -> Response.ok();
    Service s = defaultBuilder("svc").add(cg).add(g).build();
    Assertions.assertEquals(Set.of(OutputType.XML), s.supported());
  }

  @Test
  void testSupported_intersection_disjoint_emptyResult() {
    ContentGenerator cg = (req, xml) -> {};
    JsonGenerator jg = (req, json) -> Response.ok();
    Service s = defaultBuilder("svc").add(cg).add(jg).build();
    Assertions.assertTrue(s.supported().isEmpty());
  }

  // --- computeSupported (static) ---

  @Test
  void testComputeSupported_emptyList() {
    Assertions.assertTrue(Service.computeSupported(Collections.emptyList()).isEmpty());
  }

  // --- parameters for unknown generator ---

  @Test
  void testParameters_unknownGenerator_returnsEmpty() {
    NoContent registered = new NoContent();
    NoContent other = new NoContent();
    Service s = defaultBuilder("svc").add(registered).build();
    Assertions.assertTrue(s.parameters(other).isEmpty());
  }

  // --- affectStatus by target ---

  @Test
  void testAffectStatus_byTarget_matching() {
    NoContent g = new NoContent();
    ServiceStatusRule rule = ServiceStatusRule.newInstance("target:main", "HIGHEST");
    Service s = new Service.Builder()
        .id("svc").group("test").rule(rule)
        .add(g).target("main")
        .build();
    Assertions.assertTrue(s.affectStatus(g));
  }

  @Test
  void testAffectStatus_byTarget_notMatching() {
    NoContent g = new NoContent();
    ServiceStatusRule rule = ServiceStatusRule.newInstance("target:main", "HIGHEST");
    Service s = new Service.Builder()
        .id("svc").group("test").rule(rule)
        .add(g).target("sidebar")
        .build();
    Assertions.assertFalse(s.affectStatus(g));
  }

  // --- toXML ---

  @Test
  void testToXML_basicAttributes() {
    NoContent g = new NoContent();
    Service s = defaultBuilder("svc").add(g).build();
    XmlStringBuilder xml = new XmlStringBuilder();
    s.toXml(xml, HttpMethod.GET, List.of("/articles/{id}"));
    xml.flush();
    String out = xml.toString();
    Assertions.assertTrue(out.contains("id=\"svc\""), out);
    Assertions.assertTrue(out.contains("group=\"test\""), out);
    Assertions.assertTrue(out.contains("method=\"get\""), out);
  }

  @Test
  void testToXML_urlPatterns() {
    NoContent g = new NoContent();
    Service s = defaultBuilder("svc").add(g).build();
    XmlStringBuilder xml = new XmlStringBuilder();
    s.toXml(xml, HttpMethod.GET, List.of("/articles/{id}", "/news/{id}"));
    xml.flush();
    String out = xml.toString();
    Assertions.assertTrue(out.contains("pattern=\"/articles/{id}\""), out);
    Assertions.assertTrue(out.contains("pattern=\"/news/{id}\""), out);
  }

  @Test
  void testToXML_generatorElement(){
    NoContent g = new NoContent();
    Service s = defaultBuilder("svc").add(g).name("my-gen").build();
    XmlStringBuilder xml = new XmlStringBuilder();
    s.toXml(xml, HttpMethod.GET, List.of("/"));
    xml.flush();
    String out = xml.toString();
    Assertions.assertTrue(out.contains("name=\"my-gen\""), out);
    Assertions.assertTrue(out.contains("class=\"" + NoContent.class.getName() + "\""), out);
  }

  @Test
  void testToXML_cacheControlOverload() {
    NoContent g = new NoContent();
    Service s = defaultBuilder("svc").add(g).build();
    XmlStringBuilder xml = new XmlStringBuilder();
    s.toXml(xml, HttpMethod.GET, List.of("/"), "max-age=600");
    xml.flush();
    String out = xml.toString();
    Assertions.assertTrue(out.contains("cache-control=\"max-age=600\""), out);
  }

  @Test
  void testToXML_flagsAttribute() {
    NoContent g = new NoContent();
    Service s = defaultBuilder("svc").flags("secure").add(g).build();
    XmlStringBuilder xml = new XmlStringBuilder();
    s.toXml(xml, HttpMethod.GET, List.of("/"));
    xml.flush();
    String out = xml.toString();
    Assertions.assertTrue(out.contains("flags=\"secure\""), out);
  }

  @Test
  void testToXML_directAttribute() {
    NoContent g = new NoContent();
    Service s = defaultBuilder("svc").direct(true).add(g).build();
    XmlStringBuilder xml = new XmlStringBuilder();
    s.toXml(xml, HttpMethod.GET, List.of("/"));
    xml.flush();
    String out = xml.toString();
    Assertions.assertTrue(out.contains("direct=\"true\""), out);
  }

  @Test
  void testToXML_generatorParameterElement() {
    NoContent g = new NoContent();
    Parameter p = new Parameter("limit", "10");
    Service s = defaultBuilder("svc").add(g).parameter(p).build();
    XmlStringBuilder xml = new XmlStringBuilder();
    s.toXml(xml, HttpMethod.GET, List.of("/"));
    xml.flush();
    String out = xml.toString();
    Assertions.assertTrue(out.contains("name=\"limit\""), out);
    Assertions.assertTrue(out.contains("value=\"10\""), out);
  }

  @Test
  void testToXML_supportedAttributeForContentGenerator() {
    NoContent g = new NoContent();
    Service s = defaultBuilder("svc").add(g).build();
    XmlStringBuilder xml = new XmlStringBuilder();
    s.toXml(xml, HttpMethod.GET, List.of("/"));
    xml.flush();
    String out = xml.toString();
    Assertions.assertTrue(out.contains("supported=\"xml\""), out);
  }

  @Test
  void testToXML_supportedAttributeForGenerator() {
    CacheableGeneratorByRequest g = new CacheableGeneratorByRequest();
    Service s = defaultBuilder("svc").add(g).build();
    XmlStringBuilder xml = new XmlStringBuilder();
    s.toXml(xml, HttpMethod.GET, List.of("/"));
    xml.flush();
    String out = xml.toString();
    Assertions.assertTrue(out.contains("supported=\"xml,json\""), out);
  }

  @Test
  void testToXML_perGeneratorSupportedAttributeIsDistinctFromServiceLevel() {
    BerliozGenerator xmlGen = new XmlOnlyGenerator();
    BerliozGenerator jsonGen = new JsonOnlyGenerator();
    Service s = defaultBuilder("svc").add(xmlGen).add(jsonGen).build();
    XmlStringBuilder xml = new XmlStringBuilder();
    s.toXml(xml, HttpMethod.GET, List.of("/"));
    xml.flush();
    String out = xml.toString();
    // The generators support disjoint formats, so the service-level intersection is empty...
    Assertions.assertTrue(out.contains("supported=\"\""), out);
    // ...but each generator entry still reports its own supported set.
    Assertions.assertTrue(out.contains("supported=\"xml\""), out);
    Assertions.assertTrue(out.contains("supported=\"json\""), out);
  }

  @Test
  void testToXML_perGeneratorSupportedAttributeForCustomGenerator() {
    BerliozGenerator g = () -> Set.of(OutputType.RAW);
    Service s = defaultBuilder("svc").add(g).build();
    XmlStringBuilder xml = new XmlStringBuilder();
    s.toXml(xml, HttpMethod.GET, List.of("/"));
    xml.flush();
    String out = xml.toString();
    Assertions.assertTrue(out.contains("supported=\"raw\""), out);
  }

  @Test
  void testToXML_generatorTypeContent() {
    NoContent g = new NoContent();
    Service s = defaultBuilder("svc").add(g).build();
    XmlStringBuilder xml = new XmlStringBuilder();
    s.toXml(xml, HttpMethod.GET, List.of("/"));
    xml.flush();
    String out = xml.toString();
    Assertions.assertTrue(out.contains("type=\"xml\""), out);
  }

  @Test
  void testToXML_generatorTypeGenerator() {
    CacheableGeneratorByRequest g = new CacheableGeneratorByRequest();
    Service s = defaultBuilder("svc").add(g).build();
    XmlStringBuilder xml = new XmlStringBuilder();
    s.toXml(xml, HttpMethod.GET, List.of("/"));
    xml.flush();
    String out = xml.toString();
    Assertions.assertTrue(out.contains("type=\"generator\""), out);
  }

  // --- generatorType ---

  @Test
  void testGeneratorType_content() {
    Assertions.assertEquals("xml", Service.generatorType(new NoContent()));
  }

  @Test
  void testGeneratorType_generator() {
    Assertions.assertEquals("generator", Service.generatorType(new CacheableGeneratorByRequest()));
  }

  @Test
  void testGeneratorType_xml() {
    BerliozGenerator g = new XmlOnlyGenerator();
    Assertions.assertEquals("xml", Service.generatorType(g));
  }

  @Test
  void testGeneratorType_json() {
    BerliozGenerator g = new JsonOnlyGenerator();
    Assertions.assertEquals("json", Service.generatorType(g));
  }

  @Test
  void testGeneratorType_xmlJson() {
    BerliozGenerator g = new XmlAndJsonGenerator();
    Assertions.assertEquals("xml-json", Service.generatorType(g));
  }

  @Test
  void testGeneratorType_custom() {
    BerliozGenerator g = () -> Set.of(OutputType.XML);
    Assertions.assertEquals("custom", Service.generatorType(g));
  }

  // --- toString ---

  @Test
  void testToString() {
    Service s = defaultBuilder("svc").add(new NoContent()).build();
    Assertions.assertEquals("service:test/svc", s.toString());
  }

  private static Request request() {
    return (Request) Proxy.newProxyInstance(
        Request.class.getClassLoader(),
        new Class<?>[]{Request.class},
        (proxy, method, args) -> ServletDefaults.defaultValue(method.getReturnType()));
  }

  private static ContentRequest contentRequest() {
    return (ContentRequest) Proxy.newProxyInstance(
        ContentRequest.class.getClassLoader(),
        new Class<?>[]{ContentRequest.class},
        (proxy, method, args) -> ServletDefaults.defaultValue(method.getReturnType()));
  }

  private static final class CacheableContentByRequest implements ContentGenerator, Cacheable {
    @Override public String getETag(Request req) { return "request"; }
    @Override public void process(ContentRequest req, XMLWriter xml) {
      // Nothing to do here
    }
  }

  private static final class CacheableContentByContentRequest implements ContentGenerator, Cacheable {
    @SuppressWarnings("deprecation")
    @Override public String getETag(ContentRequest req) { return "content-request"; }
    @Override public void process(ContentRequest req, XMLWriter xml) {
      // Nothing to do here
    }
  }

  private static final class CacheableGeneratorByRequest implements Generator, Cacheable {
    @Override public String getETag(Request req) { return "request"; }
    @Override public Response generate(Request req, OutputWriter out) { return Response.ok(); }
  }

  private static final class CacheableGeneratorByContentRequest implements Generator, Cacheable {
    @SuppressWarnings("deprecation")
    @Override public String getETag(ContentRequest req) { return "content-request"; }
    @Override public Response generate(Request req, OutputWriter out) { return Response.ok(); }
  }

  private static final class CacheableGeneratorWithoutETag implements Generator, Cacheable {
    @Override public Response generate(Request req, OutputWriter out) { return Response.ok(); }
  }

  private static final class XmlOnlyGenerator implements XmlGenerator {
    @Override public Response generate(Request req, XmlWriter xml) { return Response.ok(); }
  }

  private static final class JsonOnlyGenerator implements JsonGenerator {
    @Override public Response generate(Request req, JsonWriter json) { return Response.ok(); }
  }

  private static final class XmlAndJsonGenerator implements XmlGenerator, JsonGenerator {
    @Override public Set<OutputType> supported() { return Set.of(OutputType.XML, OutputType.JSON); }
    @Override public Response generate(Request req, XmlWriter xml) { return Response.ok(); }
    @Override public Response generate(Request req, JsonWriter json) { return Response.ok(); }
  }

  private static final class ServletDefaults {
    private ServletDefaults() {}

    static Object defaultValue(Class<?> type) {
      if (!type.isPrimitive()) return null;
      if (type == boolean.class) return false;
      if (type == byte.class) return (byte) 0;
      if (type == short.class) return (short) 0;
      if (type == int.class) return 0;
      if (type == long.class) return 0L;
      if (type == float.class) return 0F;
      if (type == double.class) return 0D;
      if (type == char.class) return '\0';
      return null;
    }
  }

}
