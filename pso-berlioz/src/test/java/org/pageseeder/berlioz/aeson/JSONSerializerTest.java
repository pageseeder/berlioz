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

}
