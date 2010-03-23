package org.weborganic.berlioz.content;

import java.util.Map;
import java.util.Stack;

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
 * @version 24 November 2009
 */
final class ContentAccessHandler10 extends DefaultHandler {

  /**
   * Displays debug information.
   */
  private static final ZLogger LOGGER = ZLoggerFactory.getLogger(ContentAccessHandler10.class);

  /**
   * Maps path infos to generator instances.
   */
  private final GeneratorRegistry _registry;

  /**
   * A buffer for character data.
   */
  private final StringBuffer ch = new StringBuffer();

  /**
   * The stack of elements.
   */
  private final Stack<String> elements = new Stack<String>();

  /**
   * The current area.
   */
  private String _area;

  /**
   * The current path info.
   */
  private String _pathInfo;

  /**
   * The current service.
   */
  private String _service;

  /**
   * The content generator instance. 
   */
  private ContentGenerator _generator;

  /**
   * The path information to the redirected location.
   */
  private ContentRedirect _redirect;

  /**
   * Indicates whether the generator should be used as the 'home' page for the directory.
   */
  private boolean _isHome;

  /**
   * Creates a new ContentAccessHandler.
   * 
   * <p>Note: it is more efficient to pass the generators rather than access the outer class.
   * 
   * @param registry The map of generators to populate.
   */
  public ContentAccessHandler10(GeneratorRegistry registry) {
    this._registry = registry;
  }

  /**
   * {@inheritDoc}
   */
  public void startElement(String uri, String localName, String qName, Attributes atts) {
    this.ch.setLength(0);
    if ("parameter".equals(localName) && isChildOf("redirect")) {
      this._redirect.addParameter(atts.getValue("name"));
    } else if ("path-info".equals(localName) && isChildOf("generator")) {
      this._isHome = "true".equals(atts.getValue("home"));
    } else if ("generators".equals(localName) && isChildOf("web-access")) {
      this._area = atts.getValue("area");
    }
    this.elements.push(localName);
  }

  /**
   * {@inheritDoc}
   */
  public void endElement(String uri, String localName, String qName) {
    this.elements.pop();

    // the path info to access the content generator
    if ("path-info".equals(localName) && isChildOf("generator")) {
      this._pathInfo = this.ch.toString();

    // the path info to the redirected content
    } else if ("path-info".equals(localName) && isChildOf("redirect")) {
      this._redirect = new ContentRedirect(this.ch.toString());

    // the service of the content generator
    } else if ("service".equals(localName) && isChildOf("generator")) {
      this._service = this.ch.toString();

    // the class of the content generator
    } else if ("class".equals(localName) && isChildOf("generator")) {
      try {
        this._generator = (ContentGenerator)Class.forName(this.ch.toString()).newInstance();
      } catch (Exception ex) {
        LOGGER.warn("(!) Failed to load "+this.ch.toString());
      }

    // the generators
    } else if ("generators".equals(localName) && isChildOf("web-access")) {
      this._area = null;

    // the generator
    } else if ("generator".equals(localName) && isChildOf("generators")) {
      // use no content by default
      if (this._generator == null)
        this._generator = new NoContent();
      // set up the generator
      this._generator.setArea(this._area);
      this._generator.setService(this._service);
      // assign a redirect
      if (this._redirect != null && this._generator instanceof ContentGeneratorBase) {
        ((ContentGeneratorBase)this._generator).setRedirect(this._redirect);
      }
      // check that the info path has not been already assigned        
//      if (this._registry.containsKey(this._pathInfo)) {
//        LOGGER.warn("(!) Path info '"+this._pathInfo+"' has been assigned to multiple generators");
//      }
      this._registry.register(this._generator, this._pathInfo);
      ((ContentGeneratorBase)this._generator).setPathInfo(this._pathInfo);
      LOGGER.debug("Assigning "+this._pathInfo+" to "+this._generator.getService()+"/"+this._generator);
      // the home directory
      if (this._isHome) {
        if (this._pathInfo.indexOf('/') < 0) {
          LOGGER.warn("(!) The path info for this generator '"+this._pathInfo+"' cannot be used as a home");
        } else {
          String dir = this._pathInfo.substring(0, this._pathInfo.lastIndexOf('/'));
//          if (this._registry.containsKey(dir)) {
//            LOGGER.warn("(!) Path info '"+dir+"' already has a home!");
//          }
          this._registry.register(this._generator, dir);
          LOGGER.debug("Assigning "+dir+" to "+this._generator.getService()+"/"+this._generator);
        }
      }
      // reset the values
      this._redirect = null;
      this._generator = null;
    }
    this.ch.setLength(0);
  }

  /**
   * {@inheritDoc}
   */
  public void characters(char[] buf, int pos, int len) {
    this.ch.append(buf, pos, len);
  }

  /**
   * Returns <code>true</code> if the current node is a child of the specified element.
   * 
   * @param parent The current parent element.
   * 
   * @return <code>true</code> if it is a child of the specified element;
   *         <code>false</code> otherwise.
   */
  private boolean isChildOf(String parent) {
    return parent.equals(this.elements.peek());
  }

}
