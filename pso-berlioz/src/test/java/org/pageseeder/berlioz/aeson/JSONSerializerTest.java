package org.pageseeder.berlioz.aeson;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

class JSONSerializerTest {

  // ---------------------------------------------------------------------------
  // Helper
  // ---------------------------------------------------------------------------

  private static final String JSON_NS = "http://pageseeder.org/JSON";

  /** Parse an XML string through JSONSerializer and return the JSON output. */
  private static String toJSON(String xml) throws Exception {
    StringWriter out = new StringWriter();
    SAXParserFactory spf = SAXParserFactory.newInstance();
    spf.setNamespaceAware(true);
    SAXParser parser = spf.newSAXParser();
    parser.parse(new InputSource(new StringReader(xml)), new JSONSerializer(out));
    return out.toString();
  }

  // ---------------------------------------------------------------------------
  // Smoke test (original)
  // ---------------------------------------------------------------------------

  @Test
  void testParse() throws Exception {
    String xml = "<test description=\"Test\">"
        + "<json:array json:name=\"items\" xmlns:json=\"" + JSON_NS + "\">"
        + "<item title=\"A\"/>"
        + "<item title=\"B\"/>"
        + "</json:array>"
        + "</test>";
    String json = toJSON(xml);
    Assertions.assertTrue(json.contains("\"items\""));
    Assertions.assertTrue(json.contains("\"title\""));
  }

  // ---------------------------------------------------------------------------
  // asNumber — integer and decimal values
  // ---------------------------------------------------------------------------

  @Test
  void testNumberLong() throws Exception {
    String xml = "<root xmlns:json=\"" + JSON_NS + "\" json:number=\"count\">"
        + "<item count=\"42\"/></root>";
    String json = toJSON(xml);
    Assertions.assertTrue(json.contains("42"), "count should appear as a number");
    Assertions.assertFalse(json.contains("\"42\""), "count should not be a JSON string");
  }

  @Test
  void testNumberNegativeLong() throws Exception {
    String xml = "<root xmlns:json=\"" + JSON_NS + "\" json:number=\"offset\">"
        + "<item offset=\"-7\"/></root>";
    String json = toJSON(xml);
    Assertions.assertTrue(json.contains("-7"));
    Assertions.assertFalse(json.contains("\"-7\""), "negative long should not be a JSON string");
  }

  @Test
  void testNumberDouble() throws Exception {
    String xml = "<root xmlns:json=\"" + JSON_NS + "\" json:number=\"rate\">"
        + "<item rate=\"3.14\"/></root>";
    String json = toJSON(xml);
    Assertions.assertTrue(json.contains("3.14"));
    Assertions.assertFalse(json.contains("\"3.14\""), "decimal should not be a JSON string");
  }

  // ---------------------------------------------------------------------------
  // asNumber — scientific notation (regression: used to throw NFE → string fallback)
  // ---------------------------------------------------------------------------

  @Test
  void testNumberScientificLowerE() throws Exception {
    // "1e10" has no '.'; the old code called Long.parseLong which threw NFE
    String xml = "<root xmlns:json=\"" + JSON_NS + "\" json:number=\"amount\">"
        + "<item amount=\"1e10\"/></root>";
    String json = toJSON(xml);
    Assertions.assertFalse(json.contains("\"1e10\""), "1e10 must not be serialized as a JSON string");
  }

  @Test
  void testNumberScientificUpperE() throws Exception {
    String xml = "<root xmlns:json=\"" + JSON_NS + "\" json:number=\"amount\">"
        + "<item amount=\"2E5\"/></root>";
    String json = toJSON(xml);
    Assertions.assertFalse(json.contains("\"2E5\""), "2E5 must not be serialized as a JSON string");
  }

  @Test
  void testNumberScientificWithDecimalAndExponent() throws Exception {
    String xml = "<root xmlns:json=\"" + JSON_NS + "\" json:number=\"rate\">"
        + "<item rate=\"1.5e3\"/></root>";
    String json = toJSON(xml);
    Assertions.assertFalse(json.contains("\"1.5e3\""), "1.5e3 must not be serialized as a JSON string");
  }

  @Test
  void testNumberNegativeExponent() throws Exception {
    String xml = "<root xmlns:json=\"" + JSON_NS + "\" json:number=\"prob\">"
        + "<item prob=\"5E-3\"/></root>";
    String json = toJSON(xml);
    Assertions.assertFalse(json.contains("\"5E-3\""), "5E-3 must not be serialized as a JSON string");
  }

  // ---------------------------------------------------------------------------
  // asNumber — invalid value falls back to string (existing contract unchanged)
  // ---------------------------------------------------------------------------

  @Test
  void testNumberInvalidFallsBackToString() throws Exception {
    String xml = "<root xmlns:json=\"" + JSON_NS + "\" json:number=\"value\">"
        + "<item value=\"not-a-number\"/></root>";
    String json = toJSON(xml);
    Assertions.assertTrue(json.contains("\"not-a-number\""), "Invalid number should fall back to a JSON string");
  }

  // ---------------------------------------------------------------------------
  // asBoolean
  // ---------------------------------------------------------------------------

  @Test
  void testBooleanTrue() throws Exception {
    String xml = "<root xmlns:json=\"" + JSON_NS + "\" json:boolean=\"active\">"
        + "<item active=\"true\"/></root>";
    String json = toJSON(xml);
    Assertions.assertTrue(json.contains("true"));
    Assertions.assertFalse(json.contains("\"true\""), "true should not be a JSON string");
  }

  @Test
  void testBooleanFalse() throws Exception {
    String xml = "<root xmlns:json=\"" + JSON_NS + "\" json:boolean=\"active\">"
        + "<item active=\"false\"/></root>";
    String json = toJSON(xml);
    Assertions.assertTrue(json.contains("false"));
    Assertions.assertFalse(json.contains("\"false\""), "false should not be a JSON string");
  }

  @Test
  void testBooleanInvalidFallsBackToString() throws Exception {
    String xml = "<root xmlns:json=\"" + JSON_NS + "\" json:boolean=\"active\">"
        + "<item active=\"yes\"/></root>";
    String json = toJSON(xml);
    Assertions.assertTrue(json.contains("\"yes\""), "Invalid boolean should fall back to a JSON string");
  }

  // ---------------------------------------------------------------------------
  // json:null attribute
  // ---------------------------------------------------------------------------

  @Test
  void testNullProperty() throws Exception {
    String xml = "<root xmlns:json=\"" + JSON_NS + "\" json:null=\"missing\">"
        + "<item missing=\"whatever\"/></root>";
    String json = toJSON(xml);
    Assertions.assertTrue(json.contains("null"), "null-typed property should emit JSON null");
    Assertions.assertFalse(json.contains("\"whatever\""), "null-typed property should not keep the attribute value");
  }

  // ---------------------------------------------------------------------------
  // json:null element — in object, array, and root contexts
  // ---------------------------------------------------------------------------

  @Test
  void testJsonNullElementInObjectContext() throws Exception {
    String xml = "<root xmlns:json=\"" + JSON_NS + "\">"
        + "<json:null json:name=\"missing\"/>"
        + "</root>";
    String json = toJSON(xml);
    Assertions.assertTrue(json.contains("\"missing\":null"), "named null element in object context must emit \"missing\":null");
  }

  @Test
  void testJsonNullElementInArrayContext() throws Exception {
    String xml = "<root xmlns:json=\"" + JSON_NS + "\">"
        + "<json:array json:name=\"items\">"
        + "<json:null/>"
        + "</json:array>"
        + "</root>";
    String json = toJSON(xml);
    Assertions.assertTrue(json.contains("null"), "null element in array context must emit null value");
    Assertions.assertTrue(json.contains("\"items\""), "array must be present");
  }

  @Test
  void testJsonNullElementInRootContextEmitsEmptyObject() throws Exception {
    // json:null at root is illegal; the serializer substitutes an empty object
    String xml = "<json:null xmlns:json=\"" + JSON_NS + "\"/>";
    String json = toJSON(xml);
    Assertions.assertEquals("{}", json, "root null must be replaced by empty object");
  }

  // ---------------------------------------------------------------------------
  // json:array and json:object in array context (no name required)
  // ---------------------------------------------------------------------------

  @Test
  void testJsonArrayInArrayContext() throws Exception {
    String xml = "<root xmlns:json=\"" + JSON_NS + "\">"
        + "<json:array json:name=\"outer\">"
        + "<json:array/>"
        + "</json:array>"
        + "</root>";
    String json = toJSON(xml);
    Assertions.assertTrue(json.contains("[[]]"), "nested array must be emitted");
  }

  @Test
  void testJsonObjectInArrayContext() throws Exception {
    String xml = "<root xmlns:json=\"" + JSON_NS + "\">"
        + "<json:array json:name=\"list\">"
        + "<json:object/>"
        + "</json:array>"
        + "</root>";
    String json = toJSON(xml);
    Assertions.assertTrue(json.contains("[{}]"), "object in array context must produce [{}]");
  }

  // ---------------------------------------------------------------------------
  // characters() outside VALUE context are ignored
  // ---------------------------------------------------------------------------

  @Test
  void testCharactersOutsideValueContextAreIgnored() throws Exception {
    // Whitespace between elements in OBJECT context must not appear in output
    String xml = "<root xmlns:json=\"" + JSON_NS + "\">\n"
        + "  <item id=\"1\"/>\n"
        + "</root>";
    String json = toJSON(xml);
    // The output should be valid JSON with no stray text tokens
    Assertions.assertTrue(json.startsWith("{"), "Output must start with {");
    Assertions.assertFalse(json.contains("\"\\n\"") || json.contains("\"  \""), "Whitespace text must not appear as a string value in output");
  }

  // ---------------------------------------------------------------------------
  // json:object with attributes serialized as properties
  // ---------------------------------------------------------------------------

  @Test
  void testJsonObjectWithAttributes() throws Exception {
    String xml = "<root xmlns:json=\"" + JSON_NS + "\">"
        + "<json:object json:name=\"meta\" version=\"2\" label=\"test\"/>"
        + "</root>";
    String json = toJSON(xml);
    Assertions.assertTrue(json.contains("\"version\""), "version attribute must be a property");
    Assertions.assertTrue(json.contains("\"label\""), "label attribute must be a property");
    Assertions.assertTrue(json.contains("\"test\""), "label value must be serialized");
  }

}
