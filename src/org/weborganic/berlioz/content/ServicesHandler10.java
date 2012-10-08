/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.content;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weborganic.berlioz.http.HttpMethod;
import org.weborganic.berlioz.util.Pair;
import org.weborganic.berlioz.xml.SAXErrorCollector;
import org.weborganic.furi.URIPattern;
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
 * @author Christophe Lauret (Weborganic)
 * @version Berlioz 0.9.0 - 13 October 2011
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
    public static Element get(String name) {
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
  private final List<URIPattern> _patterns = new ArrayList<URIPattern>();

  /**
   * The current HTTP method for the service.
   */
  private HttpMethod _method;

  /**
   * The document locator for use when reporting errors and warnings.
   */
  private Locator _locator;

  /**
   * The service builder.
   */
  private final Service.Builder _builder = new Service.Builder();

  /**
   * The rules for the services, this list is used like a stack.
   */
  private final List<ServiceStatusRule> _rules = new ArrayList<ServiceStatusRule>();

  /**
   * Used to detect duplicate URI Patterns.
   */
  private final Set<Pair<HttpMethod, URIPattern>> _patternsToMethod = new HashSet<Pair<HttpMethod, URIPattern>>();

  /**
   * Used to detect duplicate service groups.
   */
  private final Set<String> _groups = new HashSet<String>();

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
    if (registry == null || collector == null) throw new NullPointerException("Missing argument");
    this._registry = registry;
    this._collector = collector;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setDocumentLocator(Locator locator) {
    this._locator = locator;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
    // Do not continue if there is an error
    if (this._collector.hasError()) return;
    // Identify element
    Element element = Element.get(localName);
    switch(element) {
      case SERVICE_CONFIG:
        this._registry.clear();
        break;

      case SERVICES:
        String group = atts.getValue("group");
        this._builder.group(group);
        if (!this._groups.add(group)) {
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
        this._builder.id(atts.getValue("id"));
        this._builder.cache(atts.getValue("cache-control"));
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

  /**
   * {@inheritDoc}
   */
  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    // Do not continue if there is an error
    if (this._collector.hasError()) return;
    // Identify element
    Element element = Element.get(localName);
    switch(element) {
      case SERVICE:
        // Assign the latest rule
        this._builder.rule(this._rules.get(this._rules.size() - 1));
        Service service = this._builder.build();
        if (this._method == null) {
          warning("No HTTP method for "+service.id()+" - service will be ignored");
        } else if (this._patterns.isEmpty()) {
          warning("No URI pattern match service "+service.id()+" - service will be ignored");
        } else {
          for (URIPattern pattern : this._patterns) {
            this._registry.register(service, pattern, this._method);
            LOGGER.debug("Assigning "+pattern+" ["+this._method+"] to "+service);
          }
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
  private Parameter toParameter(Attributes atts) throws SAXException {
    Parameter.Builder p = new Parameter.Builder(atts.getValue("name"));
    p.value(atts.getValue("value"));
    try {
      return p.build();
    } catch (IllegalStateException ex) {
      warning("Bad parameter specifications - ignoring");
      return null;
    }
  }

  /**
   * Handle the pattern attribute reporting duplicates and invalid patterns as warnings.
   *
   * @param pattern The URI pattern string to parse.
   *
   * @throws SAXException Only if thrown by underlying error handler.
   */
  private void handlePattern(String pattern) throws SAXException {
    try {
      URIPattern p = new URIPattern(pattern);
      Pair<HttpMethod, URIPattern> k = new Pair<HttpMethod, URIPattern>(this._method, p);
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
  private void handleResponseCode(String use, String rule) throws SAXException {
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
  private void handleMethod(String method) throws SAXException {
    try {
      this._method = HttpMethod.valueOf(method.toUpperCase());
    } catch (NullPointerException ex) {
      warning("Ignoring null method for service id "+this._builder.id(), ex);
    } catch (IllegalArgumentException ex) {
      warning("Ignoring illegal method '"+method+"' for service id "+this._builder.id(), ex);
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
    try {
      ContentGenerator generator = (ContentGenerator)Class.forName(atts.getValue("class")).newInstance();
      this._builder.add(generator);
      this._builder.target(atts.getValue("target"));
      this._builder.name(atts.getValue("name"));
    } catch (NoClassDefFoundError error) {
      ClassNotFoundException ex = new ClassNotFoundException("Class definition problem", error);
      warning("Failed to create generator "+ atts.getValue("class")+" for service "+this._builder.id(), ex);
    } catch (ClassNotFoundException ex) {
      warning("Failed to find generator "+ atts.getValue("class")+" for service "+this._builder.id(), ex);
    } catch (IllegalAccessException ex) {
      warning("Failed to access generator "+ atts.getValue("class")+" for service "+this._builder.id(), ex);
    } catch (InstantiationException ex) {
      warning("Failed to instantiate generator "+ atts.getValue("class")+" for service "+this._builder.id(), ex);
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
    SAXParseException warning = new SAXParseException(message, this._locator);
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
  public void warning(String message, Exception ex) throws SAXException {
    SAXParseException warning = new SAXParseException(message, this._locator, ex);
    warning(warning);
  }

}
