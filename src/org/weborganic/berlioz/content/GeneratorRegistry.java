package org.weborganic.berlioz.content;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.weborganic.furi.URIPattern;
import org.weborganic.furi.URIResolver;

/**
 * A registry for content generators.
 * 
 * <p>Note: this class is not synchronized and must be synchronised externally.
 * 
 * @author Christophe Lauret
 * @version 11 December 2009
 */
public final class GeneratorRegistry {

  /**
   * Maps content generators to the appropriate HTTP method.
   */
  private final Map<HttpMethod, GeneratorMap> registry;

  /**
   * The HTTP methods supported.
   */
  private enum HttpMethod {GET, POST, PUT, DELETE};

  /**
   * Creates a new registry.
   */
  public GeneratorRegistry() {
    this.registry = new Hashtable<HttpMethod, GeneratorMap>();
    // Create a map for each method
    for (HttpMethod m : HttpMethod.values()) {
      this.registry.put(m, new GeneratorMap());
    }
  }

  /**
   * Register the content generator.
   * 
   * @param generator the content generator to register.
   * @param pattern   the URL pattern to associate to this content generator.
   * @param method    the method for this url pattern.
   */
  public void register(ContentGenerator generator, String pattern, String method) {
    // preliminary checks
    if (generator == null) throw new IllegalArgumentException("No generator to register.");
    if (pattern == null) throw new IllegalArgumentException("URL Pattern must be specified to register a generator.");
    if (method == null) throw new IllegalArgumentException("HTTP Method must be specified to register a generator.");
    // Find and check the HTTP method
    HttpMethod m = getHttpMethod(method);
    // Register the generator with the URL pattern
    this.registry.get(m).put(pattern, generator);
  }

  /**
   * Register the content generator for the HTTP methods in use this registry only.
   * 
   * @param generator the content generator to register.
   * @param pattern   the URL pattern to associate to this content generator.
   */
  public void register(ContentGenerator generator, String pattern) {
    // preliminary checks
    if (generator == null) throw new IllegalArgumentException("No generator to register.");
    if (pattern == null) throw new IllegalArgumentException("URL Pattern must be specified to register a generator.");
    // Find and check the HTTP method
    for (HttpMethod m : this.registry.keySet()) {
      // Register the generator with the URL pattern
      this.registry.get(m).put(pattern, generator);
    }
  }

  /**
   * Returns the first content generator for this URL.
   * 
   * <p>This method iterates over each HTTP method in the following order: GET, POST, PUT, DELETE.
   * 
   * @param url    The URL.
   *
   * @return A content generator which URI pattern matches this URL or <code>null</code>.
   */
  public ContentGenerator get(String url) {
    for (HttpMethod m : this.registry.keySet()) {
      GeneratorMap mapping = this.registry.get(m);
      ContentGenerator generator = mapping.get(url);
      if (generator != null) return generator;
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
  public ContentGenerator get(String url, String method) {
    HttpMethod m = getHttpMethod(method);
    GeneratorMap mapping = this.registry.get(m);
    ContentGenerator generator = mapping.get(url);
    return generator;
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
  private static class GeneratorMap {

    /**
     * Maps content generators to the appropriate HTTP method.
     */
    private final Map<String,ContentGenerator> mapping = new Hashtable<String, ContentGenerator>();

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
    public boolean put(String pattern, ContentGenerator generator) {
      this.mapping.put(pattern, generator);
      this.patterns.add(new URIPattern(pattern));
      return true;
    }

    /**
     * Returns the content generator for the specified URL.
     * 
     * @param url The URL   
     * @return the content generator for the specified URL.
     */
    public ContentGenerator get(String url) {
      // Attempt to the find generator directly
      ContentGenerator generator = mapping.get(url);
      // Check if matching URI pattern
      if (generator == null) {
        // Find the URI pattern matching the given path info
        URIResolver resolver = new URIResolver(url);
        URIPattern p = resolver.find(patterns);
        // TODO: Include results in content request
//        URIResolveResult result = resolver.resolve(p);
        if (p != null)
          generator = mapping.get(p.toString());
      }
      return generator;
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
