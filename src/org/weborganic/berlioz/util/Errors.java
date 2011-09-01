/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.LocatorImpl;

import com.topologi.diffx.xml.XMLWriter;

/**
 * A utility class for the processing of errors.
 * 
 * <p>This class also includes a number of methods to turn various classes of exceptions and locators into XML.
 * 
 * @author Christophe Lauret
 * @version 30 June 2011
 */
public final class Errors {

  /**
   * Utility class.
   */
  private Errors() {
  }

  /**
   * Returns the stack trace of the specified error as a string.
   * 
   * <p>For security, this method will remove the part of the stacktrace which are specific 
   * to the servlet container.
   * 
   * @param error The throwable.
   * @param safe  <code>true</code> to only include the StackTrace up to the servlet API;
   *              <code>false</code> to include the complete stack trace.
   * 
   * @return The stacktrace.
   */
  public static String getStackTrace(Throwable error, boolean safe) {
    StringWriter sw = new StringWriter();
    error.printStackTrace(new PrintWriter(sw));
    StringBuffer stacktrace = sw.getBuffer();
    // Remove anything after the servlet API exception
    if (safe) {
      int x = stacktrace.indexOf("javax.servlet.http.HttpServlet.service");
      if (x >= 0) {
        stacktrace.setLength(x);
        stacktrace.append("...");
      }
    }
    return stacktrace.toString();
  }

  /**
   * Returns a clean message for the specified throwable.
   * 
   * <p>This method can be used to provide more user-friendly messages by removing the exception 
   * class prefix to the message if the message is identical to that of the exception causing it.
   * 
   * @param ex the throwable.
   * 
   * @return a clean message.
   */
  public static String cleanMessage(Throwable ex) {
    if (ex.getCause() == null) return ex.getMessage();
    Throwable t = ex.getCause();
    if (!ex.getMessage().equals(t.getClass().getName()+": "+t.getMessage())) return ex.getMessage();
    else return cleanMessage(t);
  }

  // XML Formatters
  // ---------------------------------------------------------------------------------------------

  /**
   * Writes the XML for the given exception.
   * 
   * <p>If there is a more specialised method for this exception defined in this class, this 
   * method will automatically use the more specific method.
   * 
   * <p>The default XML for a generic exception is:
   * <p>The XML return will be: 
   * <pre>{@code <exception class="[class]">
   *   <message>[message]</message>
   *   <stack-trace>[exception]</stack-trace>
   *   <cause>[cause exception as XML (if any)]</cause>
   * </exception>
   * }</pre>
   * 
   * @param ex  The exception to turn to XML.
   * @param xml The XML writer.
   * 
   * @throws IOException Only if thrown by the XML writer.
   */
  public static void toXML(Exception ex, XMLWriter xml) throws IOException {
    toXML(ex, xml, true);
  }

  /**
   * Returns the specified exception as XML.
   * 
   * @param ex   The exception to turn to XML.
   * @param xml  The XML writer.
   * @param wrap Whether to wrap the XML into an element.
   * 
   * @throws IOException Only if thrown by the XML writer.
   */
  public static void toXML(Throwable ex, XMLWriter xml, boolean wrap) throws IOException {
    if (ex instanceof SAXParseException)    { asSAXParseExceptionXML((SAXParseException)ex, xml, wrap); return;}
    if (ex instanceof TransformerException) { asTransformerExceptionXML((TransformerException)ex, xml, wrap); return;}
    if (ex instanceof Exception)            { asExceptionXML((Exception)ex, xml, wrap); return;}
  }

  /**
   * Returns the specified exception as XML.
   * 
   * <p>The XML for a {@link SAXParseException} is:
   * <pre>{@code <exception class="[class]" type="SAXParseException">
   *   <message>[message]</message>
   *   <stack-trace>[exception]</stack-trace>
   *   <cause>[cause exception as XML (if any)]</cause>
   *   <location line="[line]" column="[column]" public-id=[public-id]" system-id="[system-id]"/>
   * </exception>
   * }</pre>
   *
   * @param ex  The exception to turn to XML.
   * @param xml The XML writer.
   * 
   * @throws IOException Only if thrown by the XML writer.
   */
  public static void toXML(SAXParseException ex, XMLWriter xml) throws IOException {
    asSAXParseExceptionXML(ex, xml, true);
  }

  /**
   * Returns the specified exception as XML.
   * 
   * <p>The XML for a {@link TransformerException} is:
   * <pre>{@code <exception class="[class]" type="[TransformerException|TransformerConfigException]">
   *   <message>[message]</message>
   *   <stack-trace>[exception]</stack-trace>
   *   <cause>[cause exception as XML (if any)]</cause>
   *   <location line="[line]" column="[column]" public-id=[public-id]" system-id="[system-id]"/>
   * </exception>
   * }</pre>
   * 
   * @param ex  The exception to turn to XML.
   * @param xml The XML writer.
   * 
   * @throws IOException Only if thrown by the XML writer.
   */
  public static void toXML(TransformerException ex, XMLWriter xml) throws IOException {
    asTransformerExceptionXML(ex, xml, true);
  }

  /**
   * Returns the specified source locator as XML.
   * 
   * <p>Does nothing if the locator is <code>null</code>.
   * 
   * <p>The XML return will be:
   * <pre>
   * {@code <location line="[line]" column="[column]" public-id=[public-id]" system-id="[system-id]"/>}
   * </pre> 
   * 
   * @param locator The source locator.
   * @param xml     The XML writer.
   * 
   * @throws IOException Only if thrown by the XML writer.
   */
  public static void toXML(SourceLocator locator, XMLWriter xml) throws IOException {
    if (locator == null) return;
    int line = locator.getLineNumber();
    int column = locator.getColumnNumber();
    String publicId = locator.getPublicId();
    String systemId = locator.getSystemId();
    xml.openElement("location");
    if (line != -1) {
      xml.attribute("line", line);
    }
    if (column != -1) {
      xml.attribute("column", column);
    }
    if (publicId != null) {
      xml.attribute("public-id" ,publicId);
    }
    if (systemId != null) {
      xml.attribute("system-id", toWebPath(systemId));
    }
    xml.closeElement();
  }

  /**
   * Returns the specified locator as XML.
   * 
   * <p>Does nothing if the locator is <code>null</code>.
   * 
   * <p>The XML return will be:
   * <pre>
   * {@code <location line="[line]" column="[column]" public-id=[public-id]" system-id="[system-id]"/>}
   * </pre>
   * 
   * @param locator The source locator.
   * @param xml     The XML writer.
   * 
   * @throws IOException Only if thrown by the XML writer.
   */
  public static void toXML(Locator locator, XMLWriter xml) throws IOException {
    if (locator == null) return;
    int line = locator.getLineNumber();
    int column = locator.getColumnNumber();
    String publicId = locator.getPublicId();
    String systemId = locator.getSystemId();
    xml.openElement("location");
    if (line != -1) {
      xml.attribute("line", line);
    }
    if (column != -1) {
      xml.attribute("column", column);
    }
    if (publicId != null) {
      xml.attribute("public-id" , publicId);
    }
    if (systemId != null) {
      xml.attribute("system-id", toWebPath(systemId));
    }
    xml.closeElement();
  }

  // Private helpers
  // ---------------------------------------------------------------------------------------------

  /**
   * Displays the path to the file from the web application (for debugging).
   * 
   * @param s the file path.
   * @return The path from the "WEB-INF" directory
   */
  private static String toWebPath(String s) {
    String from = "WEB-INF";
    int x = s.indexOf(from);
    return x != -1? s.substring(x+from.length()).replace('\\', '/') : s.replace('\\', '/');
  }

  /**
   * Creates a locator from the given SAX parse exception.
   * @param ex The SAX Parse exception
   * @return the corresponding locator.
   */
  private static Locator toLocator(SAXParseException ex) {
    LocatorImpl locator = new LocatorImpl();
    locator.setLineNumber(ex.getLineNumber());
    locator.setColumnNumber(ex.getColumnNumber());
    locator.setPublicId(ex.getPublicId());
    locator.setSystemId(ex.getSystemId());
    return locator;
  }

  /**
   * Returns the XML for a generic exception.
   * 
   * @param ex   The exception to turn to XML.
   * @param xml  The XML writer.
   * @param wrap Whether to wrap the XML into an element.
   * 
   * @throws IOException Only if thrown by the XML writer.
   */
  private static void asExceptionXML(Exception ex, XMLWriter xml, boolean wrap) throws IOException {
    if (wrap) {
      xml.openElement("exception");
    }
    xml.attribute("class", ex.getClass().getName());
    xml.element("message", cleanMessage(ex));
    xml.element("stack-trace", Errors.getStackTrace(ex, true));
    Throwable cause = ex.getCause();
    if (cause != null) {
      xml.openElement("cause");
      toXML(cause, xml, false);
      xml.closeElement();
    }
    if (wrap) {
      xml.closeElement();
    }
  }

  /**
   * Returns the specified exception as XML
   * 
   * @param ex   The exception to turn to XML.
   * @param xml  The XML writer.
   * @param wrap Whether to wrap the XML into an element.
   * 
   * @throws IOException Only if thrown by the XML writer.
   */
  private static void asSAXParseExceptionXML(SAXParseException ex, XMLWriter xml, boolean wrap) throws IOException {
    if (wrap) {
      xml.openElement("exception");
    }
    xml.attribute("type", "SAXParseException");
    asExceptionXML(ex, xml, false);
    // Add the locator
    toXML(toLocator(ex), xml);
    if (wrap) {
      xml.closeElement();
    }
  }

  /**
   * Returns the specified exception as XML
   * 
   * @param ex   The exception to turn to XML.
   * @param xml  The XML writer.
   * @param wrap Whether to wrap the XML into an element.
   * 
   * @throws IOException Only if thrown by the XML writer.
   */
  private static void asTransformerExceptionXML(TransformerException ex, XMLWriter xml, boolean wrap) 
      throws IOException {
    if (wrap) {
      xml.openElement("exception");
    }
    boolean isConfig = ex instanceof TransformerConfigurationException;
    xml.attribute("type", isConfig? "TransformerConfigurationException" : "TransformerException");
    asExceptionXML(ex, xml, false);
    // Add the Source locator
    toXML(ex.getLocator(), xml);
    if (wrap) {
      xml.closeElement();
    }
  }

}
