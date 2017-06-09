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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;
import org.pageseeder.berlioz.furi.URIPattern;
import org.pageseeder.berlioz.generator.NoContent;
import org.pageseeder.berlioz.http.HttpMethod;
import org.pageseeder.berlioz.util.Pair;
import org.pageseeder.berlioz.xml.SAXErrorCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A SAX2 handler that parses the XML content generators access.
 *
 * <p>This class should remain protected as there is no reason to expose its method to the public API.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.11.2
 * @since Berlioz 0.7
 */
final class ServicesHandler10 extends DefaultHandler {

  /**
   * Displays debug information.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(ServicesHandler10.class);

  /**
   * Where all the information about services is collected and registered.
   */
  private final ServiceRegistry _registry;

  /**
   * The error handler to use.
   */
  private final SAXErrorCollector _collector;

  /**
   * The elements used recognised by this handler.
   */
  private enum Element {

    /** 'generator' element name */
    GENERATOR,

    /** 'parameter' element name */
    PARAMETER,

    /** 'service-config' element name */
    SERVICE_CONFIG,

    /** 'services' element name */
    SERVICES,

    /** 'service' element name */
    SERVICE,

    /** 'response-code' element name */
    RESPONSE_CODE,

    /** 'url' element name */
    URL;

    /**
     * The name of the element.
     */
    private final String _name;

    /**
     * Creates a new element using the name of the element is the lower case value of the
     * constant and uses '-' instead of '_' to separated words.
     */
    private Element() {
      this._name = name().toLowerCase().replace('_', '-');
    }

    /**
     * Returns the element corresponding to the specified element name.
     *
     * @param name The name of the element.
     * @return The name of the element.
     */
    public static @Nullable Element get(String name) {
      for (Element element : values()) {
        if (element._name.equals(name)) return element;
      }
      return null;
    }

    @Override
    public String toString() {
      return this._name;
    }

  };

  /**
   * The list of URI patterns for the current service.
   */
  private final List<URIPattern> _patterns = new ArrayList<>();

  /**
   * The current HTTP method for the service.
   */
  private HttpMethod method = HttpMethod.GET;

  /**
   * The document locator for use when reporting errors and warnings.
   */
  private @Nullable Locator locator;

  /**
   * The service builder.
   */
  private final Service.Builder _builder = new Service.Builder();

  /**
   * The rules for the services, this list is used like a stack.
   */
  private final List<ServiceStatusRule> _rules = new ArrayList<>();

  /**
   * Used to detect duplicate URI Patterns.
   */
  private final Set<Pair<HttpMethod, URIPattern>> _patternsToMethod = new HashSet<>();

  /**
   * Used to detect duplicate service groups.
   */
  private final Set<String> _groups = new HashSet<>();

  /**
   * Creates a new handler that will update the specified registry and use the given error handler.
   *
   * <p>Note: it is more efficient to pass the generators rather than access the outer class.
   *
   * @param registry  The service registry to use.
   * @param collector The error handler to collect errors.
   *
   * @throws NullPointerException If any of the method arguments is <code>null</code>.
   */
  public ServicesHandler10(ServiceRegistry registry, SAXErrorCollector collector) {
    this._registry = Objects.requireNonNull(registry, "service registry is required");
    this._collector = Objects.requireNonNull(collector, "error collector is required");
  }

  @Override
  public void setDocumentLocator(Locator locator) {
    this.locator = locator;
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
    // Do not continue if there is an error
    if (this._collector.hasError()) return;
    // Identify element
    Element element = Element.get(localName);
    if (element == null) {
      warning("Unknown element "+localName+" found");
      return;
    }
    switch(element) {
      case SERVICE_CONFIG:
        this._registry.clear();
        break;

      case SERVICES:
        String group = atts.getValue("group");
        this._builder.group(group);
        if (!this._groups.add(this._builder.group())) {
          warning("Duplicate group of services '"+group+"' - services will belong to the same group");
        }
        // If no rule where defined at the 'service-config' level, we assume the default rule
        if (this._rules.size() == 0) {
          this._rules.add(ServiceStatusRule.DEFAULT_RULE);
        }
        break;

      case SERVICE:
        // If no rule where defined at the 'services' level, we assume the default rule
        if (this._rules.size() == 1) {
          this._rules.add(ServiceStatusRule.DEFAULT_RULE);
        }
        String id = atts.getValue("id");
        this._builder.id(id != null? id : "");
        this._builder.cache(atts.getValue("cache-control"));
        this._builder.flags(atts.getValue("flags"));
        handleMethod(atts.getValue("method"));
        break;

      case URL:
        handlePattern(atts.getValue("pattern"));
        break;

      case PARAMETER:
        this._builder.parameter(toParameter(atts));
        break;

      case RESPONSE_CODE:
        handleResponseCode(atts.getValue("use"), atts.getValue("rule"));
        break;

      case GENERATOR:
        handleGenerator(atts);
        break;
      default:

    }
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    // Do not continue if there is an error
    if (this._collector.hasError()) return;
    // Identify element
    Element element = Element.get(localName);
    // We've already put a warning in the startElement
    if (element == null) return;
    switch(element) {
      case SERVICE:
        HttpMethod method = this.method;
        // Assign the latest rule
        this._builder.rule(this._rules.get(this._rules.size() - 1));
        if (!"".equals(this._builder.id())) {
          Service service = this._builder.build();
          if (this._patterns.isEmpty()) {
            warning("No URI pattern match service "+service.id()+" - service will be ignored");
          } else {
            for (URIPattern pattern : this._patterns) {
              this._registry.register(service, pattern, method);
              LOGGER.debug("Assigning "+pattern+" ["+method+"] to "+service);
            }
          }
        } else {
          warning("Service cannot be created without an id");
        }
        this._builder.reset();
        this._patterns.clear();
        // Any rule specific to the 'service'? remove it
        if (this._rules.size() == 3) {
          this._rules.remove(2);
        }
        break;
      case SERVICES:
        // Any rule specific to the 'services'? remove it
        if (this._rules.size() == 2) {
          this._rules.remove(1);
        }
        break;
      case SERVICE_CONFIG:
        // Any rule specific to the 'service-config'? remove it
        if (this._rules.size() == 1) {
          this._rules.remove(0);
        }
        break;
      default:
    }
  }

  /**
   * Ensure that we use the correct error handler so that warnings and errors can be collected.
   *
   * {@inheritDoc}
   */
  @Override
  public void warning(SAXParseException ex) throws SAXException {
    this._collector.warning(ex);
  }

  /**
   * Ensure that we use the correct error handler so that warnings and errors can be collected.
   *
   * {@inheritDoc}
   */
  @Override
  public void error(SAXParseException ex) throws SAXException {
    this._collector.error(ex);
  }

  /**
   * Ensure that we use the correct error handler so that warnings and errors can be collected.
   *
   * {@inheritDoc}
   */
  @Override
  public void fatalError(SAXParseException ex) throws SAXException {
    this._collector.fatalError(ex);
  }

  // non-SAX methods
  // ----------------------------------------------------------------------------------------------

  /**
   * Creates a parameter specifications from the given attributes.
   *
   * @param atts the attributes of the parameter element.
   * @return a new <code>Parameter</code> instance or <code>null</code>.
   *
   * @throws SAXException Only if thrown by error handler
   */
  private @Nullable Parameter toParameter(Attributes atts) throws SAXException {
    String name = atts.getValue("name");
    String value = atts.getValue("value");
    if (name == null || value == null) {
      warning("Bad parameter specifications - ignoring");
      return null;
    }
    return new Parameter(name, value);
  }

  /**
   * Handle the pattern attribute reporting duplicates and invalid patterns as warnings.
   *
   * @param pattern The URI pattern string to parse.
   *
   * @throws SAXException Only if thrown by underlying error handler.
   */
  private void handlePattern(@Nullable String pattern) throws SAXException {
    if (pattern == null) {
      warning("Ignoring null pattern", null);
      return;
    }
    try {
      URIPattern p = new URIPattern(pattern);
      Pair<HttpMethod, URIPattern> k = new Pair<>(this.method, p);
      if (this._patternsToMethod.add(k)) {
        this._patterns.add(p);
      } else {
        warning("Ignoring duplicate pattern '"+p+"'", null);
      }
    } catch (IllegalArgumentException ex) {
      warning("Ignoring invalid pattern '"+pattern+"'", ex);
    }
  }

  /**
   * Handle the response rule element.
   *
   * @param use  The 'use' attribute
   * @param rule The 'rule' attribute
   *
   * @throws SAXException Only if thrown by underlying error handler.
   */
  private void handleResponseCode(@Nullable String use, @Nullable String rule) throws SAXException {
    if (use == null) {
      warning("Ignoring response code rule: @use is null", null);
      return;
    }
    try {
      this._rules.add(ServiceStatusRule.newInstance(use, rule));
    } catch (IllegalArgumentException ex) {
      warning("Ignoring bad response code definition: "+ex.getMessage(), ex);
    }
  }

  /**
   * Handle the response rule element.
   *
   * @param method  The 'method' attribute
   *
   * @throws SAXException Only if thrown by underlying error handler.
   */
  private void handleMethod(@Nullable String method) throws SAXException {
    if (method == null) {
      warning("Ignoring null method for service id "+this._builder.id()+" defaulting to GET");
      this.method = HttpMethod.GET;
    } else {
      try {
        this.method = HttpMethod.valueOf(method.toUpperCase());
      } catch (IllegalArgumentException ex) {
        warning("Ignoring illegal method '"+method+"' for service id "+this._builder.id(), ex);
      }
    }
  }

  /**
   * Handles the loading of the content generator.
   *
   * @param atts The attributes of the 'content-generator' element.
   *
   * @throws SAXException Only if thrown by underlying error handler.
   */
  private void handleGenerator(Attributes atts) throws SAXException {
    String className = atts.getValue("class");
    ContentGenerator generator;
    try {
      // Allow unspecified class (defaults to no content)
      if (className == null || className.length() ==0) {
        generator = new NoContent();
      } else {
        generator = (ContentGenerator)Class.forName(className).newInstance();
      }
      this._builder.add(generator);
      this._builder.target(atts.getValue("target"));
      this._builder.name(atts.getValue("name"));
    } catch (NoClassDefFoundError error) {
      ClassNotFoundException ex = new ClassNotFoundException("Class definition problem", error);
      warning("Failed to create generator "+className+" for service "+this._builder.id(), ex);
    } catch (ClassNotFoundException ex) {
      warning("Failed to find generator "+className+" for service "+this._builder.id(), ex);
    } catch (IllegalAccessException ex) {
      warning("Failed to access generator "+className+" for service "+this._builder.id(), ex);
    } catch (InstantiationException ex) {
      warning("Failed to instantiate generator "+className+" for service "+this._builder.id(), ex);
    }
  }

  /**
   * Convenience method to report a warning to the underlying error handler.
   *
   * <p>This method creates the SAXParseException and provides the locator.
   *
   * @param message The message for the warning.
   *
   * @throws SAXException Only if thrown by underlying error handler.
   */
  public void warning(String message) throws SAXException {
    SAXParseException warning = new SAXParseException(message, this.locator);
    warning(warning);
  }

  /**
   * Convenience method to report a warning to the underlying error handler.
   *
   * <p>This method creates the SAXParseException and provides the locator.
   *
   * @param message The message for the warning.
   * @param ex      Any associated exception.
   *
   * @throws SAXException Only if thrown by underlying error handler.
   */
  public void warning(String message, @Nullable Exception ex) throws SAXException {
    SAXParseException warning = new SAXParseException(message, this.locator, ex);
    warning(warning);
  }

}
