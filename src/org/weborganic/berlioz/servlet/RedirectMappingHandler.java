package org.weborganic.berlioz.servlet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weborganic.berlioz.Beta;
import org.weborganic.furi.URIPattern;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Handles the XML for the URI pattern mapping configuration.
 * 
 * @author Christophe Lauret
 * @version 11 August 2010
 */
@Beta class RedirectMappingHandler extends DefaultHandler implements ContentHandler {

  /**
   * Displays debug information.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(RedirectMappingHandler.class);

  /**
   * Maps URI patterns to URI patterns.
   */
  private final Map<URIPattern, URIPattern> mapping = new HashMap<URIPattern, URIPattern>();

  /**
   * The list of permanent redirect. 
   */
  private final List<URIPattern> permanent = new ArrayList<URIPattern>();

  /**
   * {@inheritDoc}
   */
  @Override
  public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
    if ("redirect".equals(localName)) {
      URIPattern from = toPattern(atts.getValue("from"));
      URIPattern to = toPattern(atts.getValue("to"));
      if (from == null || to == null) return;
      boolean isPermanent = "yes".equals(atts.getValue("permanent"));
      this.mapping.put(from, to);
      if (isPermanent) {
        this.permanent.add(from);
      }
    }
  }

  /**
   * 
   * @param pattern
   * @return
   */
  private static URIPattern toPattern(String pattern) {
    try {
      return new URIPattern(pattern);
    } catch (IllegalArgumentException ex) {
      LOGGER.warn("Unparseable URI pattern: {} - ignored mapping", pattern);
      return null;
    }
  }
  
  public Map<URIPattern, URIPattern> getMapping() {
    return this.mapping;
  }  
  
  public List<URIPattern> getPermanent() {
    return this.permanent;
  }

}
