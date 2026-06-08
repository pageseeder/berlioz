package org.pageseeder.berlioz.furi;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class URIParametersTest {

  // --- Constructors ---

  @Test
  void testDefaultConstructor_isEmpty() {
    URIParameters params = new URIParameters();
    assertTrue(params.names().isEmpty());
  }

  @Test
  void testMapConstructor_copiesEntries() {
    Map<String, String[]> map = new HashMap<>();
    map.put("foo", new String[]{"bar"});
    URIParameters params = new URIParameters(map);
    assertEquals("bar", params.getValue("foo"));
  }

  @Test
  void testMapConstructor_defensiveCopy() {
    Map<String, String[]> map = new HashMap<>();
    map.put("key", new String[]{"original"});
    URIParameters params = new URIParameters(map);
    map.put("key", new String[]{"modified"});
    assertEquals("original", params.getValue("key"));
  }

  // --- set(name, value) ---

  @Test
  void testSet_singleValue() {
    URIParameters params = new URIParameters();
    params.set("name", "Alice");
    assertEquals("Alice", params.getValue("name"));
  }

  @Test
  void testSet_nullValueIgnored() {
    URIParameters params = new URIParameters();
    params.set("name", (String) null);
    assertFalse(params.exists("name"));
  }

  @Test
  void testSet_overwritesPreviousValue() {
    URIParameters params = new URIParameters();
    params.set("x", "first");
    params.set("x", "second");
    assertEquals("second", params.getValue("x"));
  }

  // --- set(name, values[]) ---

  @Test
  void testSet_multipleValues() {
    URIParameters params = new URIParameters();
    params.set("colors", new String[]{"red", "blue"});
    assertArrayEquals(new String[]{"red", "blue"}, params.getValues("colors"));
  }

  @Test
  void testSet_nullArrayIgnored() {
    URIParameters params = new URIParameters();
    params.set("name", (String[]) null);
    assertFalse(params.exists("name"));
  }

  // --- getValue ---

  @Test
  void testGetValue_returnsFirstValue() {
    URIParameters params = new URIParameters();
    params.set("k", new String[]{"first", "second"});
    assertEquals("first", params.getValue("k"));
  }

  @Test
  void testGetValue_returnsNullForAbsent() {
    URIParameters params = new URIParameters();
    assertNull(params.getValue("missing"));
  }

  @Test
  void testGetValue_returnsNullForEmptyArray() {
    Map<String, String[]> map = new HashMap<>();
    map.put("empty", new String[]{});
    URIParameters params = new URIParameters(map);
    assertNull(params.getValue("empty"));
  }

  // --- getValues ---

  @Test
  void testGetValues_returnsAllValues() {
    URIParameters params = new URIParameters();
    params.set("tags", new String[]{"a", "b", "c"});
    assertArrayEquals(new String[]{"a", "b", "c"}, params.getValues("tags"));
  }

  @Test
  void testGetValues_returnsNullForAbsent() {
    URIParameters params = new URIParameters();
    assertNull(params.getValues("missing"));
  }

  // --- exists ---

  @Test
  void testExists_trueAfterSet() {
    URIParameters params = new URIParameters();
    params.set("p", "v");
    assertTrue(params.exists("p"));
  }

  @Test
  void testExists_falseForAbsent() {
    URIParameters params = new URIParameters();
    assertFalse(params.exists("p"));
  }

  @Test
  void testExists_trueEvenForEmptyArray() {
    Map<String, String[]> map = new HashMap<>();
    map.put("p", new String[]{});
    URIParameters params = new URIParameters(map);
    assertTrue(params.exists("p"));
  }

  // --- hasValue ---

  @Test
  void testHasValue_trueForNonEmptyValue() {
    URIParameters params = new URIParameters();
    params.set("p", "value");
    assertTrue(params.hasValue("p"));
  }

  @Test
  void testHasValue_falseForAbsent() {
    URIParameters params = new URIParameters();
    assertFalse(params.hasValue("p"));
  }

  @Test
  void testHasValue_falseForEmptyString() {
    URIParameters params = new URIParameters();
    params.set("p", "");
    assertFalse(params.hasValue("p"));
  }

  @Test
  void testHasValue_falseForEmptyArray() {
    Map<String, String[]> map = new HashMap<>();
    map.put("p", new String[]{});
    URIParameters params = new URIParameters(map);
    assertFalse(params.hasValue("p"));
  }

  // --- names ---

  @Test
  void testNames_unmodifiable() {
    URIParameters params = new URIParameters();
    params.set("x", "1");
    Set<String> names = params.names();
    assertThrows(UnsupportedOperationException.class, () -> names.add("y"));
  }

  @Test
  void testNames_containsAllSetKeys() {
    URIParameters params = new URIParameters();
    params.set("a", "1");
    params.set("b", "2");
    assertTrue(params.names().contains("a"));
    assertTrue(params.names().contains("b"));
    assertEquals(2, params.names().size());
  }
}
