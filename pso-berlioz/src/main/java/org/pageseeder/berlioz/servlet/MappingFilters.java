/*
 * Copyright 2026 Allette Systems (Australia)
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.error.DetailLevel;
import org.pageseeder.berlioz.security.ControlAuthorization;
import org.pageseeder.berlioz.util.Redaction;
import org.slf4j.Logger;

/**
 * Coordinates the {@link RedirectFilter} and {@link RelocationFilter} instances.
 *
 * <p>A reload of the {@code redirects} scope increments a counter, once per request; each filter
 * loads its rules again on its next request once the counter has changed. So a reload reaches
 * every filter, whatever its mapping, without the request passing through it.
 */
final class MappingFilters {

  /** The maximum number of rules listed for each filter. */
  static final int RULE_LIMIT = 2000;

  /** The maximum length of a load failure message. */
  static final int MESSAGE_LIMIT = 512;

  private static final AtomicLong GENERATION = new AtomicLong();

  private static final String REQUESTED_ATTRIBUTE = MappingFilters.class.getName() + ".reload";

  private static final Set<MappingRules<?>> FILTERS = ConcurrentHashMap.newKeySet();

  private MappingFilters() {}

  /** @return the number of reloads of the {@code redirects} scope so far */
  static long generation() {
    return GENERATION.get();
  }

  /**
   * Requests a reload of the rules of every filter.
   *
   * <p>The servlet and each filter that a request passes through may call it: only the first call
   * of a request counts, so the rules are loaded once per reload.
   *
   * @param req the request with the authorized {@code redirects} scope
   */
  static void requestReload(HttpServletRequest req) {
    if (req.getAttribute(REQUESTED_ATTRIBUTE) != null) return;
    req.setAttribute(REQUESTED_ATTRIBUTE, Boolean.TRUE);
    GENERATION.incrementAndGet();
  }

  /**
   * Requests a reload of the rules of every filter if the request asks for the {@code redirects}
   * scope and is authorized for Berlioz control.
   *
   * @param req the request through a filter
   * @return {@code true} if the reload was requested
   */
  static boolean applyReload(HttpServletRequest req) {
    if (req.getParameter(ControlParameter.BERLIOZ_RELOAD.parameter()) == null) return false;
    if (!ControlAuthorization.hasControl(req)) return false;
    if (!ReloadScope.fromRequest(req).contains(ReloadScope.REDIRECTS)) return false;
    requestReload(req);
    return true;
  }

  /** @param rules the rules of a filter instance that was initialized */
  static void register(MappingRules<?> rules) {
    FILTERS.add(rules);
  }

  /** @param rules the rules of a filter instance that was destroyed */
  static void unregister(MappingRules<?> rules) {
    FILTERS.remove(rules);
  }

  /** @return {@code true} if a filter with a configuration file is initialized */
  static boolean hasConfiguredFilter() {
    return FILTERS.stream().anyMatch(MappingRules::isConfigured);
  }

  /** @return the safe snapshot of the filter instances, sorted by type and name */
  static ConfigurationSnapshot diagnosticSnapshot() {
    boolean details = DetailLevel.parse(GlobalSettings.get(BerliozOption.ERROR_DETAIL)) != DetailLevel.MINIMAL;
    List<Map<String, Object>> entries = FILTERS.stream()
        .sorted(Comparator.<MappingRules<?>, String>comparing(MappingRules::type).thenComparing(MappingRules::filterName))
        .map(rules -> rules.diagnosticEntry(details))
        .collect(Collectors.toList());
    return new ConfigurationSnapshot("redirect-config", "filters", "filter", entries, Map.of());
  }

  /**
   * Returns the configuration file of a filter, logging a warning if it is not specified or does
   * not exist: the filter has no effect then.
   *
   * @param config     the filter configuration
   * @param filterName the name of the filter instance
   * @param parameter  the {@code config} init parameter, relative to {@code WEB-INF}, if any
   * @param logger     the logger of the filter
   * @return the configuration file, or {@code null} if the parameter is not specified
   */
  static @Nullable File configFile(FilterConfig config, String filterName, @Nullable String parameter, Logger logger) {
    if (parameter == null) {
      logger.warn("Filter '{}': missing 'config' init-parameter - filter will have no effect", filterName);
      return null;
    }
    File webinf = new File(config.getServletContext().getRealPath("/"), "WEB-INF");
    File file = new File(webinf, parameter);
    if (!file.exists()) {
      logger.warn("Filter '{}': 'config' init-parameter points to non existing file {} - filter will have no effect",
          filterName, file.getAbsolutePath());
    }
    return file;
  }

  /**
   * Redacts a target pattern for diagnostics.
   *
   * <p>A target can contain a credential. The user information of an absolute URL is removed, and
   * the value of every query parameter is replaced by {@link Redaction#REDACTED}, unless it is
   * only one URI template expression such as {@code {q}}. Template expressions and the fragment
   * are kept.
   *
   * @param target the target pattern, as text
   * @return the redacted target
   */
  static String redactTarget(String target) {
    String to = removeUserInfo(target);
    int query = indexOutsideExpressions(to, '?', 0);
    int fragment = indexOutsideExpressions(to, '#', 0);
    // No query, or a '?' in the fragment
    if (query < 0 || fragment >= 0 && fragment < query) return to;
    int end = fragment < 0 ? to.length() : fragment;
    StringBuilder out = new StringBuilder(to.length()).append(to, 0, query + 1);
    int start = query + 1;
    while (true) {
      int amp = indexOutsideExpressions(to, '&', start);
      int stop = amp < 0 || amp > end ? end : amp;
      out.append(redactParameter(to.substring(start, stop)));
      if (stop == end) break;
      out.append('&');
      start = stop + 1;
    }
    return out.append(to, end, to.length()).toString();
  }

  private static String redactParameter(String parameter) {
    int equals = parameter.indexOf('=');
    if (equals < 0) return parameter;
    String value = parameter.substring(equals + 1);
    boolean expression = value.length() > 2 && value.charAt(0) == '{' && value.indexOf('}') == value.length() - 1;
    return expression ? parameter : parameter.substring(0, equals + 1) + Redaction.REDACTED;
  }

  private static String removeUserInfo(String target) {
    int start = authorityStart(target);
    if (start < 0) return target;
    int end = start;
    while (end < target.length() && "/?#".indexOf(target.charAt(end)) < 0) end++;
    int at = target.lastIndexOf('@', end - 1);
    return at < start ? target : target.substring(0, start) + target.substring(at + 1);
  }

  /**
   * @return the index of the authority in an absolute URL or network-path reference ("//host"), or -1
   */
  private static int authorityStart(String target) {
    if (target.startsWith("//")) return 2;
    int scheme = target.indexOf("://");
    if (scheme <= 0 || !target.substring(0, scheme).matches("[A-Za-z][A-Za-z0-9+.-]*")) return -1;
    return scheme + 3;
  }

  /** @return the index of the character outside template expressions, from the index, or -1 */
  private static int indexOutsideExpressions(String s, char c, int from) {
    int depth = 0;
    // Scan from the start to know the depth of expressions at 'from'
    for (int i = 0; i < s.length(); i++) {
      char x = s.charAt(i);
      if (x == '{') depth++;
      else if (x == '}' && depth > 0) depth--;
      else if (x == c && depth == 0 && i >= from) return i;
    }
    return -1;
  }

}
