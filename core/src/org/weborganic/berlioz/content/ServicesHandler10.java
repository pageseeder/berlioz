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
 * @version 11 December 2009
 */
final class ServicesHandler10 extends DefaultHandler {

  /**
   * Displays debug information.
   */
  private static final ZLogger LOGGER = ZLoggerFactory.getLogger(ServicesHandler10.class);

  /**
   * Maps path infos to generator instances.
   */
  private final GeneratorRegistry registry;

  /**
   * A buffer for character data.
   */
  private final StringBuffer ch = new StringBuffer();

  /**
   * The elements used recognised by this handler.
   */
  private enum Element {
    
    GENERATOR,
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
   * The current group (ex area).
   */
  private String _group;

  /**
   * The current path info.
   */
  private String _pattern;

  /**
   * The current HTTP method.
   */
  private String _method;

  /**
   * The content generator instance. 
   */
  private ContentGenerator _generator;

  /**
   * Creates a new ContentAccessHandler.
   * 
   * <p>Note: it is more efficient to pass the generators rather than access the outer class.
   * 
   * @param generators The map of generators to populate.
   */
  public ServicesHandler10(GeneratorRegistry registry) {
    this.registry = registry;
  }

  /**
   * {@inheritDoc}
   */
  public void startElement(String uri, String localName, String qName, Attributes atts) {
    this.ch.setLength(0);

    // Identify element
    Element element = Element.get(localName);
    switch(element) {
      case SERVICE_CONFIG:
        this.registry.clear();
        break;

      case SERVICES:
        this._group = atts.getValue("group");
        break;

      case SERVICE:
        atts.getValue("id");
        this._method = atts.getValue("method");
//      id
//      method

      case URL:
        this._pattern = atts.getValue("pattern");
        break;

      case GENERATOR:
        try {
          this._generator = (ContentGenerator)Class.forName(atts.getValue("class")).newInstance();
        } catch (Exception ex) {
          LOGGER.warn("(!) Failed to load "+this.ch.toString());
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
      case GENERATOR:
        // use no content by default
        if (this._generator == null)
          this._generator = new NoContent();
        // set up the generator
        this._generator.setArea(this._group);
        this._generator.setService(this.ch.toString());
        // TODO: check that the info path has not been already assigned
//        if (this.registry.containsKey(this._pattern)) {
//          LOGGER.warn("(!) Path info '"+this._pattern+"' has been assigned to multiple generators");
//        }
        this.registry.register(this._generator, this._pattern, this._method);
        ((ContentGeneratorBase)this._generator).setPathInfo(this._pattern);
        LOGGER.debug("Assigning "+this._pattern+" ["+this._method+"] to "+this._generator+" as "+this._generator.getService());
      default:
    }
    this.ch.setLength(0);
  }

  /**
   * {@inheritDoc}
   */
  public void characters(char[] buf, int pos, int len) {
    this.ch.append(buf, pos, len);
  }

}
