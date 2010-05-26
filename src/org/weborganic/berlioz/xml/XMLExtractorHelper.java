/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.xml;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weborganic.berlioz.BerliozException;

import com.topologi.diffx.xml.XMLWriter;
import com.topologi.diffx.xml.XMLWriterImpl;

/**
 * This manager provides a global tool for processing content for the Website.
 * 
 * @author Christophe Lauret (Allette Systems)
 * @version 20 May 2010
 */
public final class XMLExtractorHelper {

  /**
   * Logger the extractor.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(XMLExtractorHelper.class);

  /**
   * Internal cache. 
   */
  private static final Map<String, String> CACHE = new Hashtable<String, String>();
  // TODO: would be better to use a library like EH Cache

  /**
   * Prevent creation of instances.
   */
  private XMLExtractorHelper() {
  }

  /**
   * Gets the content from the file system.
   * 
   * Any error is reported as XML on the XML writer.
   * 
   * @param file  The file.
   * @param xml   The XML writer.
   * @param cache <code>true</code> to enable caching for the resource;
   *              <code>false</code> to load the file directly.
   * 
   * @throws IOException should an error occur
   */
  public static void extract(File file, XMLWriter xml, boolean cache) throws IOException {
    URL url = toURL(file);
    if (url == null) return;

    // the item is cached
    if (cache && CACHE.containsKey(url.toString())) {
      LOGGER.debug("Reading "+url.toString()+" from cache");
      xml.writeXML(CACHE.get(url.toString()).toString());

    // the item isn't cached or caching is disabled
    } else {

      // load
      if (file.exists()) {
        try {
          // writers to use
          StringWriter writer = new StringWriter();
          XMLWriter internal = new XMLWriterImpl(writer);

          // extract the data
          XMLUtils.parse(new XMLExtractor(internal), file, false);
          internal.flush();
          String parsed = writer.toString();

          // write to XML writer
          xml.writeXML(parsed);
          if (cache) {
            LOGGER.info("Caching "+url.toString());
            CACHE.put(url.toString(), parsed);
          }

        // an error was reported by the parser
        } catch (BerliozException ex) {
          LOGGER.warn("An error was reported by the parser while parsing"+url, ex);
          xml.openElement("no-data");
          xml.attribute("error", "parsing");
          xml.attribute("details", ex.getMessage());
          xml.closeElement();
        }

      // the file does not exist
      } else {
        LOGGER.warn("Could not find "+url);
        xml.openElement("no-data");
        xml.attribute("error", "file-not-found");
        xml.closeElement();
      }
    }
  }

  /**
   * Removes the given file from the cache.
   * 
   * Any error is reported as XML on the XML writer.
   * 
   * @param file The file.
   * 
   * @throws IOException should an error occur.
   */
  public static void unload(File file) throws IOException {
    if (file == null || !file.exists()) return;
    // if this a directory
    if (file.isDirectory()) {
      File[] files = file.listFiles();
      for (int i = 0; i < files.length; i++) {
        XMLExtractorHelper.unload(files[i]);
      }
    // this is a file
    } else {
      URL url = toURL(file);
      if (url == null) return;
      synchronized (CACHE) {
        if (CACHE.containsKey(url.toString())) {
          LOGGER.debug("Removing "+url.toString()+" from cache");
          CACHE.remove(url.toString());
        }
      }
    }
  }

// private helpers --------------------------------------------------------------------------------

  /**
   * Returns the URL for specified file.
   * 
   * @param file The file.
   * 
   * @return The corresponding URL.
   */
  private static URL toURL(File file) {
    try {
      return file.toURL();
    } catch (MalformedURLException ex) {
      LOGGER.warn("The file does has a well formed URL.", ex);
      return null;
    }
  }

}
