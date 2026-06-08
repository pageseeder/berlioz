package org.pageseeder.berlioz.furi;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class URIResolveResultTest {

  private static final URIPattern PATTERN = new URIPattern("/group/{id}/home");

  @Test
  void testInitialStatus_isUnresolved() {
    URIResolveResult result = new URIResolveResult(PATTERN);
    assertEquals(URIResolveResult.Status.UNRESOLVED, result.getStatus());
  }

  @Test
  void testGetURIPattern_returnsConstructorArg() {
    URIResolveResult result = new URIResolveResult(PATTERN);
    assertSame(PATTERN, result.getURIPattern());
  }

  @Test
  void testNames_emptyInitially() {
    URIResolveResult result = new URIResolveResult(PATTERN);
    assertTrue(result.names().isEmpty());
  }

  @Test
  void testGet_returnsNullForAbsentName() {
    URIResolveResult result = new URIResolveResult(PATTERN);
    assertNull(result.get("missing"));
  }

  @Test
  void testPut_and_get() {
    URIResolveResult result = new URIResolveResult(PATTERN);
    result.put("id", "42");
    assertEquals("42", result.get("id"));
  }

  @Test
  void testNames_reflectsAddedVariables() {
    URIResolveResult result = new URIResolveResult(PATTERN);
    result.put("id", "1");
    result.put("type", "home");
    Set<String> names = result.names();
    assertTrue(names.contains("id"));
    assertTrue(names.contains("type"));
    assertEquals(2, names.size());
  }

  @Test
  void testSetStatus_resolved() {
    URIResolveResult result = new URIResolveResult(PATTERN);
    result.setStatus(URIResolveResult.Status.RESOLVED);
    assertEquals(URIResolveResult.Status.RESOLVED, result.getStatus());
  }

  @Test
  void testSetStatus_error() {
    URIResolveResult result = new URIResolveResult(PATTERN);
    result.setStatus(URIResolveResult.Status.ERROR);
    assertEquals(URIResolveResult.Status.ERROR, result.getStatus());
  }

  @Test
  void testPut_overwritesExistingValue() {
    URIResolveResult result = new URIResolveResult(PATTERN);
    result.put("id", "first");
    result.put("id", "second");
    assertEquals("second", result.get("id"));
  }

  @Test
  void testStatus_allValues() {
    assertNotNull(URIResolveResult.Status.valueOf("UNRESOLVED"));
    assertNotNull(URIResolveResult.Status.valueOf("RESOLVED"));
    assertNotNull(URIResolveResult.Status.valueOf("ERROR"));
  }
}
