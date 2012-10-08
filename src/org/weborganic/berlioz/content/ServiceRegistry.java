/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.content;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.weborganic.berlioz.http.HttpMethod;
import org.weborganic.furi.URIPattern;
import org.weborganic.furi.URIResolver;
import org.weborganic.furi.URIResolver.MatchRule;

/**
 * A registry for services.
 *
 * <p>Note: this class is not synchronized and must be synchronized externally.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.3 - 9 December 2011
 * @since Berlioz 0.8
 */
public final class ServiceRegistry {

  /**
   * Maps content generators to the appropriate HTTP method.
   */
  private final Map<HttpMethod, ServiceMap> registry;

  /**
   * When the service registry was last loaded.
   */
  private long version;

  /**
   * Creates a new registry.
   */
  public ServiceRegistry() {
    this.registry = new EnumMap<HttpMethod, ServiceMap>(HttpMethod.class);
    // Create a map for each mappable HTTP method
    for (HttpMethod m : HttpMethod.mappable()) {
      this.registry.put(m, new ServiceMap());
    }
    this.version = System.currentTimeMillis();
  }

  /**
   * Register the content generator.
   *
   * @param service the service to register.
   * @param pattern the URL pattern to associate to this content generator.
   * @param method  the method for this URL pattern.
   *
   * @throws NullPointerException If any argument is <code>null</code>
   */
  public void register(Service service, URIPattern pattern, HttpMethod method) {
    // preliminary checks
    if (service == null) throw new NullPointerException("No service to register.");
    if (pattern == null) throw new NullPointerException("URL Pattern must be specified to register a service.");
    if (method == null) throw new NullPointerException("HTTP Method must be specified to register a service.");
    // Register the generator with the URL pattern
    this.registry.get(method).put(pattern, service);
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
   * Returns the list of HTTP methods allowed for this URL.
   *
   * <p>If the GET method is supported, then the HEAD method is also supported and
   * added to this list.
   *
   * @param url The URL.
   *
   * @return the list of HTTP methods allowed for this URL or an empty list.
   */
  public List<String> allows(String url) {
    List<String> methods = new ArrayList<String>();
    for (HttpMethod m : this.registry.keySet()) {
      ServiceMap mapping = this.registry.get(m);
      MatchingService service = mapping.match(url);
      if (service != null) {
        methods.add(m.toString());
        if (m == HttpMethod.GET) {
          methods.add(HttpMethod.HEAD.name());
        }
      }
    }
    return methods;
  }

  /**
   * Returns the HTTP method this service is mapped to.
   *
   * @param service The Berlioz service.
   *
   * @return the list of HTTP methods this Berlioz service is mapped to or <code>null</code>.
   */
  public HttpMethod getMethod(Service service) {
    if (service == null) return null;
    for (HttpMethod m : this.registry.keySet()) {
      ServiceMap mapping = this.registry.get(m);
      if (mapping.isMapped(service)) {
        return m;
      }
    }
    return null;
  }

  /**
   * Returns the list of URI Patterns that this service matches.
   *
   * @param service The Berlioz service.
   *
   * @return the list of URI Patterns that this service matches or an empty list.
   */
  public List<String> matches(Service service) {
    if (service == null) return Collections.emptyList();
    for (HttpMethod m : this.registry.keySet()) {
      ServiceMap mapping = this.registry.get(m);
      boolean mapped = mapping.isMapped(service);
      if (mapped) {
        return mapping.matches(service);
      }
    }
    return Collections.emptyList();
  }

  /**
   * Returns the content generator for this URL and HTTP method.
   *
   * <p>If the HTTP method specified is HEAD, this method will return the service for a GET request.
   *
   * @param url    The URL.
   * @param method The HTTP method.
   *
   * @return A content generator which URI pattern matches this URL and HTTP method or <code>null</code>.
   */
  public MatchingService get(String url, String method) {
    if (method == null) return null;
    return get(url, getHttpMethod(method));
  }

  /**
   * Returns the content generator for this URL and HTTP method.
   *
   * <p>If the HTTP method specified is HEAD, this method will return the service for a GET request.
   *
   * @param url    The URL.
   * @param method The HTTP method.
   *
   * @return A content generator which URI pattern matches this URL and HTTP method or <code>null</code>.
   */
  public MatchingService get(String url, HttpMethod method) {
    if (method == null) return null;
    HttpMethod m = method;
    if (method == HttpMethod.HEAD) {
      m = HttpMethod.GET;
    }
    ServiceMap mapping = this.registry.get(m);
    MatchingService service = mapping.match(url);
    return service;
  }

  /**
   * Returns an unmodifiable map of services by URI Pattern for the specified HTTP method.
   *
   * @param method the HTTP method.
   * @return an unmodifiable map of services by URI Pattern
   */
  public Map<String, Service> getServiceMap(HttpMethod method) {
    ServiceMap map = this.registry.get(method);
    return Collections.unmodifiableMap(map.mapping);
  }

  /**
   * Returns the set of registered services.
   *
   * @return the set of registered services.
   */
  public List<Service> getServices() {
    List<Service> services = new ArrayList<Service>();
    for (ServiceMap map : this.registry.values()) {
     services.addAll(map.mapping.values());
    }
    return services;
  }

  /**
   * Returns the list of services for the specified HTTP method.
   *
   * @param method the HTTP method.
   * @return the list of services.
   */
  public List<Service> getServices(HttpMethod method) {
    ServiceMap map = this.registry.get(method);
    Set<Service> services = new HashSet<Service>(map.mapping.values());
    return new ArrayList<Service>(services);
  }

  /**
   * Clears each generator mapping.
   */
  public void clear() {
    for (ServiceMap map : this.registry.values()) {
      map.clear();
    }
  }

  /**
   * @return The version of this registry.
   */
  public long version() {
    return this.version;
  }

  /**
   * Changed the version of this registry.
   */
  protected void touch() {
    this.version = System.currentTimeMillis();
  }

  /**
   * Returns the HTTP method for the specified value (case insensitive)
   *
   * @param method The method to find
   * @return The corresponding instance or <code>null</code> if no match.
   *
   * @throws IllegalArgumentException if the HTTP method is not valid
   */
  private HttpMethod getHttpMethod(String method) throws IllegalArgumentException {
    for (HttpMethod m : HttpMethod.values()) {
      if (m.name().equals(method.toUpperCase())) return m;
    }
    return null;
  }

  /**
   * Simply Maps generators to URI patterns.
   *
   * @author Christophe Lauret
   * @version 9 December 2011
   */
  private static class ServiceMap {

    /**
     * Maps services to the URI Pattern.
     */
    private final Map<String, Service> mapping = new Hashtable<String, Service>();

    /**
     * List of URI Patterns that match a service.
     */
    private final List<URIPattern> patterns = new ArrayList<URIPattern>();

    /**
     * Puts the given content generator in this map.
     *
     * @param pattern The URL pattern for this generator.
     * @param service The service to add.
     *
     * @return Always <code>true</code> ???
     */
    public boolean put(URIPattern pattern, Service service) {
      this.mapping.put(pattern.toString(), service);
      this.patterns.add(pattern);
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
      Service service = this.mapping.get(url);
      if (service != null) {
        URIPattern p = new URIPattern(url);
        match = new MatchingService(service, p, new URIResolver(url).resolve(p));

      // Check if matching URI pattern
      } else {
        // Find the URI pattern matching the given path info
        URIResolver resolver = new URIResolver(url);
        URIPattern p = resolver.find(this.patterns, MatchRule.BEST_MATCH);
        if (p != null) {
          service = this.mapping.get(p.toString());
          match = new MatchingService(service, p, resolver.resolve(p));
        }
      }
      return match;
    }

    /**
     * Indicates whether the specified service is mapped to any URL.
     *
     * @param service The Berlioz service to check.
     * @return <code>true</code> if mapped to any URL; <code>false</code> otherwise.
     */
    public boolean isMapped(Service service) {
      return this.mapping.containsValue(service);
    }

    /**
     * Returns the list of URI patterns that this service matches.
     *
     * @param service the Berlioz Service.
     * @return the list URI pattern is matches
     */
    public List<String> matches(Service service) {
      List<String> urls = new ArrayList<String>();
      for (Entry<String, Service> e : this.mapping.entrySet()) {
        if (e.getValue() == service) {
          urls.add(e.getKey());
        }
      }
      return urls;
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
