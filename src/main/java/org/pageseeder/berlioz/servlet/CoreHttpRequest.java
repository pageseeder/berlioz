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
package org.pageseeder.berlioz.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.pageseeder.berlioz.content.Environment;
import org.pageseeder.berlioz.content.Location;

/**
 * Encapsulate all the common and immutable information for an HTTP request.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.13.0
 * @since Berlioz 0.9.13
 */
final class CoreHttpRequest {

  /**
   * The wrapped {@link javax.servlet.ServletRequest}.
   */
  private final HttpServletRequest req;

  /**
   * The wrapped {@link javax.servlet.ServletResponse}.
   */
  private final HttpServletResponse res;

  /**
   * The environment.
   */
  private final Environment env;

  /**
   * The location of the resource requested.
   */
  private final Location loc;

  /**
   * Creates a object containing all the common HTTP information.
   *
   * @param req The request to wrap.
   * @param res The response to wrap.
   * @param env The environment for this request.
   */
  public CoreHttpRequest(HttpServletRequest req, HttpServletResponse res, Environment env) {
    this.req = req;
    this.res = res;
    this.env = env;
    this.loc = HttpLocation.build(req);
  }

  /**
   * @return the _loc
   */
  public Location location() {
    return this.loc;
  }

  /**
   * @return the _req
   */
  public HttpServletRequest request() {
    return this.req;
  }

  /**
   * @return the _env
   */
  public Environment environment() {
    return this.env;
  }

  /**
   * @return the _res
   */
  public HttpServletResponse response() {
    return this.res;
  }
}
