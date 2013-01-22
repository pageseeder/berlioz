/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.weborganic.berlioz.content.Environment;
import org.weborganic.berlioz.content.Location;

/**
 * Encapsulate all the common and immutable information for an HTTP request.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.13 - 21 January 2013
 * @since Berlioz 0.9.13
 */
final class CoreHttpRequest {

  /**
   * The wrapped {@link javax.servlet.ServletRequest}.
   */
  private final HttpServletRequest _req;

  /**
   * The wrapped {@link javax.servlet.ServletResponse}.
   */
  private final HttpServletResponse _res;

  /**
   * The environment.
   */
  private final Environment _env;

  /**
   * The location of the resource requested.
   */
  private final Location _loc;

  /**
   * Creates a object containing all the common HTTP information.
   *
   * @param req The request to wrap.
   * @param res The response to wrap.
   * @param env The environment for this request.
   */
  public CoreHttpRequest(HttpServletRequest req, HttpServletResponse res, Environment env) {
    this._req = req;
    this._res = res;
    this._env = env;
    this._loc = HttpLocation.build(req);
  }

  /**
   * @return the _loc
   */
  public Location location() {
    return this._loc;
  }

  /**
   * @return the _req
   */
  public HttpServletRequest request() {
    return this._req;
  }

  /**
   * @return the _env
   */
  public Environment environment() {
    return this._env;
  }

  /**
   * @return the _res
   */
  public HttpServletResponse response() {
    return this._res;
  }
}
