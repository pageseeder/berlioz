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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;

import org.pageseeder.xmlwriter.XMLWriter;

/**
 * List the Java libraries in use in the application.
 *
 * <p>This generator scans the <code>/WEB-INF/lib/</code> folder of the current Web application
 * for <code>.jar</code> files and extracts the metadata from their manifest.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.32
 * @since Berlioz 0.9.32
 */
@Beta
public final class ListLibraries implements ContentGenerator {

  /**
   * Only accepts files ending with ".jar".
   */
  private static final FileFilter JAR_FILES = new FileFilter() {

    @Override
    public boolean accept(File file) {
      return file.isFile() && file.getName().endsWith(".jar");
    }

  };

  @Override
  public void process(ContentRequest req, XMLWriter xml) throws BerliozException, IOException {

    File lib = req.getEnvironment().getPrivateFile("lib");
    xml.openElement("libraries");
    if (lib.exists() && lib.isDirectory()) {

      // List all the jars
      File[] jars = lib.listFiles(JAR_FILES);

      // Iterate over each library
      for (File f : jars) {
        String filename = f.getName();

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

        JarFile jar = null;
        try {
          jar = new JarFile(f);
          Manifest manifest = jar.getManifest();
          Attributes attributes = manifest.getMainAttributes();
          getAll(xml, attributes);

        } finally {
          if (jar != null) {
            jar.close();
          }
        }

        xml.closeElement();
      }

    }
    xml.closeElement();
  }

  /**
   * Extracts all the attributes of the manifest as XML
   *
   * @param xml        The XML
   * @param attributes The attributes from the Manifest
   *
   * @throws IOException If an error occurs while writing the XML.
   */
  private static void getAll(XMLWriter xml, Attributes attributes) throws IOException {
    Map<String, List<String>> keys = new HashMap<String, List<String>>();
    for (Object o : attributes.keySet()) {
      String key = o.toString();
      int dash = key.indexOf('-');
      if (dash == -1) {
        // Just add as an attribute
        xml.attribute(key.toLowerCase(), attributes.getValue(key));
      } else {
        // Sort the composed manifest attributes
        String category = key.substring(0, dash);
        String value = key.substring(dash+1);
        List<String> values = keys.get(category);
        if (values == null) {
          values = new ArrayList<String>();
          keys.put(category, values);
        }
        values.add(value);
      }
    }

    // Elements
    for (Entry<String, List<String>> e : keys.entrySet()) {
      xml.openElement(e.getKey().toLowerCase());
      for (String key :  e.getValue()) {
        String value = attributes.getValue(e.getKey()+'-'+key);
        xml.attribute(key.toLowerCase(), value);
      }
      xml.closeElement();
    }

  }

}
