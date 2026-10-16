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
package org.pageseeder.berlioz.servlet;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers the diagnostic contract documented in README.md: "Unsafe paths are omitted while
 * preserving source/selection information." In particular, a classpath stylesheet reference
 * rejected by {@link org.pageseeder.berlioz.xslt.StylesheetLocation#extractClasspathReference}
 * (e.g. a {@code ..} traversal segment) must still report {@code source="classpath"} — so an
 * operator can tell this was a classpath-style misconfiguration, not a filesystem one — with
 * {@code path} omitted rather than the raw unsafe value leaking into the diagnostic output.
 */
final class DiagnosticConfigurationTest {

  @Test
  void stylesheet_validClasspathReference_reportsSourceAndPath() {
    Map<String, Object> fields = DiagnosticConfiguration.stylesheet("classpath:xslt/{GROUP}.xsl");
    assertEquals("classpath", fields.get("source"));
    assertEquals("classpath:xslt/{GROUP}.xsl", fields.get("path"));
  }

  @Test
  void stylesheet_validResourceReference_reportsCanonicalClasspathPath() {
    Map<String, Object> fields = DiagnosticConfiguration.stylesheet("resource:xslt/main.xsl");
    assertEquals("classpath", fields.get("source"));
    assertEquals("classpath:xslt/main.xsl", fields.get("path"));
  }

  @Test
  void stylesheet_filesystemReference_reportsSourceAndPath() {
    Map<String, Object> fields = DiagnosticConfiguration.stylesheet("xslt/{GROUP}.xsl");
    assertEquals("filesystem", fields.get("source"));
    assertEquals("xslt/{GROUP}.xsl", fields.get("path"));
  }

  @Test
  void stylesheet_rejectedClasspathTraversal_stillReportsClasspathSourceWithoutLeakingPath() {
    Map<String, Object> fields = DiagnosticConfiguration.stylesheet("classpath:a/../../etc/passwd");
    assertEquals("classpath", fields.get("source"), "source must survive rejection, per README's documented contract");
    assertFalse(fields.containsKey("path"), "an unsafe/rejected path must never be reported");
  }

  @Test
  void stylesheet_rejectedResourceTraversal_stillReportsClasspathSourceWithoutLeakingPath() {
    Map<String, Object> fields = DiagnosticConfiguration.stylesheet("resource:a/../../etc/passwd");
    assertEquals("classpath", fields.get("source"));
    assertFalse(fields.containsKey("path"));
  }

  @Test
  void safePath_rejectedClasspathTraversal_returnsNull() {
    assertNull(DiagnosticConfiguration.safePath("classpath:a/../../etc/passwd"));
  }
}
