/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.content;

import org.weborganic.furi.URIPattern;
import org.weborganic.furi.URIResolveResult;

/**
 * Encapsulates the results of the service matching.
 * 
 * <p>Implementation note: all fields are immutable.
 * 
 * @author Christophe Lauret
 * @version 31 May 2010
 */
public final class MatchingService {

  /**
   * The matched service.
   */
  private final Service _service;

  /**
   * The URI pattern it matched.
   */
  private final URIPattern _pattern;

  /**
   * The resolved URI variables.
   */
  private final URIResolveResult _result;

  /**
   * Creates a new matching service.
   * 
   * @param service The matched service.
   * @param pattern The URI pattern it matched.
   * @param result  The resolved URI variables.
   */
  public MatchingService(Service service, URIPattern pattern, URIResolveResult result) {
    if (service == null) throw new NullPointerException("Cannot match null service");
    if (pattern == null) throw new NullPointerException("Pattern must be specified");
    if (result == null) throw new NullPointerException("Resolution results must be specified");
    this._service = service;
    this._pattern = pattern;
    this._result = result;
  }

  /**
   * Indicates whether this response is cacheable.
   * 
   * <p>A response is cacheable only is the service has been found and all its generators are 
   * cacheable.
   * 
   * @return <code>true</code> if this response is cacheable;
   *         <code>false</code> otherwise.
   */
  public boolean isCacheable() {
    if (this._service == null) return false;
    return this._service.isCacheable();
  }

  /**
   * Always returns the matched service (always a value).
   * 
   * @return The matched service (never <code>null</code>).
   */
  public Service service() {
    return this._service;
  }

  /**
   * Always returns the matching URI pattern.
   * 
   * @return The URI pattern it matched (never <code>null</code>).
   */
  public URIPattern pattern() {
    return this._pattern;
  }

  /**
   * Always returns the resolved URI variables.
   * 
   * @return The resolved URI variables (never <code>null</code>).
   */
  public URIResolveResult result() {
    return this._result;
  }

}
