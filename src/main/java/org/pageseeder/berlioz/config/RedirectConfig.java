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
import org.pageseeder.berlioz.furi.URIPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Holds the config for the redirection filter.
 *
 * <p>The redirect mapping can be specified as below:
 *
 * <pre>{@code
 * <?xml version="1.0" encoding="utf-8"?>
 * <redirect-mapping>
 *   <redirect from="/"             to="/html/home"/>
 *   <redirect from="/index.html"   to="/html/home"/>
 *   <redirect from="/html"         to="/html/home"/>
 *   <redirect from="/xml"          to="/xml/home"/>
 *   <redirect from="/{+path}.psml" to="/html/{+path}"/>
 * </redirect-mapping>
 * }</pre>
 *
 * @version Berlioz 0.12.4
 * @since Berlioz 0.12.4
 */
public final class RedirectConfig {

  /**
   * Displays debug information.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(RedirectConfig.class);

  /**
   * Maps URI patterns to redirect to URI pattern target.
   */
  private final List<RedirectPattern> _patterns;

  RedirectConfig() {
    this._patterns = new ArrayList<>();
  }

  private RedirectConfig(List<RedirectPattern>  patterns) {
    this._patterns = patterns;
  }

  /**
   * Load the URI relocation configuration file.
   */
  public static RedirectConfig newInstance(File file) throws ConfigException {
    return ConfigLoader.parse(new RedirectConfig.Handler(), file);
  }

  public static RedirectConfig newInstance(InputStream in) throws ConfigException {
    return ConfigLoader.parse(new RedirectConfig.Handler(), in);
  }

  public @Nullable RedirectLocation redirect(String from) {
    // Evaluate URI patterns
    for (RedirectPattern pattern : this._patterns) {
      if (pattern.match(from)) return pattern.redirect(from);
    }
    // No match
    return null;
  }

  boolean isEmpty() {
    return this._patterns.isEmpty();
  }

  /**
   * Handles the XML for the URI pattern mapping configuration.
   */
  private static class Handler extends ConfigLoader.ConfigHandler<RedirectConfig> {

    private List<RedirectPattern> patterns = new ArrayList<>();

    /** Used to detect duplicates */
    private final Set<String> matchingPatterns = new HashSet<>();

    public String getSchema() {
      return "redirect-mapping-1.0";
    }

    @Override
    public void startDocument() throws SAXException {
      this.patterns = new ArrayList<>();
      this.matchingPatterns.clear();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) {
      if ("redirect".equals(localName)) {
        URIPattern from = toPattern(atts.getValue("from"));
        URIPattern to = toPattern(atts.getValue("to"));
        if (from == null || to == null) return;
        if (!this.matchingPatterns.add(from.toString())) {
          LOGGER.warn("URI pattern: {} is mapped twice", from);
        }
        boolean isPermanent = "yes".equals(atts.getValue("permanent"));
        this.patterns.add(new RedirectPattern(from, to, isPermanent));
      }
    }

    /**
     * Parse the specified URI Pattern.
     *
     * @param pattern The URI pattern as a string.
     * @return the <code>URIPattern</code> instance or <code>null</code>.
     */
    private static @Nullable URIPattern toPattern(@Nullable String pattern) {
      if (pattern == null) return null;
      try {
        return new URIPattern(pattern);
      } catch (IllegalArgumentException ex) {
        LOGGER.warn("Unparseable URI pattern: {} - ignored mapping", pattern);
        return null;
      }
    }

    @Override
    RedirectConfig getConfig() {
      return new RedirectConfig(this.patterns);
    }
  }

  private static class RedirectPattern extends MovedLocationPattern {

    final boolean permanent;

    public RedirectPattern(URIPattern from, URIPattern to, boolean permanent) {
      super(from, to);
      this.permanent = permanent;
    }

    public boolean isPermanent() {
      return this.permanent;
    }

    public RedirectLocation redirect(String path) {
      if (!this.match(path)) return null;
      String target = this.getTarget(path);
      return new RedirectLocation(path, target, this.permanent);
    }

  }

}
