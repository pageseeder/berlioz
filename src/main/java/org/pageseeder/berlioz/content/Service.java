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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.content.ServiceStatusRule.SelectType;
import org.pageseeder.berlioz.http.HttpMethod;
import org.pageseeder.xmlwriter.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A list of of content generators or content instructions.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.11.2
 * @since Berlioz 0.7
 */
public final class Service {

  /**
   * The ID of this service.
   */
  private final String _id;

  /**
   * The group this service is part of.
   */
  private final String _group;

  /**
   * Indicates whether this service can be cached.
   */
  private final boolean _cacheable;

  /**
   * The 'Cache-Control' header for this service.
   */
  private final String _cache;

  /**
   * The flags attached to this service.
   */
  private final String _flags;

  /**
   * How the status code this service is calculated.
   */
  private final ServiceStatusRule _rule;

  /**
   * The list of generators associated with this service.
   */
  private final List<ContentGenerator> _generators;

  /**
   * Maps parameter specifications to a given generator instance.
   */
  private final Map<ContentGenerator, List<Parameter>> _parameters;

  /**
   * Maps targets to a given generator instance.
   */
  private final Map<ContentGenerator, String> _targets;

  /**
   * Maps names to a given generator instance.
   */
  private final Map<ContentGenerator, String> _names;

  /**
   * Creates a new service.
   *
   * @param builder The builder used to create this service.
   */
  private Service(Builder builder) {
    this._id = Objects.requireNonNull(builder.id, "The service must have an id");
    this._group = Objects.requireNonNull(builder.group, "The service must belong to a collection (group)");
    this._rule = Objects.requireNonNull(builder.rule, "There must be a rule for this service");
    this._cache = builder.cache;
    this._flags = builder.flags;
    this._generators = immutableList(builder._generators);
    this._parameters = immutableMap(builder._parameters);
    this._cacheable = isCacheable(this._generators);
    this._names = immutable3(builder._names);
    this._targets = immutable3(builder._targets);
  }

  /**
   * Returns the ID of this service.
   *
   * @return the ID of this service.
   */
  public String id() {
    return this._id;
  }

  /**
   * Returns the group this service is part of.
   *
   * @return the group this service is part of.
   */
  public String group() {
    return this._group;
  }

  /**
   * Returns the value of the 'Cache-Control' for this service.
   *
   * @return the value of the 'Cache-Control' for this service.
   */
  public String cache() {
    return this._cache;
  }

  /**
   * Returns the flags attached to this service.
   *
   * @return the flags attached to this service.
   */
  public String flags() {
    return this._flags;
  }

  /**
   * Returns the status rule for this service.
   *
   * @return the status rule for this service.
   */
  public ServiceStatusRule rule() {
    return this._rule;
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
    return this._cacheable;
  }

  /**
   * Returns the list of generators for this service.
   *
   * @return the list of generators for this service.
   */
  public List<ContentGenerator> generators() {
    return this._generators;
  }

  /**
   * Returns the list of parameter specifications for the given generator.
   *
   * @param generator the content generator for which we need to parameters.
   * @return the list of parameter specifications for the given generator.
   */
  public List<Parameter> parameters(ContentGenerator generator) {
    List<Parameter> parameters = this._parameters.get(generator);
    if (parameters == null) return Collections.emptyList();
    return parameters;
  }

  /**
   * Returns the target of the given generator.
   *
   * @param generator the content generator for which we need the target.
   * @return the target if any (may be <code>null</code>).
   */
  public @Nullable String target(ContentGenerator generator) {
    return this._targets.get(generator);
  }

  /**
   * Returns the name of the given generator.
   *
   * @param generator the content generator for which we need the name.
   * @return the name.
   */
  public String name(ContentGenerator generator) {
    String name = this._names.get(generator);
    // TODO Use snake-case for consistency
    return name != null? name : generator.getClass().getSimpleName();
  }

  /**
   * Indicates whether the specified generator affects the status of the service.
   * @param generator The generator.
   * @return <code>true</code> if the generator affects the status of the service;
   *         <code>false</code> otherwise.
   */
  public boolean affectStatus(ContentGenerator generator) {
    if (this._rule.appliesToAll()) return true;
    SelectType use = this._rule.use();
    switch (use) {
      case NAME:   return this._rule.appliesTo(name(generator));
      case TARGET: return this._rule.appliesTo(target(generator));
      default:     return false;
    }
  }

  @Override
  public String toString() {
    return "service:"+this._group+"/"+this._id;
  }

  /**
   * Serialises the specified service as XML.
   *
   * @param xml     the XML writer
   * @param method  the HTTP method the service is mapped to.
   * @param urls    the URI patterns this service matches
   *
   * @throws IOException if thrown by the XML writer.
   */
  @Beta
  public void toXML(XMLWriter xml, HttpMethod method, List<String> urls) throws IOException {
    toXML(xml, method, urls, null);
  }

  /**
   * Serialises the specified service as XML.
   *
   * @param xml          the XML writer
   * @param method       the HTTP method the service is mapped to.
   * @param urls         the URI patterns this service matches
   * @param cacheControl the cache control directives.
   *
   * @throws IOException if thrown by the XML writer.
   */
  @Beta
  public void toXML(XMLWriter xml, HttpMethod method, List<String> urls, @Nullable String cacheControl) throws IOException {
    xml.openElement("service", true);
    xml.attribute("id", this._id);
    xml.attribute("group", this._group);
    if (method != null) {
      xml.attribute("method", method.toString().toLowerCase());
    }
    if (this._flags.length() > 0) {
      xml.attribute("flags", this._flags);
    }

    // Caching information
    xml.attribute("cacheable", Boolean.toString(this._cacheable));
    if (this._cacheable) {
      if (this._cache.length()>0) {
        xml.attribute("cache-control", this._cache.length());
      } else if (cacheControl != null) {
        xml.attribute("cache-control", cacheControl);
      }
    }

    // How the response code is calculated
    xml.openElement("response-code", true);
    xml.attribute("use", this._rule.use().toString().toLowerCase());
    xml.attribute("rule", this._rule.rule().toString().toLowerCase());
    xml.closeElement();

    // URI patterns
    if (urls != null) {
      for (String url : urls) {
        xml.openElement("url", true);
        xml.attribute("pattern", url);
        xml.closeElement();
      }
    }

    // Generators
    for (ContentGenerator generator : this._generators) {
      String target = target(generator);
      List<Parameter> parameters = parameters(generator);
      xml.openElement("generator", !parameters.isEmpty());
      xml.attribute("class", generator.getClass().getName());
      xml.attribute("name", name(generator));
      if (target != null) {
        xml.attribute("target", target);
      }
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

  /**
   * Indicates whether the list of generators are all cacheable.
   *
   * @param generators the list of generators to evaluate.
   * @return <code>true</code> if all generators implement the {@link Cacheable} interface;
   *         <code>false</code> otherwise.
   */
  static boolean isCacheable(List<ContentGenerator> generators) {
    for (ContentGenerator g : generators) {
      if (!(g instanceof Cacheable)) return false;
    }
    return true;
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
     * Maps targets to a given generator instance.
     */
    private @Nullable ServiceStatusRule rule;

    /**
     * The list of generators associated with this service.
     */
    private final List<ContentGenerator> _generators = new ArrayList<>();

    /**
     * Maps parameter specifications to a given generator instance.
     */
    private final Map<ContentGenerator, List<Parameter>> _parameters = new HashMap<>();

    /**
     * Maps names to a given generator instance.
     */
    private final Map<ContentGenerator, String> _names = new HashMap<>();

    /**
     * Maps targets to a given generator instance.
     */
    private final Map<ContentGenerator, String> _targets = new HashMap<>();

    /**
     * Creates a new builder.
     */
    public Builder() {
    }

    /**
     * Returns the ID of the service to build.
     *
     * @return the ID of the service to build.
     */
    public @Nullable String id() {
      return this.id;
    }

    /**
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
      this.group = group != null? group : "default";
      return this;
    }

    /**
     * Sets the cache control for this service.
     *
     * @param cache the 'Cache-Control' value of the service to build.
     * @return this builder for easy chaining.
     */
    public Builder cache(@Nullable String cache) {
      this.cache = cache != null? cache : "";
      return this;
    }

    /**
     * Sets the flags for this service.
     *
     * @param flags the flags of the service to build.
     * @return this builder for easy chaining.
     */
    public Builder flags(@Nullable String flags) {
      this.flags = flags != null? flags : "";
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
      if (this._generators.size() > 0 && p != null) {
        ContentGenerator generator = this._generators.get(this._generators.size() - 1);
        List<Parameter> parameters = this._parameters.get(generator);
        if (parameters == null) {
          parameters = new ArrayList<>();
          this._parameters.put(generator, parameters);
        }
        parameters.add(p);
      }
      return this;
    }

    /**
     * Adds a content generator to this service.
     *
     * @param g the content generator to add to this service.
     * @return this builder for easy chaining.
     */
    public Builder add(ContentGenerator g) {
      this._generators.add(g);
      return this;
    }

    /**
     * Sets the target of the latest content generator added.
     *
     * @param target the target for the latest content generator.
     * @return this builder for easy chaining.
     */
    public Builder target(@Nullable String target) {
      if (this._generators.size() > 0 && target != null) {
        ContentGenerator generator = this._generators.get(this._generators.size() - 1);
        this._targets.put(generator, target);
      }
      return this;
    }

    /**
     * Sets the name of the latest content generator added.
     *
     * @param name the name for the latest content generator.
     * @return this builder for easy chaining.
     */
    public Builder name(@Nullable String name) {
      if (this._generators.size() > 0 && name != null) {
        ContentGenerator generator = this._generators.get(this._generators.size() - 1);
        this._names.put(generator, name);
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
      // warn when attempting to use cache control with uncacheable service
      if (this.cache.length() > 0 && !isCacheable(this._generators)) {
        Logger logger = LoggerFactory.getLogger(Builder.class);
        logger.warn("Building non-cacheable service {} - cache control ignored.", this.id);
      }
      return new Service(this);
    }

    /**
     * Resets the all the class attributes (except group).
     */
    public void reset() {
      this.id = null;
      this.cache = "";
      this.flags = "";
      this._generators.clear();
      this._parameters.clear();
      this._names.clear();
      this._targets.clear();
    }

  }

  /**
   * Returns a new identical immutable list.
   *
   * @param original the list maintained by the builder.
   * @return a new identical immutable list.
   */
  private static <T> List<T> immutableList(List<@NonNull T> original) {
    if (original.isEmpty())
      return Collections.emptyList();
    else if (original.size() == 1) return Collections.singletonList(original.get(0));
    else
      return Collections.unmodifiableList(new ArrayList<>(original));
  }

  /**
   * Returns a new identical immutable map.
   *
   * @param original the map maintained by the builder.
   * @return a new identical immutable map.
   */
  private static Map<ContentGenerator, List<Parameter>> immutableMap(Map<ContentGenerator, List<Parameter>> original) {
    if (original.isEmpty())
      return Collections.emptyMap();
    else if (original.size() == 1) {
      Entry<ContentGenerator, List<Parameter>> entry = original.entrySet().iterator().next();
      return Collections.singletonMap(entry.getKey(), immutableList(entry.getValue()));
    } else {
      Map<ContentGenerator, List<Parameter>> map = new HashMap<>();
      for (Entry<ContentGenerator, List<Parameter>> entry : original.entrySet()) {
        map.put(entry.getKey(), immutableList(entry.getValue()));
      }
      return Collections.unmodifiableMap(map);
    }
  }

  /**
   * Returns a new identical immutable map.
   *
   * @param original the map maintained by the builder.
   * @return a new identical immutable map.
   */
  private static Map<ContentGenerator, String> immutable3(Map<ContentGenerator, String> original) {
    if (original.isEmpty())
      return Collections.emptyMap();
    else if (original.size() == 1) {
      Entry<ContentGenerator, String> entry = original.entrySet().iterator().next();
      return Collections.singletonMap(entry.getKey(), entry.getValue());
    } else {
      Map<ContentGenerator, String> map = new HashMap<>();
      for (Entry<ContentGenerator, String> entry : original.entrySet()) {
        map.put(entry.getKey(), entry.getValue());
      }
      return Collections.unmodifiableMap(map);
    }
  }
}
