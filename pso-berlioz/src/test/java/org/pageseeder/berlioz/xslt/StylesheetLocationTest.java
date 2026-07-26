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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

/**
 * Tests for {@link StylesheetLocation}.
 */
final class StylesheetLocationTest {

  @TempDir
  Path tempDir;

  // ---------------------------------------------------------------------------
  // identity()
  // ---------------------------------------------------------------------------

  @Test
  void identity_isIdentityKind() {
    StylesheetLocation location = StylesheetLocation.identity();
    Assertions.assertEquals(StylesheetSourceKind.IDENTITY, location.kind());
    Assertions.assertTrue(location.isIdentity());
    Assertions.assertEquals("IDENTITY", location.logicalPath());
    Assertions.assertNull(location.path());
    Assertions.assertNull(location.url());
    Assertions.assertNull(location.toUrl());
    Assertions.assertFalse(location.isMutable());
  }

  // ---------------------------------------------------------------------------
  // forFile(Path)
  // ---------------------------------------------------------------------------

  @Test
  void forFile_isFilesystemKindAndMutable() {
    Path path = this.tempDir.resolve("style.xsl");
    StylesheetLocation location = StylesheetLocation.forFile(path);
    Assertions.assertEquals(StylesheetSourceKind.FILESYSTEM, location.kind());
    Assertions.assertFalse(location.isIdentity());
    Assertions.assertTrue(location.isMutable());
    Assertions.assertEquals(path, location.path());
  }

  @Test
  void forFile_logicalPathStripsUpToWebInf() {
    Path path = this.tempDir.resolve("WEB-INF").resolve("xslt").resolve("html.xsl");
    StylesheetLocation location = StylesheetLocation.forFile(path);
    Assertions.assertEquals("/xslt/html.xsl", location.logicalPath());
  }

  @Test
  void forFile_toUrl_returnsFileUrl() throws MalformedURLException {
    Path path = this.tempDir.resolve("style.xsl");
    StylesheetLocation location = StylesheetLocation.forFile(path);
    Assertions.assertEquals(path.toUri().toURL(), location.toUrl());
  }

  @Test
  void forFile_nullPath_throwsNPE() {
    Assertions.assertThrows(NullPointerException.class, () -> StylesheetLocation.forFile(null));
  }

  // ---------------------------------------------------------------------------
  // forClasspath(URL, String)
  // ---------------------------------------------------------------------------

  @Test
  void forClasspath_resolvedUrl_isClasspathKindAndImmutable() throws MalformedURLException {
    URL url = this.tempDir.resolve("style.xsl").toUri().toURL();
    StylesheetLocation location = StylesheetLocation.forClasspath(url, "classpath:style.xsl");
    Assertions.assertEquals(StylesheetSourceKind.CLASSPATH, location.kind());
    Assertions.assertFalse(location.isMutable());
    Assertions.assertEquals(url, location.url());
    Assertions.assertEquals("classpath:style.xsl", location.logicalPath());
    Assertions.assertEquals(url, location.toUrl());
  }

  @Test
  void forClasspath_unresolvedUrl_cacheKeyFallsBackToLogicalPath() {
    StylesheetLocation location = StylesheetLocation.forClasspath(null, "classpath:missing.xsl");
    Assertions.assertNull(location.url());
    Assertions.assertNull(location.toUrl());
    Assertions.assertEquals("classpath:missing.xsl", location.cacheKey());
  }

  @Test
  void forClasspath_nullLogicalPath_throwsNPE() {
    Assertions.assertThrows(NullPointerException.class, () -> StylesheetLocation.forClasspath(null, null));
  }

  @Test
  void forClasspath_differentUrls_haveDifferentCacheKeys() throws MalformedURLException {
    URL urlA = this.tempDir.resolve("a.xsl").toUri().toURL();
    URL urlB = this.tempDir.resolve("b.xsl").toUri().toURL();
    StylesheetLocation locationA = StylesheetLocation.forClasspath(urlA, "classpath:style.xsl");
    StylesheetLocation locationB = StylesheetLocation.forClasspath(urlB, "classpath:style.xsl");
    // Same logical path (e.g. same resource name in two different JARs), different cache key.
    Assertions.assertEquals(locationA.logicalPath(), locationB.logicalPath());
    Assertions.assertNotEquals(locationA.cacheKey(), locationB.cacheKey());
  }

  // ---------------------------------------------------------------------------
  // extractClasspathReference(String)
  // ---------------------------------------------------------------------------

  @Test
  void extractClasspathReference_classpathPrefixNoLeadingSlash() {
    Assertions.assertEquals("a/b.xsl", StylesheetLocation.extractClasspathReference("classpath:a/b.xsl"));
  }

  @Test
  void extractClasspathReference_classpathPrefixWithLeadingSlash() {
    Assertions.assertEquals("a/b.xsl", StylesheetLocation.extractClasspathReference("classpath:/a/b.xsl"));
  }

  @Test
  void extractClasspathReference_resourcePrefixNoLeadingSlash() {
    Assertions.assertEquals("a/b.xsl", StylesheetLocation.extractClasspathReference("resource:a/b.xsl"));
  }

  @Test
  void extractClasspathReference_resourcePrefixWithLeadingSlash() {
    Assertions.assertEquals("a/b.xsl", StylesheetLocation.extractClasspathReference("resource:/a/b.xsl"));
  }

  @Test
  void extractClasspathReference_plainPath_returnsNull() {
    Assertions.assertNull(StylesheetLocation.extractClasspathReference("/xslt/html/{GROUP}.xsl"));
  }

  @Test
  void extractClasspathReference_identity_returnsNull() {
    Assertions.assertNull(StylesheetLocation.extractClasspathReference("IDENTITY"));
  }

  // ---------------------------------------------------------------------------
  // resolveApplicationClassLoader()
  // ---------------------------------------------------------------------------

  @Test
  void resolveApplicationClassLoader_usesContextClassLoaderWhenSet() {
    ClassLoader previous = Thread.currentThread().getContextClassLoader();
    ClassLoader marker = new ClassLoader(previous) { };
    Thread.currentThread().setContextClassLoader(marker);
    try {
      Assertions.assertSame(marker, StylesheetLocation.resolveApplicationClassLoader());
    } finally {
      Thread.currentThread().setContextClassLoader(previous);
    }
  }

  @Test
  void resolveApplicationClassLoader_fallsBackWhenNoContextClassLoader() {
    ClassLoader previous = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(null);
    try {
      Assertions.assertNotNull(StylesheetLocation.resolveApplicationClassLoader());
    } finally {
      Thread.currentThread().setContextClassLoader(previous);
    }
  }

}
