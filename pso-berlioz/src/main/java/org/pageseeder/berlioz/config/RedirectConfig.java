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

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.furi.URIPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.*;

import java.io.File;
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
 *   <redirect from="/old"          to="/new"          status="308"/>
 * </redirect-mapping>
 * }</pre>
 *
 * <p>The {@code status} attribute sets the HTTP status of a redirect: 301, 302, 307 or 308. Without
 * it, the redirect is temporary (302).
 *
 * <p>The {@code permanent} attribute is deprecated since 0.14.6 and will be removed in 1.0: without
 * {@code status}, {@code permanent="yes"} gives 301, and {@code status} takes precedence when both
 * are specified. A warning is logged when a file uses it.
 *
 * @version 0.14.6
 * @since 0.12.4
 */
public final class RedirectConfig {

  /**
   * Displays debug information.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(RedirectConfig.class);

  /**
   * Maps URI patterns to redirect to the URI pattern target.
   */
  private final List<RedirectPattern> patterns;

  /**
   * The rules ignored when loading.
   */
  private final List<IgnoredRule> ignored;

  /**
   * Creates an empty redirect configuration with no patterns.
   */
  public RedirectConfig() {
    this(Collections.emptyList());
  }

  /**
   * Creates a redirect configuration with the given list of patterns.
   *
   * @param patterns The redirect patterns; must not be {@code null}.
   */
  public RedirectConfig(List<RedirectPattern> patterns) {
    this(patterns, Collections.emptyList());
  }

  private RedirectConfig(List<RedirectPattern> patterns, List<IgnoredRule> ignored) {
    this.patterns = patterns;
    this.ignored = ignored;
  }

  /**
   * Load the redirect configuration from a file.
   *
   * @param file The XML file to load.
   * @return A new {@code RedirectConfig} populated from the file.
   * @throws ConfigException If the file cannot be read or is invalid.
   */
  public static RedirectConfig newInstance(File file) throws ConfigException {
    return ConfigLoader.parse(new RedirectConfig.Handler(), file);
  }

  /**
   * Load the redirect configuration from an input stream.
   *
   * @param in The XML input stream to parse.
   * @return A new {@code RedirectConfig} populated from the stream.
   * @throws ConfigException If the stream cannot be read or is invalid.
   */
  public static RedirectConfig newInstance(InputStream in) throws ConfigException {
    return ConfigLoader.parse(new RedirectConfig.Handler(), in);
  }

  /**
   * Returns the redirect location for the given path, or {@code null} if no pattern matches.
   *
   * @param from The incoming request path.
   * @return The matching {@link RedirectLocation}, or {@code null} if none matches.
   */
  public @Nullable RedirectLocation redirect(String from) {
    for (RedirectPattern pattern : this.patterns) {
      if (pattern.match(from)) return pattern.redirect(from);
    }
    return null;
  }

  /**
   * Returns the redirect patterns of this configuration, in file order.
   *
   * @return An unmodifiable list of patterns.
   * @since 0.14.6
   */
  public List<RedirectPattern> patterns() {
    return Collections.unmodifiableList(this.patterns);
  }

  /**
   * Returns the rules ignored when the file was loaded, in file order.
   *
   * @return An unmodifiable list, empty if no rule was ignored.
   * @since 0.14.6
   */
  public List<IgnoredRule> ignored() {
    return Collections.unmodifiableList(this.ignored);
  }

  /**
   * Returns the number of redirect patterns in this configuration.
   *
   * @return The pattern count.
   */
  public int size() {
    return this.patterns.size();
  }

  /**
   * Returns {@code true} if this configuration contains no redirect patterns.
   *
   * @return {@code true} if empty.
   */
  public boolean isEmpty() {
    return this.patterns.isEmpty();
  }

  /**
   * Handles the XML for the URI pattern mapping configuration.
   */
  private static class Handler extends ConfigLoader.ConfigHandler<RedirectConfig> {

    private List<RedirectPattern> patterns = new ArrayList<>();

    private List<IgnoredRule> ignored = new ArrayList<>();

    private int position = 0;

    /** The number of rules using the deprecated <code>permanent</code> attribute. */
    private int deprecated = 0;

    /** Used to detect duplicates */
    private final Set<String> matchingPatterns = new HashSet<>();

    public String getSchema() {
      return "redirect-mapping-1.0";
    }

    @Override
    public void startDocument() {
      this.patterns = new ArrayList<>();
      this.ignored = new ArrayList<>();
      this.position = 0;
      this.deprecated = 0;
      this.matchingPatterns.clear();
    }

    @Override
    public void endDocument() {
      if (this.deprecated > 0) {
        LOGGER.warn("{} redirect rule(s) use the 'permanent' attribute, deprecated since 0.14.6 and"
            + " scheduled for removal in 1.0; use status=\"301\" instead of permanent=\"yes\","
            + " or remove permanent=\"no\"", this.deprecated);
      }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) {
      if ("redirect".equals(localName)) {
        this.position++;
        URIPattern from = toPattern(atts.getValue("from"));
        URIPattern to = toPattern(atts.getValue("to"));
        if (from == null || to == null) {
          IgnoredRule ignore = IgnoredRule.check(this.position, atts.getValue("from"), from, atts.getValue("to"), to);
          if (ignore != null) this.ignored.add(ignore);
          return;
        }
        if (!this.matchingPatterns.add(from.toString())) {
          LOGGER.warn("URI pattern: {} is mapped twice", from);
        }
        String permanent = atts.getValue("permanent");
        if (permanent != null) this.deprecated++;
        RedirectPattern pattern = new RedirectPattern(from, to, toStatus(atts.getValue("status"), permanent));
        this.patterns.add(pattern);
        LOGGER.debug("Added pattern: {}", pattern);
      }
    }

    /**
     * Returns the status of a redirect from its attributes; <code>status</code> takes precedence
     * over the deprecated <code>permanent</code>.
     *
     * <p>The schema only allows 301, 302, 307 and 308; any other value falls back to
     * <code>permanent</code>.
     *
     * @param status    The value of the <code>status</code> attribute, if any.
     * @param permanent The value of the <code>permanent</code> attribute, if any.
     * @return the redirect status
     */
    private static ContentStatus toStatus(@Nullable String status, @Nullable String permanent) {
      if (status != null) {
        switch (status) {
          case "301": return ContentStatus.MOVED_PERMANENTLY;
          case "302": return ContentStatus.FOUND;
          case "307": return ContentStatus.TEMPORARY_REDIRECT;
          case "308": return ContentStatus.PERMANENT_REDIRECT;
          default: LOGGER.warn("Unsupported redirect status: {} - using permanent attribute", status);
        }
      }
      return "yes".equals(permanent) ? ContentStatus.MOVED_PERMANENTLY : ContentStatus.FOUND;
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
      return new RedirectConfig(this.patterns, this.ignored);
    }
  }

}
