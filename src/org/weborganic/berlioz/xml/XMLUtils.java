/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.xml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weborganic.berlioz.BerliozException;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.topologi.diffx.xml.XMLWriter;

/**
 * A utility class to help with some simple XML operations.
 *
 * @author Christophe Lauret (Weborganic)
 * 
 * @version 3 December 2009
 */
public final class XMLUtils {

  /**
   * The logger for this class.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(XMLUtils.class);

  /**
   * The date format used for date values (without time)
   */
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("d MMMM yyyy");

  /**
   * The date format used for date values (including time)
   */
  private static final DateFormat DATETIME_FORMAT = new SimpleDateFormat("d MMMM yyyy hh:mm:ss a");

  /**
   * The date format used for years.
   */
  private static final DateFormat YYYY = new SimpleDateFormat("yyyy");

  /**
   * The date format used for the hours in 24h format.
   */
  private static final DateFormat HH = new SimpleDateFormat("HH");

  /**
   * The date format used for the minute.
   */
  private static final DateFormat MM = new SimpleDateFormat("mm");

  /**
   * The date format used for the day.
   */
  private static final DateFormat DD = new SimpleDateFormat("dd");

  /**
   * The date format used for the month.
   */
  private static final DateFormat MO = new SimpleDateFormat("MM");

  /**
   * The SAX parser factory to generate non-validating XML readers.
   */
  private static transient SAXParserFactory nfactory;

  /**
   * The SAX parser factory to generate validating XML readers.
   */
  private static transient SAXParserFactory vfactory;

  /**
   * Prevents creation of instances.
   */
  private XMLUtils() {
  }

  /**
   * Writes the content model of a date as XML.
   * 
   * <p>The XML is as follows:
   * <pre class="xml">
   *   &lt;[date_element_name]
   *      day="[day]" month="[month]" year="[year]"
   *   &gt;[dd MMM YYYY]&lt;/[date_element_name]&gt;
   * </pre>
   * 
   * <p>Does nothing if the date is <code>null</code>.
   * 
   * @see SimpleDateFormat
   * 
   * @deprecated Dates should be serialised as ISO 8601
   * 
   * @param xml  The XML writer to use.
   * @param date The date to format.
   * 
   * @throws IOException If thrown by the XML writer.
   * @throws NullPointerException If the XML writer is <code>null</code>
   */
  @Deprecated public static void dateAsXML(XMLWriter xml, Date date)
      throws IOException, NullPointerException {
    if (date == null) return;
    xml.attribute("day", DD.format(date));
    xml.attribute("month", MO.format(date));
    xml.attribute("year", YYYY.format(date));
    xml.writeText(DATE_FORMAT.format(date));
  }

  /**
   * Writes the content model of a date and time as XML.
   *
   * <pre class="xml">
   *   &lt;[date_element_name] day="[day-value]"
   *                      month="[month-value]"
   *                      year="[year-value]"
   *                      hour="[hour-value]"
   *                      minute="[minute-value]"
   *   &gt;[dd MMM YYYY hh:mm aa]
   *   &lt;/[date_element_name]&gt;
   * </pre>
   * 
   * <p>Hour attribute is written using 24hrs format.
   * 
   * <p>Does nothing if the date is <code>null</code>.
   * 
   * @see SimpleDateFormat
   * 
   * @deprecated Dates should be serialised as ISO 8601
   * 
   * @param xml  The XML writer to use.
   * @param date The date to format.
   * 
   * @throws IOException If thrown by the XML writer.
   * @throws NullPointerException If the XML writer is <code>null</code>
   */
  @Deprecated public static void datetimeAsXML(XMLWriter xml, Date date)
      throws IOException, NullPointerException {
    if (date == null) return;
    xml.attribute("day", DD.format(date));
    xml.attribute("month", MO.format(date));
    xml.attribute("year", YYYY.format(date));
    xml.attribute("hour", HH.format(date));
    xml.attribute("minute", MM.format(date));
    xml.writeText(DATETIME_FORMAT.format(date));
  }

  /**
   * Parses the specified file using the given handler.
   * 
   * @param handler The content handler to use.
   * @param xml     The XML file to parse.
   * 
   * @throws BerliozException Should something unexpected happen.
   */
  public static void parse(ContentHandler handler, File xml) throws BerliozException {
    parse(handler, xml, true);
  }

  /**
   * Parses the specified file using the given handler.
   * 
   * @param handler  The content handler to use.
   * @param xml      The XML file to parse.
   * @param validate whether to validate or not.
   * 
   * @throws BerliozException Should something unexpected happen.
   */
  public static void parse(ContentHandler handler, File xml, boolean validate) throws BerliozException {
    SAXParser parser = getParser(validate);
    try {
      // get the reader
      XMLReader reader = parser.getXMLReader();
      // configure the reader
      reader.setContentHandler(handler);
      reader.setEntityResolver(BerliozEntityResolver.getInstance());
      reader.setErrorHandler(BerliozErrorHandler.getInstance());
      // parse
      if (xml.isDirectory()) {
        File[] xmls = xml.listFiles(new XMLFilenameFilter());
        for (int i = 0; i < xmls.length; i++) {
          reader.parse(new InputSource(xmls[i].toURI().toString()));
        }
        // TODO: and if there is no XML files in the directory?
      } else {
        LOGGER.info("parsing "+xml.toURI().toString());
        reader.parse(new InputSource(xml.toURI().toString()));
      }
    } catch (SAXException ex) {
      throw new BerliozException("Could not parse file. " + ex.getMessage(), ex);
    } catch (FileNotFoundException ex) {
      ex.printStackTrace();
      throw new BerliozException("Could not find file.", ex);
    } catch (IOException ex) {
      ex.printStackTrace();
      throw new BerliozException("Could not read file.", ex);
    }
  }

  /**
   * Returns the requested SAX Parser factory.
   * 
   * @param validating <code>true</code> for the validating factory;
   *                   <code>false</code> for the non-validating factory;
   * @return the SAX parser factory to use.
   * 
   * @throws BerliozException If one of the features is not recognised or supported by the factory.
   */
  public static SAXParser getParser(boolean validating) throws BerliozException {
    SAXParserFactory factory = validating? XMLUtils.vfactory : XMLUtils.nfactory;
    SAXParser parser = null;
    try {
      if (factory == null) {
        // use the SAX parser factory to ensure validation
        factory = SAXParserFactory.newInstance();
        factory.setValidating(validating);
        factory.setNamespaceAware(true);
        // also specify the features
        factory.setFeature("http://xml.org/sax/features/validation", validating);
        factory.setFeature("http://xml.org/sax/features/namespaces", true);
        factory.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
        // set at the end, do not configure factory directly to avoid synchronisation problems
        if (validating) {
          XMLUtils.vfactory = factory;
        } else {
          XMLUtils.nfactory = factory;
        }
      }
      // get a new parser
      parser = factory.newSAXParser();
    } catch (ParserConfigurationException ex) {
      throw new BerliozException("Could not configure SAX parser.", ex);
    } catch (SAXException ex) {
      throw new BerliozException("Could not setup SAX parser factory: " + ex.getMessage(), ex);
    }
    return parser;
  }

}
