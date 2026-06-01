package org.pageseeder.berlioz.aeson;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.InputSource;

public class JSONSerializerTest {

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
  public void testParse() throws Exception {
    String xml = "<test description=\"Test\">"
        + "<json:array json:name=\"items\" xmlns:json=\"" + JSON_NS + "\">"
        + "<item title=\"A\"/>"
        + "<item title=\"B\"/>"
        + "</json:array>"
        + "</test>";
    String json = toJSON(xml);
    Assert.assertTrue(json.contains("\"items\""));
    Assert.assertTrue(json.contains("\"title\""));
  }

  // ---------------------------------------------------------------------------
  // asNumber — integer and decimal values
  // ---------------------------------------------------------------------------

  @Test
  public void testNumberLong() throws Exception {
    String xml = "<root xmlns:json=\"" + JSON_NS + "\" json:number=\"count\">"
        + "<item count=\"42\"/></root>";
    String json = toJSON(xml);
    Assert.assertTrue("count should appear as a number", json.contains("42"));
    Assert.assertFalse("count should not be a JSON string", json.contains("\"42\""));
  }

  @Test
  public void testNumberNegativeLong() throws Exception {
    String xml = "<root xmlns:json=\"" + JSON_NS + "\" json:number=\"offset\">"
        + "<item offset=\"-7\"/></root>";
    String json = toJSON(xml);
    Assert.assertTrue(json.contains("-7"));
    Assert.assertFalse("negative long should not be a JSON string", json.contains("\"-7\""));
  }

  @Test
  public void testNumberDouble() throws Exception {
    String xml = "<root xmlns:json=\"" + JSON_NS + "\" json:number=\"rate\">"
        + "<item rate=\"3.14\"/></root>";
    String json = toJSON(xml);
    Assert.assertTrue(json.contains("3.14"));
    Assert.assertFalse("decimal should not be a JSON string", json.contains("\"3.14\""));
  }

  // ---------------------------------------------------------------------------
  // asNumber — scientific notation (regression: used to throw NFE → string fallback)
  // ---------------------------------------------------------------------------

  @Test
  public void testNumberScientificLowerE() throws Exception {
    // "1e10" has no '.'; the old code called Long.parseLong which threw NFE
    String xml = "<root xmlns:json=\"" + JSON_NS + "\" json:number=\"amount\">"
        + "<item amount=\"1e10\"/></root>";
    String json = toJSON(xml);
    Assert.assertFalse("1e10 must not be serialized as a JSON string", json.contains("\"1e10\""));
  }

  @Test
  public void testNumberScientificUpperE() throws Exception {
    String xml = "<root xmlns:json=\"" + JSON_NS + "\" json:number=\"amount\">"
        + "<item amount=\"2E5\"/></root>";
    String json = toJSON(xml);
    Assert.assertFalse("2E5 must not be serialized as a JSON string", json.contains("\"2E5\""));
  }

  @Test
  public void testNumberScientificWithDecimalAndExponent() throws Exception {
    String xml = "<root xmlns:json=\"" + JSON_NS + "\" json:number=\"rate\">"
        + "<item rate=\"1.5e3\"/></root>";
    String json = toJSON(xml);
    Assert.assertFalse("1.5e3 must not be serialized as a JSON string", json.contains("\"1.5e3\""));
  }

  @Test
  public void testNumberNegativeExponent() throws Exception {
    String xml = "<root xmlns:json=\"" + JSON_NS + "\" json:number=\"prob\">"
        + "<item prob=\"5E-3\"/></root>";
    String json = toJSON(xml);
    Assert.assertFalse("5E-3 must not be serialized as a JSON string", json.contains("\"5E-3\""));
  }

  // ---------------------------------------------------------------------------
  // asNumber — invalid value falls back to string (existing contract unchanged)
  // ---------------------------------------------------------------------------

  @Test
  public void testNumberInvalidFallsBackToString() throws Exception {
    String xml = "<root xmlns:json=\"" + JSON_NS + "\" json:number=\"value\">"
        + "<item value=\"not-a-number\"/></root>";
    String json = toJSON(xml);
    Assert.assertTrue("Invalid number should fall back to a JSON string",
        json.contains("\"not-a-number\""));
  }

  // ---------------------------------------------------------------------------
  // asBoolean
  // ---------------------------------------------------------------------------

  @Test
  public void testBooleanTrue() throws Exception {
    String xml = "<root xmlns:json=\"" + JSON_NS + "\" json:boolean=\"active\">"
        + "<item active=\"true\"/></root>";
    String json = toJSON(xml);
    Assert.assertTrue(json.contains("true"));
    Assert.assertFalse("true should not be a JSON string", json.contains("\"true\""));
  }

  @Test
  public void testBooleanFalse() throws Exception {
    String xml = "<root xmlns:json=\"" + JSON_NS + "\" json:boolean=\"active\">"
        + "<item active=\"false\"/></root>";
    String json = toJSON(xml);
    Assert.assertTrue(json.contains("false"));
    Assert.assertFalse("false should not be a JSON string", json.contains("\"false\""));
  }

  @Test
  public void testBooleanInvalidFallsBackToString() throws Exception {
    String xml = "<root xmlns:json=\"" + JSON_NS + "\" json:boolean=\"active\">"
        + "<item active=\"yes\"/></root>";
    String json = toJSON(xml);
    Assert.assertTrue("Invalid boolean should fall back to a JSON string",
        json.contains("\"yes\""));
  }

  // ---------------------------------------------------------------------------
  // json:null attribute
  // ---------------------------------------------------------------------------

  @Test
  public void testNullProperty() throws Exception {
    String xml = "<root xmlns:json=\"" + JSON_NS + "\" json:null=\"missing\">"
        + "<item missing=\"whatever\"/></root>";
    String json = toJSON(xml);
    Assert.assertTrue("null-typed property should emit JSON null", json.contains("null"));
    Assert.assertFalse("null-typed property should not keep the attribute value",
        json.contains("\"whatever\""));
  }

  // ---------------------------------------------------------------------------
  // json:null element — in object, array, and root contexts
  // ---------------------------------------------------------------------------

  @Test
  public void testJsonNullElementInObjectContext() throws Exception {
    String xml = "<root xmlns:json=\"" + JSON_NS + "\">"
        + "<json:null json:name=\"missing\"/>"
        + "</root>";
    String json = toJSON(xml);
    Assert.assertTrue("named null element in object context must emit \"missing\":null",
        json.contains("\"missing\":null"));
  }

  @Test
  public void testJsonNullElementInArrayContext() throws Exception {
    String xml = "<root xmlns:json=\"" + JSON_NS + "\">"
        + "<json:array json:name=\"items\">"
        + "<json:null/>"
        + "</json:array>"
        + "</root>";
    String json = toJSON(xml);
    Assert.assertTrue("null element in array context must emit null value", json.contains("null"));
    Assert.assertTrue("array must be present", json.contains("\"items\""));
  }

  @Test
  public void testJsonNullElementInRootContextEmitsEmptyObject() throws Exception {
    // json:null at root is illegal; the serializer substitutes an empty object
    String xml = "<json:null xmlns:json=\"" + JSON_NS + "\"/>";
    String json = toJSON(xml);
    Assert.assertEquals("root null must be replaced by empty object", "{}", json);
  }

  // ---------------------------------------------------------------------------
  // json:array and json:object in array context (no name required)
  // ---------------------------------------------------------------------------

  @Test
  public void testJsonArrayInArrayContext() throws Exception {
    String xml = "<root xmlns:json=\"" + JSON_NS + "\">"
        + "<json:array json:name=\"outer\">"
        + "<json:array/>"
        + "</json:array>"
        + "</root>";
    String json = toJSON(xml);
    Assert.assertTrue("nested array must be emitted", json.contains("[[]]"));
  }

  @Test
  public void testJsonObjectInArrayContext() throws Exception {
    String xml = "<root xmlns:json=\"" + JSON_NS + "\">"
        + "<json:array json:name=\"list\">"
        + "<json:object/>"
        + "</json:array>"
        + "</root>";
    String json = toJSON(xml);
    Assert.assertTrue("object in array context must produce [{}]", json.contains("[{}]"));
  }

  // ---------------------------------------------------------------------------
  // characters() outside VALUE context are ignored
  // ---------------------------------------------------------------------------

  @Test
  public void testCharactersOutsideValueContextAreIgnored() throws Exception {
    // Whitespace between elements in OBJECT context must not appear in output
    String xml = "<root xmlns:json=\"" + JSON_NS + "\">\n"
        + "  <item id=\"1\"/>\n"
        + "</root>";
    String json = toJSON(xml);
    // The output should be valid JSON with no stray text tokens
    Assert.assertTrue("Output must start with {", json.startsWith("{"));
    Assert.assertFalse("Whitespace text must not appear as a string value in output",
        json.contains("\"\\n\"") || json.contains("\"  \""));
  }

  // ---------------------------------------------------------------------------
  // json:object with attributes serialized as properties
  // ---------------------------------------------------------------------------

  @Test
  public void testJsonObjectWithAttributes() throws Exception {
    String xml = "<root xmlns:json=\"" + JSON_NS + "\">"
        + "<json:object json:name=\"meta\" version=\"2\" label=\"test\"/>"
        + "</root>";
    String json = toJSON(xml);
    Assert.assertTrue("version attribute must be a property", json.contains("\"version\""));
    Assert.assertTrue("label attribute must be a property", json.contains("\"label\""));
    Assert.assertTrue("label value must be serialized", json.contains("\"test\""));
  }

}
