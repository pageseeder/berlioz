/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.xml;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

/**
 * The default error handler for Berlioz parsers.
 *
 * <p>This implementation treats all errors as fatal and will always throw an exception.
 *
 * @author Christophe Lauret (Weborganic)
 *
 * @version Berlioz 0.6.0 - 24 March 2010
 * @since Berlioz 0.6
 */
public final class BerliozErrorHandler implements ErrorHandler {

  /**
   * A single instance.
   */
  private static final BerliozErrorHandler SINGLETON = new BerliozErrorHandler();

  /**
   * Creates a new Berlioz error handler.
   */
  private BerliozErrorHandler() {
  }

  /**
   * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
   *
   * @param exception A SAX parse fatal reported by the SAX parser.
   *
   * @throws SAXParseException Identical to the parameter.
   */
  @Override
  public void fatalError(SAXParseException exception) throws SAXParseException {
    throw exception;
  }

  /**
   * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
   *
   * @param exception A SAX parse error reported by the SAX parser.
   *
   * @throws SAXParseException Identical to the parameter.
   */
  @Override
  public void error(SAXParseException exception) throws SAXParseException {
    throw exception;
  }

  /**
   * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
   *
   * @param exception A SAX parse warning reported by the SAX parser.
   *
   * @throws SAXParseException Identical to the parameter.
   */
  @Override
  public void warning(SAXParseException exception) throws SAXParseException {
    throw exception;
  }

  /**
   * Returns an error handler instance.
   *
   * @return an error handler instance.
   */
  public static BerliozErrorHandler getInstance() {
    return SINGLETON;
  }
}
