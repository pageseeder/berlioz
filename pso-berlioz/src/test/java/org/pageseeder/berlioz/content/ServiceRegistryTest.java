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

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.pageseeder.berlioz.furi.URIPattern;
import org.pageseeder.berlioz.generator.NoContent;
import org.pageseeder.berlioz.http.HttpMethod;

public final class ServiceRegistryTest {

  private ServiceRegistry registry;

  private static Service buildService(String id) {
    return new Service.Builder()
        .id(id)
        .group("test")
        .rule(ServiceStatusRule.DEFAULT_RULE)
        .add(new NoContent())
        .build();
  }

  @Before
  public void setUp() {
    registry = new ServiceRegistry();
  }

  @Test(expected = NullPointerException.class)
  public void testRegister_nullService() {
    registry.register(null, new URIPattern("/home"), HttpMethod.GET);
  }

  @Test(expected = NullPointerException.class)
  public void testRegister_nullPattern() {
    registry.register(buildService("svc"), null, HttpMethod.GET);
  }

  @Test(expected = NullPointerException.class)
  public void testRegister_nullMethod() {
    registry.register(buildService("svc"), new URIPattern("/home"), null);
  }

  @Test
  public void testGet_byMethodAndUrl_match() {
    Service svc = buildService("home");
    registry.register(svc, new URIPattern("/home"), HttpMethod.GET);

    MatchingService match = registry.get("/home", HttpMethod.GET);
    Assert.assertNotNull(match);
    Assert.assertSame(svc, match.service());
  }

  @Test
  public void testGet_byMethodAndUrl_noMatch() {
    registry.register(buildService("home"), new URIPattern("/home"), HttpMethod.GET);
    Assert.assertNull(registry.get("/other", HttpMethod.GET));
  }

  @Test
  public void testGet_byMethodAndUrl_wrongMethod() {
    registry.register(buildService("home"), new URIPattern("/home"), HttpMethod.GET);
    Assert.assertNull(registry.get("/home", HttpMethod.POST));
  }

  @Test
  public void testGet_headFallsBackToGet() {
    Service svc = buildService("home");
    registry.register(svc, new URIPattern("/home"), HttpMethod.GET);

    MatchingService match = registry.get("/home", HttpMethod.HEAD);
    Assert.assertNotNull(match);
    Assert.assertSame(svc, match.service());
  }

  @Test
  public void testGet_byMethodString_match() {
    Service svc = buildService("home");
    registry.register(svc, new URIPattern("/home"), HttpMethod.GET);

    MatchingService match = registry.get("/home", "GET");
    Assert.assertNotNull(match);
    Assert.assertSame(svc, match.service());
  }

  @Test
  public void testGet_byMethodString_nullMethod() {
    registry.register(buildService("home"), new URIPattern("/home"), HttpMethod.GET);
    Assert.assertNull(registry.get("/home", (String) null));
  }

  @Test
  public void testGet_byMethodString_unknownMethod() {
    registry.register(buildService("home"), new URIPattern("/home"), HttpMethod.GET);
    Assert.assertNull(registry.get("/home", "UNKNOWN"));
  }

  @Test
  public void testGet_anyMethod_match() {
    Service svc = buildService("home");
    registry.register(svc, new URIPattern("/home"), HttpMethod.GET);

    MatchingService match = registry.get("/home");
    Assert.assertNotNull(match);
    Assert.assertSame(svc, match.service());
  }

  @Test
  public void testGet_templatePattern() {
    Service svc = buildService("detail");
    registry.register(svc, new URIPattern("/items/{id}"), HttpMethod.GET);

    MatchingService match = registry.get("/items/42", HttpMethod.GET);
    Assert.assertNotNull(match);
    Assert.assertSame(svc, match.service());
  }

  @Test
  public void testAllows_getAlsoAddsHead() {
    registry.register(buildService("home"), new URIPattern("/home"), HttpMethod.GET);
    List<String> methods = registry.allows("/home");
    Assert.assertTrue(methods.contains("GET"));
    Assert.assertTrue(methods.contains("HEAD"));
  }

  @Test
  public void testAllows_postOnly() {
    registry.register(buildService("home"), new URIPattern("/home"), HttpMethod.POST);
    List<String> methods = registry.allows("/home");
    Assert.assertTrue(methods.contains("POST"));
    Assert.assertFalse(methods.contains("HEAD"));
  }

  @Test
  public void testAllows_noMatch() {
    registry.register(buildService("home"), new URIPattern("/home"), HttpMethod.GET);
    Assert.assertTrue(registry.allows("/other").isEmpty());
  }

  @Test
  public void testGetMethod() {
    Service svc = buildService("home");
    registry.register(svc, new URIPattern("/home"), HttpMethod.GET);
    Assert.assertEquals(HttpMethod.GET, registry.getMethod(svc));
  }

  @Test
  public void testGetMethod_null() {
    Assert.assertNull(registry.getMethod(null));
  }

  @Test
  public void testGetMethod_notRegistered() {
    Assert.assertNull(registry.getMethod(buildService("unknown")));
  }

  @Test
  public void testMatches() {
    Service svc = buildService("home");
    registry.register(svc, new URIPattern("/home"), HttpMethod.GET);
    List<String> patterns = registry.matches(svc);
    Assert.assertEquals(1, patterns.size());
    Assert.assertEquals("/home", patterns.get(0));
  }

  @Test
  public void testMatches_null() {
    Assert.assertTrue(registry.matches(null).isEmpty());
  }

  @Test
  public void testGetServiceMap() {
    Service svc = buildService("home");
    registry.register(svc, new URIPattern("/home"), HttpMethod.GET);
    Map<String, Service> map = registry.getServiceMap(HttpMethod.GET);
    Assert.assertTrue(map.containsKey("/home"));
    Assert.assertSame(svc, map.get("/home"));
  }

  @Test
  public void testGetServices() {
    Service s1 = buildService("svc1");
    Service s2 = buildService("svc2");
    registry.register(s1, new URIPattern("/a"), HttpMethod.GET);
    registry.register(s2, new URIPattern("/b"), HttpMethod.POST);
    List<Service> services = registry.getServices();
    Assert.assertEquals(2, services.size());
  }

  @Test
  public void testGetServices_byMethod() {
    Service svcGet = buildService("svcGet");
    Service svcPost = buildService("svcPost");
    registry.register(svcGet, new URIPattern("/a"), HttpMethod.GET);
    registry.register(svcPost, new URIPattern("/b"), HttpMethod.POST);

    List<Service> gets = registry.getServices(HttpMethod.GET);
    Assert.assertEquals(1, gets.size());
    Assert.assertSame(svcGet, gets.get(0));
  }

  @Test
  public void testClear() {
    registry.register(buildService("home"), new URIPattern("/home"), HttpMethod.GET);
    registry.clear();
    Assert.assertNull(registry.get("/home", HttpMethod.GET));
    Assert.assertTrue(registry.getServices().isEmpty());
  }

  @Test
  public void testVersion_changesAfterTouch() {
    long before = registry.version();
    registry.touch();
    Assert.assertTrue(registry.version() >= before);
  }
}
