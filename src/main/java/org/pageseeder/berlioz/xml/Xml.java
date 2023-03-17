package org.pageseeder.berlioz.xml;

import org.pageseeder.berlioz.BerliozException;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.Writer;

/**
 *
 * @version Berlioz 0.12.0
 * @since Berlioz 0.12.0
 */
public class Xml {

  private Xml() {
  }

  /**
   * Always return a XML Writer.
   *
   * @param writer The writer receiving the XML output.
   *
   * @return The corresponding XML writer to use.
   */
  public static XmlWriter newWriter(Writer writer) {
    return new XmlAppendable<Writer>(writer);
  }

  /**
   * @return A SAX Parser
   */
  public static SAXParser newSafeParser() throws ParserConfigurationException, SAXException {
    return newSafeParser(false);
  }

  /**
   * @return A SAX Parser
   */
  public static SAXParser newSafeParser(boolean validating) throws ParserConfigurationException, SAXException {
    // use the SAX parser factory to ensure validation
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setValidating(validating);
    factory.setNamespaceAware(true);
    factory.setXIncludeAware(false);
    // also specify the features
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setFeature("http://xml.org/sax/features/validation", validating);
    factory.setFeature("http://xml.org/sax/features/namespaces", true);
    factory.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
    // get a new parser
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    return factory.newSAXParser();
  }

}
