/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weborganic.berlioz.GlobalSettings;
import org.weborganic.berlioz.content.Service;
import org.weborganic.berlioz.util.MD5;

import com.topologi.diffx.xml.XMLUtils;

/**
 * Performs the XSLT transformation from the generated XML content. 
 * 
 * <p>By default, all XSLT templates are cached, use the global property <code>berlioz.cache.xslt</code>
 * to change this behaviour.
 * 
 * @author Christophe Lauret
 * @version 12 April 2011
 */
public final class XSLTransformer {

  /**
   * Name of the global property to use to enable caching of XSLT (<code>true</code> by default).
   */
  public static final String ENABLE_CACHE = "berlioz.cache.xslt";

  /**
   * Displays debug information.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(XSLTransformer.class);

  /**
   * Maps XSLT templates to their name for easy retrieval.
   */
  private static final Map<File, Templates> CACHE = new Hashtable<File, Templates>();

  /**
   * The location of the XSLT templates.
   * 
   * <p>For example, "/WEB-INF/xslt/html/global.xsl"
   */
  private final File _templates;

  /**
   * An etag for these templates.
   */
  private transient String _etag = null;

  /**
   * Creates a new XSLT Transformer.
   * 
   * @param templates The location of the templates.
   */
  public XSLTransformer(File templates) {
    if (templates == null) throw new NullPointerException("No Templates file specified");
    this._templates = templates;
    this._etag = computeEtag(templates);
  }

  /**
   * Transforms the Specified content using XSLT.
   * 
   * @param content The XML content to transform.
   * @param req     The HTTP Servlet request.
   * @param service Required only to provide more information in the logs in case of errors. 
   * 
   * @return the results of the transformation.
   */
  public XSLTransformResult transform(String content, HttpServletRequest req, Service service) {
    StringWriter buffer = new StringWriter();
    long time = 0;
    Templates templates = null;

    try {
      // Creates a transformer from the templates
      templates = getTemplates(this._templates);

      // Setup the transformer
      Map<String, String> parameters = toParameters(req);

      // Setup the source
      StreamSource source = new StreamSource(new StringReader(content));
      source.setPublicId("-//Berlioz//Service/XML/"+service.group()+"/"+service.id());
      // TODO: provide better info (identify the service)
      source.setSystemId(req.getRequestURI().replaceAll("/html/", "/xml/"));

      // Setup the result
      StreamResult result = new StreamResult(buffer);

      // Transform!
      time = transform(source, result, templates, parameters);

    // very likely to be an error in the XML or a dynamic error
    } catch (TransformerException ex) {
      StringWriter error = new StringWriter();
      handle(ex, error);
      return new XSLTransformResult(error.toString(), ex, templates);

      // Catch the error details and try to process them with the error template
//      String xml = handle(ex, component, xmlsource, parameters);
//      Map<String, String> noparameters = Collections.emptyMap();
//      String xhtml = transformCommon(layout, "error", xml, noparameters);
//      return new XSLTProcessResult(xhtml, 0, Status.ERROR);
    }

    // All good!
    return new XSLTransformResult(buffer.toString(), time, templates);
  }

  /**
   * Returns the file used by this transformer to produce the templates.
   *
   * @return  the file used by this transformer to produce the templates.
   */
  public File templates() {
    return this._templates;
  }

  /**
   * Returns an ETag corresponding to the templates. 
   * 
   * @return an ETag corresponding to the templates.
   */
  public String getEtag() {
    return this._etag;
  }

  /**
   * Clears the internal XSLT cache.
   */
  public void clearCache() {
    LOGGER.debug("Clearing XSLT cache.");
    CACHE.clear();
  }

// private helpers --------------------------------------------------------------------------------

  /**
   * Computes the etag for the templates.
   * @param templates The main file for the templates.
   * @return The corresponding etag.
   */
  private static String computeEtag(File templates) {
    List<File> files = new ArrayList<File>(); 
    listTemplateFiles(templates.getParentFile(), files);
    StringBuilder b = new StringBuilder();
    try {
      for (File f : files) { b.append(MD5.hash(f)); }
    } catch (IOException ex) {
      LOGGER.warn("Error thrown while trying to calculate template etag", ex);
      return null;
    }
    return MD5.hash(b.toString());
  }

  /**
   * Lists all the files in the specified directory and its descendants. 
   * 
   * @param dir      the root directory to scan.
   * @param collected files collected so far.
   */
  private static void listTemplateFiles(File dir, List<File> collected) {
    // get all the files in the current directory
    File[] files = dir.listFiles();
    // iterate over the files, collect
    for (File f : files) {
      // scan directories
      if (f.isDirectory()) {
        listTemplateFiles(f, collected);
      } else {
        // collect files only
        collected.add(f);
      }
    }
  }

  /**
   * Utility function to transforms the specified XML source and returns the results as XML.
   * 
   * <p>Problems will be reported in the logs, the output will simply produce results as a comment.
   * 
   * @param source     The Source XML data.
   * @param result     The Result XHTML data.
   * @param templates  The XSLT templates to use.
   * @param parameters Parameters to transmit to the transformer for use by the stylesheet (optional)
   * 
   * @return The time it took to process the stylesheet.
   * 
   * @throws TransformerException For XSLT Transformation errors or XSLT config errors
   */
  private static long transform(StreamSource source, StreamResult result, Templates templates, Map<String, String> parameters)
    throws TransformerException {

    // Create a transformer from the templates
    Transformer transformer = templates.newTransformer();

    // Transmit the properties to the transformer
    if (parameters != null) {
      for (Entry<String, String> e : parameters.entrySet()) {
        transformer.setParameter(e.getKey(), e.getValue());
      }
    }

    // Process, write directly to the result
    long before = System.currentTimeMillis();
    UIErrorListener listener = new UIErrorListener();
    transformer.setErrorListener(listener);
    try {
      transformer.transform(source, result);
    } catch (TransformerException ex) {
      throw new UITransformerException(ex, listener.xml.toString());
    }
    return System.currentTimeMillis() - before;
  }

  // private helpers
  // ----------------------------------------------------------------------------------------------

  /**
   * Returns the templates corresponding to the specified file.
   * 
   * This method uses the caching mechanism.
   * 
   * @param f The path to the XSLT style sheet.
   * 
   * @return The corresponding templates
   * 
   * @throws TransformerConfigurationException If the templates could not parsed. 
   */
  private Templates getTemplates(File f) throws TransformerConfigurationException {
    boolean store = GlobalSettings.get(ENABLE_CACHE, true);
    String stylesheet = toWebPath(f.getAbsolutePath());
    Templates templates = store? CACHE.get(f) : null;
    if (templates == null) {
      LOGGER.info("Loading XSLT stylesheet '{}' [caching {}]", stylesheet, store? "enabled" : "disabled");
      // Generate the templates if necessary
      long t0 = System.currentTimeMillis();
      templates = toTemplates(f);
      long t1 = System.currentTimeMillis();
      LOGGER.debug("Templates loaded in {}ms", (t1 - t0));
      // Recalculate the Etag
      this._etag = computeEtag(f);
      if (store) {
        CACHE.put(f, templates);
        LOGGER.info("Caching XSLT stylesheet '{}'", stylesheet);
      }
    }
    return templates;
  }

  /**
   * Return the XSLT templates from the given style.
   *
   * @param stylepath The path to the XSLT style sheet
   *
   * @return the corresponding XSLT templates object
   * 
   * @throws TransformerConfigurationException If the loading fails.
   */
  private static Templates toTemplates(File stylepath) throws TransformerConfigurationException {
    // load the templates from the source file
    InputStream in = null;
    Templates templates = null;
    try {
      in = new FileInputStream(stylepath);
      Source source = new StreamSource(in);
      source.setSystemId(stylepath.toURI().toString());
      TransformerFactory factory = TransformerFactory.newInstance();
      UIErrorListener listener = new UIErrorListener();
      factory.setErrorListener(listener);
      try {
        templates = factory.newTemplates(source);
      } catch (TransformerConfigurationException ex) {
        throw new UITransformerConfigurationException(ex, listener.xml.toString());
      }
    } catch (FileNotFoundException ex) {
      // Should not happen because we check before that the file exists, so we can safely ignore
      LOGGER.warn("Unable to find template file: {}", stylepath);
    } finally {
      closeQuietly(in);
    }
    return templates;
  }

  /**
   * Returns the XSLT parameters for the transformer from the HTTP parameters starting with 'xsl-'.
   * 
   * @param req The servlet request.
   * @return the map of parameters to pass to the XSLT as parameters.
   */
  private static Map<String, String> toParameters(ServletRequest req) {
    // Adding parameters from HTTP parameters
    Map<String, String> p = null;
    for (Enumeration<?> names = req.getParameterNames(); names.hasMoreElements();) {
      String param = (String)names.nextElement();
      if (param.startsWith("xsl-")) {
        if (p == null) p = new HashMap<String, String>();
        p.put(param.substring(4), req.getParameter(param));
      }
    }
    // Return parameters
    if (p != null) return p;
    else return Collections.emptyMap();
  }

  /**
   * Returns the stack trace of the specified error as a string.
   * 
   * <p>For security, this method will remove the part of the stacktrace which are specific 
   * to the servlet container. 
   * 
   * @param error The throwable.
   * @param safe  <code>true</code> to only include the StackTrace up to the servlet API.
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
   * Handles transformation errors - to be used in catch blocks.
   * 
   * @param ex  An error occurring during an XSLT transformation.
   * @param out Where the output goes.
   */
  private static void handle(TransformerException ex, Writer out) {
    LOGGER.error("As error occurred while transforming content", ex);
    // Capture the error
    StringWriter w = new StringWriter();
    PrintWriter error = new PrintWriter(w);
    ex.printStackTrace(error);
    error.flush();
    // Remove all double dash so that it may be inserted in the XML comment
    PrintWriter pout = new PrintWriter(out);
    pout.println("<!-- ");
    pout.println(ex.getMessageAndLocation());
    pout.println(w.toString().replaceAll("-+", "-"));
    pout.println(" -->");
    pout.flush();
  }

  /**
   * Displays the path to the file from the web application (for debugging).
   * 
   * @param s the file path.
   * @return The path from the "WEB-INF" directory
   */
  private static String toWebPath(String s) {
    String from = "WEB-INF";
    int x = s.indexOf(from);
    return x != -1? s.substring(x+from.length()) : s;
  }

  /**
   * Close an input stream ignoring any exception.
   * @param in the input stream
   */
  private static void closeQuietly(InputStream in) {
    if (in != null) {
      try {
        in.close();
      } catch (IOException ex) {
        LOGGER.debug("Error thrown while trying to quietly close stream - ignored", ex);
      }
    }
  }

  /**
   * Returns a new XML source that can be used by an error handler.
   * 
   * @param error      The transformation error.
   * @param xml        The source XML.
   * @param parameters The parameters sent to the transformer.
   * 
   * @return the new source
   */
  private String handle(TransformerException error, String xml, Map<String, String> parameters) {
    StringBuilder sb = new StringBuilder();
    sb.append("<content>\n");
    sb.append("<transform>\n");
    sb.append("<root>\n");
    sb.append("  <status>error</status>\n");
    sb.append("  <message>").append(XMLUtils.escape(error.getMessage())).append("</message>\n");
    sb.append("  <date>").append(String.format("%1s", new Date())).append("</date>\n");
    sb.append("  <errors>\n");
    if (error instanceof UITransformerException) {
      sb.append(((UITransformerException)error).getErrorsAsXML());
    } else if (error instanceof UITransformerConfigurationException) {
      sb.append(((UITransformerConfigurationException)error).getErrorsAsXML());
    }
    sb.append("  </errors>\n");
    sb.append("  <stacktrace>");
    String stacktrace = getStackTrace(error, true);
    sb.append(XMLUtils.escape(stacktrace));
    sb.append("  </stacktrace>");
    sb.append("  <parameters>");
    for (Entry<String, String> p : parameters.entrySet()) {
      sb.append("  <parameter name=\"").append(XMLUtils.escapeAttr(p.getKey())).append("\"");
      sb.append("    value=\"").append(XMLUtils.escapeAttr(p.getValue())).append("\"/>");
    }
    sb.append("  </parameters>");
    sb.append("  <source>");
    sb.append(XMLUtils.escape(xml));
    sb.append("  </source>\n");
    sb.append("</root>\n");
    sb.append("</transform>\n");
    sb.append("</content>");
    return sb.toString();
  }

  // Listeners and exceptions for better reporting of errors
  // ----------------------------------------------------------------------------------------------

  /**
   * An error listener wrapping the XSLT engines default listener and recording occurring errors 
   * as XML so that they can be used. 
   * 
   * @author Christophe Lauret
   * @version 8 February 2010
   */
  private static class UIErrorListener implements ErrorListener {

    /** XML errors are recorded here */
    private StringBuilder xml = new StringBuilder();

    /**
     * {@inheritDoc}
     */
    public void fatalError(TransformerException exception) throws TransformerException {
      this.xml.append(toXML(exception, "fatal"));
      LOGGER.error("Fatal error captured by transformer: {}", exception.getMessageAndLocation());
      LOGGER.error("Fatal error - additional details: {}", exception);
    }

    /**
     * {@inheritDoc}
     */
    public void warning(TransformerException exception) throws TransformerException {
      this.xml.append(toXML(exception, "warning"));
      LOGGER.warn("Warning captured by transformer: {}", exception.getMessageAndLocation());
      LOGGER.warn("Warning - additional details: {}", exception);
    }

    /**
     * {@inheritDoc}
     */
    public void error(TransformerException exception) throws TransformerException {
      this.xml.append(toXML(exception, "error"));
      LOGGER.error("Error captured by transformer: {}", exception.getMessageAndLocation());
      LOGGER.error("Error - additional details: {}", exception);
    }

    /**
     * Returns the transform exception as XML
     * 
     * @param ex   the source locator.
     * @param type the type of error.
     * @return the corresponding XML.
     */
    private static String toXML(TransformerException ex, String type) {
      StringBuilder xml = new StringBuilder();
      xml.append("<error type=\"").append(type).append("\">");
      xml.append(toXML(ex.getLocator()));
      String message = ex.getMessage();
      xml.append("<message>").append(XMLUtils.escape(message)).append("</message>");
      Throwable cause = ex.getCause();
      if (cause != null) {
        xml.append("<cause>").append(XMLUtils.escape(cause.getMessage())).append("</cause>");
      }
      xml.append("</error>");
      return xml.toString();
    }

    /**
     * Returns the source locator as XML
     * 
     * @param locator the source locator.
     * @return the corresponding XML.
     */
    private static String toXML(SourceLocator locator) {
      if (locator == null) return "";
      StringBuilder xml = new StringBuilder();
      int line = locator.getLineNumber();
      int column = locator.getColumnNumber();
      String publicId = locator.getPublicId();
      String systemId = locator.getSystemId();
      xml.append("<location");
      if (line != -1) xml.append(" line=\"").append(line).append('"');
      if (column != -1) xml.append(" column=\"").append(column).append('"');
      if (publicId != null) xml.append(" public-id=\"").append(publicId).append('"');
      if (systemId != null) {
        if (systemId.indexOf("WEB-INF") != -1) systemId = systemId.substring(systemId.indexOf("WEB-INF"));
        xml.append(" system-id=\"").append(toWebPath(systemId)).append('"');
      }
      xml.append("/>");
      return xml.toString();
    }

  }

  /**
   * Extends the transformer exception to preserve API and include additional details.
   * 
   * @author Christophe Lauret
   * @version 8 February 2010
   */
  private static class UITransformerException extends TransformerException {

    /** Holds the error details as XML. */
    private final String _xml;

    /**
     * Creates a new UI transformation exception wrapping an existing one.
     * 
     * @param ex  the wrapped transformer exception.
     * @param xml the error details as XML.
     */
    public UITransformerException(TransformerException ex, String xml) {
      super(ex);
      this._xml = xml;
    }

    /**
     * Returns the errors as XML.
     * @return the errors as XML.
     */
    public String getErrorsAsXML() {
      return this._xml;
    }

  }

  /**
   * Extends the transformer exception to preserve API and include additional details.
   * 
   * @author Christophe Lauret
   * @version 8 February 2010
   */
  private static class UITransformerConfigurationException extends TransformerConfigurationException {

    /** Holds the error details as XML. */
    private final String _xml;

    /**
     * Creates a new UI transformation exception wrapping an existing one.
     * 
     * @param ex  the wrapped transformer exception.
     * @param xml the error details as XML.
     */
    public UITransformerConfigurationException(TransformerConfigurationException ex, String xml) {
      super(ex.getMessage(), ex.getLocator(), ex.getException());
      this._xml = xml;
    }

    /**
     * Returns the errors as XML.
     * @return the errors as XML.
     */
    public String getErrorsAsXML() {
      return this._xml;
    }
  }

}
