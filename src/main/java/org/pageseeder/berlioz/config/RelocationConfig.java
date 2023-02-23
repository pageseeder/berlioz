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
package org.pageseeder.berlioz.config;

import org.eclipse.jdt.annotation.Nullable;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.furi.URIPattern;
import org.pageseeder.berlioz.servlet.RelocationFilter;
import org.pageseeder.berlioz.xml.XMLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds the mapping for the relocation filter
 *
 * <p>The relocation mapping can be specified as below:
 *
 * <pre>{@code
 * <?xml version="1.0" encoding="utf-8"?>
 * <relocation-mapping>
 *   <relocation from="/"             to="/html/home"/>
 *   <relocation from="/index.html"   to="/html/home"/>
 *   <relocation from="/html"         to="/html/home"/>
 *   <relocation from="/xml"          to="/xml/home"/>
 *   <relocation from="/{+path}.psml" to="/html/{+path}"/>
 * </relocation-mapping>
 * }</pre>
 *
 * @version Berlioz 0.12.4
 * @since Berlioz 0.12.4
 */
public final class RelocationConfig {

  /**
   * Displays debug information.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(RelocationFilter.class);

  /**
   * Maps URI patterns to relocate to URI pattern target.
   */
  private final List<MovedLocationPattern> _mapping;

  private RelocationConfig(List<MovedLocationPattern> mapping) {
    this._mapping = mapping;
  }

  public @Nullable String relocate(String from) {
    // Evaluate URI patterns
    for (MovedLocationPattern pattern : this._mapping) {
      if (pattern.match(from)) return pattern.getTarget(from);
    }
    // No match
    return null;
  }

  public static RelocationConfig newInstance(@Nullable File file) {
    return load(file);
  }

  /**
   * Load the URI relocation configuration file.
   *
   * @return <code>true</code> if loaded correctly;
   *         <code>false</code> otherwise.
   */
  private static RelocationConfig load(@Nullable File file) {
    List<MovedLocationPattern> mapping = new ArrayList<>();
    if (file != null) {
      Handler handler = new Handler(mapping);
      try {
        XMLUtils.parse(handler, file, false);
      } catch (BerliozException ex) {
        LOGGER.error("Unable to load relocation mapping {} : {}", file, ex);
      }
    }
    return new RelocationConfig(mapping);
  }

  /**
   * Handles the XML for the URI pattern mapping configuration.
   */
  private static class Handler extends DefaultHandler implements ContentHandler {

    /**
     * Maps URI patterns to URI patterns.
     */
    private final List<MovedLocationPattern> _mapping;

    /**
     * @param mapping The mapping to use for relocation.
     */
    public Handler(List<MovedLocationPattern> mapping) {
      this._mapping = mapping;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) {
      if ("relocation".equals(localName)) {
        URIPattern from = toPattern(atts.getValue("from"));
        URIPattern to = toPattern(atts.getValue("to"));
        if (from == null || to == null) return;
        this._mapping.add(new MovedLocationPattern(from, to));
      }
    }

    /**
     * Parse the specified URI Pattern.
     *
     * @param pattern The URI pattern as a string.
     * @return the <code>URIPattern</code> instance or <code>null</code>.
     */
    private @Nullable URIPattern toPattern(@Nullable String pattern) {
      if (pattern == null) return null;
      try {
        return new URIPattern(pattern);
      } catch (IllegalArgumentException ex) {
        LOGGER.warn("Unparseable URI pattern: {} - ignored mapping", pattern);
        return null;
      }
    }

  }

}
