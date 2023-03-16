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
package org.pageseeder.berlioz.content;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;
import org.pageseeder.berlioz.furi.URIPattern;
import org.pageseeder.berlioz.furi.URIResolver;
import org.pageseeder.berlioz.furi.URIResolver.MatchRule;
import org.pageseeder.berlioz.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A registry for services.
 *
 * <p>Note: this class is not synchronized and must be synchronized externally.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.12.4
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
    this.registry = new EnumMap<>(HttpMethod.class);
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
    Objects.requireNonNull(service, "No service to register.");
    Objects.requireNonNull(pattern, "URL Pattern must be specified to register a service.");
    Objects.requireNonNull(method, "HTTP Method must be specified to register a service.");
    // Register the generator with the URL pattern
    getMapping(method).put(pattern, service);
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
  public @Nullable MatchingService get(String url) {
    for (Entry<HttpMethod, ServiceRegistry.ServiceMap> allMethods : this.registry.entrySet()) {
      ServiceMap mapping = allMethods.getValue();
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
    List<String> methods = new ArrayList<>();
    for (Entry<HttpMethod, ServiceMap> e : this.registry.entrySet()) {
      HttpMethod m = e.getKey();
      ServiceMap mapping = e.getValue();
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
  public @Nullable HttpMethod getMethod(Service service) {
    if (service == null) return null;
    for (Entry<HttpMethod, ServiceMap> e : this.registry.entrySet()) {
      ServiceMap mapping = e.getValue();
      if (mapping.isMapped(service)) return e.getKey();
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
    for (ServiceMap mapping : this.registry.values()) {
      boolean mapped = mapping.isMapped(service);
      if (mapped) return mapping.matches(service);
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
  public @Nullable MatchingService get(String url, String method) {
    if (method == null) return null;
    HttpMethod m = getHttpMethod(method);
    if (m == null) return null;
    return get(url, m);
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
  public @Nullable MatchingService get(String url, HttpMethod method) {
    if (method == null) return null;
    HttpMethod m = method;
    if (method == HttpMethod.HEAD) {
      m = HttpMethod.GET;
    }
    ServiceMap mapping = getMapping(m);
    return mapping.match(url);
  }

  /**
   * Returns an unmodifiable map of services by URI Pattern for the specified HTTP method.
   *
   * @param method the HTTP method.
   * @return an unmodifiable map of services by URI Pattern
   */
  public Map<String, Service> getServiceMap(HttpMethod method) {
    ServiceMap map = getMapping(method);
    return Collections.unmodifiableMap(map.mapping);
  }

  /**
   * Returns the set of registered services.
   *
   * @return the set of registered services.
   */
  public List<Service> getServices() {
    List<Service> services = new ArrayList<>();
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
    ServiceMap map = getMapping(method);
    Set<Service> services = new HashSet<>(map.mapping.values());
    return new ArrayList<>(services);
  }

  /**
   * Clears the service registry.
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
  void touch() {
    this.version = System.currentTimeMillis();
  }

  /**
   * Returns the HTTP method for the specified value (case insensitive)
   *
   * @param method The method to find
   * @return The corresponding instance or <code>null</code> if no match.
   */
  private @Nullable HttpMethod getHttpMethod(String method) {
    for (HttpMethod m : HttpMethod.values()) {
      if (m.name().equals(method.toUpperCase())) return m;
    }
    return null;
  }

  private ServiceMap getMapping(HttpMethod method) {
    ServiceMap mapping = this.registry.get(method);
    // this should never happen since we initialise method
    if (mapping == null) throw new IllegalStateException("Failure to initialize service registry");
    return mapping;
  }

  /**
   * Simply Maps generators to URI patterns.
   *
   * @author Christophe Lauret
   * @version Berlioz 0.12.4
   */
  private static class ServiceMap {

    /**
     * To report errors.
     */
    private final Logger logger = LoggerFactory.getLogger(ServiceRegistry.class);

    /**
     * Maps services to the URI Pattern.
     */
    private final Map<String, Service> mapping = new Hashtable<>();

    /**
     * List of URI Patterns that match a service.
     */
    private final List<URIPattern> patterns = new ArrayList<>();

    /**
     * Puts the given content generator in this map.
     *
     * @param pattern The URL pattern for this generator.
     * @param service The service to add.
     *
     * @return Always <code>true</code>
     */
    public boolean put(URIPattern pattern, Service service) {
      Service previous = this.mapping.put(pattern.toString(), service);
      if (previous != null) {
        this.logger.warn("Service ID={} was already registered to {}", previous, pattern);
      }
      this.patterns.add(pattern);
      return true;
    }

    /**
     * Returns the content generator for the specified URL.
     *
     * @param url The URL
     * @return the content generator for the specified URL.
     */
    public @Nullable MatchingService match(String url) {
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
          if (service != null) {
            match = new MatchingService(service, p, resolver.resolve(p));
          }
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
      List<String> urls = new ArrayList<>();
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
