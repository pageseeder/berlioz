package org.weborganic.berlioz.xml;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.weborganic.berlioz.Beta;
import org.weborganic.berlioz.util.CollectedError;
import org.weborganic.berlioz.util.CollectedError.Level;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

/**
 * Collects SAX errors.
 * 
 * @author Christophe Lauret (Weborganic)
 * @version 28 June 2011
 */
@Beta
public final class ErrorCollector implements ErrorHandler {

  /**
   * The collected errors.
   */
  private List<CollectedError<SAXParseException>> errors = new ArrayList<CollectedError<SAXParseException>>();

  /**
   * Indicates whether an error has been reported during parsing.
   * 
   * <p>This flag is used to indicate that there is no point in processing the file any further
   * because they could cause exceptions.
   */
  private boolean _hasError = false;

  /**
   * The logger to use to report errors
   */
  private final Logger _logger;

  /**
   * Creates a new Berlioz error handler.
   * 
   * @param logger A logger to report errors when the SAX methods are called.
   */
  public ErrorCollector(Logger logger) {
    this._logger = logger;
  }

  /**
   * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
   * 
   * @param exception A SAX parse fatal reported by the SAX parser.
   * 
   * @throws SAXParseException Identical to the parameter.
   */
  public void fatalError(SAXParseException exception) throws SAXParseException {
    this.errors.add(new CollectedError<SAXParseException>(Level.FATAL, exception));
    _logger.error("{} (line: {})", exception.getMessage(), exception.getLineNumber());
    throw exception;
  }

  /**
   * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
   * 
   * @param exception A SAX parse error reported by the SAX parser.
   */
  public void error(SAXParseException exception) {
    this.errors.add(new CollectedError<SAXParseException>(Level.ERROR, exception));
    _logger.error("{} (line: {})", exception.getMessage(), exception.getLineNumber());
  }

  /**
   * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
   * 
   * @param exception A SAX parse warning reported by the SAX parser.
   */
  public void warning(SAXParseException exception) {
    this.errors.add(new CollectedError<SAXParseException>(Level.WARNING, exception));
    _logger.warn("{} (line: {})", exception.getMessage(), exception.getLineNumber());
  }

  /**
   * Returns the list of collected errors.
   * 
   * @return the list of collected errors.
   */
  public List<CollectedError<SAXParseException>> getErrors() {
    return this.errors;
  }

  /**
   * Indicate whether the services registry could be produced without errors.
   * 
   * @return <code>true</code> if any error was reported;
   *         <code>false</code> otherwise.
   */
  public boolean hasError() {
    return this._hasError;
  }
}
