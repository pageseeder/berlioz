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
    this._service = service;
    this._pattern = pattern;
    this._result = result;
  }

  /**
   * @return The matched service.
   */
  public Service service() {
    return this._service;
  }

  /**
   * @return The URI pattern it matched.
   */
  public URIPattern pattern() {
    return this._pattern;
  }

  /**
   * @return The resolved URI variables.
   */
  public URIResolveResult result() {
    return this._result;
  }

}
