package org.pageseeder.berlioz.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.furi.URIPattern;

final class MovedLocationPatternTest {

  // match()
  // ---------------------------------------------------------------------------

  @Test
  void testMatchReturnsTrueForMatchingPath() {
    MovedLocationPattern p = pattern("/old", "/new");
    Assertions.assertTrue(p.match("/old"));
  }

  @Test
  void testMatchReturnsFalseForNonMatchingPath() {
    MovedLocationPattern p = pattern("/old", "/new");
    Assertions.assertFalse(p.match("/other"));
  }

  @Test
  void testMatchWithUriVariablePattern() {
    MovedLocationPattern p = pattern("/{+path}.psml", "/html/{+path}");
    Assertions.assertTrue(p.match("/example.psml"));
    Assertions.assertTrue(p.match("/folder/page.psml"));
    Assertions.assertFalse(p.match("/example.html"));
  }

  @Test
  void testMatchRootPath() {
    MovedLocationPattern p = pattern("/", "/home");
    Assertions.assertTrue(p.match("/"));
    Assertions.assertFalse(p.match("/other"));
  }

  // getTarget()
  // ---------------------------------------------------------------------------

  @Test
  void testGetTargetSimpleMapping() {
    MovedLocationPattern p = pattern("/old", "/new");
    Assertions.assertEquals(p.getTarget("/old"), "/new");
  }

  @Test
  void testGetTargetExpandsUriVariable() {
    MovedLocationPattern p = pattern("/{+path}.psml", "/html/{+path}");
    Assertions.assertEquals(p.getTarget("/example.psml"), "/html/example");
  }

  @Test
  void testGetTargetExpandsNestedPath() {
    MovedLocationPattern p = pattern("/{+path}.psml", "/html/{+path}");
    Assertions.assertEquals(p.getTarget("/folder/page.psml"), "/html/folder/page");
  }

  @Test
  void testGetTargetRootMapping() {
    MovedLocationPattern p = pattern("/", "/home");
    Assertions.assertEquals(p.getTarget("/"), "/home");
  }

  // from() / to()
  // ---------------------------------------------------------------------------

  @Test
  void testFromReturnsSourcePattern() {
    URIPattern from = new URIPattern("/old");
    URIPattern to = new URIPattern("/new");
    MovedLocationPattern p = new MovedLocationPattern(from, to);
    Assertions.assertEquals(from, p.from());
  }

  @Test
  void testToReturnsTargetPattern() {
    URIPattern from = new URIPattern("/old");
    URIPattern to = new URIPattern("/new");
    MovedLocationPattern p = new MovedLocationPattern(from, to);
    Assertions.assertEquals(to, p.to());
  }

  // helpers
  // ---------------------------------------------------------------------------

  private static MovedLocationPattern pattern(String from, String to) {
    return new MovedLocationPattern(new URIPattern(from), new URIPattern(to));
  }

}
