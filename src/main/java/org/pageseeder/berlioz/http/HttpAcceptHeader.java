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
package org.pageseeder.berlioz.http;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility class to help dealing with the HTTP/1.1 request headers such as 'Accept', 'Accept-Language',
 * 'Accept-Encoding', 'Accept-Charset'.
 *
 * <p>The quality values returned are floats from 0.0f to 1.0f.
 *
 * <p>For more info on Quality values see:
 * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.9">Hypertext Transfer Protocol
 *  -- HTTP/1.1 - 3.9 Quality Values</a>.
 *
 * <p>For more information on Accept HTTP headers, see:
 * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html">Hypertext Transfer Protocol
 *  -- HTTP/1.1 - 14 Header Field Definitions</a>.
 *
 * <p>Implementation note: for efficiency the results are cached and reused.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.8.2 - 27 June 2011
 * @since Berlioz 0.6
 */
public final class HttpAcceptHeader { // NO_UCD

  /**
   * Accepts headers that have already been processed.
   */
  private static final Map<String, Map<String, Float>> MAPS =
    Collections.synchronizedMap(new HashMap<String, Map<String, Float>>());

  /**
   * The maximum size of the cache for security, once the internal cache reaches this number of entries,
   * nothing more will be cached.
   */
  private static final int MAX_SIZE = 96;

  /**
   * The pattern matching an accept header with a Q value.
   */
  private static final Pattern ACCEPT_WITH_QVALUE = Pattern.compile("([^;]+);\\s*q=(\\d\\.?\\d*)\\s*");

  /**
   * Utility class.
   */
  private HttpAcceptHeader() {
  }

  /**
   * Returns the list of accepted content types mapped to their quality level.
   *
   * @param accept The 'Accept' HTTP/1.1 header.
   * @return an unmodifiable map of the accept header.
   */
  public static Map<String, Float> get(String accept) {
    // no value, return an empty map
    if (accept == null || "".equals(accept)) return Collections.emptyMap();
    // Try to see if this has been processed already
    Map<String, Float> map = MAPS.get(accept);
    // Parse the accept header.
    if (map == null) {
      map = Collections.unmodifiableMap(parse(accept));
      if (MAPS.size() < MAX_SIZE) {
        synchronized (MAPS) {
          MAPS.put(accept, map);
        }
      }
    }
    return map;
  }

  /**
   * Indicates whether the given 'Accept' header accepts the specified value.
   *
   * <p>To be acceptable the value must
   *
   * @param accept The 'Accept' or 'Accept-*' HTTP/1.1 header.
   * @param value  The value to look for.
   *
   * @return <code>true</code> if the specified value has a Q value  strictly greater than 0;
   *         <code>false</code> otherwise.
   */
  public static boolean accepts(String accept, String value) {
    return accepts(get(accept), value);
  }

  /**
   * Indicates whether the given 'Accept' header accepts the specified value.
   *
   * <p>To be acceptable the value must
   *
   * @param accept An accept map produced by this class.
   * @param value  The value to look for.
   *
   * @return <code>true</code> if the specified value has a Q value  strictly greater than 0;
   *         <code>false</code> otherwise.
   */
  public static boolean accepts(Map<String, Float> accept, String value) {
    Float q = null;
    // look for the value first
    q = accept.get(value);
    if (q != null && q > 0.0f) return true;
    // check if client accepts everything
    q = accept.get("*/*");
    if (q != null && q > 0.0f) return true;
    q = accept.get("*");
    if (q != null && q > 0.0f) return true;
    // check content types
    int slash = value.indexOf('/');
    if (slash >= 0) {
      q = accept.get(value.substring(0, slash)+"/*");
      return q != null && q > 0.0f;
    }
    // not acceptable
    return false;
  }

  /**
   * Parses the accept header and returns the corresponding map.
   *
   * @param accept The 'Accept' or 'Accept-*' HTTP/1.1 header.
   * @return the accepted content types mapped to their quality value (0 to 1000).
   */
  protected static Map<String, Float> parse(String accept) {
    Map<String, Float> values = new LinkedHashMap<String, Float>();
    StringTokenizer t = new StringTokenizer(accept, ",");
    while (t.hasMoreElements()) {
      String token = t.nextToken();
      Matcher m = ACCEPT_WITH_QVALUE.matcher(token);
      if (m.matches()) {
        values.put(m.group(1), Float.parseFloat(m.group(2)));
      } else {
        values.put(token, 1.0f);
      }
    }
    return values;
  }

  /**
   * Clears the internal cache.
   */
  protected synchronized void clear() {
    MAPS.clear();
  }
}
