/*
 * Copyright 2015 Allette Systems (Australia)
 * http://www.allette.com.au
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pageseeder.berlioz.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.pageseeder.berlioz.BerliozErrorID;
import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.aeson.JSONResult;
import org.pageseeder.berlioz.content.Service;
import org.pageseeder.berlioz.util.CollectedError;
import org.pageseeder.berlioz.util.Errors;
import org.pageseeder.berlioz.util.ISO8601;
import org.pageseeder.berlioz.util.MD5;
import org.pageseeder.berlioz.xslt.XSLTErrorCollector;
import org.pageseeder.xmlwriter.XMLWriter;
import org.pageseeder.xmlwriter.XMLWriterImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXParseException;

/**
 * Performs the XSLT transformation from the generated XML content.
 *
 * <p>By default, all XSLT templates are cached, use the global property <code>berlioz.cache.xslt</code>
 * to change this behaviour.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.32
 * @since Berlioz 0.7
 */
public final class XSLTransformer {

  /**
   * Displays debug information.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(XSLTransformer.class);

  /**
   * Maps XSLT templates to their name for easy retrieval.
   */
  private static final Map<File, Templates> CACHE = new ConcurrentHashMap<File, Templates>();

  /**
   * The location of the XSLT templates.
   *
   * <p>For example, "/WEB-INF/xslt/html/global.xsl"
   */
  private final File _templates;

  /**
   * The URL to a fallback template.
   */
  private final URL _fallback;

  /**
   * An etag for these templates.
   */
  private transient String _etag = null;

  /**
   * Creates a new XSLT Transformer with no fallback templates.
   *
   * @param templates The location of the templates.
   */
  public XSLTransformer(File templates) {
    this(templates, null);
  }

  /**
   * Creates a new XSLT Transformer.
   *
   * @param templates The location of the templates.
   * @param fallback  The URL to the fallback templates (optional)
   */
  public XSLTransformer(File templates, URL fallback) {
    if (templates == null) throw new NullPointerException("No Templates file specified");
    this._templates = templates;
    this._fallback = fallback;
    this._etag = computeEtag(templates, fallback);
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
    Map<String, String> parameters = null;

    try {
      // Creates a transformer from the templates
      templates = getTemplates(this._templates);

      // Setup the transformer
      parameters = toParameters(req);

      // Setup the source
      StreamSource source = new StreamSource(new StringReader(content));
      source.setPublicId("-//Berlioz//Service/XML/"+service.group()+"/"+service.id());
      // TODO: provide better info (identify the service)
      String uri = req.getRequestURI();
      int dot = uri.lastIndexOf('.');
      if (dot >= 0) {
        source.setSystemId(req.getRequestURI().replaceAll(uri.substring(dot), ".src"));
      }

      // Setup the result
      StreamResult result = new StreamResult(buffer);

      // Transform!
      time = transform(source, result, templates, parameters);

    // very likely to be an error in the XML or a dynamic error
    } catch (TransformerException ex) {
      String error = toXML(ex, content, parameters);
      ClassLoader loader = XSLTransformer.class.getClassLoader();
      URL url = loader.getResource("org/pageseeder/berlioz/xslt/failsafe-error-html.xsl");
      Templates failsafe = toTemplates(url);
      // Try to use the fail-safe template to present the error
      error = transformFailSafe(error, failsafe);
      return new XSLTransformResult(error, ex, failsafe);
    }

    // All good!
    return new XSLTransformResult(buffer.toString(), time, templates);
  }

  /**
   * Performs a fail safe transformation using the internal templates.
   *
   * @param content The XML to transform.
   * @param url     The URL to use.
   *
   * @return the content transformed safely.
   */
  public static String transformFailSafe(String content, URL url) {
    Templates failsafe = toTemplates(url);
    return transformFailSafe(content, failsafe);
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
  public synchronized void clearCache() {
    LOGGER.debug("Clearing XSLT cache.");
    CACHE.remove(this._templates);
  }

  /**
   * Clears the internal XSLT cache.
   */
  public static synchronized void clearAllCache() {
    LOGGER.debug("Clearing XSLT cache.");
    CACHE.clear();
  }

// private helpers --------------------------------------------------------------------------------

  /**
   * Computes the etag for the templates.
   * @param templates The main file for the templates.
   * @return The corresponding etag.
   */
  private static String computeEtag(File templates, URL fallback) {
    if (!templates.exists()) {
      if (fallback != null) return MD5.hash(fallback.toString());
      else {
        LOGGER.error("Unable to find XSLT stylesheet '{}'.", templates.getName());
        LOGGER.error("Create a stylesheet at the path below:");
        LOGGER.error(templates.getPath());
        return null;
      }
    }
    List<File> files = new ArrayList<File>();
    listTemplateFiles(templates.getParentFile(), files);
    StringBuilder b = new StringBuilder();
    try {
      for (File f : files) { b.append(MD5.hash(f, false)); }
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
   * @return The nano time it took to process the stylesheet.
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

    // Check for JSON
    Result r = JSONResult.newInstanceIfSupported(transformer, result);

    // Process, write directly to the result
    long before = System.nanoTime();
    XSLTErrorCollector listener = new XSLTErrorCollector(LOGGER);
    transformer.setErrorListener(listener);
    try {
      transformer.transform(source, r);
    } catch (TransformerException ex) {
      throw new TransformerExceptionWrapper(ex, listener);
    }
    return System.nanoTime() - before;
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
   * @throws TransformerException If the templates could not parsed.
   */
  private synchronized Templates getTemplates(File f) throws TransformerException {
    boolean store = GlobalSettings.has(BerliozOption.XSLT_CACHE);
    String stylesheet = toWebPath(f.getAbsolutePath());
    Templates templates = store? CACHE.get(f) : null;
    if (templates == null) {
      LOGGER.info("Loading XSLT stylesheet '{}' [caching {}]", stylesheet, store? "enabled" : "disabled");
      // Generate the templates if necessary
      long t0 = System.currentTimeMillis();
      templates = toTemplates(f, this._fallback);
      long t1 = System.currentTimeMillis();
      LOGGER.debug("Templates loaded in {}ms", (t1 - t0));
      // Recalculate the Etag
      this._etag = computeEtag(f, this._fallback);
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
   * @param fallback  The URL to the fallback XSLT style sheet
   *
   * @return the corresponding XSLT templates object
   *
   * @throws TransformerException If the loading fails.
   */
  private static Templates toTemplates(File stylepath, URL fallback) throws TransformerException {
    // load the templates from the source file
    InputStream in = null;
    Templates templates = null;
    try {
      in = new FileInputStream(stylepath);
      Source source = new StreamSource(in);
      source.setSystemId(stylepath.toURI().toString());
      TransformerFactory factory = TransformerFactory.newInstance();
      XSLTErrorCollector listener = new XSLTErrorCollector(LOGGER);
      factory.setErrorListener(listener);
      try {
        templates = factory.newTemplates(source);
      } catch (TransformerConfigurationException ex) {
        throw new TransformerExceptionWrapper(ex, listener);
      }
    } catch (FileNotFoundException ex) {
      // The file does not exist
      if (fallback != null) {
        LOGGER.warn("Unable to find template file: {} using fallback templates {}", stylepath, fallback);
        templates = toTemplates(fallback);
      } else {
        LOGGER.warn("Unable to find template file: {}", stylepath);
        throw new TransformerConfigurationException("Unable to find stylesheet: "+toWebPath(stylepath.getPath()), ex);
      }
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
    final int xsl_prefix = 4;
    for (Enumeration<?> names = req.getParameterNames(); names.hasMoreElements();) {
      String param = (String)names.nextElement();
      if (param.startsWith("xsl-")) {
        if (p == null) {
          p = new HashMap<String, String>();
        }
        p.put(param.substring(xsl_prefix), req.getParameter(param));
      }
    }
    // Return parameters
    if (p != null) return p;
    else return Collections.emptyMap();
  }

  // Error Handling
  // ----------------------------------------------------------------------------------------------

  /**
   * Handles transformation errors - to be used in catch blocks.
   *
   * @param ex         An error occurring during an XSLT transformation.
   * @param source     The XML source being transformed
   * @param parameters The XSLT parameters passed to the transformer
   * @return the error details as XML
   */
  private static String toXML(TransformerException ex, String source, Map<String, String> parameters) {
    // Remove all double dash so that it may be inserted in the XML comment
    StringWriter out = new StringWriter();
    try {
      XMLWriter xml = new XMLWriterImpl(out);
      xml.openElement("server-error");
      xml.attribute("http-code", HttpServletResponse.SC_SERVICE_UNAVAILABLE);
      xml.attribute("datetime", ISO8601.format(System.currentTimeMillis(), ISO8601.DATETIME));

      // Here are the objects we'll deal with...
      TransformerException actual = ex;
      XSLTErrorCollector collector = null;

      // Unwrap if needed
      if (ex instanceof TransformerExceptionWrapper) {
        TransformerExceptionWrapper wrapper = (TransformerExceptionWrapper)ex;
        actual = (TransformerException)wrapper.getException();
        collector = wrapper.collector();
      }

      // Let's guess the Berlioz internal code
      BerliozErrorID id = toErrorID(actual);
      xml.attribute("id", id.id());

      // Berlioz info
      xml.openElement("berlioz");
      xml.attribute("version", GlobalSettings.getVersion());
      xml.closeElement();
      xml.element("title", toTitle(id));
      xml.element("message", Errors.cleanMessage(ex));

      // Generate the XML for the exception
      Errors.toXML(actual, xml);

      // Also copy the errors collected here
      if (collector != null) {
        xml.openElement("collected-errors");
        for (CollectedError<TransformerException> item : collector.getErrors()) {
          item.toXML(xml);
        }
        xml.closeElement();
      }

      // XSLT parameters
      if (parameters != null) {
        xml.openElement("parameters");
        for (Entry<String, String> p : parameters.entrySet()) {
          xml.openElement("parameter");
          xml.attribute("name", p.getKey());
          xml.attribute("value", p.getValue());
          xml.closeElement();
        }
        xml.closeElement();
      }

      xml.closeElement();
      xml.flush();
    } catch (IOException io) {
      LOGGER.warn("Unable to produce transform error details for error below:");
      LOGGER.error("An error occurred while transforming content", ex);
    }

    return out.toString();
  }

  /**
   * Loads the fail safe templates.
   * @param url The URL to load (within Berlioz Package)
   * @return templates or <code>null</code>.
   */
  private static Templates toTemplates(URL url) {
    // load the templates from the source file
    InputStream in = null;
    Templates templates = null;
    try {
      in = url.openStream();
      Source source = new StreamSource(in);
      source.setSystemId(url.toString());
      TransformerFactory factory = TransformerFactory.newInstance();
      templates = factory.newTemplates(source);
      // Any error we need to give up...
    } catch (IOException ex) {
      LOGGER.warn("Unable to load fallback/failsafe templates!", ex);
      return null;
    } catch (TransformerException ex) {
      LOGGER.warn("Unable to load fallback/failsafe templates!", ex);
      return null;
    } finally {
      closeQuietly(in);
    }
    return templates;
  }

  /**
   * Perform a fail safe transformation using the built-in stylesheet.
   *
   * <p>Note: If the transformation fails the source XML is returned verbatim as there is nothing
   * more we can do.
   *
   * @param xml       The XML to transform
   * @param templates The fail-safe templates to use.
   *
   * @return The results of the transformation.
   */
  private static String transformFailSafe(String xml, Templates templates) {
    // Let's try to format it
    String out = null;
    try {
      Source source = new StreamSource(new StringReader(xml));
      StringWriter html = new StringWriter();
      Result result = new StreamResult(html);
      templates.newTransformer().transform(source, result);
      out = html.toString();
    } catch (TransformerException disaster) {
      LOGGER.error("Fail-safe stylesheet failed! - returning error details as XML", disaster.getMessageAndLocation());
      // Fail-safe failed!
      out = xml;
    } catch (Exception catastrophe) {
      LOGGER.error("Fail-safe stylesheet failed! - returning error details as XML", catastrophe);
      // Fail-safe failed!
      out = xml;
    }
    return out;
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
    return x != -1? s.substring(x+from.length()).replace('\\', '/') : s.replace('\\', '/');
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
   * Guess the Berlioz Error ID from the exception thrown.
   *
   * @param ex The captured Transformer exception.
   *
   * @return the Berlioz internal error ID corresponding to the specified exception.
   */
  private static BerliozErrorID toErrorID(TransformerException ex) {
    // Let's guess the Berlioz internal code
    if (ex instanceof TransformerConfigurationException) {
      if (ex.getCause() instanceof FileNotFoundException) return BerliozErrorID.TRANSFORM_NOT_FOUND;
      else
        return BerliozErrorID.TRANSFORM_INVALID;
    }
    if (ex.getCause() instanceof SAXParseException) return BerliozErrorID.TRANSFORM_MALFORMED_SOURCE_XML;
    else
      return BerliozErrorID.TRANSFORM_DYNAMIC_ERROR;
  }

  /**
   * Return the title ID based on the ID.
   *
   * @param id the ID
   * @return the corresponding message
   */
  private static String toTitle(BerliozErrorID id) {
    switch (id) {
      case TRANSFORM_NOT_FOUND:            return "XSLT Not Found";
      case TRANSFORM_INVALID:              return "XSLT Static Error";
      case TRANSFORM_DYNAMIC_ERROR:        return "XSLT Dynamic Error";
      case TRANSFORM_MALFORMED_SOURCE_XML: return "XML is not well formed";
      default: return "Unindentified XSLT error!";
    }
  }

  // Listeners and exceptions for better reporting of errors
  // ----------------------------------------------------------------------------------------------

  /**
   * Extends the transformer exception to preserve API and include additional details.
   *
   * @author Christophe Lauret
   * @version 8 February 2010
   */
  private static class TransformerExceptionWrapper extends TransformerException {

    /** As required by the Serializable interface. */
    private static final long serialVersionUID = -7816677212503520650L;

    /** Holds the error details as XML. */
    private final XSLTErrorCollector _collector;

    /**
     * Creates a new UI transformation exception wrapping an existing one.
     *
     * @param ex        the wrapped transformer exception.
     * @param collector the error details as XML.
     */
    public TransformerExceptionWrapper(TransformerException ex, XSLTErrorCollector collector) {
      super(ex);
      this._collector = collector;
    }

    /**
     * Returns the errors as XML.
     * @return the errors as XML.
     */
    public XSLTErrorCollector collector() {
      return this._collector;
    }

  }

}
