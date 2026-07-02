package org.pageseeder.berlioz.servlet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BerliozConfigTest {

  // hasControl(req, controlKey) static method

  @Test
  void testHasControl_nullKey_alwaysTrue() {
    HttpServletRequest req = ServletTestSupport.request().build();
    assertTrue(BerliozConfig.hasControl(req, null));
  }

  @Test
  void testHasControl_emptyKey_alwaysTrue() {
    HttpServletRequest req = ServletTestSupport.request().build();
    assertTrue(BerliozConfig.hasControl(req, ""));
  }

  @Test
  void testHasControl_matchingQueryParameter_returnsTrue() {
    HttpServletRequest req = ServletTestSupport.request()
        .parameter("berlioz-control", "secret123")
        .build();
    assertTrue(BerliozConfig.hasControl(req, "secret123"));
  }

  @Test
  void testHasControl_wrongQueryParameter_returnsFalse() {
    // Need getHeaders to return empty enumeration to avoid NPE
    HttpServletRequest req = requestWithHeaders(
        Map.of("berlioz-control", "wrong"), Map.of());
    assertFalse(BerliozConfig.hasControl(req, "secret123"));
  }

  @Test
  void testHasControl_matchingAuthorizationHeader_returnsTrue() {
    HttpServletRequest req = requestWithHeaders(
        Map.of(), Map.of("Authorization", "Berlioz secret123"));
    assertTrue(BerliozConfig.hasControl(req, "secret123"));
  }

  @Test
  void testHasControl_wrongAuthorizationHeader_returnsFalse() {
    HttpServletRequest req = requestWithHeaders(
        Map.of(), Map.of("Authorization", "Berlioz wrongkey"));
    assertFalse(BerliozConfig.hasControl(req, "secret123"));
  }

  @Test
  void testHasControl_noParameterNoHeader_returnsFalse() {
    HttpServletRequest req = requestWithHeaders(Map.of(), Map.of());
    assertFalse(BerliozConfig.hasControl(req, "mykey"));
  }

  @Test
  void testHasControl_partialKeySuffix_returnsFalse() {
    // Ensure "Berlioz xyzSECRET" doesn't match key "SECRET"
    HttpServletRequest req = requestWithHeaders(
        Map.of(), Map.of("Authorization", "Berlioz xyzSECRET"));
    assertFalse(BerliozConfig.hasControl(req, "SECRET"));
  }

  // Listener static methods (just verify they don't throw)

  @Test
  void testGetListener_initiallyNull() {
    // Reset to null before checking
    BerliozConfig.setListener(null);
    assertNull(BerliozConfig.getListener());
  }

  @Test
  void resetETagSeed_generatesAndPersistsSeed(@TempDir Path contextRoot) throws Exception {
    Files.createDirectories(contextRoot.resolve("WEB-INF"));
    BerliozConfig config = BerliozConfig.newConfig(servletConfig(contextRoot));

    config.resetETagSeed();

    assertAll(
        () -> assertNotEquals(0L, config.getETagSeed()),
        () -> assertTrue(Files.exists(contextRoot.resolve("WEB-INF/berlioz.etag")))
    );
  }

  // Helper: build a request proxy that handles getParameter and getHeaders
  private static HttpServletRequest requestWithHeaders(
      Map<String, String> params, Map<String, String> headers) {
    return (HttpServletRequest) Proxy.newProxyInstance(
        HttpServletRequest.class.getClassLoader(),
        new Class<?>[]{HttpServletRequest.class},
        (proxy, m, args) -> {
          switch (m.getName()) {
            case "getParameter": return params.get(args[0]);
            case "getHeaders": {
              String name = (String) args[0];
              String value = headers.get(name);
              return value != null
                  ? Collections.enumeration(Collections.singletonList(value))
                  : Collections.emptyEnumeration();
            }
            case "hashCode": return System.identityHashCode(proxy);
            case "equals":   return proxy == args[0];
            default:         return ServletTestSupport.defaultValue(m.getReturnType());
          }
        });
  }

  private static ServletConfig servletConfig(Path contextRoot) {
    ServletContext context = (ServletContext) Proxy.newProxyInstance(
        ServletContext.class.getClassLoader(),
        new Class<?>[]{ServletContext.class},
        (proxy, m, args) -> {
          if ("getRealPath".equals(m.getName())) return contextRoot.toString();
          return ServletTestSupport.defaultValue(m.getReturnType());
        });
    return (ServletConfig) Proxy.newProxyInstance(
        ServletConfig.class.getClassLoader(),
        new Class<?>[]{ServletConfig.class},
        (proxy, m, args) -> {
          if ("getServletContext".equals(m.getName())) return context;
          if ("getServletName".equals(m.getName())) return "test-config";
          if ("getInitParameter".equals(m.getName())) return null;
          return ServletTestSupport.defaultValue(m.getReturnType());
        });
  }
}
