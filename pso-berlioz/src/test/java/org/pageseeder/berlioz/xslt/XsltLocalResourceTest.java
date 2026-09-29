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
package org.pageseeder.berlioz.xslt;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import javax.xml.transform.TransformerException;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/** Regression coverage for network URLs hidden behind classpath and JAR references. */
final class XsltLocalResourceTest {

  @TempDir Path directory;

  @AfterEach
  void clearCache() {
    XsltTemplateCache.clearAllCache();
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "http://example.invalid/style.xsl", "https://example.invalid/style.xsl",
      "jar:http://example.invalid/styles.jar!/style.xsl",
      "jar:https://example.invalid/styles.jar!/style.xsl",
      "jar:ftp://example.invalid/styles.jar!/style.xsl",
      "file://example.invalid/style.xsl", "jar:file://example.invalid/styles.jar!/style.xsl"
  })
  void rejectsRemoteResourcesBeforeAnyConnection(String location) throws Exception {
    AtomicInteger connections = new AtomicInteger();
    URL url = new URL(null, location, new URLStreamHandler() {
      @Override
      protected URLConnection openConnection(URL ignored) throws IOException {
        connections.incrementAndGet();
        throw new IOException("Unexpected resource access");
      }
    });
    ClassLoader loader = new ClassLoader(getClass().getClassLoader()) {
      @Override public URL getResource(String name) { return url; }
    };
    SecureXsltUriResolver resolver = new SecureXsltUriResolver(loader);
    assertThrows(TransformerException.class, () -> resolver.resolve("classpath:style.xsl", null));
    assertThrows(TransformerException.class, () -> resolver.resolve("resource:style.xsl", null));
    assertThrows(TransformerException.class, () -> resolver.resolve(location, null));
    assertThrows(TransformerException.class, () -> resolver.resolve("other.xsl", location));

    // Construction computes the ETag and must not read even JAR metadata remotely.
    XsltTemplateCache primary = new XsltTemplateCache(
        StylesheetLocation.forClasspath(url, "classpath:style.xsl"), null);
    assertNull(primary.getEtag());
    assertThrows(TransformerException.class, primary::getTemplates);
    assertTrue(XsltTemplateCache.isIdentity(XsltTemplateCache.compile(url)));
    XsltTemplateCache fileFallback = new XsltTemplateCache(directory.resolve("missing.xsl"), url);
    assertThrows(TransformerException.class, fileFallback::getTemplates);
    XsltTemplateCache classpathFallback = new XsltTemplateCache(
        StylesheetLocation.forClasspath(null, "classpath:missing.xsl"), url);
    assertThrows(TransformerException.class, classpathFallback::getTemplates);
    assertEquals(0, connections.get());
  }

  @Test
  void permitsLocalFilesAndLocalJarResources() throws Exception {
    URL file = directory.resolve("style.xsl").toUri().toURL();
    URL jar = new URL("jar:" + directory.resolve("styles.jar").toUri() + "!/style.xsl");
    SecureXsltUriResolver resolver = new SecureXsltUriResolver(getClass().getClassLoader());
    assertDoesNotThrow(() -> resolver.resolve(file.toString(), null));
    assertDoesNotThrow(() -> resolver.resolve(jar.toString(), null));
  }

  @Test
  void remoteJarImportIsRejectedWithoutHttpRequest() throws Exception {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (JarOutputStream jar = new JarOutputStream(bytes)) {
      jar.putNextEntry(new JarEntry("part.xsl"));
      jar.write(stylesheet("<xsl:template match='/'><remote/></xsl:template>").getBytes(StandardCharsets.UTF_8));
      jar.closeEntry();
    }
    AtomicInteger requests = new AtomicInteger();
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/styles.jar", exchange -> {
      requests.incrementAndGet();
      byte[] payload = bytes.toByteArray();
      exchange.sendResponseHeaders(200, payload.length);
      try (var out = exchange.getResponseBody()) { out.write(payload); }
    });
    ClassLoader previous = Thread.currentThread().getContextClassLoader();
    server.start();
    try {
      URL remote = new URL("jar:http://127.0.0.1:" + server.getAddress().getPort() + "/styles.jar!/part.xsl");
      Thread.currentThread().setContextClassLoader(new ClassLoader(previous) {
        @Override public URL getResource(String name) {
          return "part.xsl".equals(name) ? remote : super.getResource(name);
        }
      });
      Path primary = Files.writeString(directory.resolve("main.xsl"),
          stylesheet("<xsl:import href='classpath:part.xsl'/>"));
      assertThrows(TransformerException.class, () -> new XsltTemplateCache(primary, null).getTemplates());
      assertEquals(0, requests.get());
    } finally {
      Thread.currentThread().setContextClassLoader(previous);
      server.stop(0);
    }
  }

  private static String stylesheet(String body) {
    return "<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>"
        + body + "</xsl:stylesheet>";
  }
}
