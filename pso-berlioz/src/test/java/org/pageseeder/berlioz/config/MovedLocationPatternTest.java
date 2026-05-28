package org.pageseeder.berlioz.config;

import org.junit.Assert;
import org.junit.Test;
import org.pageseeder.berlioz.furi.URIPattern;

public final class MovedLocationPatternTest {

  // match()
  // ---------------------------------------------------------------------------

  @Test
  public void testMatchReturnsTrueForMatchingPath() {
    MovedLocationPattern p = pattern("/old", "/new");
    Assert.assertTrue(p.match("/old"));
  }

  @Test
  public void testMatchReturnsFalseForNonMatchingPath() {
    MovedLocationPattern p = pattern("/old", "/new");
    Assert.assertFalse(p.match("/other"));
  }

  @Test
  public void testMatchWithUriVariablePattern() {
    MovedLocationPattern p = pattern("/{+path}.psml", "/html/{+path}");
    Assert.assertTrue(p.match("/example.psml"));
    Assert.assertTrue(p.match("/folder/page.psml"));
    Assert.assertFalse(p.match("/example.html"));
  }

  @Test
  public void testMatchRootPath() {
    MovedLocationPattern p = pattern("/", "/home");
    Assert.assertTrue(p.match("/"));
    Assert.assertFalse(p.match("/other"));
  }

  // getTarget()
  // ---------------------------------------------------------------------------

  @Test
  public void testGetTargetSimpleMapping() {
    MovedLocationPattern p = pattern("/old", "/new");
    Assert.assertEquals("/new", p.getTarget("/old"));
  }

  @Test
  public void testGetTargetExpandsUriVariable() {
    MovedLocationPattern p = pattern("/{+path}.psml", "/html/{+path}");
    Assert.assertEquals("/html/example", p.getTarget("/example.psml"));
  }

  @Test
  public void testGetTargetExpandsNestedPath() {
    MovedLocationPattern p = pattern("/{+path}.psml", "/html/{+path}");
    Assert.assertEquals("/html/folder/page", p.getTarget("/folder/page.psml"));
  }

  @Test
  public void testGetTargetRootMapping() {
    MovedLocationPattern p = pattern("/", "/home");
    Assert.assertEquals("/home", p.getTarget("/"));
  }

  // from() / to()
  // ---------------------------------------------------------------------------

  @Test
  public void testFromReturnsSourcePattern() {
    URIPattern from = new URIPattern("/old");
    URIPattern to = new URIPattern("/new");
    MovedLocationPattern p = new MovedLocationPattern(from, to);
    Assert.assertEquals(from, p.from());
  }

  @Test
  public void testToReturnsTargetPattern() {
    URIPattern from = new URIPattern("/old");
    URIPattern to = new URIPattern("/new");
    MovedLocationPattern p = new MovedLocationPattern(from, to);
    Assert.assertEquals(to, p.to());
  }

  // helpers
  // ---------------------------------------------------------------------------

  private static MovedLocationPattern pattern(String from, String to) {
    return new MovedLocationPattern(new URIPattern(from), new URIPattern(to));
  }

}
