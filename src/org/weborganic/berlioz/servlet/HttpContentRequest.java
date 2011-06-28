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
import org.weborganic.berlioz.content.Environment;
import org.weborganic.berlioz.content.ContentStatus;

/**
 * Wraps a {@link javax.servlet.ServletRequest} instance and provide methods to access the parameters and attributes in
 * a consistent manner.
 * 
 * @author Christophe Lauret (Weborganic)
 * 
 * @version 28 June 2011
 */
public final class HttpContentRequest extends HttpRequestWrapper implements ContentRequest {

  /**
   * Maps parameter names to their values.
   */
  private final ContentGenerator _generator;

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
  protected HttpContentRequest(HttpRequestWrapper wrapper, ContentGenerator generator) {
    super(wrapper);
    if (generator == null) throw new NullPointerException("No generator specified");
    this._generator = generator;
  }

  /**
   * Creates a new wrapper around the specified HTTP servlet request.
   * 
   * @param req        The request to wrap.
   * @param res        The response to wrap.
   * @param env        The environment for this request.
   * @param parameters The parameters to use.
   * @param generator  The generator for which this request is for.
   * 
   * @throws IllegalArgumentException If the request is <code>null</code>.
   */
  protected HttpContentRequest(HttpServletRequest req, HttpServletResponse res, Environment env,
      Map<String, String> parameters, ContentGenerator generator) throws IllegalArgumentException {
    super(req, res, env, parameters);
    if (generator == null) throw new NullPointerException("No generator specified");
    this._generator = generator;
  }

  /**
   * {@inheritDoc}
   */
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
    return _generator;
  }

}
