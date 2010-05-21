package org.weborganic.berlioz.content;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.weborganic.furi.URIPattern;
import org.weborganic.furi.URIResolver;

/**
 * A registry for services.
 * 
 * <p>Note: this class is not synchronized and must be synchronised externally.
 * 
 * @author Christophe Lauret
 * @version 21 May 2010
 */
public class ServiceRegistry {

  /**
   * Maps content generators to the appropriate HTTP method.
   */
  private final Map<HttpMethod, ServiceMap> registry;

  /**
   * The HTTP methods supported.
   */
  private enum HttpMethod {GET, POST, PUT, DELETE};

  /**
   * Creates a new registry.
   */
  public ServiceRegistry() {
    this.registry = new Hashtable<HttpMethod, ServiceMap>();
    // Create a map for each method
    for (HttpMethod m : HttpMethod.values()) {
      this.registry.put(m, new ServiceMap());
    }
  }

  /**
   * Register the content generator.
   * 
   * @param generator the content generator to register.
   * @param pattern   the URL pattern to associate to this content generator.
   * @param method    the method for this url pattern.
   */
  public void register(Service service, String pattern, String method) {
    // preliminary checks
    if (service == null) throw new IllegalArgumentException("No service to register.");
    if (pattern == null) throw new IllegalArgumentException("URL Pattern must be specified to register a service.");
    if (method == null) throw new IllegalArgumentException("HTTP Method must be specified to register a service.");
    // Find and check the HTTP method
    HttpMethod m = getHttpMethod(method);
    // Register the generator with the URL pattern
    this.registry.get(m).put(pattern, service);
  }

  /**
   * Register the content generator for the HTTP methods in use this registry only.
   * 
   * @param generator the content generator to register.
   * @param pattern   the URL pattern to associate to this content generator.
   */
  public void register(Service service, String pattern) {
    // preliminary checks
    if (service == null) throw new IllegalArgumentException("No service to register.");
    if (pattern == null) throw new IllegalArgumentException("URL Pattern must be specified to register a service.");
    // Find and check the HTTP method
    for (HttpMethod m : this.registry.keySet()) {
      // Register the generator with the URL pattern
      this.registry.get(m).put(pattern, service);
    }
  }

  /**
   * Returns the list of content generators for this URL.
   * 
   * <p>This method iterates over each HTTP method in the following order: GET, POST, PUT, DELETE.
   * 
   * @param url The URL.
   *
   * @return A content generator which URI pattern matches this URL or <code>null</code>.
   */
  public MatchingService get(String url) {
    for (HttpMethod m : this.registry.keySet()) {
      ServiceMap mapping = this.registry.get(m);
      MatchingService service = mapping.match(url);
      if (service != null) return service;
    }
    // no match
    return null;
  }

  /**
   * Returns the content generator for this URL and HTTP method.
   * 
   * @param url    The URL.
   * @param method The HTTP method.
   * 
   * @return A content generator which URI pattern matches this URL and HTTP method or <code>null</code>.
   */
  public MatchingService get(String url, String method) {
    HttpMethod m = getHttpMethod(method);
    ServiceMap mapping = this.registry.get(m);
    MatchingService service = mapping.match(url);
    return service;
  }

  /**
   * Clears each generator mapping.
   */
  public void clear() {
    for (HttpMethod m : this.registry.keySet()) {
      this.registry.get(m).clear();
    }
  }

  /**
   * Returns the HTTP method for the specified value (case insensitive)
   * 
   * @param method The method to find
   * @return The corresponding instance or <code>null</code> if no match.
   * 
   * @throws IllegalArgumentException if the HTTP method is not valid
   */
  private HttpMethod getHttpMethod(String method) {
    for (HttpMethod m : HttpMethod.values()) {
      if (m.name().equals(method.toUpperCase())) return m;
    }
    throw new IllegalArgumentException("Unknown HTTP method:"+method);
  }

  /**
   * Simply Maps generators to URI patterns.
   * 
   * @author Christophe Lauret
   * @version 11 December 2009
   */
  private static class ServiceMap {

    /**
     * Maps content generators to the appropriate HTTP method.
     */
    private final Map<String, Service> mapping = new Hashtable<String, Service>();

    /**
     * Maps content generators to the appropriate HTTP method.
     */
    private final List<URIPattern> patterns = new ArrayList<URIPattern>();

    /**
     * Puts the given content generator in this map.
     * 
     * @param pattern   The URL pattern for this generator.
     * @param generator The content generator.
     */
    public boolean put(String pattern, Service service) {
      mapping.put(pattern, service);
      this.patterns.add(new URIPattern(pattern));
      return true;
    }

    /**
     * Returns the content generator for the specified URL.
     * 
     * @param url The URL   
     * @return the content generator for the specified URL.
     */
    public MatchingService match(String url) {
      // Attempt to the find service directly
      MatchingService match = null;
      Service service = mapping.get(url);
      if (service != null) {
        URIPattern p = new URIPattern(url);
        match = new MatchingService(service, p, new URIResolver(url).resolve(p));
        
      // Check if matching URI pattern
      } else {
        // Find the URI pattern matching the given path info
        URIResolver resolver = new URIResolver(url);
        URIPattern p = resolver.find(patterns);
        if (p != null) {
          service = mapping.get(p.toString());
          match = new MatchingService(service, p, resolver.resolve(p));
        }
      }
      return match;
    }

    /**
     * Clears mapping and patterns.
     */
    public void clear() {
      this.mapping.clear();
      this.patterns.clear();
    }
  }

}
