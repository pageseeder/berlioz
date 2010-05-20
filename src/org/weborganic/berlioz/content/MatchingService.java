package org.weborganic.berlioz.content;

import org.weborganic.furi.URIPattern;
import org.weborganic.furi.URIResolveResult;

/**
 * 
 * 
 * @author Christophe
 */
public class MatchingService {

  private final Service _service;
  
  private final URIPattern _pattern;
  
  private final URIResolveResult _result;
  
  public MatchingService(Service service, URIPattern pattern, URIResolveResult result) {
    this._service = service;
    this._pattern = pattern;
    this._result = result;
  }
  
  public Service service() {
    return this._service;
  }
  public URIPattern pattern() {
    return this._pattern;
  }
  public URIResolveResult result() {
    return this._result;
  }
}
