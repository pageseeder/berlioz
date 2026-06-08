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

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Tests for the default methods on the {@link Request} interface.
 *
 * <p>The abstract methods are trivially implemented by a minimal stub. The focus is on
 * verifying that {@link Request#parameter(String)} and {@link Request#parameter(ParameterSpec)}
 * delegate correctly to {@link Request#getParameter(String)} and produce the expected typed
 * values.</p>
 */
final class RequestTest {

  /**
   * Minimal stub that backs {@code getParameter} from a fixed parameter map.
   */
  private static Request stub(Map<String, String> params) {
    return new Request() {
      @Override public String getBerliozPath() { return "/test"; }

      @Override public String getParameter(String name) {
        String v = params.get(name);
        return (v != null && !v.isEmpty()) ? v : null;
      }

      @Override public String getParameter(String name, String def) {
        String v = getParameter(name);
        return v != null ? v : def;
      }

      @Override public Collection<String> parameterNames() { return params.keySet(); }

      @Override public List<String> parameterValues(String name) {
        String v = params.get(name);
        return v != null ? List.of(v) : List.of();
      }

      @Override public Object getAttribute(String name) { return null; }
      @Override public void setAttribute(String name, Object o) {}
      @Override public List<Cookie> cookies() { return List.of(); }
      @Override public HttpSession getSession() { return null; }
      @Override public Environment getEnvironment() { return null; }
      @Override public Location getLocation() { return null; }
    };
  }

  // ---------------------------------------------------------------------------
  // parameter(String name) — default method returning ParameterBuilder
  // ---------------------------------------------------------------------------

  @Test
  void parameter_byName_returnsNonNullBuilder() {
    Request req = stub(Map.of());
    Assertions.assertNotNull(req.parameter("anything"));
  }

  @Test
  void parameter_byName_presentValue_resolvesCorrectly() {
    Request req = stub(Map.of("page", "7"));
    int page = req.parameter("page").asInt().defaultValue(1);
    Assertions.assertEquals(7, page);
  }

  @Test
  void parameter_byName_absentValue_usesDefault() {
    Request req = stub(Map.of());
    int page = req.parameter("page").asInt().defaultValue(42);
    Assertions.assertEquals(42, page);
  }

  @Test
  void parameter_byName_presentStringValue_resolvesAsString() {
    Request req = stub(Map.of("sort", "name"));
    String sort = req.parameter("sort").asString().defaultValue("date");
    Assertions.assertEquals("name", sort);
  }

  @Test
  void parameter_byName_absentStringValue_usesDefault() {
    Request req = stub(Map.of());
    String sort = req.parameter("sort").asString().defaultValue("date");
    Assertions.assertEquals("date", sort);
  }

  // ---------------------------------------------------------------------------
  // parameter(ParameterSpec<T>) — default method using spec resolver
  // ---------------------------------------------------------------------------

  private static final ParameterSpec<Integer> PAGE =
      ParameterSpec.of("page", b -> b.asInt().clamp(1, 9999).defaultValue(1));

  private static final ParameterSpec<String> SORT =
      ParameterSpec.of("sort", b -> b.oneOf("name", "date", "title").defaultValue("date"));

  @Test
  void parameter_bySpec_presentValue_resolvesCorrectly() {
    Request req = stub(Map.of("page", "5"));
    int page = req.parameter(PAGE);
    Assertions.assertEquals(5, page);
  }

  @Test
  void parameter_bySpec_absentValue_usesSpecDefault() {
    Request req = stub(Map.of());
    int page = req.parameter(PAGE);
    Assertions.assertEquals(1, page);
  }

  @Test
  void parameter_bySpec_outOfRange_clampsToMax() {
    Request req = stub(Map.of("page", "99999"));
    int page = req.parameter(PAGE);
    Assertions.assertEquals(9999, page);
  }

  @Test
  void parameter_bySpec_usesSpecName() {
    // Only "sort" is present; a spec named "sort" should resolve it, not "other"
    Request req = stub(Map.of("sort", "title"));
    String sort = req.parameter(SORT);
    Assertions.assertEquals("title", sort);
  }

  @Test
  void parameter_bySpec_absentStringValue_usesSpecDefault() {
    Request req = stub(Map.of());
    String sort = req.parameter(SORT);
    Assertions.assertEquals("date", sort);
  }

}
