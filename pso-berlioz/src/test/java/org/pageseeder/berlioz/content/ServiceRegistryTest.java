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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

  @BeforeEach
  public void setUp() {
    registry = new ServiceRegistry();
  }

  @Test
  public void testRegister_nullService() {
    Assertions.assertThrows(NullPointerException.class, () -> registry.register(null, new URIPattern("/home"), HttpMethod.GET));
  }

  @Test
  public void testRegister_nullPattern() {
    Assertions.assertThrows(NullPointerException.class, () -> registry.register(buildService("svc"), null, HttpMethod.GET));
  }

  @Test
  public void testRegister_nullMethod() {
    Assertions.assertThrows(NullPointerException.class, () -> registry.register(buildService("svc"), new URIPattern("/home"), null));
  }

  @Test
  public void testGet_byMethodAndUrl_match() {
    Service svc = buildService("home");
    registry.register(svc, new URIPattern("/home"), HttpMethod.GET);

    MatchingService match = registry.get("/home", HttpMethod.GET);
    Assertions.assertNotNull(match);
    Assertions.assertSame(svc, match.service());
  }

  @Test
  public void testGet_byMethodAndUrl_noMatch() {
    registry.register(buildService("home"), new URIPattern("/home"), HttpMethod.GET);
    Assertions.assertNull(registry.get("/other", HttpMethod.GET));
  }

  @Test
  public void testGet_byMethodAndUrl_wrongMethod() {
    registry.register(buildService("home"), new URIPattern("/home"), HttpMethod.GET);
    Assertions.assertNull(registry.get("/home", HttpMethod.POST));
  }

  @Test
  public void testGet_headFallsBackToGet() {
    Service svc = buildService("home");
    registry.register(svc, new URIPattern("/home"), HttpMethod.GET);

    MatchingService match = registry.get("/home", HttpMethod.HEAD);
    Assertions.assertNotNull(match);
    Assertions.assertSame(svc, match.service());
  }

  @Test
  public void testGet_byMethodString_match() {
    Service svc = buildService("home");
    registry.register(svc, new URIPattern("/home"), HttpMethod.GET);

    MatchingService match = registry.get("/home", "GET");
    Assertions.assertNotNull(match);
    Assertions.assertSame(svc, match.service());
  }

  @Test
  public void testGet_byMethodString_nullMethod() {
    registry.register(buildService("home"), new URIPattern("/home"), HttpMethod.GET);
    Assertions.assertNull(registry.get("/home", (String) null));
  }

  @Test
  public void testGet_byMethodString_unknownMethod() {
    registry.register(buildService("home"), new URIPattern("/home"), HttpMethod.GET);
    Assertions.assertNull(registry.get("/home", "UNKNOWN"));
  }

  @Test
  public void testGet_anyMethod_match() {
    Service svc = buildService("home");
    registry.register(svc, new URIPattern("/home"), HttpMethod.GET);

    MatchingService match = registry.get("/home");
    Assertions.assertNotNull(match);
    Assertions.assertSame(svc, match.service());
  }

  @Test
  public void testGet_templatePattern() {
    Service svc = buildService("detail");
    registry.register(svc, new URIPattern("/items/{id}"), HttpMethod.GET);

    MatchingService match = registry.get("/items/42", HttpMethod.GET);
    Assertions.assertNotNull(match);
    Assertions.assertSame(svc, match.service());
  }

  @Test
  public void testAllows_getAlsoAddsHead() {
    registry.register(buildService("home"), new URIPattern("/home"), HttpMethod.GET);
    List<String> methods = registry.allows("/home");
    Assertions.assertTrue(methods.contains("GET"));
    Assertions.assertTrue(methods.contains("HEAD"));
  }

  @Test
  public void testAllows_postOnly() {
    registry.register(buildService("home"), new URIPattern("/home"), HttpMethod.POST);
    List<String> methods = registry.allows("/home");
    Assertions.assertTrue(methods.contains("POST"));
    Assertions.assertFalse(methods.contains("HEAD"));
  }

  @Test
  public void testAllows_noMatch() {
    registry.register(buildService("home"), new URIPattern("/home"), HttpMethod.GET);
    Assertions.assertTrue(registry.allows("/other").isEmpty());
  }

  @Test
  public void testGetMethod() {
    Service svc = buildService("home");
    registry.register(svc, new URIPattern("/home"), HttpMethod.GET);
    Assertions.assertEquals(HttpMethod.GET, registry.getMethod(svc));
  }

  @Test
  public void testGetMethod_null() {
    Assertions.assertNull(registry.getMethod(null));
  }

  @Test
  public void testGetMethod_notRegistered() {
    Assertions.assertNull(registry.getMethod(buildService("unknown")));
  }

  @Test
  public void testMatches() {
    Service svc = buildService("home");
    registry.register(svc, new URIPattern("/home"), HttpMethod.GET);
    List<String> patterns = registry.matches(svc);
    Assertions.assertEquals(1, patterns.size());
    Assertions.assertEquals(patterns.get(0), "/home");
  }

  @Test
  public void testMatches_null() {
    Assertions.assertTrue(registry.matches(null).isEmpty());
  }

  @Test
  public void testGetServiceMap() {
    Service svc = buildService("home");
    registry.register(svc, new URIPattern("/home"), HttpMethod.GET);
    Map<String, Service> map = registry.getServiceMap(HttpMethod.GET);
    Assertions.assertTrue(map.containsKey("/home"));
    Assertions.assertSame(svc, map.get("/home"));
  }

  @Test
  public void testGetServices() {
    Service s1 = buildService("svc1");
    Service s2 = buildService("svc2");
    registry.register(s1, new URIPattern("/a"), HttpMethod.GET);
    registry.register(s2, new URIPattern("/b"), HttpMethod.POST);
    List<Service> services = registry.getServices();
    Assertions.assertEquals(2, services.size());
  }

  @Test
  public void testGetServices_byMethod() {
    Service svcGet = buildService("svcGet");
    Service svcPost = buildService("svcPost");
    registry.register(svcGet, new URIPattern("/a"), HttpMethod.GET);
    registry.register(svcPost, new URIPattern("/b"), HttpMethod.POST);

    List<Service> gets = registry.getServices(HttpMethod.GET);
    Assertions.assertEquals(1, gets.size());
    Assertions.assertSame(svcGet, gets.get(0));
  }

  @Test
  public void testClear() {
    registry.register(buildService("home"), new URIPattern("/home"), HttpMethod.GET);
    registry.clear();
    Assertions.assertNull(registry.get("/home", HttpMethod.GET));
    Assertions.assertTrue(registry.getServices().isEmpty());
  }

  @Test
  public void testVersion_changesAfterTouch() {
    long before = registry.version();
    registry.touch();
    Assertions.assertTrue(registry.version() >= before);
  }
}
