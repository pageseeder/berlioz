/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.content;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
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
 * @version 8 July 2010
 */
final class ServicesHandler10 extends DefaultHandler {

  /**
   * Displays debug information.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(ServicesHandler10.class);

  /**
   * Maps path infos to generator instances.
   */
  private final ServiceRegistry _registry;

  /**
   * The error handler to use.
   */
  private final ErrorHandler _errorHandler;

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
  private List<String> _patterns = new ArrayList<String>();

  /**
   * The current HTTP method for the service.
   */
  private String _method;

  /**
   * The document locator for use when reporting errors and warnings.
   */
  private Locator _locator;

  /**
   * The service builder.
   */
  private Service.Builder _builder = new Service.Builder();

  /**
   * The rules for the services.
   */
  private List<ServiceStatusRule> rules = new ArrayList<ServiceStatusRule>();

  /**
   * Creates a new ContentAccessHandler.
   * 
   * <p>Note: it is more efficient to pass the generators rather than access the outer class.
   * 
   * @param registry     The service registry to use.
   * @param errorHandler The error handler to use.
   */
  public ServicesHandler10(ServiceRegistry registry, ErrorHandler errorHandler) {
    this._registry = registry;
    this._errorHandler = errorHandler;
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
    // Identify element
    Element element = Element.get(localName);
    switch(element) {
      case SERVICE_CONFIG:
        this._registry.clear();
        break;

      case SERVICES:
        this._builder.group(atts.getValue("group"));
        if (rules.size() == 0) this.rules.add(ServiceStatusRule.DEFAULT_RULE);
        break;

      case SERVICE:
        if (rules.size() == 1) this.rules.add(ServiceStatusRule.DEFAULT_RULE);
        this._builder.id(atts.getValue("id"));
        this._builder.cache(atts.getValue("cache-control"));
        this._method = atts.getValue("method");
        break;

      case URL:
        this._patterns.add(atts.getValue("pattern"));
        break;

      case PARAMETER:
        this._builder.parameter(toParameter(atts));
        break;

      case RESPONSE_CODE:
        this.rules.add(ServiceStatusRule.newInstance(atts.getValue("use"), atts.getValue("rule")));
        break;

      case GENERATOR:
        try {
          ContentGenerator generator = (ContentGenerator)Class.forName(atts.getValue("class")).newInstance();
          this._builder.add(generator);
          this._builder.target(atts.getValue("target"));
          this._builder.name(atts.getValue("name"));
        } catch (Exception ex) {
          String message = "Failed to load generator "+ atts.getValue("class")+" for service "+this._builder.id();
          SAXParseException warning = new SAXParseException(message, this._locator, ex);
          warning(warning);
        }
        break;
      default:

    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void endElement(String uri, String localName, String qName) {
    // Identify element
    Element element = Element.get(localName);
    switch(element) {
      case SERVICE:
        this._builder.rule(this.rules.get(rules.size() - 1));
        Service service = this._builder.build();
        for (String pattern : this._patterns) {
          this._registry.register(service, pattern, this._method);
          LOGGER.debug("Assigning "+pattern+" ["+this._method+"] to "+service);
        }
        this._builder.reset();
        this._patterns.clear();
        if (rules.size() == 3) this.rules.remove(2);
        break;
      case SERVICES:
        if (rules.size() == 2) this.rules.remove(1);
        break;
      case SERVICE_CONFIG:
        if (rules.size() == 1) this.rules.remove(0);
        break;
      default:
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void warning(SAXParseException ex) throws SAXException {
    this._errorHandler.warning(ex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void error(SAXParseException ex) throws SAXException {
    this._errorHandler.error(ex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void fatalError(SAXParseException ex) throws SAXException {
    this._errorHandler.fatalError(ex);
  }

  // non-SAX methods
  // ----------------------------------------------------------------------------------------------

  /**
   * Creates a parameter specifications from the given attributes.
   *
   * @param atts the attributes of the parameter element.
   * @return a new <code>Parameter</code> instance or <code>null</code>.
   */
  private static Parameter toParameter(Attributes atts) {
    Parameter.Builder p = new Parameter.Builder(atts.getValue("name")); 
    p.value(atts.getValue("value")).source(atts.getValue("source")).def(atts.getValue("default"));
    try {
      return p.build();
    } catch (IllegalStateException ex) {
      LOGGER.debug("Bad parameter specifications - ignoring", ex);
      return null;
    }
  }

}
