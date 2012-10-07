/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.servlet;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.weborganic.berlioz.content.ContentGenerator;
import org.weborganic.berlioz.content.ContentRequest;
import org.weborganic.berlioz.content.ContentStatus;
import org.weborganic.berlioz.content.Environment;
import org.weborganic.berlioz.content.Service;

/**
 * Wraps a {@link javax.servlet.ServletRequest} instance and provide methods to access the parameters and attributes in
 * a consistent manner.
 *
 * @author Christophe Lauret (Weborganic)
 * @version Berlioz 0.9.3 - 9 December 2011
 * @since Berlioz 0.9
 */
public final class HttpContentRequest extends HttpRequestWrapper implements ContentRequest {

  /**
   * Generator for which the request is designed for.
   */
  private final ContentGenerator _generator;

  /**
   * Service the generator is part of.
   */
  private final Service _service;

  /**
   * The status for this request.
   */
  private ContentStatus _status = ContentStatus.OK;

  // sole constructor -------------------------------------------------------------------------------

  /**
   * Creates a new wrapper around the specified HTTP servlet request.
   *
   * @param wrapper The HTTP reque3st wrapper.
   * @param generator The generator for which this request is used.
   */
  protected HttpContentRequest(HttpRequestWrapper wrapper, ContentGenerator generator, Service service) {
    super(wrapper);
    if (generator == null) throw new NullPointerException("No generator specified");
    this._generator = generator;
    this._service = service;
  }

  /**
   * Creates a new wrapper around the specified HTTP servlet request.
   *
   * @param req        The request to wrap.
   * @param res        The response to wrap.
   * @param env        The environment for this request.
   * @param parameters The parameters to use.
   * @param generator  The generator for which this request is for.
   * @param service    The service this request is part of.
   *
   * @throws IllegalArgumentException If the request is <code>null</code>.
   */
  protected HttpContentRequest(HttpServletRequest req, HttpServletResponse res, Environment env,
      Map<String, String> parameters, ContentGenerator generator, Service service) {
    super(req, res, env, parameters);
    if (generator == null) throw new NullPointerException("No generator specified");
    if (service == null) throw new NullPointerException("No generator specified");
    this._generator = generator;
    this._service = service;
  }

  /**
   * Sets the status of this request.
   *
   * @param status the status of this request.
   * @throws NullPointerException if the status is <code>null</code>.
   */
  @Override
  public void setStatus(ContentStatus status) {
    if (status == null) throw new NullPointerException("Cannot set status to null");
    this._status = status;
  }

  /**
   * Returns the status of this request.
   *
   * @return the status of this request.
   */
  public ContentStatus getStatus() {
    return this._status;
  }

  /**
   * Returns the generator for which this request is used for.
   *
   * @return the generator for which this request is used for.
   */
  public ContentGenerator generator() {
    return this._generator;
  }

  /**
   * Service the generator is part of.
   *
   * @return the service the generator is part of.
   */
  public Service getService() {
    return this._service;
  }
}
