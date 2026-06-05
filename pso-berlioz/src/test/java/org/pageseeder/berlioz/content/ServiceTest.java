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

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.generator.NoContent;

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

  // --- toString ---

  @Test
  void testToString() {
    Service s = defaultBuilder("svc").add(new NoContent()).build();
    Assertions.assertEquals("service:test/svc", s.toString());
  }
}
