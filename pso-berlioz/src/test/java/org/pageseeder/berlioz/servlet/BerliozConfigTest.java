package org.pageseeder.berlioz.servlet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BerliozConfigTest {

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

  @Test
  void contentType_usesNamedCharsetParameter(@TempDir Path contextRoot) throws Exception {
    Files.createDirectories(contextRoot.resolve("WEB-INF"));
    ServletConfig servletConfig = servletConfig(contextRoot, Map.of(
        "content-type", "text/plain;note=\"charset=ISO-8859-1\"; charset = \"UTF-16\""));

    BerliozConfig config = BerliozConfig.newConfig(servletConfig);

    assertAll(
        () -> assertEquals("text/plain", config.getMediaType()),
        () -> assertEquals(StandardCharsets.UTF_16, config.getCharset()),
        () -> assertEquals("text/plain;charset=UTF-16", config.getContentType())
    );
  }

  @Test
  void contentType_charsetTextInAnotherParameterDefaultsToUtf8(@TempDir Path contextRoot)
      throws Exception {
    Files.createDirectories(contextRoot.resolve("WEB-INF"));
    ServletConfig servletConfig = servletConfig(contextRoot,
        Map.of("content-type", "text/plain;note=\"charset=ISO-8859-1\""));

    BerliozConfig config = BerliozConfig.newConfig(servletConfig);

    assertEquals(StandardCharsets.UTF_8, config.getCharset());
  }

  private static ServletConfig servletConfig(Path contextRoot) {
    return servletConfig(contextRoot, Map.of());
  }

  private static ServletConfig servletConfig(Path contextRoot, Map<String, String> initParameters) {
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
          if ("getInitParameter".equals(m.getName())) return initParameters.get(args[0]);
          return ServletTestSupport.defaultValue(m.getReturnType());
        });
  }
}
