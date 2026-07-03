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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.content.ServiceStatusRule.SelectType;
import org.pageseeder.berlioz.http.HttpMethod;
import org.pageseeder.berlioz.output.OutputType;
import org.pageseeder.berlioz.util.Strings;
import org.pageseeder.berlioz.xml.XmlWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A list of content generators or content instructions.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.0
 * @since 0.7
 */
public final class Service {

  private static final String GENERATOR = "generator";

  /**
   * The ID of this service.
   */
  private final String id;

  /**
   * The group this service is part of.
   */
  private final String group;

  /**
   * Indicates whether this service can be cached.
   */
  private final boolean cacheable;

  /**
   * The 'Cache-Control' header for this service.
   */
  private final String cache;

  /**
   * The flags attached to this service.
   */
  private final String flags;

  /**
   * Whether this service returns generator output directly, without a Berlioz envelope.
   */
  private final boolean direct;

  /**
   * How the status code of this service is calculated.
   */
  private final ServiceStatusRule rule;

  /**
   * The list of generators associated with this service.
   */
  private final List<BerliozGenerator> generators;

  /**
   * The intersection of output formats supported by all generators in this service.
   */
  private final Set<OutputType> supported;

  /**
   * Maps parameter specifications to a given generator instance.
   */
  private final Map<BerliozGenerator, List<Parameter>> allParameters;

  /**
   * Maps targets to a given generator instance.
   */
  private final Map<BerliozGenerator, String> targets;

  /**
   * Maps names to a given generator instance.
   */
  private final Map<BerliozGenerator, String> names;

  /**
   * Creates a new service.
   *
   * @param builder The builder used to create this service.
   */
  private Service(Builder builder) {
    this.id = Objects.requireNonNull(builder.id, "The service must have an id");
    this.group = Objects.requireNonNull(builder.group, "The service must belong to a collection (group)");
    this.rule = Objects.requireNonNull(builder.rule, "There must be a rule for this service");
    this.cache = Objects.requireNonNull(builder.cache, "The cache configuration cannot be null, use empty string for no cache");
    this.flags = Objects.requireNonNull(builder.flags, "The flags configuration cannot be null, use empty string for no flags");
    this.direct = builder.direct;
    this.generators = immutableList(builder.generators);
    this.allParameters = immutableMap(builder.allParameters);
    this.cacheable = isCacheable(this.generators);
    this.supported = computeSupported(this.generators);
    this.names = immutable3(builder.names);
    this.targets = immutable3(builder.targets);
  }

  /**
   * Returns the ID of this service.
   *
   * @return the ID of this service.
   */
  public String id() {
    return this.id;
  }

  /**
   * Returns the group this service is part of.
   *
   * @return the group this service is part of.
   */
  public String group() {
    return this.group;
  }

  /**
   * Returns the value of the 'Cache-Control' for this service.
   *
   * @return the value of the 'Cache-Control' for this service.
   */
  public String cache() {
    return this.cache;
  }

  /**
   * Returns the flags attached to this service.
   *
   * @return the flags attached to this service.
   */
  public String flags() {
    return this.flags;
  }

  /**
   * Returns whether this service returns generator output directly, without a Berlioz envelope.
   *
   * <p>When {@code true}, the single generator's output becomes the complete response body.
   * The {@code <root>} service wrapper, {@code XmlResponseHeader}, and {@code <content>} elements
   * are omitted for XML; the generator name wrapper is omitted for JSON.</p>
   *
   * @return {@code true} if the service is configured as direct; {@code false} otherwise.
   */
  public boolean isDirect() {
    return this.direct;
  }

  /**
   * Returns the status rule for this service.
   *
   * @return the status rule for this service.
   */
  public ServiceStatusRule rule() {
    return this.rule;
  }

  /**
   * Indicates whether this service is cacheable.
   *
   * <p>A service is cacheable only if all its generators are cacheable.
   *
   * @return <code>true</code> if this response is cacheable;
   *         <code>false</code> otherwise.
   */
  public boolean isCacheable() {
    return this.cacheable;
  }

  /**
   * Returns the list of generators for this service.
   *
   * @return the list of generators for this service.
   */
  public List<BerliozGenerator> generators() {
    return this.generators;
  }

  /**
   * Returns the set of output formats supported by this service.
   *
   * <p>This is the intersection of the {@link BerliozGenerator#supported()} sets of all
   * generators in this service. An empty set indicates a misconfigured service.</p>
   *
   * @return the supported output types; never {@code null}
   */
  public Set<OutputType> supported() {
    return this.supported;
  }

  /**
   * Returns the list of parameter specifications for the given generator.
   *
   * @param generator the generator for which we need the parameters.
   * @return the list of parameter specifications for the given generator.
   */
  public List<Parameter> parameters(BerliozGenerator generator) {
    List<Parameter> parameters = this.allParameters.get(generator);
    if (parameters == null) return List.of();
    return parameters;
  }

  /**
   * Returns the target of the given generator.
   *
   * @param generator the generator for which we need the target.
   * @return the target if any (might be <code>null</code>).
   */
  public @Nullable String target(BerliozGenerator generator) {
    return this.targets.get(generator);
  }

  /**
   * Returns the name of the given generator.
   *
   * @param generator the generator for which we need the name.
   * @return the name.
   */
  public String name(BerliozGenerator generator) {
    String name = this.names.get(generator);
    return name != null ? name : Strings.toKebabCase(generator.getClass().getSimpleName(), GENERATOR);
  }

  /**
   * Indicates whether the specified generator affects the status of the service.
   * @param generator The generator.
   * @return <code>true</code> if the generator affects the status of the service;
   *         <code>false</code> otherwise.
   */
  public boolean affectStatus(BerliozGenerator generator) {
    if (this.rule.appliesToAll()) return true;
    SelectType use = this.rule.use();
    switch (use) {
      case NAME:   return this.rule.appliesTo(name(generator));
      case TARGET: return this.rule.appliesTo(target(generator));
      default:     return false;
    }
  }

  @Override
  public String toString() {
    return "service:"+this.group +"/"+this.id;
  }

  /**
   * Serializes the specified service as XML.
   *
   * @param xml     the XML writer
   * @param method  the HTTP method the service is mapped to.
   * @param urls    the URI patterns this service matches
   */
  @Beta
  public void toXml(XmlWriter xml, HttpMethod method, List<String> urls) {
    toXml(xml, method, urls, null);
  }

  /**
   * Serializes the specified service as XML.
   *
   * @param xml          the XML writer
   * @param method       the HTTP method the service is mapped to.
   * @param urls         the URI patterns this service matches
   * @param cacheControl the cache control directives.
   */
  @Beta
  public void toXml(XmlWriter xml, HttpMethod method, List<String> urls, @Nullable String cacheControl) {
    xml.openElement("service", true);
    xml.attribute("id", this.id);
    xml.attribute("group", this.group);
    xml.attribute("method", method.toString().toLowerCase());
    if (!this.flags.isEmpty()) {
      xml.attribute("flags", this.flags);
    }
    if (this.direct) {
      xml.attribute("direct", "true");
    }

    // Caching information
    xml.attribute("cacheable", Boolean.toString(this.cacheable));
    if (this.cacheable) {
      if (!this.cache.isEmpty()) {
        xml.attribute("cache-control", this.cache.length());
      } else if (cacheControl != null) {
        xml.attribute("cache-control", cacheControl);
      }
    }

    xml.attribute("supported", supportedAttribute());

    // How the response code is calculated
    xml.openElement("response-code", true);
    xml.attribute("use", this.rule.use().toString().toLowerCase());
    xml.attribute("rule", this.rule.rule().toString().toLowerCase());
    xml.closeElement();

    // URI patterns
    //noinspection ConstantValue
    if (urls != null) {
      for (String url : urls) {
        xml.openElement("url", true);
        xml.attribute("pattern", url);
        xml.closeElement();
      }
    }

    // Generators
    for (BerliozGenerator generator : this.generators) {
      String target = target(generator);
      List<Parameter> parameters = parameters(generator);
      xml.openElement(GENERATOR, !parameters.isEmpty());
      xml.attribute("class", generator.getClass().getName());
      xml.attribute("name", name(generator));
      if (target != null) {
        xml.attribute("target", target);
      }
      xml.attribute("type", generatorType(generator));
      xml.attribute("cacheable", Boolean.toString(generator instanceof Cacheable));
      xml.attribute("affect-status", Boolean.toString(affectStatus(generator)));
      for (Parameter p : parameters) {
        xml.openElement("parameter", false);
        xml.attribute("name", p.name());
        xml.attribute("value", p.value());
        xml.closeElement();
      }
      xml.closeElement();
    }

    xml.closeElement();
  }

  private String supportedAttribute() {
    StringBuilder sb = new StringBuilder();
    for (OutputType t : OutputType.values()) {
      if (this.supported.contains(t)) {
        if (sb.length() > 0) sb.append(',');
        sb.append(t.name().toLowerCase(Locale.ROOT));
      }
    }
    return sb.toString();
  }

  /**
   * Indicates whether the generators are all cacheable.
   *
   * @param generators the list of generators to evaluate.
   * @return <code>true</code> if all generators implement the {@link Cacheable} interface;
   *         <code>false</code> otherwise.
   */
  static boolean isCacheable(List<BerliozGenerator> generators) {
    for (BerliozGenerator g : generators) {
      if (!(g instanceof Cacheable)) return false;
    }
    return true;
  }

  /**
   * Warns when a {@link Cacheable} generator has no usable {@code getETag} method.
   *
   * <p>The rule is:
   * <ul>
   *   <li>Any generator may override {@code getETag(Request)}</li>
   *   <li>Legacy {@link ContentGenerator} implementations may also override {@code getETag(ContentRequest)}</li>
   *   <li>All other generator types must override {@code getETag(Request)}</li>
   * </ul>
   *
   * @param generator the generator to check
   * @param logger the logger to use for warnings
   */
  static void warnCacheableMethod(BerliozGenerator generator, Logger logger) {
    String warning = cacheableMethodWarning(generator);
    if (warning != null) logger.warn(warning);
  }

  static @Nullable String cacheableMethodWarning(BerliozGenerator generator) {
    if (!(generator instanceof Cacheable)) return null;
    Class<?> cls = generator.getClass();
    boolean hasRequest = overridesMethod(cls, "getETag", Request.class);
    boolean hasContentRequest = overridesMethod(cls, "getETag", ContentRequest.class);
    if (!hasRequest && !hasContentRequest) {
      return cls.getName() + " implements Cacheable but overrides neither getETag method - ETag will always be null.";
    }
    if (!(generator instanceof ContentGenerator) && hasContentRequest && !hasRequest) {
      return cls.getName() + " implements Cacheable via getETag(ContentRequest) but is not a ContentGenerator - override getETag(Request) instead.";
    }
    return null;
  }

  private static boolean overridesMethod(Class<?> cls, String name, Class<?> paramType) {
    try {
      return !cls.getMethod(name, paramType).getDeclaringClass().isInterface();
    } catch (NoSuchMethodException e) {
      return false;
    }
  }

  /**
   * Computes the intersection of supported output formats across all generators.
   *
   * @param generators the list of generators.
   * @return the intersection of their supported sets; empty if any generator's set is disjoint.
   */
  static Set<OutputType> computeSupported(List<BerliozGenerator> generators) {
    if (generators.isEmpty()) return Set.of();
    Set<OutputType> result = new HashSet<>(generators.get(0).supported());
    for (int i = 1; i < generators.size(); i++) {
      result.retainAll(generators.get(i).supported());
      if (result.isEmpty()) return Set.of();
    }
    return Set.copyOf(result);
  }

  /**
   * Returns a string label identifying the most specific {@link BerliozGenerator} subtype.
   *
   * <ul>
   *   <li>{@code "generator"} — implements {@link Generator}</li>
   *   <li>{@code "raw"} — implements {@link RawGenerator}</li>
   *   <li>{@code "xml-json"} — implements both {@link XmlGenerator} and {@link JsonGenerator}</li>
   *   <li>{@code "xml"} — implements {@link XmlGenerator} only</li>
   *   <li>{@code "json"} — implements {@link JsonGenerator} only</li>
   *   <li>{@code "content"} — implements legacy {@link ContentGenerator}</li>
   *   <li>{@code "custom"} — direct {@link BerliozGenerator} implementation</li>
   * </ul>
   */
  static String generatorType(BerliozGenerator generator) {
    if (generator instanceof Generator) return GENERATOR;
    if (generator instanceof RawGenerator) return "raw";
    boolean isXml = generator instanceof XmlGenerator;
    boolean isJson = generator instanceof JsonGenerator;
    if (isXml && isJson) return "xml-json";
    if (isXml) return "xml";
    if (isJson) return "json";
    if (generator instanceof ContentGenerator) return "content";
    return "custom";
  }

  /**
   * A builder for services to ensure that <code>Service</code> instances are immutable.
   *
   * <p>The same builder can be used for builder multiple services.
   *
   * @author Christophe Lauret
   */
  static final class Builder {

    /**
     * The ID of the service to build.
     */
    private @Nullable String id;

    /**
     * The group the service to build belongs to.
     */
    private String group = "default";

    /**
     * The value of the 'Cache-Control' header for this service.
     */
    private String cache = "";

    /**
     * The value of the 'Cache-Control' header for this service.
     */
    private String flags = "";

    /**
     * Whether this service returns generator output directly, without a Berlioz envelope.
     */
    private boolean direct = false;

    /**
     * Maps targets to a given generator instance.
     */
    private @Nullable ServiceStatusRule rule;

    /**
     * The list of generators associated with this service.
     */
    private final List<BerliozGenerator> generators = new ArrayList<>();

    /**
     * Maps parameter specifications to a given generator instance.
     */
    private final Map<BerliozGenerator, List<Parameter>> allParameters = new HashMap<>();

    /**
     * Maps names to a given generator instance.
     */
    private final Map<BerliozGenerator, String> names = new HashMap<>();

    /**
     * Maps targets to a given generator instance.
     */
    private final Map<BerliozGenerator, String> targets = new HashMap<>();

    /**
     * Returns the ID of the service to build.
     *
     * @return the ID of the service to build.
     */
    public @Nullable String id() {
      return this.id;
    }

    /**
     * Returns the group of the service to build.
     *
     * @return the group of the service to build.
     */
    public String group() {
      return this.group;
    }

    /**
     * Sets the ID of the service to build.
     *
     * @param id the ID of the service to build.
     * @return this builder for easy chaining.
     */
    public Builder id(String id) {
      this.id = id;
      return this;
    }

    /**
     * Sets the group of the service to build.
     *
     * @param group the group of the service to build.
     * @return this builder for easy chaining.
     */
    public Builder group(@Nullable String group) {
      this.group = group != null ? group : "default";
      return this;
    }

    /**
     * Sets the cache control for this service.
     *
     * @param cache the 'Cache-Control' value of the service to build.
     * @return this builder for easy chaining.
     */
    public Builder cache(@Nullable String cache) {
      this.cache = cache != null ? cache : "";
      return this;
    }

    /**
     * Sets the flags for this service.
     *
     * @param flags the flags of the service to build.
     * @return this builder for easy chaining.
     */
    public Builder flags(@Nullable String flags) {
      this.flags = flags != null ? flags : "";
      return this;
    }

    /**
     * Sets whether this service returns generator output directly, without a Berlioz envelope.
     *
     * @param direct {@code true} for direct output; {@code false} (default) for a wrapped response.
     * @return this builder for easy chaining.
     */
    public Builder direct(boolean direct) {
      this.direct = direct;
      return this;
    }

    /**
     * Sets the status rule of the service to build.
     *
     * @param rule the status rule of the service to build.
     * @return this builder for easy chaining.
     */
    public Builder rule(ServiceStatusRule rule) {
      this.rule = rule;
      return this;
    }

    /**
     * Adds a parameter to the last content generator entered.
     *
     * @param p The parameter to add to the latest generator added.
     * @return this builder for easy chaining.
     */
    public Builder parameter(@Nullable Parameter p) {
      if (!this.generators.isEmpty() && p != null) {
        BerliozGenerator generator = this.generators.get(this.generators.size() - 1);
        List<Parameter> parameters = this.allParameters.computeIfAbsent(generator, k -> new ArrayList<>());
        parameters.add(p);
      }
      return this;
    }

    /**
     * Adds a generator to this service.
     *
     * @param g the generator to add to this service.
     * @return this builder for easy chaining.
     */
    public Builder add(BerliozGenerator g) {
      this.generators.add(g);
      return this;
    }

    /**
     * Sets the target of the latest generator added.
     *
     * @param target the target for the latest generator.
     * @return this builder for easy chaining.
     */
    public Builder target(@Nullable String target) {
      if (!this.generators.isEmpty() && target != null) {
        BerliozGenerator generator = this.generators.get(this.generators.size() - 1);
        this.targets.put(generator, target);
      }
      return this;
    }

    /**
     * Sets the name of the latest generator added.
     *
     * @param name the name for the latest generator.
     * @return this builder for easy chaining.
     */
    public Builder name(@Nullable String name) {
      if (!this.generators.isEmpty() && name != null) {
        BerliozGenerator generator = this.generators.get(this.generators.size() - 1);
        this.names.put(generator, name);
      }
      return this;
    }

    /**
     * Builds the service from the attributes in this builder.
     *
     * <p>Note: use the <code>reset</code> method to reset the class attributes.
     *
     * @return a new service instance.
     */
    public Service build() {
      Logger logger = LoggerFactory.getLogger(Builder.class);
      if (!this.cache.isEmpty() && !isCacheable(this.generators)) {
        logger.warn("Building non-cacheable service {} - cache control ignored.", this.id);
      }
      for (BerliozGenerator g : this.generators) {
        warnCacheableMethod(g, logger);
      }
      return new Service(this);
    }

    /**
     * Resets all the class attributes (except group).
     */
    public void reset() {
      this.id = null;
      this.cache = "";
      this.flags = "";
      this.direct = false;
      this.generators.clear();
      this.allParameters.clear();
      this.names.clear();
      this.targets.clear();
    }

  }

  /**
   * Returns a new identical immutable list.
   *
   * @param original the list maintained by the builder.
   * @return a new identical immutable list.
   */
  private static <T> List<T> immutableList(List<T> original) {
    if (original.isEmpty())
      return List.of();
    else if (original.size() == 1) return List.of(original.get(0));
    else
      return List.copyOf(original);
  }

  /**
   * Returns a new identical immutable map.
   *
   * @param original the map maintained by the builder.
   * @return a new identical immutable map.
   */
  private static Map<BerliozGenerator, List<Parameter>> immutableMap(Map<BerliozGenerator, List<Parameter>> original) {
    if (original.isEmpty()) return Map.of();
    Map<BerliozGenerator, List<Parameter>> map = new HashMap<>(original.size());
    for (Entry<BerliozGenerator, List<Parameter>> entry : original.entrySet()) {
      map.put(entry.getKey(), immutableList(entry.getValue()));
    }
    return Map.copyOf(map);
  }

  /**
   * Returns a new identical immutable map.
   *
   * @param original the map maintained by the builder.
   * @return a new identical immutable map.
   */
  private static Map<BerliozGenerator, String> immutable3(Map<BerliozGenerator, String> original) {
    return original.isEmpty() ? Map.of() : Map.copyOf(original);
  }
}
