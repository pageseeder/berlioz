package org.pageseeder.berlioz.servlet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;

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
