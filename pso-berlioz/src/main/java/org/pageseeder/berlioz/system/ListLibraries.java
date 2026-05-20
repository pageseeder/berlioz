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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.servlet.HttpContentRequest;
import org.pageseeder.xmlwriter.XMLWriter;

/**
 * List the Java libraries in use in the application.
 *
 * <p>This generator scans the <code>/WEB-INF/lib/</code> folder of the current Web application
 * for <code>.jar</code> files and extracts the metadata from their manifest.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.11.2
 * @since Berlioz 0.9.32
 */
@Beta
public final class ListLibraries implements ContentGenerator {

  /**
   * Maps the library paths to the main attributes of the manifest.
   *
   * We cap the amount of entries to 100 to avoid potential memory leaks.
   */
  private final Map<String, Map<String, String>> manifest = createLRUMap(100);

  @Override
  public void process(ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    HttpServletRequest http = ((HttpContentRequest)req).getHttpRequest();
    ServletContext context = http.getServletContext();
    extractLibs(context, xml);
  }

  private void extractLibs(ServletContext context, XMLWriter xml) throws IOException {

    // TODO: Use Weak cache to avoid having to reopen all libs, reload on Berlioz Reload
    Set<String> paths = context.getResourcePaths("/WEB-INF/lib");

    xml.openElement("libraries");
    if (paths != null) {

      // List all the jars
      for (String path : paths) {

        String filename = path.indexOf('/')>=0? path.substring(path.lastIndexOf('/')) : path;

        // Get the name and version from the file name
        int dot = filename.lastIndexOf('.');
        int dash = filename.lastIndexOf('-');
        String name = dash != -1? filename.substring(0, dash) : filename.substring(0, dot);
        String version = dash != -1? filename.substring(dash+1, dot) : null;

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
  private Map<String, String> getMainAttributes(String path, ServletContext context) {
    Map<String, String> attributes = this.manifest.get(path);
    if (attributes == null) {
      attributes = loadMainAttributes(path, context);
      this.manifest.put(path, attributes);
    }
    return attributes;
  }

  /**
   * Loads the main attributes from the manifest of the jat corresponding to the specified path
   *
   * @param path    The path to the Jar
   * @param context The servlet context
   *
   * @return Always a Map.
   */
  private static Map<String, String> loadMainAttributes(String path, ServletContext context) {
    Map<String, String> m = new HashMap<>();
    try (InputStream in = context.getResourceAsStream(path)) {
      if (in != null) {
        try (JarInputStream jar = new JarInputStream(in)) {
          Manifest manifest = jar.getManifest();
          if (manifest != null) {
            Attributes attributes = manifest.getMainAttributes();
            for (Entry<Object, Object> e : attributes.entrySet()) {
              String key = e.getKey().toString().toLowerCase();
              Object o = e.getValue();
              if (o != null) {
                m.put(key, o.toString());
              }
            }
          }
        }
      }
    } catch (IOException ex) {
      // TODO We should log this
    }
    return m;
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
    Map<String, List<String>> keys = new HashMap<>();
    for (Entry<String, String> entry : attributes.entrySet()) {
      String key = entry.getKey();
      int dash = key.indexOf('-');
      if (dash == -1) {
        // Just add as an attribute
        xml.attribute(key.toLowerCase(), Objects.toString(entry.getValue(), ""));
      } else {
        // Sort the composed manifest attributes
        String category = key.substring(0, dash);
        String value = key.substring(dash+1);
        List<String> values = keys.computeIfAbsent(category, k -> new ArrayList<>());
        values.add(value);
      }
    }

    // Elements
    for (Entry<String, List<String>> e : keys.entrySet()) {
      xml.openElement(e.getKey().toLowerCase());
      for (String key :  e.getValue()) {
        String value = attributes.get(e.getKey()+'-'+key);
        xml.attribute(key.toLowerCase(), Objects.toString(value, ""));
      }
      xml.closeElement();
    }

  }

  @SuppressWarnings("serial")
  private static <K, V> Map<K, V> createLRUMap(final int maxEntries) {
    return new LinkedHashMap<K, V>(maxEntries*10/7, 0.7f, true) {
      @Override
      protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxEntries;
      }
    };
  }

}
