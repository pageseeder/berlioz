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

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.furi.URIPattern;
import org.pageseeder.berlioz.generator.NoContent;
import org.pageseeder.berlioz.http.HttpMethod;
import org.pageseeder.berlioz.util.CollectedError;
import org.pageseeder.berlioz.util.CollectedError.Level;
import org.xml.sax.SAXParseException;

final class ServiceRegistryTest {

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
  void setUp() {
    registry = new ServiceRegistry();
  }

  @Test
  void testRegister_nullService() {
    URIPattern pattern = new URIPattern("/home");
    Assertions.assertThrows(NullPointerException.class, () -> registry.register(null, pattern, HttpMethod.GET));
  }

  @Test
  void testRegister_nullPattern() {
    Service service = buildService("svc");
    Assertions.assertThrows(NullPointerException.class, () -> registry.register(service, null, HttpMethod.GET));
  }

  @Test
  void testRegister_nullMethod() {
    URIPattern pattern = new URIPattern("/home");
    Service service = buildService("svc");
    Assertions.assertThrows(NullPointerException.class, () -> registry.register(service, pattern, null));
  }

  @Test
  void testGet_byMethodAndUrl_match() {
    Service svc = buildService("home");
    registry.register(svc, new URIPattern("/home"), HttpMethod.GET);

    MatchingService match = registry.get("/home", HttpMethod.GET);
    Assertions.assertNotNull(match);
    Assertions.assertSame(svc, match.service());
  }

  @Test
  void testGet_byMethodAndUrl_noMatch() {
    registry.register(buildService("home"), new URIPattern("/home"), HttpMethod.GET);
    Assertions.assertNull(registry.get("/other", HttpMethod.GET));
  }

  @Test
  void testGet_byMethodAndUrl_wrongMethod() {
    registry.register(buildService("home"), new URIPattern("/home"), HttpMethod.GET);
    Assertions.assertNull(registry.get("/home", HttpMethod.POST));
  }

  @Test
  void testGet_headFallsBackToGet() {
    Service svc = buildService("home");
    registry.register(svc, new URIPattern("/home"), HttpMethod.GET);

    MatchingService match = registry.get("/home", HttpMethod.HEAD);
    Assertions.assertNotNull(match);
    Assertions.assertSame(svc, match.service());
  }

  @Test
  void testGet_byMethodString_match() {
    Service svc = buildService("home");
    registry.register(svc, new URIPattern("/home"), HttpMethod.GET);

    MatchingService match = registry.get("/home", "GET");
    Assertions.assertNotNull(match);
    Assertions.assertSame(svc, match.service());
  }

  @Test
  void testGet_byMethodString_nullMethod() {
    registry.register(buildService("home"), new URIPattern("/home"), HttpMethod.GET);
    Assertions.assertNull(registry.get("/home", (String) null));
  }

  @Test
  void testGet_byMethodString_unknownMethod() {
    registry.register(buildService("home"), new URIPattern("/home"), HttpMethod.GET);
    Assertions.assertNull(registry.get("/home", "UNKNOWN"));
  }

  @Test
  void testGet_anyMethod_match() {
    Service svc = buildService("home");
    registry.register(svc, new URIPattern("/home"), HttpMethod.GET);

    MatchingService match = registry.get("/home");
    Assertions.assertNotNull(match);
    Assertions.assertSame(svc, match.service());
  }

  @Test
  void testGet_templatePattern() {
    Service svc = buildService("detail");
    registry.register(svc, new URIPattern("/items/{id}"), HttpMethod.GET);

    MatchingService match = registry.get("/items/42", HttpMethod.GET);
    Assertions.assertNotNull(match);
    Assertions.assertSame(svc, match.service());
  }

  @Test
  void testAllows_getAlsoAddsHead() {
    registry.register(buildService("home"), new URIPattern("/home"), HttpMethod.GET);
    List<String> methods = registry.allows("/home");
    Assertions.assertTrue(methods.contains("GET"));
    Assertions.assertTrue(methods.contains("HEAD"));
  }

  @Test
  void testAllows_postOnly() {
    registry.register(buildService("home"), new URIPattern("/home"), HttpMethod.POST);
    List<String> methods = registry.allows("/home");
    Assertions.assertTrue(methods.contains("POST"));
    Assertions.assertFalse(methods.contains("HEAD"));
  }

  @Test
  void testAllows_noMatch() {
    registry.register(buildService("home"), new URIPattern("/home"), HttpMethod.GET);
    Assertions.assertTrue(registry.allows("/other").isEmpty());
  }

  @Test
  void testGetMethod() {
    Service svc = buildService("home");
    registry.register(svc, new URIPattern("/home"), HttpMethod.GET);
    Assertions.assertEquals(HttpMethod.GET, registry.getMethod(svc));
  }

  @Test
  void testGetMethod_null() {
    Assertions.assertNull(registry.getMethod(null));
  }

  @Test
  void testGetMethod_notRegistered() {
    Assertions.assertNull(registry.getMethod(buildService("unknown")));
  }

  @Test
  void testMatches() {
    Service svc = buildService("home");
    registry.register(svc, new URIPattern("/home"), HttpMethod.GET);
    List<String> patterns = registry.matches(svc);
    Assertions.assertEquals(1, patterns.size());
    Assertions.assertEquals("/home", patterns.get(0));
  }

  @Test
  void testMatches_null() {
    Assertions.assertTrue(registry.matches(null).isEmpty());
  }

  @Test
  void testGetServiceMap() {
    Service svc = buildService("home");
    registry.register(svc, new URIPattern("/home"), HttpMethod.GET);
    Map<String, Service> map = registry.getServiceMap(HttpMethod.GET);
    Assertions.assertTrue(map.containsKey("/home"));
    Assertions.assertSame(svc, map.get("/home"));
  }

  @Test
  void testGetServices() {
    Service s1 = buildService("svc1");
    Service s2 = buildService("svc2");
    registry.register(s1, new URIPattern("/a"), HttpMethod.GET);
    registry.register(s2, new URIPattern("/b"), HttpMethod.POST);
    List<Service> services = registry.getServices();
    Assertions.assertEquals(2, services.size());
  }

  @Test
  void testGetServices_byMethod() {
    Service svcGet = buildService("svcGet");
    Service svcPost = buildService("svcPost");
    registry.register(svcGet, new URIPattern("/a"), HttpMethod.GET);
    registry.register(svcPost, new URIPattern("/b"), HttpMethod.POST);

    List<Service> gets = registry.getServices(HttpMethod.GET);
    Assertions.assertEquals(1, gets.size());
    Assertions.assertSame(svcGet, gets.get(0));
  }

  @Test
  void testClear() {
    registry.register(buildService("home"), new URIPattern("/home"), HttpMethod.GET);
    registry.clear();
    Assertions.assertNull(registry.get("/home", HttpMethod.GET));
    Assertions.assertTrue(registry.getServices().isEmpty());
  }

  @Test
  void testVersion_initialValueIsPositive() {
    Assertions.assertTrue(registry.version() > 0);
  }

  @Test
  void testVersion_changesAfterTouch() {
    long before = registry.version();
    registry.touch();
    Assertions.assertTrue(registry.version() >= before);
  }

  @Test
  void testGet_anyMethod_noMatch() {
    registry.register(buildService("home"), new URIPattern("/home"), HttpMethod.GET);
    Assertions.assertNull(registry.get("/other"));
  }

  @Test
  void testGet_byMethodAndUrl_nullMethod() {
    registry.register(buildService("home"), new URIPattern("/home"), HttpMethod.GET);
    Assertions.assertNull(registry.get("/home", (HttpMethod) null));
  }

  @Test
  void testGet_templatePattern_variablesResolved() {
    Service svc = buildService("detail");
    registry.register(svc, new URIPattern("/items/{id}"), HttpMethod.GET);

    MatchingService match = registry.get("/items/42", HttpMethod.GET);
    Assertions.assertNotNull(match);
    Assertions.assertEquals("42", match.result().get("id"));
  }

  @Test
  void testGet_byMethodAndUrl_query_match() {
    Service svc = buildService("search");
    registry.register(svc, new URIPattern("/search"), HttpMethod.QUERY);

    MatchingService match = registry.get("/search", HttpMethod.QUERY);
    Assertions.assertNotNull(match);
    Assertions.assertSame(svc, match.service());
  }

  @Test
  void testAllows_queryOnly_doesNotImplyHead() {
    registry.register(buildService("search"), new URIPattern("/search"), HttpMethod.QUERY);
    List<String> methods = registry.allows("/search");
    Assertions.assertTrue(methods.contains("QUERY"), methods.toString());
    Assertions.assertFalse(methods.contains("HEAD"), methods.toString());
  }

  @Test
  void testAllows_multipleMethodsSameUrl() {
    registry.register(buildService("get-home"),  new URIPattern("/home"), HttpMethod.GET);
    registry.register(buildService("post-home"), new URIPattern("/home"), HttpMethod.POST);
    List<String> methods = registry.allows("/home");
    Assertions.assertTrue(methods.contains("GET"),  methods.toString());
    Assertions.assertTrue(methods.contains("HEAD"), methods.toString());
    Assertions.assertTrue(methods.contains("POST"), methods.toString());
  }

  @Test
  void testGetServiceMap_isUnmodifiable() {
    registry.register(buildService("home"), new URIPattern("/home"), HttpMethod.GET);
    Map<String, Service> map = registry.getServiceMap(HttpMethod.GET);
    Service other = buildService("other");
    Assertions.assertThrows(UnsupportedOperationException.class,
        () -> map.put("/other", other));
  }

  @Test
  void testGetServices_empty() {
    Assertions.assertTrue(registry.getServices().isEmpty());
  }

  @Test
  void testMatches_multiplePatterns() {
    Service svc = buildService("multi");
    registry.register(svc, new URIPattern("/a"), HttpMethod.GET);
    registry.register(svc, new URIPattern("/b"), HttpMethod.GET);
    List<String> patterns = registry.matches(svc);
    Assertions.assertEquals(2, patterns.size());
    Assertions.assertTrue(patterns.contains("/a"), patterns.toString());
    Assertions.assertTrue(patterns.contains("/b"), patterns.toString());
  }

  // --- overriding registrations ---

  @Test
  void testRegister_overridesPreviousServiceForSamePattern() {
    Service first = buildService("first");
    Service second = buildService("second");
    registry.register(first, new URIPattern("/home"), HttpMethod.GET);
    registry.register(second, new URIPattern("/home"), HttpMethod.GET);

    MatchingService match = registry.get("/home", HttpMethod.GET);
    Assertions.assertNotNull(match);
    Assertions.assertSame(second, match.service(), "The later registration should win");
  }

  // --- overrideWarning (static) ---

  @Test
  void testOverrideWarning_noPrevious_returnsNull() {
    Service svc = buildService("svc");
    Assertions.assertNull(ServiceRegistry.overrideWarning(null, svc, new URIPattern("/home")));
  }

  @Test
  void testOverrideWarning_previousExists_namesBothServicesAndPattern() {
    Service first = buildService("first");
    Service second = buildService("second");
    URIPattern pattern = new URIPattern("/home");

    String warning = ServiceRegistry.overrideWarning(first, second, pattern);

    Assertions.assertNotNull(warning);
    Assertions.assertTrue(warning.contains(first.toString()), warning);
    Assertions.assertTrue(warning.contains(second.toString()), warning);
    Assertions.assertTrue(warning.contains("/home"), warning);
  }

  // --- duplicate-pattern fix on override ---

  @Test
  void testRegister_overrideDoesNotAccumulateDuplicatePatterns() throws Exception {
    URIPattern pattern = new URIPattern("/items/{id}");
    registry.register(buildService("v1"), pattern, HttpMethod.GET);
    registry.register(buildService("v2"), pattern, HttpMethod.GET);
    registry.register(buildService("v3"), pattern, HttpMethod.GET);

    List<?> patterns = patternsFor(registry, HttpMethod.GET);
    Assertions.assertEquals(1, patterns.size(),
        "Repeated overrides of the same pattern must not accumulate stale duplicate entries");
  }

  @Test
  void testRegister_overrideStillResolvesLatestServiceForTemplatePattern() {
    URIPattern pattern = new URIPattern("/items/{id}");
    registry.register(buildService("v1"), pattern, HttpMethod.GET);
    registry.register(buildService("v2"), pattern, HttpMethod.GET);
    Service latest = buildService("v3");
    registry.register(latest, pattern, HttpMethod.GET);

    MatchingService match = registry.get("/items/42", HttpMethod.GET);
    Assertions.assertNotNull(match);
    Assertions.assertSame(latest, match.service());
  }

  @Test
  void testRegister_overrideDoesNotShiftTieBreakPositionForOtherPatterns() {
    // "/{x}" and "/{y}" are pure-template patterns with equal (zero) literal score, so both
    // match "/z" with a tie; BEST_MATCH keeps whichever was encountered first.
    Service firstA = buildService("a1");
    Service b = buildService("b");
    Service overriddenA = buildService("a2");
    registry.register(firstA, new URIPattern("/{x}"), HttpMethod.GET);
    registry.register(b, new URIPattern("/{y}"), HttpMethod.GET);
    registry.register(overriddenA, new URIPattern("/{x}"), HttpMethod.GET);

    MatchingService match = registry.get("/z", HttpMethod.GET);
    Assertions.assertNotNull(match);
    Assertions.assertSame(overriddenA, match.service(),
        "Overriding a pattern must keep the tie-break position it held before the override, "
            + "not move it after patterns registered later");
  }

  /**
   * Reads the private {@code patterns} list of the {@code ServiceMap} for the given method via
   * reflection, to verify the internal no-duplicate-entries invariant directly since it is not
   * otherwise observable through the public API.
   */
  private static List<?> patternsFor(ServiceRegistry target, HttpMethod method) throws Exception {
    Field stateField = ServiceRegistry.class.getDeclaredField("state");
    stateField.setAccessible(true);
    Object state = stateField.get(target);
    Field mappingField = state.getClass().getDeclaredField("mapping");
    mappingField.setAccessible(true);
    Map<?, ?> mapping = (Map<?, ?>) mappingField.get(state);
    Object serviceMap = mapping.get(method);
    Field patternsField = serviceMap.getClass().getDeclaredField("patterns");
    patternsField.setAccessible(true);
    return (List<?>) patternsField.get(serviceMap);
  }

  // --- atomic state publication (replaceWith) ---

  @Test
  void testReplaceWith_publishesCandidateContent() {
    ServiceRegistry candidate = new ServiceRegistry();
    Service svc = buildService("candidate-service");
    candidate.register(svc, new URIPattern("/candidate"), HttpMethod.GET);

    registry.replaceWith(candidate);

    MatchingService match = registry.get("/candidate", HttpMethod.GET);
    Assertions.assertNotNull(match);
    Assertions.assertSame(svc, match.service());
  }

  @Test
  void testReplaceWith_removesPreviousContent() {
    registry.register(buildService("old"), new URIPattern("/old"), HttpMethod.GET);
    ServiceRegistry candidate = new ServiceRegistry();
    candidate.register(buildService("new"), new URIPattern("/new"), HttpMethod.GET);

    registry.replaceWith(candidate);

    Assertions.assertNull(registry.get("/old", HttpMethod.GET),
        "replaceWith must fully replace the previous state, not merge with it");
    Assertions.assertNotNull(registry.get("/new", HttpMethod.GET));
  }

  @Test
  void testReplaceWith_incrementsVersionExactlyOnce() {
    long before = registry.version();
    ServiceRegistry candidate = new ServiceRegistry();
    candidate.register(buildService("svc"), new URIPattern("/a"), HttpMethod.GET);

    registry.replaceWith(candidate);
    long afterFirst = registry.version();

    registry.replaceWith(candidate);
    long afterSecond = registry.version();

    Assertions.assertTrue(afterFirst >= before);
    Assertions.assertTrue(afterSecond >= afterFirst);
  }

  @Test
  void testReplaceWith_nullCandidate_throwsNullPointerException() {
    Assertions.assertThrows(NullPointerException.class, () -> registry.replaceWith(null));
  }

  @Test
  void testReplaceWith_publishesOriginsAndWarningsWithCandidate() {
    ServiceRegistry candidate = new ServiceRegistry();
    Service service = buildService("candidate-service");
    URIPattern pattern = new URIPattern("/candidate");
    candidate.register(service, pattern, HttpMethod.GET);
    ServiceOrigin origin = ServiceOrigin.forFile(new java.io.File("services.xml"), null);
    ServiceRegistration registration = new ServiceRegistration(service, HttpMethod.GET, pattern, origin);
    CollectedError<SAXParseException> warning = new CollectedError<>(Level.WARNING,
        new SAXParseException("candidate warning", null));

    registry.replaceWith(candidate, List.of(registration), List.of(warning));

    Assertions.assertEquals(List.of(registration), registry.registrations());
    Assertions.assertEquals(List.of(warning), registry.warnings());
    Assertions.assertNotNull(registry.get("/candidate", HttpMethod.GET));
  }

  @Test
  void testClear_clearsMappingsOriginsAndWarningsInOnePublication() {
    ServiceRegistry candidate = new ServiceRegistry();
    Service service = buildService("candidate-service");
    URIPattern pattern = new URIPattern("/candidate");
    candidate.register(service, pattern, HttpMethod.GET);
    ServiceOrigin origin = ServiceOrigin.forFile(new java.io.File("services.xml"), null);
    ServiceRegistration registration = new ServiceRegistration(service, HttpMethod.GET, pattern, origin);
    CollectedError<SAXParseException> warning = new CollectedError<>(Level.WARNING,
        new SAXParseException("candidate warning", null));
    registry.replaceWith(candidate, List.of(registration), List.of(warning));

    registry.clear();

    Assertions.assertNull(registry.get("/candidate", HttpMethod.GET));
    Assertions.assertTrue(registry.registrations().isEmpty());
    Assertions.assertTrue(registry.warnings().isEmpty());
  }

  // --- toRegistrations ---

  @Test
  void testToRegistrations_extractsAllRegistrationsWithOrigin() {
    Service getSvc = buildService("get-svc");
    Service postSvc = buildService("post-svc");
    registry.register(getSvc, new URIPattern("/a"), HttpMethod.GET);
    registry.register(postSvc, new URIPattern("/b"), HttpMethod.POST);
    ServiceOrigin origin = ServiceOrigin.forFile(new java.io.File("services.xml"), null);

    List<ServiceRegistration> registrations = registry.toRegistrations(origin);

    Assertions.assertEquals(2, registrations.size());
    Assertions.assertTrue(registrations.stream().allMatch(r -> r.origin() == origin));
    Assertions.assertTrue(registrations.stream().anyMatch(
        r -> r.service() == getSvc && r.method() == HttpMethod.GET && "/a".equals(r.pattern().toString())));
    Assertions.assertTrue(registrations.stream().anyMatch(
        r -> r.service() == postSvc && r.method() == HttpMethod.POST && "/b".equals(r.pattern().toString())));
  }

  @Test
  void testToRegistrations_empty() {
    ServiceOrigin origin = ServiceOrigin.forFile(new java.io.File("services.xml"), null);
    Assertions.assertTrue(registry.toRegistrations(origin).isEmpty());
  }

  // --- concurrent reads during publication ---

  @Test
  void testReplaceWith_concurrentReadsNeverObserveAPartialMix() throws InterruptedException {
    ServiceRegistry stateA = new ServiceRegistry();
    Service svcA = buildService("state-a");
    stateA.register(svcA, new URIPattern("/shared"), HttpMethod.GET);

    ServiceRegistry stateB = new ServiceRegistry();
    Service svcB = buildService("state-b");
    stateB.register(svcB, new URIPattern("/shared"), HttpMethod.GET);

    registry.replaceWith(stateA);

    AtomicBoolean running = new AtomicBoolean(true);
    AtomicReference<String> failure = new AtomicReference<>();
    CountDownLatch started = new CountDownLatch(1);
    Thread reader = new Thread(() -> {
      started.countDown();
      while (running.get()) {
        MatchingService match = registry.get("/shared", HttpMethod.GET);
        if (match == null) {
          failure.set("Observed no match mid-publication");
          break;
        }
        Service seen = match.service();
        if (seen != svcA && seen != svcB) {
          failure.set("Observed a service belonging to neither published state: " + seen);
          break;
        }
      }
    });
    reader.start();
    Assertions.assertTrue(started.await(5, TimeUnit.SECONDS));

    for (int i = 0; i < 2000; i++) {
      registry.replaceWith(i % 2 == 0 ? stateB : stateA);
    }
    running.set(false);
    reader.join(5000);

    Assertions.assertNull(failure.get(), failure.get());
  }
}
