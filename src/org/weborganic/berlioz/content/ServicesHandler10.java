package org.weborganic.berlioz.content;

import org.weborganic.berlioz.logging.ZLogger;
import org.weborganic.berlioz.logging.ZLoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A SAX2 handler that parses the XML content generators access.
 * 
 * <p>This class should remain protected as there is no reason to expose its method to the public API. 
 * 
 * @author Christophe Lauret (Weborganic)
 * @version 20 May 2010
 */
final class ServicesHandler10 extends DefaultHandler {

  /**
   * Displays debug information.
   */
  private static final ZLogger LOGGER = ZLoggerFactory.getLogger(ServicesHandler10.class);

  /**
   * Maps path infos to generator instances.
   */
  private final ServiceRegistry registry;

  /**
   * The elements used recognised by this handler.
   */
  private enum Element {

    GENERATOR,
    PARAMETER,
    SERVICE_CONFIG,
    SERVICES,
    SERVICE,
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
   * The current URI pattern for the service.
   */
  private String _pattern;

  /**
   * The current HTTP method for the service.
   */
  private String _method;

  /**
   * The service builder.
   */
  private Service.Builder _builder = new Service.Builder();

  /**
   * Creates a new ContentAccessHandler.
   * 
   * <p>Note: it is more efficient to pass the generators rather than access the outer class.
   * 
   * @param generators The map of generators to populate.
   */
  public ServicesHandler10(ServiceRegistry registry) {
    this.registry = registry;
  }

  /**
   * {@inheritDoc}
   */
  public void startElement(String uri, String localName, String qName, Attributes atts) {
    // Identify element
    Element element = Element.get(localName);
    switch(element) {
      case SERVICE_CONFIG:
        this.registry.clear();
        break;

      case SERVICES:
        this._builder.group(atts.getValue("group"));
        break;

      case SERVICE:
        this._builder.id(atts.getValue("id"));
        this._method = atts.getValue("method");

      case URL:
        this._pattern = atts.getValue("pattern");
        break;

      case PARAMETER:
        this._builder.parameter(toParameter(atts));
        break;

      case GENERATOR:
        try {
          ContentGenerator generator = (ContentGenerator)Class.forName(atts.getValue("class")).newInstance();
          ((ContentGeneratorBase)generator).setPathInfo(this._pattern);
          this._builder.add(generator);
        } catch (Exception ex) {
          LOGGER.warn("(!) Failed to load "+atts.getValue("class"));
          ex.printStackTrace();
        }
        break;
      default:

    }
  }

  /**
   * {@inheritDoc}
   */
  public void endElement(String uri, String localName, String qName) {
    // Identify element
    Element element = Element.get(localName);
    switch(element) {
      case SERVICE:
        Service service = this._builder.build();
        this.registry.register(service, this._pattern, this._method);
        this._builder.reset();
        LOGGER.debug("Assigning "+this._pattern+" ["+this._method+"] to "+service.id());
      default:
    }
  }

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
