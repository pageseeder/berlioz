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

import javax.xml.transform.Templates;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests for {@link XsltTemplateCache}.
 *
 * <p>Each test clears the shared static cache before and after execution to prevent
 * cross-test pollution.
 */
final class XsltTemplateCacheTest {

  private static final String MINIMAL_XSL =
      "<?xml version=\"1.0\"?>"
      + "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">"
      + "<xsl:template match=\"/\"/>"
      + "</xsl:stylesheet>";

  @TempDir
  Path tempDir;

  @BeforeEach
  void clearCache() {
    XsltTemplateCache.clearAllCache();
  }

  @AfterEach
  void clearCacheAfter() {
    XsltTemplateCache.clearAllCache();
  }

  // ---------------------------------------------------------------------------
  // Constructor
  // ---------------------------------------------------------------------------

  @Test
  void constructor_nullPath_throwsNPE() {
    Assertions.assertThrows(NullPointerException.class, () -> new XsltTemplateCache(null, null));
  }

  // ---------------------------------------------------------------------------
  // templatesPath()
  // ---------------------------------------------------------------------------

  @Test
  void templatesPath_returnsGivenPath() {
    Path p = tempDir.resolve("style.xsl");
    XsltTemplateCache cache = new XsltTemplateCache(p, null);
    Assertions.assertEquals(p, cache.templatesPath());
  }

  // ---------------------------------------------------------------------------
  // getEtag()
  // ---------------------------------------------------------------------------

  @Test
  void getEtag_missingFileNoFallback_returnsNull() {
    Path absent = tempDir.resolve("nonexistent.xsl");
    XsltTemplateCache cache = new XsltTemplateCache(absent, null);
    Assertions.assertNull(cache.getEtag());
  }

  @Test
  void getEtag_missingFileWithFallback_returnsNonNull() throws IOException {
    Path absent = tempDir.resolve("nonexistent.xsl");
    URL fallback = writeXsl("fallback.xsl").toUri().toURL();
    XsltTemplateCache cache = new XsltTemplateCache(absent, fallback);
    Assertions.assertNotNull(cache.getEtag());
  }

  @Test
  void getEtag_existingFile_returnsNonNull() throws IOException {
    Path xsl = writeXsl("style.xsl");
    XsltTemplateCache cache = new XsltTemplateCache(xsl, null);
    Assertions.assertNotNull(cache.getEtag());
  }

  // ---------------------------------------------------------------------------
  // getTemplates() — compilation and caching (default MANUAL mode)
  // ---------------------------------------------------------------------------

  @Test
  void getTemplates_compilesFromExistingFile() throws IOException, TransformerException {
    Path xsl = writeXsl("style.xsl");
    XsltTemplateCache cache = new XsltTemplateCache(xsl, null);
    Templates templates = cache.getTemplates();
    Assertions.assertNotNull(templates);
    Assertions.assertFalse(XsltTemplateCache.isIdentity(templates));
  }

  @Test
  void getTemplates_returnsSameInstanceOnSecondCall() throws IOException, TransformerException {
    Path xsl = writeXsl("style.xsl");
    XsltTemplateCache cache = new XsltTemplateCache(xsl, null);
    Templates first = cache.getTemplates();
    Templates second = cache.getTemplates();
    Assertions.assertSame(first, second);
  }

  @Test
  void getTemplates_usesFallbackWhenFileAbsent() throws IOException, TransformerException {
    Path absent = tempDir.resolve("nonexistent.xsl");
    URL fallback = writeXsl("fallback.xsl").toUri().toURL();
    XsltTemplateCache cache = new XsltTemplateCache(absent, fallback);
    Templates templates = cache.getTemplates();
    Assertions.assertNotNull(templates);
  }

  @Test
  void getTemplates_missingFileNoFallback_throwsTransformerException() {
    Path absent = tempDir.resolve("nonexistent.xsl");
    XsltTemplateCache cache = new XsltTemplateCache(absent, null);
    Assertions.assertThrows(TransformerException.class, cache::getTemplates);
  }

  // ---------------------------------------------------------------------------
  // clearCache()
  // ---------------------------------------------------------------------------

  @Test
  void clearCache_causesRecompileOnNextCall() throws IOException, TransformerException {
    Path xsl = writeXsl("style.xsl");
    XsltTemplateCache cache = new XsltTemplateCache(xsl, null);
    Templates first = cache.getTemplates();
    cache.clearCache();
    Templates second = cache.getTemplates();
    Assertions.assertNotSame(first, second);
  }

  @Test
  void clearCache_doesNotAffectOtherCacheEntries() throws IOException, TransformerException {
    Path xsl1 = writeXsl("a.xsl");
    Path xsl2 = writeXsl("b.xsl");
    XsltTemplateCache cache1 = new XsltTemplateCache(xsl1, null);
    XsltTemplateCache cache2 = new XsltTemplateCache(xsl2, null);
    Templates second1 = cache2.getTemplates();
    cache1.clearCache();
    // cache2 entry is intact — second call returns same instance
    Assertions.assertSame(second1, cache2.getTemplates());
  }

  // ---------------------------------------------------------------------------
  // clearAllCache()
  // ---------------------------------------------------------------------------

  @Test
  void clearAllCache_causesRecompileForAllEntries() throws IOException, TransformerException {
    Path xsl1 = writeXsl("a.xsl");
    Path xsl2 = writeXsl("b.xsl");
    XsltTemplateCache cache1 = new XsltTemplateCache(xsl1, null);
    XsltTemplateCache cache2 = new XsltTemplateCache(xsl2, null);
    Templates first1 = cache1.getTemplates();
    Templates first2 = cache2.getTemplates();
    XsltTemplateCache.clearAllCache();
    Assertions.assertNotSame(first1, cache1.getTemplates());
    Assertions.assertNotSame(first2, cache2.getTemplates());
  }

  // ---------------------------------------------------------------------------
  // compile(URL) — static
  // ---------------------------------------------------------------------------

  @Test
  void compile_null_returnsIdentityTemplates() {
    Templates t = XsltTemplateCache.compile((URL) null);
    Assertions.assertNotNull(t);
    Assertions.assertTrue(XsltTemplateCache.isIdentity(t));
  }

  @Test
  void compile_validUrl_returnsNonIdentityTemplates() throws IOException {
    URL url = writeXsl("style.xsl").toUri().toURL();
    Templates t = XsltTemplateCache.compile(url);
    Assertions.assertNotNull(t);
    Assertions.assertFalse(XsltTemplateCache.isIdentity(t));
  }

  // ---------------------------------------------------------------------------
  // isIdentity(Templates) — static
  // ---------------------------------------------------------------------------

  @Test
  void isIdentity_identityTemplates_returnsTrue() {
    Templates identity = XsltTemplateCache.compile((URL) null);
    Assertions.assertTrue(XsltTemplateCache.isIdentity(identity));
  }

  @Test
  void isIdentity_compiledTemplates_returnsFalse() throws IOException {
    URL url = writeXsl("style.xsl").toUri().toURL();
    Templates compiled = XsltTemplateCache.compile(url);
    Assertions.assertFalse(XsltTemplateCache.isIdentity(compiled));
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private Path writeXsl(String name) throws IOException {
    Path xsl = tempDir.resolve(name);
    Files.write(xsl, MINIMAL_XSL.getBytes(StandardCharsets.UTF_8));
    return xsl;
  }

}
