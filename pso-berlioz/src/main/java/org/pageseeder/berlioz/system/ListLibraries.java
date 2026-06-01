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
package org.pageseeder.berlioz.system;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.servlet.HttpContentRequest;
import org.pageseeder.xmlwriter.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * List the Java libraries in use in the application.
 *
 * <p>This generator scans the <code>/WEB-INF/lib/</code> folder of the current Web application
 * for <code>.jar</code> files and extracts the metadata from their manifest.
 *
 * @author Christophe Lauret
 *
 * @version 0.11.2
 * @since 0.9.32
 */
@Beta
public final class ListLibraries implements ContentGenerator {

  /**
   * Logger for this generator.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(ListLibraries.class);

  /**
   * Path to the web application libraries.
   */
  @SuppressWarnings("java:S1075") // Servlet spec-defined path, not a configurable URI
  private static final String LIBRARIES_PATH = "/WEB-INF/lib/";

  /**
   * Maximum number of manifests to retain per servlet context.
   */
  private static final int MAX_CACHED_MANIFESTS = 100;

  /**
   * Maps each servlet context to its cached manifest attributes.
   *
   * <p>Servlet containers may keep generator instances around for a long time, so the outer map
   * uses weak keys to avoid retaining undeployed applications. Each context cache is still capped
   * to prevent unbounded growth when a web application contains many or changing libraries.
   */
  private static final Map<ServletContext, Map<String, Map<String, String>>> MANIFESTS =
      new WeakHashMap<>();

  static {
    GlobalSettings.registerListener(ListLibraries::clearCache);
  }

  @Override
  public void process(ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    HttpServletRequest http = ((HttpContentRequest)req).getHttpRequest();
    ServletContext context = http.getServletContext();
    extractLibs(context, xml);
  }

  /**
   * Extracts the libraries from the servlet context as XML.
   *
   * @param context The servlet context to inspect.
   * @param xml     The XML output.
   *
   * @throws IOException If an error occurs while writing the XML.
   */
  void extractLibs(ServletContext context, XMLWriter xml) throws IOException {

    List<String> paths = getLibraryPaths(context);

    xml.openElement("libraries");
    for (String path : paths) {

      String filename = filename(path);
      String base = filename.substring(0, filename.length() - ".jar".length());
      int dash = base.lastIndexOf('-');
      String name = dash > 0 && dash < base.length() - 1 ? base.substring(0, dash) : base;
      String version = dash > 0 && dash < base.length() - 1 ? base.substring(dash+1) : null;

      // Start writing out the XML
      xml.openElement("library");
      xml.attribute("file", filename);
      xml.attribute("name", name);
      if (version != null) {
        xml.attribute("version", version);
      }

      // Get attributes
      Map<String, String> attributes = getMainAttributes(path, context);
      toXML(xml, attributes);

      xml.closeElement();
    }
    xml.closeElement();
  }

  /**
   * Get the attributes from the cache if available otherwise parse the jar.
   *
   * @param path    The path to the Jar
   * @param context The servlet context
   *
   * @return the attributes
   */
  private static Map<String, String> getMainAttributes(String path, ServletContext context) {
    Map<String, Map<String, String>> cache = cache(context);
    synchronized (cache) {
      Map<String, String> attributes = cache.get(path);
      if (attributes != null) {
        return attributes;
      }
    }

    Map<String, String> loaded = loadMainAttributes(path, context);
    synchronized (cache) {
      Map<String, String> attributes = cache.get(path);
      if (attributes != null) {
        return attributes;
      }
      cache.put(path, loaded);
      return loaded;
    }
  }

  /**
   * Loads the main attributes from the manifest of the jar corresponding to the specified path.
   *
   * @param path    The path to the Jar
   * @param context The servlet context
   *
   * @return Always a Map.
   */
  private static Map<String, String> loadMainAttributes(String path, ServletContext context) {
    Map<String, String> m = new TreeMap<>();
    try (InputStream in = context.getResourceAsStream(path)) {
      if (in != null) {
        try (JarInputStream jar = new JarInputStream(in)) {
          Manifest manifest = jar.getManifest();
          if (manifest != null) {
            Attributes attributes = manifest.getMainAttributes();
            for (Entry<Object, Object> e : attributes.entrySet()) {
              String key = e.getKey().toString().toLowerCase(Locale.ROOT);
              Object o = e.getValue();
              if (o != null) {
                m.put(key, o.toString());
              }
            }
          }
        }
      } else {
        LOGGER.debug("Unable to open library {} from servlet context", path);
      }
    } catch (IOException ex) {
      LOGGER.warn("Unable to read manifest attributes from {}", path, ex);
    }
    return Map.copyOf(m);
  }


  /**
   * Extracts all the attributes of the manifest as XML
   *
   * @param xml        The XML
   * @param attributes The attributes from the Manifest
   *
   * @throws IOException If an error occurs while writing the XML.
   */
  private static void toXML(XMLWriter xml, Map<String, String> attributes) throws IOException {
    Map<String, String> xmlAttributes = new TreeMap<>();
    Map<String, Map<String, String>> elements = new TreeMap<>();
    for (Entry<String, String> entry : attributes.entrySet()) {
      String key = entry.getKey();
      int dash = key.indexOf('-');
      if (dash == -1) {
        xmlAttributes.put(key.toLowerCase(Locale.ROOT), Objects.toString(entry.getValue(), ""));
      } else {
        String category = key.substring(0, dash);
        String value = key.substring(dash+1);
        Map<String, String> values = elements.computeIfAbsent(category, k -> new TreeMap<>());
        values.put(value, Objects.toString(entry.getValue(), ""));
      }
    }

    // Attributes must be written before any child element.
    for (Entry<String, String> e : xmlAttributes.entrySet()) {
      xml.attribute(e.getKey(), e.getValue());
    }

    // Elements
    for (Entry<String, Map<String, String>> e : elements.entrySet()) {
      xml.openElement(e.getKey().toLowerCase(Locale.ROOT));
      for (Entry<String, String> attribute : e.getValue().entrySet()) {
        xml.attribute(attribute.getKey().toLowerCase(Locale.ROOT), attribute.getValue());
      }
      xml.closeElement();
    }

  }

  /**
   * Clears all cached manifests.
   *
   * <p>This method is registered as a {@link GlobalSettings} listener so Berlioz reloads also
   * force the next request to re-read library manifests.
   */
  static void clearCache() {
    synchronized (MANIFESTS) {
      MANIFESTS.clear();
    }
  }

  private static Map<String, Map<String, String>> cache(ServletContext context) {
    synchronized (MANIFESTS) {
      Map<String, Map<String, String>> cache = MANIFESTS.get(context);
      if (cache == null) {
        cache = createLRUMap(MAX_CACHED_MANIFESTS);
        MANIFESTS.put(context, cache);
      }
      return cache;
    }
  }

  private static List<String> getLibraryPaths(ServletContext context) {
    Set<String> resources = context.getResourcePaths(LIBRARIES_PATH);
    if (resources == null || resources.isEmpty()) return List.of();
    return resources.stream()
        .filter(ListLibraries::isJar)
        .sorted()
        .collect(Collectors.toList());
  }

  private static boolean isJar(String path) {
    return !path.endsWith("/") && path.toLowerCase(Locale.ROOT).endsWith(".jar");
  }

  private static String filename(String path) {
    int slash = path.lastIndexOf('/');
    return slash >= 0 ? path.substring(slash+1) : path;
  }

  private static <K, V> Map<K, V> createLRUMap(final int maxEntries) {
    return new LinkedHashMap<K, V>(maxEntries*10/7, 0.7f, true) {
      @Override
      protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxEntries;
      }
    };
  }

}
