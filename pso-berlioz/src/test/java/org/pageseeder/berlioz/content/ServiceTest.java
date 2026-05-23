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

import org.junit.Assert;
import org.junit.Test;
import org.pageseeder.berlioz.generator.NoContent;

public final class ServiceTest {

  private static Service.Builder defaultBuilder(String id) {
    return new Service.Builder()
        .id(id)
        .group("test")
        .rule(ServiceStatusRule.DEFAULT_RULE);
  }

  // --- Builder ---

  @Test
  public void testBuilder_id() {
    Service s = defaultBuilder("my-service").add(new NoContent()).build();
    Assert.assertEquals("my-service", s.id());
  }

  @Test
  public void testBuilder_group() {
    Service s = defaultBuilder("svc").add(new NoContent()).build();
    Assert.assertEquals("test", s.group());
  }

  @Test
  public void testBuilder_groupDefault() {
    Service s = new Service.Builder()
        .id("svc")
        .group(null)
        .rule(ServiceStatusRule.DEFAULT_RULE)
        .add(new NoContent())
        .build();
    Assert.assertEquals("default", s.group());
  }

  @Test
  public void testBuilder_cache() {
    Service s = defaultBuilder("svc").cache("max-age=3600").add(new NoContent()).build();
    Assert.assertEquals("max-age=3600", s.cache());
  }

  @Test
  public void testBuilder_cacheDefault() {
    Service s = defaultBuilder("svc").add(new NoContent()).build();
    Assert.assertEquals("", s.cache());
  }

  @Test
  public void testBuilder_flags() {
    Service s = defaultBuilder("svc").flags("secure").add(new NoContent()).build();
    Assert.assertEquals("secure", s.flags());
  }

  @Test
  public void testBuilder_flagsDefault() {
    Service s = defaultBuilder("svc").add(new NoContent()).build();
    Assert.assertEquals("", s.flags());
  }

  @Test(expected = NullPointerException.class)
  public void testBuilder_idRequired() {
    new Service.Builder()
        .group("test")
        .rule(ServiceStatusRule.DEFAULT_RULE)
        .add(new NoContent())
        .build();
  }

  @Test
  public void testBuilder_reset() {
    Service.Builder builder = defaultBuilder("first");
    builder.add(new NoContent());
    builder.build();
    builder.reset();

    Assert.assertNull(builder.id());
    Assert.assertEquals("test", builder.group());

    builder.id("second").add(new NoContent());
    Service second = builder.build();
    Assert.assertEquals("second", second.id());
  }

  // --- Generators ---

  @Test
  public void testGenerators_single() {
    NoContent g = new NoContent();
    Service s = defaultBuilder("svc").add(g).build();
    Assert.assertEquals(1, s.generators().size());
    Assert.assertSame(g, s.generators().get(0));
  }

  @Test
  public void testGenerators_multiple() {
    NoContent g1 = new NoContent();
    NoContent g2 = new NoContent();
    Service s = defaultBuilder("svc").add(g1).add(g2).build();
    Assert.assertEquals(2, s.generators().size());
  }

  // --- Cacheability ---

  @Test
  public void testIsCacheable_allCacheable() {
    Service s = defaultBuilder("svc").add(new NoContent()).build();
    Assert.assertTrue(s.isCacheable());
  }

  @Test
  public void testIsCacheable_notCacheable() {
    ContentGenerator nonCacheable = (req, xml) -> {};
    Service s = defaultBuilder("svc").add(nonCacheable).build();
    Assert.assertFalse(s.isCacheable());
  }

  @Test
  public void testIsCacheable_mixedGenerators() {
    Service s = defaultBuilder("svc").add(new NoContent()).add((req, xml) -> {}).build();
    Assert.assertFalse(s.isCacheable());
  }

  @Test
  public void testIsCacheable_static_emptyList() {
    Assert.assertTrue(Service.isCacheable(Collections.emptyList()));
  }

  @Test
  public void testIsCacheable_static_allCacheable() {
    Assert.assertTrue(Service.isCacheable(List.of(new NoContent())));
  }

  @Test
  public void testIsCacheable_static_notCacheable() {
    ContentGenerator nonCacheable = (req, xml) -> {};
    Assert.assertFalse(Service.isCacheable(List.of(nonCacheable)));
  }

  // --- Name and Target ---

  @Test
  public void testName_defaultIsKebabCase() {
    NoContent g = new NoContent();
    Service s = defaultBuilder("svc").add(g).build();
    Assert.assertEquals("no-content", s.name(g));
  }

  @Test
  public void testName_explicit() {
    NoContent g = new NoContent();
    Service s = defaultBuilder("svc").add(g).name("my-generator").build();
    Assert.assertEquals("my-generator", s.name(g));
  }

  @Test
  public void testTarget_absent() {
    NoContent g = new NoContent();
    Service s = defaultBuilder("svc").add(g).build();
    Assert.assertNull(s.target(g));
  }

  @Test
  public void testTarget_set() {
    NoContent g = new NoContent();
    Service s = defaultBuilder("svc").add(g).target("main").build();
    Assert.assertEquals("main", s.target(g));
  }

  // --- Parameters ---

  @Test
  public void testParameters_empty() {
    NoContent g = new NoContent();
    Service s = defaultBuilder("svc").add(g).build();
    Assert.assertTrue(s.parameters(g).isEmpty());
  }

  @Test
  public void testParameters_withParameter() {
    NoContent g = new NoContent();
    Parameter p = new Parameter("key", "value");
    Service s = defaultBuilder("svc").add(g).parameter(p).build();
    List<Parameter> params = s.parameters(g);
    Assert.assertEquals(1, params.size());
    Assert.assertEquals("key", params.get(0).name());
    Assert.assertEquals("value", params.get(0).value());
  }

  @Test
  public void testParameters_nullParameterIgnored() {
    NoContent g = new NoContent();
    Service s = defaultBuilder("svc").add(g).parameter(null).build();
    Assert.assertTrue(s.parameters(g).isEmpty());
  }

  // --- affectStatus ---

  @Test
  public void testAffectStatus_appliesToAll() {
    NoContent g = new NoContent();
    Service s = defaultBuilder("svc").add(g).build();
    // DEFAULT_RULE applies to all
    Assert.assertTrue(s.affectStatus(g));
  }

  @Test
  public void testAffectStatus_byName_matching() {
    NoContent g = new NoContent();
    ServiceStatusRule rule = ServiceStatusRule.newInstance("name:my-gen", "HIGHEST");
    Service s = new Service.Builder()
        .id("svc").group("test").rule(rule)
        .add(g).name("my-gen")
        .build();
    Assert.assertTrue(s.affectStatus(g));
  }

  @Test
  public void testAffectStatus_byName_notMatching() {
    NoContent g = new NoContent();
    ServiceStatusRule rule = ServiceStatusRule.newInstance("name:other-gen", "HIGHEST");
    Service s = new Service.Builder()
        .id("svc").group("test").rule(rule)
        .add(g).name("my-gen")
        .build();
    Assert.assertFalse(s.affectStatus(g));
  }

  // --- toString ---

  @Test
  public void testToString() {
    Service s = defaultBuilder("svc").add(new NoContent()).build();
    Assert.assertEquals("service:test/svc", s.toString());
  }
}
