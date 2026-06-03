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
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.BerliozErrorID;
import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.XSLTCacheMode;
import org.pageseeder.berlioz.aeson.JSONResult;
import org.pageseeder.berlioz.content.Service;
import org.pageseeder.berlioz.util.*;
import org.pageseeder.berlioz.xml.Xml;
import org.pageseeder.berlioz.xslt.XSLTErrorCollector;
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
 * <p>By default, all XSLT templates are cached; use the global property {@code berlioz.xslt.cache}
 * to change this behavior.
 *
 * @author Christophe Lauret
 *
 * @version 0.13.1
 * @since 0.7
 */
public final class XSLTransformer {

  /**
   * Displays debug information.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(XSLTransformer.class);

  /**
   * How long (ms) to wait between filesystem staleness checks in AUTO mode.
   */
  private static final long AUTO_CHECK_INTERVAL_MS = 500L;

  /**
   * Maps XSLT template paths to their cached entry for easy retrieval.
   */
  private static final Map<Path, CachedEntry> CACHE = new ConcurrentHashMap<>();

  /**
   * Identity templates for the worst-case scenario.
   */
  private static final Templates IDENTITY_TEMPLATES = new Templates() {
    @Override
    public Transformer newTransformer() throws TransformerConfigurationException {
      return newTransformerFactory().newTransformer();
    }
    @Override
    public Properties getOutputProperties() {
      return new Properties();
    }
  };

  /**
   * The location of the XSLT templates.
   *
   * <p>For example, "/WEB-INF/xslt/html/global.xsl"
   */
  private final Path templatesPath;

  /**
   * The URL to a fallback template.
   */
  private final @Nullable URL fallback;

  /**
   * An etag for these templates.
   */
  private @Nullable String etag;

  /**
   * Creates a new XSLT Transformer with no fallback templates.
   *
   * @param templates The location of the templates.
   */
  public XSLTransformer(Path templates) {
    this(templates, null);
  }

  /**
   * Creates a new XSLT Transformer.
   *
   * @param templates The location of the templates.
   * @param fallback  The URL to the fallback templates (optional)
   */
  public XSLTransformer(Path templates, @Nullable URL fallback) {
    this.templatesPath = Objects.requireNonNull(templates, "The template path is required");
    this.fallback = fallback;
    this.etag = computeEtag(templates, fallback);
  }

  /**
   * Creates a new XSLT Transformer with no fallback templates.
   *
   * @param templates The location of the templates.
   * @deprecated Use {@link #XSLTransformer(Path)} instead.
   */
  @Deprecated(since = "0.13.1")
  public XSLTransformer(File templates) {
    this(templates.toPath(), null);
  }

  /**
   * Creates a new XSLT Transformer.
   *
   * @param templates The location of the templates.
   * @param fallback  The URL to the fallback templates (optional)
   * @deprecated Use {@link #XSLTransformer(Path, URL)} instead.
   */
  @Deprecated(since = "0.13.1")
  public XSLTransformer(File templates, @Nullable URL fallback) {
    this(templates.toPath(), fallback);
  }

  /**
   * Transforms the specified content using XSLT.
   *
   * @param content The XML content to transform.
   * @param req     The HTTP Servlet request.
   * @param service Required only to provide more information in the logs in case of errors.
   *
   * @return the results of the transformation.
   */
  public XSLTransformResult transform(String content, HttpServletRequest req, Service service) {
    StringWriter buffer = new StringWriter();
    long time;
    Templates templates;
    Map<String, String> parameters = toParameters(req);

    try {
      // Creates a transformer from the templates
      templates = getTemplates(this.templatesPath);

      // Set up the source
      Source source = toXMLSource(content, req, service);

      // Set up the result
      StreamResult result = new StreamResult(buffer);
      // Transform!
      time = transform(source, result, templates, parameters);

      // very likely to be an error in the XML or a dynamic error
    } catch (TransformerException ex) {
      String error = toXML(ex, parameters);
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
   * Performs a fail-safe transformation using the internal templates.
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
   * Returns the path used by this transformer to locate the templates.
   *
   * @return the path to the templates file.
   */
  public Path templatesPath() {
    return this.templatesPath;
  }

  /**
   * Returns the file used by this transformer to produce the templates.
   *
   * @return the file used by this transformer to produce the templates.
   * @deprecated Use {@link #templatesPath()} instead.
   */
  @Deprecated(since = "0.13.1")
  public File templates() {
    return this.templatesPath.toFile();
  }

  /**
   * Returns an ETag corresponding to the templates.
   *
   * @return an ETag corresponding to the templates.
   */
  public @Nullable String getEtag() {
    return this.etag;
  }

  /**
   * Clears the cached entry for this transformer's stylesheet.
   */
  public synchronized void clearCache() {
    LOGGER.debug("Clearing XSLT cache.");
    CACHE.remove(this.templatesPath);
  }

  /**
   * Clears the entire XSLT template cache.
   */
  public static synchronized void clearAllCache() {
    LOGGER.debug("Clearing ALL XSLT cache.");
    CACHE.clear();
  }

// private helpers --------------------------------------------------------------------------------

  /**
   * Computes the etag for the templates by hashing the path, size, and last-modified timestamp
   * of every file in the template directory tree. Falls back to hashing the fallback URL string
   * if the templates file does not exist.
   */
  private static @Nullable String computeEtag(Path templates, @Nullable URL fallback) {
    if (!Files.exists(templates)) {
      if (fallback != null) return SHA256.hash(fallback.toString());
      LOGGER.error("Unable to find XSLT stylesheet '{}'.", templates.getFileName());
      LOGGER.error("Create a stylesheet at the path below: {}", templates);
      return null;
    }
    Path parent = templates.getParent();
    if (parent == null) return null;
    StringBuilder b = new StringBuilder();
    try (Stream<Path> stream = Files.walk(parent)) {
      List<Path> files = stream.filter(Files::isRegularFile).collect(Collectors.toList());
      for (Path f : files) {
        b.append(SHA256.hash(f, false));
      }
    } catch (IOException ex) {
      LOGGER.warn("Error thrown while trying to calculate template etag", ex);
      return null;
    }
    return SHA256.hash(b.toString());
  }

  /**
   * Utility function to transform the specified XML source and return the result.
   *
   * @param source     The Source XML data.
   * @param result     The Result XHTML data.
   * @param templates  The XSLT templates to use.
   * @param parameters Parameters to transmit to the transformer for use by the stylesheet (optional)
   *
   * @return The nano time it took to process the stylesheet.
   *
   * @throws TransformerException For XSLT transformation errors or XSLT config errors.
   */
  private static long transform(Source source, StreamResult result, Templates templates, @Nullable Map<String, String> parameters)
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
   * Returns the templates corresponding to the specified path.
   *
   * <p>This method uses the caching mechanism controlled by {@link BerliozOption#XSLT_CACHE}.
   *
   * @param p The path to the XSLT stylesheet.
   *
   * @return The corresponding templates.
   *
   * @throws TransformerException If the templates could not be parsed.
   */
  private synchronized Templates getTemplates(Path p) throws TransformerException {
    XSLTCacheMode mode = XSLTCacheMode.from(GlobalSettings.get(BerliozOption.XSLT_CACHE));
    String stylesheet = toWebPath(p.toAbsolutePath().toString());

    if (mode == XSLTCacheMode.NO) {
      LOGGER.info("Loading XSLT stylesheet '{}' [caching disabled]", stylesheet);
      return loadTemplates(p);
    }

    CachedEntry cached = CACHE.get(p);
    if (cached != null) {
      if (mode == XSLTCacheMode.MANUAL || !isStale(p, cached)) {
        return cached.templates;
      }
      LOGGER.info("XSLT stylesheet '{}' changed, reloading", stylesheet);
      CACHE.remove(p);
    } else {
      LOGGER.info("Loading XSLT stylesheet '{}' [caching {}]", stylesheet, mode);
    }

    Templates templates = loadTemplates(p);
    CACHE.put(p, new CachedEntry(templates, maxLastModified(p.getParent())));
    return templates;
  }

  /**
   * Loads and compiles templates from the given path and updates the etag.
   */
  private Templates loadTemplates(Path p) throws TransformerException {
    long t0 = System.currentTimeMillis();
    Templates templates = toTemplates(p, this.fallback);
    LOGGER.debug("Templates loaded in {}ms", System.currentTimeMillis() - t0);
    this.etag = computeEtag(p, this.fallback);
    return templates;
  }

  /**
   * Returns whether the cached entry is stale by comparing the max last-modified timestamp
   * across all files in the template directory. Updates {@code checkedAt} on every call so the
   * filesystem scan is debounced to at most once per {@link #AUTO_CHECK_INTERVAL_MS}.
   */
  private static boolean isStale(Path p, CachedEntry entry) {
    long now = System.currentTimeMillis();
    if (now - entry.checkedAt < AUTO_CHECK_INTERVAL_MS) return false;
    entry.checkedAt = now;
    return maxLastModified(p.getParent()) != entry.maxLastModified;
  }

  /**
   * Returns the highest last-modified time (in ms) across all regular files under {@code dir},
   * or {@code 0} if the directory is {@code null}, empty, or cannot be read.
   */
  private static long maxLastModified(@Nullable Path dir) {
    if (dir == null) return 0L;
    try (Stream<Path> stream = Files.walk(dir)) {
      return stream
          .filter(Files::isRegularFile)
          .mapToLong(f -> {
            try { return Files.getLastModifiedTime(f).toMillis(); }
            catch (IOException e) { return 0L; }
          })
          .max()
          .orElse(0L);
    } catch (IOException ex) {
      LOGGER.warn("Unable to scan template directory {}", dir, ex);
      return 0L;
    }
  }

  /**
   * Returns the compiled XSLT templates from the given path, falling back to the fallback URL
   * if the path does not exist.
   *
   * @param stylepath The path to the XSLT stylesheet.
   * @param fallback  The URL to the fallback XSLT stylesheet.
   *
   * @return the corresponding XSLT templates object.
   *
   * @throws TransformerException If the loading fails.
   */
  private static Templates toTemplates(Path stylepath, @Nullable URL fallback) throws TransformerException {
    Templates templates;
    try (InputStream in = Files.newInputStream(stylepath)) {
      Source source = new StreamSource(in);
      source.setSystemId(stylepath.toUri().toString());
      TransformerFactory factory = newTransformerFactory();
      XSLTErrorCollector listener = new XSLTErrorCollector(LOGGER);
      factory.setErrorListener(listener);
      templates = newTemplates(factory, source, listener);
    } catch (NoSuchFileException ex) {
      if (fallback != null) {
        LOGGER.warn("Unable to find template file: {} using fallback templates {}", stylepath, fallback);
        templates = toTemplates(fallback);
      } else {
        LOGGER.warn("Unable to find template file: {}", stylepath);
        throw new TransformerConfigurationException("Unable to find stylesheet: "+toWebPath(stylepath.toString()), ex);
      }
    } catch (IOException ex) {
      throw new TransformerConfigurationException("Unable to read stylesheet: "+toWebPath(stylepath.toString()), ex);
    }
    return templates;
  }

  private static Templates newTemplates(TransformerFactory factory, Source source, XSLTErrorCollector listener)
      throws TransformerException {
    try {
      return factory.newTemplates(source);
    } catch (TransformerConfigurationException ex) {
      throw new TransformerExceptionWrapper(ex, listener);
    }
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
      String name = (String)names.nextElement();
      String value = req.getParameter(name);
      if (name != null && value != null && name.startsWith("xsl-")) {
        if (p == null) {
          p = new HashMap<>();
        }
        p.put(name.substring(xsl_prefix), value);
      }
    }
    // Return parameters
    return p != null ? p : Map.of();
  }

  // Error Handling
  // ----------------------------------------------------------------------------------------------

  /**
   * Handles transformation errors — to be used in catch blocks.
   *
   * @param ex         An error occurring during an XSLT transformation.
   * @param parameters The XSLT parameters passed to the transformer.
   * @return the error details as XML.
   */
  private static String toXML(TransformerException ex, @Nullable Map<String, String> parameters) {
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
        TransformerException wrapped = (TransformerException)wrapper.getException();
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
   * Loads the fail-safe templates.
   *
   * @param url The URL to load (within Berlioz package).
   * @return templates, or the identity templates if loading fails.
   */
  private static Templates toTemplates(@Nullable URL url) {
    if (url == null) return IDENTITY_TEMPLATES;
    // load the templates from the URL
    try (InputStream in = url.openStream()) {
      Source source = new StreamSource(in);
      source.setSystemId(url.toString());
      TransformerFactory factory = newTransformerFactory();
      return factory.newTemplates(source);
      // Any error we need to give up...
    } catch (IOException | TransformerException ex) {
      LOGGER.warn("Unable to load fallback/failsafe templates!", ex);
      return IDENTITY_TEMPLATES;
    }
  }

  /**
   * Performs a fail-safe transformation using the built-in stylesheet.
   *
   * <p>If the transformation fails, the source XML is returned verbatim.
   *
   * @param xml       The XML to transform.
   * @param templates The fail-safe templates to use.
   *
   * @return The results of the transformation.
   */
  private static String transformFailSafe(String xml, Templates templates) {
    // No need to process, let's directly copy the output
    if (templates == IDENTITY_TEMPLATES) return xml;
    // Let's try to format it
    try {
      Source source = toXMLSource(xml);
      StringWriter html = new StringWriter();
      Result result = new StreamResult(html);
      templates.newTransformer().transform(source, result);
      return html.toString();
    } catch (TransformerException disaster) {
      LOGGER.error("Fail-safe stylesheet failed! - returning error details as XML: {}", disaster.getMessageAndLocation());
      // Fail-safe failed!
      return xml;
    } catch (Exception catastrophe) {
      LOGGER.error("Fail-safe stylesheet failed! - returning error details as XML", catastrophe);
      // Fail-safe failed!
      return xml;
    }
  }

  /**
   * Creates a hardened TransformerFactory for loading stylesheets and transforming XML.
   *
   * @return a configured transformer factory.
   *
   * @throws TransformerConfigurationException if the factory cannot enable secure processing.
   */
  @SuppressWarnings("java:S2755") // file-only access needed for xsl:import/include
  private static TransformerFactory newTransformerFactory() throws TransformerConfigurationException {
    TransformerFactory factory = TransformerFactory.newInstance();
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "file");
    return factory;
  }

  /**
   * Creates a SAX source that does not resolve external entities.
   *
   * @param xml the XML content.
   * @return the corresponding source.
   *
   * @throws TransformerException if the parser cannot be configured.
   */
  private static Source toXMLSource(String xml) throws TransformerException {
    return toXMLSource(xml, null, null);
  }

  /**
   * Creates a SAX source that does not resolve external entities.
   *
   * @param xml     the XML content.
   * @param req     the HTTP request.
   * @param service the Berlioz service being transformed.
   * @return the corresponding source.
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
        input.setPublicId("-//Berlioz//Service/XML/"+service.group()+"/"+service.id());
      }
      if (req != null) {
        String uri = req.getRequestURI();
        int dot = uri.lastIndexOf('.');
        if (dot >= 0) {
          input.setSystemId(req.getRequestURI().replaceAll("\\.([a-z]+)$", ".src"));
        }
      }
      return new SAXSource(reader, input);
    } catch (ParserConfigurationException | SAXException ex) {
      throw new TransformerConfigurationException("Unable to configure XML parser", ex);
    }
  }

  /**
   * Displays the path to the file from the web application (for debugging).
   *
   * @param s the file path string.
   * @return The path from the "WEB-INF" directory.
   */
  private static String toWebPath(String s) {
    String from = "WEB-INF";
    int x = s.indexOf(from);
    return x != -1? s.substring(x+from.length()).replace('\\', '/') : s.replace('\\', '/');
  }

  /**
   * Guesses the Berlioz Error ID from the exception thrown.
   *
   * @param ex The captured Transformer exception.
   *
   * @return the Berlioz internal error ID corresponding to the specified exception.
   */
  private static BerliozErrorID toErrorID(TransformerException ex) {
    // Let's guess the Berlioz internal code
    if (ex instanceof TransformerConfigurationException) {
      Throwable cause = ex.getCause();
      if (cause instanceof NoSuchFileException) return BerliozErrorID.TRANSFORM_NOT_FOUND;
      else return BerliozErrorID.TRANSFORM_INVALID;
    }
    if (ex.getCause() instanceof SAXParseException) return BerliozErrorID.TRANSFORM_MALFORMED_SOURCE_XML;
    return BerliozErrorID.TRANSFORM_DYNAMIC_ERROR;
  }

  /**
   * Returns a display title based on the error ID.
   *
   * @param id the error ID.
   * @return the corresponding message.
   */
  private static String toTitle(BerliozErrorID id) {
    switch (id) {
      case TRANSFORM_NOT_FOUND:            return "XSLT Not Found";
      case TRANSFORM_INVALID:              return "XSLT Static Error";
      case TRANSFORM_DYNAMIC_ERROR:        return "XSLT Dynamic Error";
      case TRANSFORM_MALFORMED_SOURCE_XML: return "XML is not well formed";
      default: return "Unidentified XSLT error!";
    }
  }

  // Inner types
  // ----------------------------------------------------------------------------------------------

  /**
   * Holds a compiled {@link Templates} object alongside the metadata used by AUTO mode to
   * detect source file changes without reading file content.
   */
  private static final class CachedEntry {

    /** The compiled templates. */
    final Templates templates;

    /** Highest last-modified time (ms) across all files in the template directory at load time. */
    final long maxLastModified;

    /** Timestamp of the last staleness check; volatile so reads across instances are coherent. */
    volatile long checkedAt;

    CachedEntry(Templates templates, long maxLastModified) {
      this.templates = templates;
      this.maxLastModified = maxLastModified;
      this.checkedAt = System.currentTimeMillis();
    }
  }

  /**
   * Extends the transformer exception to preserve API and include additional details.
   */
  private static class TransformerExceptionWrapper extends TransformerException {

    /** As required by the Serializable interface. */
    private static final long serialVersionUID = -7816677212503520650L;

    /** Holds the error details as XML. */
    private final transient XSLTErrorCollector collector;

    /**
     * Creates a new UI transformation exception wrapping an existing one.
     *
     * @param ex        the wrapped transformer exception.
     * @param collector the error details as XML.
     */
    public TransformerExceptionWrapper(TransformerException ex, XSLTErrorCollector collector) {
      super(ex);
      this.collector = collector;
    }

    /**
     * Returns the errors as XML.
     * @return the errors as XML.
     */
    public XSLTErrorCollector collector() {
      return this.collector;
    }

  }

}
