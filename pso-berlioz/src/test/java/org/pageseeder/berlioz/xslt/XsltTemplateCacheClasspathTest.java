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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.GlobalSettings;

import javax.xml.transform.Templates;
import javax.xml.transform.TransformerException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * Tests for {@link XsltTemplateCache}'s classpath-backed ({@code CLASSPATH}) behavior: cache-mode
 * semantics, ETag computation, and cache invalidation.
 */
final class XsltTemplateCacheClasspathTest {

  private static final String MINIMAL_XSL =
      "<?xml version=\"1.0\"?>"
      + "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">"
      + "<xsl:template match=\"/\"/>"
      + "</xsl:stylesheet>";

  private static final String ALTERNATE_XSL =
      "<?xml version=\"1.0\"?>"
      + "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">"
      + "<xsl:template match=\"/\"><changed/></xsl:template>"
      + "</xsl:stylesheet>";

  @TempDir
  Path tempDir;

  private URLClassLoader classLoader;

  @BeforeEach
  void setUp() throws IOException, ReflectiveOperationException {
    XsltTemplateCache.clearAllCache();
    this.classLoader = new URLClassLoader(new URL[] { this.tempDir.toUri().toURL() },
        XsltTemplateCacheClasspathTest.class.getClassLoader());
  }

  @AfterEach
  void tearDown() throws IOException, ReflectiveOperationException {
    XsltTemplateCache.clearAllCache();
    this.classLoader.close();
    removeOption(BerliozOption.XSLT_CACHE);
  }

  // ---------------------------------------------------------------------------
  // Compilation
  // ---------------------------------------------------------------------------

  @Test
  void getTemplates_compilesFromClasspathResource() throws IOException, TransformerException {
    writeXsl("style.xsl", MINIMAL_XSL);
    XsltTemplateCache cache = new XsltTemplateCache(classpathLocation("style.xsl"), null);
    Templates templates = cache.getTemplates();
    Assertions.assertNotNull(templates);
    Assertions.assertFalse(XsltTemplateCache.isIdentity(templates));
  }

  @Test
  void getTemplates_missingClasspathResourceNoFallback_throwsNotFound() {
    XsltTemplateCache cache = new XsltTemplateCache(classpathLocation("missing.xsl"), null);
    TransformerException ex = Assertions.assertThrows(TransformerException.class, cache::getTemplates);
    Assertions.assertTrue(ex.getCause() instanceof FileNotFoundException, String.valueOf(ex.getCause()));
  }

  @Test
  void getTemplates_missingClasspathResourceWithFallback_usesFallback() throws IOException, TransformerException {
    URL fallback = writeXsl("fallback.xsl", MINIMAL_XSL).toUri().toURL();
    XsltTemplateCache cache = new XsltTemplateCache(classpathLocation("missing.xsl"), fallback);
    Templates templates = cache.getTemplates();
    Assertions.assertNotNull(templates);
  }

  // ---------------------------------------------------------------------------
  // Cache-mode semantics — classpath sources are treated as immutable under auto/manual
  // ---------------------------------------------------------------------------

  @Test
  void getTemplates_manualMode_reusesInstanceEvenAfterContentChanges() throws IOException, TransformerException {
    writeXsl("style.xsl", MINIMAL_XSL);
    Templates first = new XsltTemplateCache(classpathLocation("style.xsl"), null).getTemplates();
    writeXsl("style.xsl", ALTERNATE_XSL);
    Templates second = new XsltTemplateCache(classpathLocation("style.xsl"), null).getTemplates();
    Assertions.assertSame(first, second);
  }

  @Test
  void getTemplates_autoMode_treatsClasspathAsImmutable() throws IOException, TransformerException, ReflectiveOperationException {
    setOption(BerliozOption.XSLT_CACHE, "auto");
    writeXsl("style.xsl", MINIMAL_XSL);
    Templates first = new XsltTemplateCache(classpathLocation("style.xsl"), null).getTemplates();
    writeXsl("style.xsl", ALTERNATE_XSL);
    Templates second = new XsltTemplateCache(classpathLocation("style.xsl"), null).getTemplates();
    Assertions.assertSame(first, second);
  }

  @Test
  void getTemplates_noMode_recompilesEveryTime() throws IOException, TransformerException, ReflectiveOperationException {
    setOption(BerliozOption.XSLT_CACHE, "no");
    writeXsl("style.xsl", MINIMAL_XSL);
    XsltTemplateCache cache = new XsltTemplateCache(classpathLocation("style.xsl"), null);
    Templates first = cache.getTemplates();
    Templates second = cache.getTemplates();
    Assertions.assertNotSame(first, second);
  }

  // ---------------------------------------------------------------------------
  // clearCache() / clearAllCache()
  // ---------------------------------------------------------------------------

  @Test
  void clearCache_invalidatesClasspathEntry() throws IOException, TransformerException {
    writeXsl("style.xsl", MINIMAL_XSL);
    XsltTemplateCache cache = new XsltTemplateCache(classpathLocation("style.xsl"), null);
    Templates first = cache.getTemplates();
    cache.clearCache();
    Templates second = cache.getTemplates();
    Assertions.assertNotSame(first, second);
  }

  @Test
  void clearAllCache_invalidatesClasspathEntries() throws IOException, TransformerException {
    writeXsl("style.xsl", MINIMAL_XSL);
    XsltTemplateCache cache = new XsltTemplateCache(classpathLocation("style.xsl"), null);
    Templates first = cache.getTemplates();
    XsltTemplateCache.clearAllCache();
    Templates second = cache.getTemplates();
    Assertions.assertNotSame(first, second);
  }

  // ---------------------------------------------------------------------------
  // Cache key uniqueness across different classloaders/JARs
  // ---------------------------------------------------------------------------

  @Test
  void cacheKey_distinguishesSameResourceNameFromDifferentOrigins() throws IOException, TransformerException {
    writeXsl("style.xsl", MINIMAL_XSL);
    Path otherDir = Files.createTempDirectory("xslt-cache-other");
    try {
      Files.write(otherDir.resolve("style.xsl"), ALTERNATE_XSL.getBytes(StandardCharsets.UTF_8));
      try (URLClassLoader otherLoader = new URLClassLoader(new URL[] { otherDir.toUri().toURL() }, null)) {
        StylesheetLocation locationA = classpathLocation("style.xsl");
        StylesheetLocation locationB = StylesheetLocation.forClasspath(
            otherLoader.getResource("style.xsl"), "classpath:style.xsl");
        Assertions.assertNotEquals(locationA.cacheKey(), locationB.cacheKey());

        Templates templatesA = new XsltTemplateCache(locationA, null).getTemplates();
        Templates templatesB = new XsltTemplateCache(locationB, null).getTemplates();
        Assertions.assertNotSame(templatesA, templatesB);
      }
    } finally {
      deleteRecursively(otherDir);
    }
  }

  // ---------------------------------------------------------------------------
  // ETag
  // ---------------------------------------------------------------------------

  @Test
  void getEtag_existingClasspathResource_returnsNonNull() throws IOException {
    writeXsl("style.xsl", MINIMAL_XSL);
    XsltTemplateCache cache = new XsltTemplateCache(classpathLocation("style.xsl"), null);
    Assertions.assertNotNull(cache.getEtag());
  }

  @Test
  void getEtag_missingClasspathResourceNoFallback_returnsNull() {
    XsltTemplateCache cache = new XsltTemplateCache(classpathLocation("missing.xsl"), null);
    Assertions.assertNull(cache.getEtag());
  }

  @Test
  void getEtag_changesWhenExplodedResourceContentChanges() throws IOException {
    writeXsl("style.xsl", MINIMAL_XSL);
    String etag1 = new XsltTemplateCache(classpathLocation("style.xsl"), null).getEtag();
    writeXsl("style.xsl", ALTERNATE_XSL);
    String etag2 = new XsltTemplateCache(classpathLocation("style.xsl"), null).getEtag();
    Assertions.assertNotEquals(etag1, etag2);
  }

  @Test
  void getEtag_jarBackedResource_returnsNonNull() throws IOException {
    Path jarFile = buildJar("style.xsl", MINIMAL_XSL);
    try (URLClassLoader jarLoader = new URLClassLoader(new URL[] { jarFile.toUri().toURL() }, null)) {
      URL url = jarLoader.getResource("style.xsl");
      Assertions.assertEquals("jar", url.getProtocol());
      XsltTemplateCache cache = new XsltTemplateCache(StylesheetLocation.forClasspath(url, "classpath:style.xsl"), null);
      Assertions.assertNotNull(cache.getEtag());
    }
  }

  @Test
  void getEtag_jarBackedResource_changesWhenJarRebuilt() throws IOException {
    Path jarFile = this.tempDir.resolve("rebuilt.jar");
    writeJar(jarFile, "style.xsl", MINIMAL_XSL);
    String etag1;
    try (URLClassLoader jarLoader = new URLClassLoader(new URL[] { jarFile.toUri().toURL() }, null)) {
      URL url = jarLoader.getResource("style.xsl");
      etag1 = new XsltTemplateCache(StylesheetLocation.forClasspath(url, "classpath:style.xsl"), null).getEtag();
    }
    writeJar(jarFile, "style.xsl", ALTERNATE_XSL);
    try (URLClassLoader jarLoader = new URLClassLoader(new URL[] { jarFile.toUri().toURL() }, null)) {
      URL url = jarLoader.getResource("style.xsl");
      String etag2 = new XsltTemplateCache(StylesheetLocation.forClasspath(url, "classpath:style.xsl"), null).getEtag();
      Assertions.assertNotEquals(etag1, etag2);
    }
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private StylesheetLocation classpathLocation(String name) {
    URL url = this.classLoader.getResource(name);
    return StylesheetLocation.forClasspath(url, "classpath:" + name);
  }

  private Path writeXsl(String name, String content) throws IOException {
    Path file = this.tempDir.resolve(name);
    Files.write(file, content.getBytes(StandardCharsets.UTF_8));
    return file;
  }

  private Path buildJar(String entryName, String content) throws IOException {
    Path jarFile = Files.createTempFile(this.tempDir, "artifact", ".jar");
    writeJar(jarFile, entryName, content);
    return jarFile;
  }

  private static void writeJar(Path jarFile, String entryName, String content) throws IOException {
    try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(jarFile))) {
      jar.putNextEntry(new JarEntry(entryName));
      jar.write(content.getBytes(StandardCharsets.UTF_8));
      jar.closeEntry();
    }
  }

  private static void deleteRecursively(Path dir) throws IOException {
    if (!Files.exists(dir)) return;
    try (var stream = Files.walk(dir)) {
      stream.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
        try { Files.delete(p); } catch (IOException ignored) { /* best effort cleanup */ }
      });
    }
  }

  private static void setOption(BerliozOption option, String value) throws ReflectiveOperationException {
    AtomicReference<Map<String, String>> ref = settingsRef();
    ref.compareAndSet(null, new HashMap<>());
    ref.get().put(option.property(), value);
  }

  private static void removeOption(BerliozOption option) throws ReflectiveOperationException {
    AtomicReference<Map<String, String>> ref = settingsRef();
    ref.compareAndSet(null, new HashMap<>());
    ref.get().remove(option.property());
  }

  @SuppressWarnings("unchecked")
  private static AtomicReference<Map<String, String>> settingsRef() throws ReflectiveOperationException {
    Field f = GlobalSettings.class.getDeclaredField("SETTINGS");
    f.setAccessible(true);
    return (AtomicReference<Map<String, String>>) f.get(null);
  }

}
