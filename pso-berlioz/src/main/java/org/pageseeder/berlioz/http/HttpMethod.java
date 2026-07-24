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

import java.util.EnumSet;
import java.util.Set;

/**
 * An enumeration of HTTP methods supported by Berlioz.
 *
 * @author Christophe Lauret
 *
 * @version 0.13.0
 * @since 0.8.2
 */
public enum HttpMethod {

  /**
   * GET HTTP Method.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-9.3">HTTP/1.1 - 9.3 GET</a>
   */
  GET(true),

  /**
   * POST HTTP Method.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-9.5">HTTP/1.1 - 9.5 POST</a>
   */
  POST(true),

  /**
   * PUT HTTP Method.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-9.6">HTTP/1.1 - 9.6 PUT</a>
   */
  PUT(true),

  /**
   * DELETE HTTP Method.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-9.7">HTTP/1.1 - 9.7 DELETE</a>
   */
  DELETE(true),

  /**
   * HEAD HTTP Method.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-9.4">HTTP/1.1 - 9.4 HEAD</a>
   */
  HEAD(false),

  /**
   * PATCH HTTP Method.
   */
  PATCH(true),

  /**
   * OPTIONS HTTP method.
   */
  OPTIONS(false),

  /**
   * QUERY HTTP method: a safe, idempotent method with a request body, used to convey a query
   * that would otherwise require an oversized GET query string or a POST-as-GET tunnel.
   *
   * @see <a href="https://www.rfc-editor.org/rfc/rfc10008.html">
   *      HTTP QUERY Method</a>
   */
  QUERY(true);

  /**
   * Set of HTTP methods mappable to a service.
   */
  private static final EnumSet<HttpMethod> MAPPABLE_METHODS = EnumSet.of(GET, POST, PUT, PATCH, DELETE, QUERY);

  /**
   * Indicates whether it can be mapped to a content generator.
   */
  private final boolean mappable;

  /**
   * Creates a new constant.
   * @param isMappable whether it can be mapped to a Berlioz Service.
   */
  HttpMethod(boolean isMappable) {
    this.mappable = isMappable;
  }

  /**
   * Indicates whether it can be mapped to a Berlioz Service.
   *
   * @return <code>true</code> if it can be mapped to a Berlioz Service;
   *         <code>false</code> otherwise.
   */
  public boolean isMappable() {
    return this.mappable;
  }

  /**
   * Returns the HTTP methods mappable to a service.
   *
   * @return Set of HTTP methods mappable to a service.
   */
  public static Set<HttpMethod> mappable() {
    return MAPPABLE_METHODS;
  }
}
