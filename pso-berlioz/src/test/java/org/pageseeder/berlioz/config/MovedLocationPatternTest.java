package org.pageseeder.berlioz.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.furi.URIPattern;

public final class MovedLocationPatternTest {

  // match()
  // ---------------------------------------------------------------------------

  @Test
  public void testMatchReturnsTrueForMatchingPath() {
    MovedLocationPattern p = pattern("/old", "/new");
    Assertions.assertTrue(p.match("/old"));
  }

  @Test
  public void testMatchReturnsFalseForNonMatchingPath() {
    MovedLocationPattern p = pattern("/old", "/new");
    Assertions.assertFalse(p.match("/other"));
  }

  @Test
  public void testMatchWithUriVariablePattern() {
    MovedLocationPattern p = pattern("/{+path}.psml", "/html/{+path}");
    Assertions.assertTrue(p.match("/example.psml"));
    Assertions.assertTrue(p.match("/folder/page.psml"));
    Assertions.assertFalse(p.match("/example.html"));
  }

  @Test
  public void testMatchRootPath() {
    MovedLocationPattern p = pattern("/", "/home");
    Assertions.assertTrue(p.match("/"));
    Assertions.assertFalse(p.match("/other"));
  }

  // getTarget()
  // ---------------------------------------------------------------------------

  @Test
  public void testGetTargetSimpleMapping() {
    MovedLocationPattern p = pattern("/old", "/new");
    Assertions.assertEquals(p.getTarget("/old"), "/new");
  }

  @Test
  public void testGetTargetExpandsUriVariable() {
    MovedLocationPattern p = pattern("/{+path}.psml", "/html/{+path}");
    Assertions.assertEquals(p.getTarget("/example.psml"), "/html/example");
  }

  @Test
  public void testGetTargetExpandsNestedPath() {
    MovedLocationPattern p = pattern("/{+path}.psml", "/html/{+path}");
    Assertions.assertEquals(p.getTarget("/folder/page.psml"), "/html/folder/page");
  }

  @Test
  public void testGetTargetRootMapping() {
    MovedLocationPattern p = pattern("/", "/home");
    Assertions.assertEquals(p.getTarget("/"), "/home");
  }

  // from() / to()
  // ---------------------------------------------------------------------------

  @Test
  public void testFromReturnsSourcePattern() {
    URIPattern from = new URIPattern("/old");
    URIPattern to = new URIPattern("/new");
    MovedLocationPattern p = new MovedLocationPattern(from, to);
    Assertions.assertEquals(from, p.from());
  }

  @Test
  public void testToReturnsTargetPattern() {
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
