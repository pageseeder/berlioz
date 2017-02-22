package org.pageseeder.berlioz.aeson;

import java.io.StringReader;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.junit.Test;
import org.xml.sax.InputSource;

public class JSONSerializerTest {

  @Test
  public void testParse() throws Exception {
    String xml = "<test description=\"Test\">"
    + "<json:array json:name=\"items\" xmlns:json=\"http://pageseeder.org/JSON\">"
    + "<item title=\"A\"/>"
    + "<item title=\"B\"/>"
    + "<item title=\"C\"/>"
    + "<item title=\"D\"/>"
    + "</json:array>"
    +"</test>";
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setNamespaceAware(true);
    SAXParser parser = factory.newSAXParser();
    parser.parse(new InputSource(new StringReader(xml)), new JSONSerializer(System.out));
  }

}
