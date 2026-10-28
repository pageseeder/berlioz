package org.pageseeder.berlioz.generator;

import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.output.JsonOutputAdapter;
import org.pageseeder.berlioz.output.OutputWriter;
import org.pageseeder.berlioz.output.XmlOutputAdapter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

class ListBerliozOptionsTest {

  // XML
  // ---------------------------------------------------------------------------

  @Test
  void testXmlListsEveryOptionInDeclarationOrder() throws Exception {
    Document doc = parseXml(process(new XmlOutputAdapter()));
    Element root = doc.getDocumentElement();
    assertEquals("berlioz-options", root.getTagName());
    NodeList options = root.getElementsByTagName("option");
    BerliozOption[] expected = BerliozOption.values();
    assertEquals(expected.length, options.getLength());
    for (int i = 0; i < expected.length; i++) {
      Element option = (Element) options.item(i);
      assertEquals(expected[i].property(), option.getAttribute("property"));
      assertEquals(expected[i].description(), option.getAttribute("description"));
      assertEquals(expected[i].defaultTo().toString(), option.getAttribute("default"));
    }
  }

  @Test
  void testXmlCurrentOption() {
    String xml = process(new XmlOutputAdapter());
    assertTrue(xml.startsWith("<berlioz-options><option property=\"berlioz.http.compression\""
        + " type=\"boolean\" default=\"true\" description=\""
        + BerliozOption.HTTP_COMPRESSION.description() + "\"/>"), xml);
  }

  @Test
  void testXmlDeprecatedOption() {
    String xml = process(new XmlOutputAdapter());
    assertTrue(xml.contains("<option property=\"berlioz.http.get-via-post\" type=\"boolean\" default=\"false\""
        + " deprecated=\"0.14.0\" removal=\"1.0\" description=\""), xml);
  }

  @Test
  void testXmlStringOption() {
    String xml = process(new XmlOutputAdapter());
    assertTrue(xml.contains("<option property=\"berlioz.xslt.cache\" type=\"string\" default=\"manual\" description=\""), xml);
    assertTrue(xml.contains("<option property=\"berlioz.control.key\" type=\"string\" default=\"\" description=\""), xml);
  }

  // JSON
  // ---------------------------------------------------------------------------

  @Test
  void testJsonShape() {
    String json = process(new JsonOutputAdapter());
    assertTrue(json.startsWith("{\"options\":[{\"property\":\"berlioz.http.compression\",\"type\":\"boolean\","
        + "\"default\":true,\"description\":\""), json);
    assertTrue(json.endsWith("}]}"), json);
  }

  @Test
  void testJsonDeprecatedAndStringOptions() {
    String json = process(new JsonOutputAdapter());
    assertTrue(json.contains("{\"property\":\"berlioz.http.get-via-post\",\"type\":\"boolean\",\"default\":false,"
        + "\"deprecated\":\"0.14.0\",\"removal\":\"1.0\",\"description\":\""), json);
    assertTrue(json.contains("{\"property\":\"berlioz.xslt.cache\",\"type\":\"string\",\"default\":\"manual\","
        + "\"description\":\""), json);
  }

  // ETag
  // ---------------------------------------------------------------------------

  @Test
  void testETagIsStable() {
    ListBerliozOptions generator = new ListBerliozOptions();
    Request req = GeneratorTestSupport.request().build();
    String etag = generator.getETag(req);
    assertNotNull(etag);
    assertFalse(etag.isEmpty());
    assertEquals(etag, new ListBerliozOptions().getETag(req));
  }

  // helpers
  // ---------------------------------------------------------------------------

  private static String process(OutputWriter out) {
    new ListBerliozOptions().generate(GeneratorTestSupport.request().build(), out);
    return out.toString();
  }

  private static Document parseXml(String xml) throws Exception {
    return DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse(new InputSource(new StringReader(xml)));
  }
}
