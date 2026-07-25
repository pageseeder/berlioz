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

import java.util.*;
import java.util.Map.Entry;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.furi.URIPattern;
import org.pageseeder.berlioz.furi.URIResolver;
import org.pageseeder.berlioz.furi.URIResolver.MatchRule;
import org.pageseeder.berlioz.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A registry for services.
 *
 * <p>Reads always observe a single, internally consistent snapshot of the registry: the mapping
 * of services and the registry {@link #version()} are held together in an immutable
 * {@link RegistryState} behind a single volatile reference, so a concurrent reader never sees a
 * mapping paired with the wrong version. Incremental mutation via {@link #register(Service,
 * URIPattern, HttpMethod)} and {@link #clear()} still mutates the live state in place — these are
 * low-level operations retained for direct/legacy use — whereas {@link #replaceWith(ServiceRegistry)}
 * swaps the entire state in a single assignment, which is what {@link ServiceLoader} uses to
 * publish a fully-built candidate registry atomically.
 *
 * <p>Note: this class is not synchronized and must be synchronized externally for compound
 * (read-then-write) operations; each individual method call is internally consistent.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.2
 * @since 0.8
 */
public final class ServiceRegistry {

  /**
   * The current, immutable state of this registry (mapping and version together), published as a
   * whole so readers never observe a mapping paired with a mismatched version.
   */
  private volatile RegistryState state;

  /**
   * Creates a new registry.
   */
  public ServiceRegistry() {
    Map<HttpMethod, ServiceMap> mapping = new EnumMap<>(HttpMethod.class);
    // Create a map for each mappable HTTP method
    for (HttpMethod m : HttpMethod.mappable()) {
      mapping.put(m, new ServiceMap());
    }
    this.state = new RegistryState(mapping, System.currentTimeMillis());
  }

  /**
   * Register the content generator.
   *
   * @param service the service to register.
   * @param pattern the URL pattern to associate with this content generator.
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
   * Computes a warning message when a service registration overrides a previously registered
   * service for the same URI pattern.
   *
   * @param previous the service that was previously registered for the pattern, if any.
   * @param next     the service being registered for the pattern.
   * @param pattern  the URI pattern both services were registered against.
   * @return the warning message, or {@code null} if there was no previous registration.
   */
  static @Nullable String overrideWarning(@Nullable Service previous, Service next, URIPattern pattern) {
    if (previous == null) return null;
    return next + " overrides " + previous + " for pattern " + pattern
        + " - " + previous + " will no longer be reachable";
  }

  /**
   * Returns the list of content generators for this URL.
   *
   * <p>This method iterates over each mappable HTTP method, in enum declaration order (see
   * {@link HttpMethod}).
   *
   * @param url The URL.
   *
   * @return A content generator which URI pattern matches this URL or <code>null</code>.
   */
  public @Nullable MatchingService get(String url) {
    for (Entry<HttpMethod, ServiceRegistry.ServiceMap> allMethods : this.state.mapping.entrySet()) {
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
    for (Entry<HttpMethod, ServiceMap> e : this.state.mapping.entrySet()) {
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
  public @Nullable HttpMethod getMethod(@Nullable Service service) {
    if (service == null) return null;
    for (Entry<HttpMethod, ServiceMap> e : this.state.mapping.entrySet()) {
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
  public List<String> matches(@Nullable Service service) {
    if (service == null) return List.of();
    for (ServiceMap mapping : this.state.mapping.values()) {
      boolean mapped = mapping.isMapped(service);
      if (mapped) return mapping.matches(service);
    }
    return List.of();
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
  public @Nullable MatchingService get(String url, @Nullable String method) {
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
  public @Nullable MatchingService get(String url, @Nullable HttpMethod method) {
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
   * Returns the list of registered services.
   *
   * @return the list of registered services.
   */
  public List<Service> getServices() {
    List<Service> services = new ArrayList<>();
    for (ServiceMap map : this.state.mapping.values()) {
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
   *
   * <p>This is a destructive, immediate operation on the live state — unlike a failed
   * {@link ServiceLoader} reload, which must leave the previously published registry untouched.
   */
  public void clear() {
    for (ServiceMap map : this.state.mapping.values()) {
      map.clear();
    }
  }

  /**
   * @return The version of this registry.
   */
  public long version() {
    return this.state.version;
  }

  /**
   * Changed the version of this registry.
   */
  void touch() {
    this.state = new RegistryState(this.state.mapping, System.currentTimeMillis());
  }

  /**
   * Extracts the current contents of this registry as a list of individual registrations, each
   * tagged with the given origin.
   *
   * <p>Used by {@link ServiceLoader} to convert a freshly-parsed, isolated per-source registry
   * into the {@link ServiceRegistration} values it merges into an aggregate candidate.
   *
   * @param origin the origin to tag every extracted registration with.
   *
   * @return the extracted registrations; empty if this registry has no services registered.
   */
  List<ServiceRegistration> toRegistrations(ServiceOrigin origin) {
    List<ServiceRegistration> registrations = new ArrayList<>();
    for (Entry<HttpMethod, ServiceMap> e : this.state.mapping.entrySet()) {
      HttpMethod method = e.getKey();
      for (Entry<String, Service> se : e.getValue().mapping.entrySet()) {
        registrations.add(new ServiceRegistration(se.getValue(), method, new URIPattern(se.getKey()), origin));
      }
    }
    return registrations;
  }

  /**
   * Atomically replaces the complete state of this registry (mapping and version together) with
   * the state currently held by {@code candidate}, in a single volatile assignment.
   *
   * <p>This is how {@link ServiceLoader} publishes a fully-built, validated candidate registry:
   * concurrent readers of this instance observe either the complete previous state or the
   * complete new state, never a partial mix of the two, and the version changes exactly once.
   *
   * <p>The {@code candidate} instance must not be used or mutated after this call; its internal
   * mapping is adopted by reference, not copied.
   *
   * @param candidate the fully-built candidate registry to publish.
   *
   * @throws NullPointerException if {@code candidate} is {@code null}.
   */
  void replaceWith(ServiceRegistry candidate) {
    Objects.requireNonNull(candidate, "candidate is required");
    this.state = new RegistryState(candidate.state.mapping, System.currentTimeMillis());
  }

  /**
   * Returns the HTTP method for the specified value (case-insensitive)
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
    ServiceMap mapping = this.state.mapping.get(method);
    // this should never happen since we initialize method
    if (mapping == null) throw new IllegalStateException("Failure to initialize service registry");
    return mapping;
  }

  /**
   * The immutable pairing of a registry's service mapping and its version, published as a whole
   * behind {@link ServiceRegistry#state} so the two never observably drift apart.
   */
  private static final class RegistryState {

    private final Map<HttpMethod, ServiceMap> mapping;

    private final long version;

    RegistryState(Map<HttpMethod, ServiceMap> mapping, long version) {
      this.mapping = mapping;
      this.version = version;
    }
  }

  /**
   * Simply Maps generators to URI patterns.
   *
   * @author Christophe Lauret
   * @version 0.12.4
   * @since 0.6.0
   */
  private static class ServiceMap {

    /**
     * To report errors.
     */
    private final Logger logger = LoggerFactory.getLogger(ServiceMap.class);

    /**
     * Maps services to the URI Pattern.
     */
    private final Map<String, Service> mapping = new HashMap<>();

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
      String warning = overrideWarning(previous, service, pattern);
      if (warning != null) {
        this.logger.warn(warning);
        // An entry for this exact pattern string is already in `patterns`, at its original
        // position; `mapping` (keyed by string) now resolves it to the new service, so there is
        // no need to add another entry — doing so would leave a stale duplicate that serves no
        // purpose and, if instead removed and re-appended, would shift this pattern's tie-break
        // position relative to unrelated patterns of equal score.
      } else {
        this.patterns.add(pattern);
      }
      return true;
    }

    /**
     * Returns the content generator for the specified URL.
     *
     * @param url The URL
     * @return the content generator for the specified URL.
     */
    public @Nullable MatchingService match(String url) {
      // Attempt to find the service directly
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
