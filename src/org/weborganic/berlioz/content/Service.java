package org.weborganic.berlioz.content;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A list of of content generators or content instructions.
 * 
 * @author Christophe Lauret
 * @version 20 May 2010
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
   * The list of generators associated with this service.
   */
  private final List<ContentGenerator> _generators;

  /**
   * Maps parameter specifications to a given generator instance.
   */
  private final Map<ContentGenerator, List<Parameter>> _parameters;

  /**
   * Creates a new service.
   * 
   * @param id         the ID of the service.
   * @param group      the group the service is part of.
   * @param generators the list of generators.
   * @param parameters the parameters spec for each generator.
   */
  private Service(String id, String group, List<ContentGenerator> generators, Map<ContentGenerator, List<Parameter>> parameters) {
    this._id = id;
    this._group = group;
    this._generators = generators;
    this._parameters = parameters;
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
   * A builder for services to ensure that service instances are immutable
   * 
   * @author Christophe Lauret
   * @version 20 May 2010
   */
  static final class Builder {

    /** 
     * The ID of the service to build.
     */
    private String _id;

    /** 
     * The ID of the service to build.
     */
    private String _group;

    /**
     * The list of generators associated with this service.
     */
    private final List<ContentGenerator> _generators = new ArrayList<ContentGenerator>();

    /**
     * Maps parameter specifications to a given generator instance.
     */
    private final Map<ContentGenerator, List<Parameter>> _parameters = new HashMap<ContentGenerator, List<Parameter>>();

    /**
     * Creates a new builder
     */
    public Builder() {
    }

    public Builder id(String id) {
      this._id = id;
      return this;
    }

    public Builder group(String group) {
      this._group = group;
      return this;
    }

    /**
     * Adds a parameter to the last content generator entered
     * 
     * @return
     */
    public Builder parameter(Parameter p) {
      if (this._generators.size() > 0) {
        ContentGenerator generator = this._generators.get(this._generators.size() - 1);
        List<Parameter> parameters = this._parameters.get(generator);
        if (parameters == null) {
          parameters = new ArrayList<Parameter>();
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
     * @return this builder
     */
    public Builder add(ContentGenerator g) {
      this._generators.add(g);
      // Backward compatibility
      g.setArea(this._group);
      g.setService(this._id);
      return this;
    }

    /**
     * Builds the service from the attributes in this builder.
     * 
     * <p>Note: use <code>reset</code> method to reset.
     * 
     * @return a new service.
     */
    public Service build() {
      return new Service(this._id, this._group, immutable(this._generators), immutable(this._parameters));
    }

    /**
     * Resets the class attributes.
     */
    public void reset() {
      this._id = null;
      this._generators.clear();
      this._parameters.clear();
    }

    /**
     * Returns a new identical immutable list.
     * 
     * @return a new identical immutable list.
     */
    private static List<ContentGenerator> immutable(List<ContentGenerator> original) {
      if (original.isEmpty()) {
        return Collections.emptyList();
      } else if (original.size() == 1) {
        return Collections.singletonList(original.get(0));
      } else {
        return Collections.unmodifiableList(new ArrayList<ContentGenerator>(original));
      }
    }

    /**
     * Returns a new identical immutable map.
     * 
     * @param the map maintained by the builder.
     * @return a new identical immutable map.
     */
    private static Map<ContentGenerator, List<Parameter>> immutable(Map<ContentGenerator, List<Parameter>> original) {
      if (original.isEmpty()) {
        return Collections.emptyMap();
      } else if (original.size() == 1) {
        Entry<ContentGenerator, List<Parameter>> entry = original.entrySet().iterator().next();
        return Collections.singletonMap(entry.getKey(), immutable2(entry.getValue()));
      } else {
        Map<ContentGenerator, List<Parameter>> map = new HashMap<ContentGenerator, List<Parameter>>();
        for (Entry<ContentGenerator, List<Parameter>> entry : original.entrySet()) {
          map.put(entry.getKey(), immutable2(entry.getValue()));
        }
        return Collections.unmodifiableMap(map);
      }
    }

    /**
     * Returns a new identical immutable list.
     * 
     * @return a new identical immutable list.
     */
    private static List<Parameter> immutable2(List<Parameter> original) {
      if (original.isEmpty()) {
        return Collections.emptyList();
      } else if (original.size() == 1) {
        return Collections.singletonList(original.get(0));
      } else {
        return Collections.unmodifiableList(new ArrayList<Parameter>(original));
      }
    }

  }

}
