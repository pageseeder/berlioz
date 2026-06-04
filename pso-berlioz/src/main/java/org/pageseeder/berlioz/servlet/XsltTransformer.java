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
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.io.FileNotFoundException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.BerliozErrorID;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.aeson.JSONResult;
import org.pageseeder.berlioz.content.Service;
import org.pageseeder.berlioz.util.*;
import org.pageseeder.berlioz.xml.Xml;
import org.pageseeder.berlioz.xslt.XsltTemplateCache;
import org.pageseeder.berlioz.xslt.XsltErrorCollector;
import org.pageseeder.berlioz.xslt.XsltExceptionWrapper;
import org.pageseeder.xmlwriter.XMLWriter;
import org.pageseeder.xmlwriter.XMLWriterImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

/**
 * Performs the XSLT transformation from the generated XML content.
 *
 * <p>Template compilation and caching is delegated to {@link XsltTemplateCache}; this class
 * handles the HTTP-specific layer: extracting XSLT parameters from the request, setting up
 * the XML source, executing the transformation, and rendering errors.
 *
 * <p>Use the global property {@code berlioz.xslt.cache} to control caching behavior.
 *
 * @author Christophe Lauret
 *
 * @version 0.13.1
 * @since 0.7
 */
public final class XsltTransformer {

  private static final Logger LOGGER = LoggerFactory.getLogger(XsltTransformer.class);

  /**
   * Compiled failsafe stylesheet — loaded once from the classpath, never changes.
   */
  private static final Templates FAILSAFE_TEMPLATES;
  static {
    ClassLoader loader = XsltTransformer.class.getClassLoader();
    URL url = loader.getResource("org/pageseeder/berlioz/xslt/failsafe-error-html.xsl");
    FAILSAFE_TEMPLATES = XsltTemplateCache.compile(url);
  }

  /**
   * Handles template compilation and caching for this transformer's stylesheet.
   */
  private final XsltTemplateCache cache;

  /**
   * Creates a new XSLT Transformer with no fallback templates.
   *
   * @param templates The location of the templates.
   */
  public XsltTransformer(Path templates) {
    this(templates, null);
  }

  /**
   * Creates a new XSLT Transformer.
   *
   * @param templates The location of the templates.
   * @param fallback  The URL to the fallback templates (optional).
   */
  public XsltTransformer(Path templates, @Nullable URL fallback) {
    Objects.requireNonNull(templates, "The template path is required");
    this.cache = new XsltTemplateCache(templates, fallback);
  }

  /**
   * Creates a new XSLT Transformer with no fallback templates.
   *
   * @param templates The location of the templates.
   * @deprecated Use {@link #XsltTransformer(Path)} instead.
   */
  @Deprecated(since = "0.13.1")
  public XsltTransformer(File templates) {
    this(templates.toPath(), null);
  }

  /**
   * Creates a new XSLT Transformer.
   *
   * @param templates The location of the templates.
   * @param fallback  The URL to the fallback templates (optional).
   * @deprecated Use {@link #XsltTransformer(Path, URL)} instead.
   */
  @Deprecated(since = "0.13.1")
  public XsltTransformer(File templates, @Nullable URL fallback) {
    this(templates.toPath(), fallback);
  }

  /**
   * Transforms the specified XML content using XSLT.
   *
   * @param content The XML content to transform.
   * @param req     The HTTP servlet request.
   * @param service Required only to provide more information in the logs in case of errors.
   *
   * @return the results of the transformation.
   */
  public XsltTransformResult transform(String content, HttpServletRequest req, Service service) {
    StringWriter buffer = new StringWriter();
    long time;
    Templates templates;
    Map<String, String> parameters = toParameters(req);

    try {
      // Creates a transformer from the templates
      templates = this.cache.getTemplates();

      // Set up the source
      Source source = toXMLSource(content, req, service);

      // Set up the result
      StreamResult result = new StreamResult(buffer);

      // Transform!
      time = execute(source, result, templates, parameters);

      // very likely to be an error in the XML or a dynamic error
    } catch (TransformerException ex) {
      String error = toXML(ex, parameters);
      // Try to use the fail-safe template to present the error
      error = transformFailSafe(error, FAILSAFE_TEMPLATES);
      return new XsltTransformResult(error, ex, FAILSAFE_TEMPLATES);
    }

    // All good!
    return new XsltTransformResult(buffer.toString(), time, templates);
  }

  /**
   * Performs a fail-safe transformation using the internal templates.
   *
   * @param content The XML to transform.
   * @param url     The URL to use.
   *
   * @return the content transformed safely.
   */
  public static String transformFailSafe(String content, URL url) {
    return transformFailSafe(content, XsltTemplateCache.compile(url));
  }

  /**
   * Returns the path used by this transformer to locate the templates.
   *
   * @return the path to the templates file.
   */
  public Path templatesPath() {
    return this.cache.templatesPath();
  }

  /**
   * Returns the file used by this transformer to produce the templates.
   *
   * @return the file used by this transformer to produce the templates.
   * @deprecated Use {@link #templatesPath()} instead.
   */
  @Deprecated(since = "0.13.1")
  public File templates() {
    return this.cache.templatesPath().toFile();
  }

  /**
   * Returns an ETag corresponding to the templates.
   *
   * @return an ETag corresponding to the templates.
   */
  public @Nullable String getEtag() {
    return this.cache.getEtag();
  }

  /**
   * Clears the cached entry for this transformer's stylesheet.
   */
  public void clearCache() {
    this.cache.clearCache();
  }

  /**
   * Clears all cached XSLT template entries.
   */
  public static void clearAllCache() {
    XsltTemplateCache.clearAllCache();
  }

  // private helpers
  // ----------------------------------------------------------------------------------------------

  /**
   * Executes the transformation from {@code source} into {@code result} using {@code templates}.
   *
   * @return the elapsed nanoseconds.
   * @throws TransformerException on any XSLT error.
   */
  private static long execute(Source source, StreamResult result, Templates templates,
      @Nullable Map<String, String> parameters) throws TransformerException {

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
    XsltErrorCollector listener = new XsltErrorCollector(LOGGER);
    transformer.setErrorListener(listener);
    try {
      transformer.transform(source, r);
    } catch (TransformerException ex) {
      throw new XsltExceptionWrapper(ex, listener);
    }
    return System.nanoTime() - before;
  }

  /**
   * Returns the XSLT parameters for the transformer from HTTP parameters starting with {@code xsl-}.
   */
  private static Map<String, String> toParameters(ServletRequest req) {
    // Adding parameters from HTTP parameters
    Map<String, String> p = null;
    final int xsl_prefix = 4;
    for (Enumeration<?> names = req.getParameterNames(); names.hasMoreElements();) {
      String name = (String) names.nextElement();
      String value = req.getParameter(name);
      if (name != null && value != null && name.startsWith("xsl-")) {
        if (p == null) p = new HashMap<>();
        p.put(name.substring(xsl_prefix), value);
      }
    }
    // Return parameters
    return p != null ? p : Map.of();

  }

  /**
   * Creates a SAX source that does not resolve external entities.
   *
   * @throws TransformerException if the parser cannot be configured.
   */
  private static Source toXMLSource(String xml) throws TransformerException {
    return toXMLSource(xml, null, null);
  }

  /**
   * Creates a SAX source that does not resolve external entities.
   *
   * @throws TransformerException if the parser cannot be configured.
   */
  private static Source toXMLSource(String xml, @Nullable HttpServletRequest req, @Nullable Service service)
      throws TransformerException {
    try {
      XMLReader reader = Xml.newSafeParser(false).getXMLReader();
      reader.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
      InputSource input = new InputSource(new StringReader(xml));
      if (service != null) {
        input.setPublicId("-//Berlioz//Service/XML/" + service.group() + "/" + service.id());
      }
      if (req != null) {
        String uri = req.getRequestURI();
        if (uri.lastIndexOf('.') >= 0) {
          input.setSystemId(uri.replaceAll("\\.([a-z]+)$", ".src"));
        }
      }
      return new SAXSource(reader, input);
    } catch (ParserConfigurationException | SAXException ex) {
      throw new TransformerConfigurationException("Unable to configure XML parser", ex);
    }
  }

  // Error Handling
  // ----------------------------------------------------------------------------------------------

  /**
   * Serializes transformation error details as XML for use with the fail-safe stylesheet.
   */
  private static String toXML(TransformerException ex, @Nullable Map<String, String> parameters) {
    StringWriter out = new StringWriter();
    try {
      XMLWriter xml = new XMLWriterImpl(out);
      xml.openElement("server-error");
      xml.attribute("http-code", HttpServletResponse.SC_SERVICE_UNAVAILABLE);
      xml.attribute("datetime", ISO8601.format(System.currentTimeMillis(), ISO8601.DATETIME));

      TransformerException actual = ex;
      XsltErrorCollector collector = null;

      if (ex instanceof XsltExceptionWrapper) {
        XsltExceptionWrapper wrapper = (XsltExceptionWrapper) ex;
        TransformerException wrapped = (TransformerException) wrapper.getException();
        if (wrapped != null) {
          actual = wrapped;
          collector = wrapper.collector();
        }
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
   * Performs a fail-safe transformation. If {@code templates} are the identity templates
   * or if the transformation itself fails, the source XML is returned verbatim.
   */
  private static String transformFailSafe(String xml, Templates templates) {
    // No need to process, let's directly copy the output
    if (XsltTemplateCache.isIdentity(templates)) return xml;
    // Let's try to format it
    try {
      Source source = toXMLSource(xml);
      StringWriter html = new StringWriter();
      templates.newTransformer().transform(source, new StreamResult(html));
      return html.toString();
    } catch (TransformerException disaster) {
      LOGGER.error("Fail-safe stylesheet failed — returning error details as XML: {}", disaster.getMessageAndLocation());
      // Fail-safe failed!
      return xml;
    } catch (Exception catastrophe) {
      LOGGER.error("Fail-safe stylesheet failed — returning error details as XML", catastrophe);
      // Fail-safe failed!
      return xml;
    }
  }

  /**
   * Guesses the Berlioz error ID from the transformer exception.
   */
  private static BerliozErrorID toErrorID(TransformerException ex) {
    if (ex instanceof TransformerConfigurationException) {
      Throwable cause = ex.getCause();
      if (cause instanceof NoSuchFileException || cause instanceof FileNotFoundException) return BerliozErrorID.TRANSFORM_NOT_FOUND;
      return BerliozErrorID.TRANSFORM_INVALID;
    }
    if (ex.getCause() instanceof SAXParseException) return BerliozErrorID.TRANSFORM_MALFORMED_SOURCE_XML;
    return BerliozErrorID.TRANSFORM_DYNAMIC_ERROR;
  }

  private static String toTitle(BerliozErrorID id) {
    switch (id) {
      case TRANSFORM_NOT_FOUND:            return "XSLT Not Found";
      case TRANSFORM_INVALID:              return "XSLT Static Error";
      case TRANSFORM_DYNAMIC_ERROR:        return "XSLT Dynamic Error";
      case TRANSFORM_MALFORMED_SOURCE_XML: return "XML is not well formed";
      default: return "Unidentified XSLT error!";
    }
  }

}
