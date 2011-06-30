package org.weborganic.berlioz.xslt;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

import org.slf4j.Logger;
import org.weborganic.berlioz.util.ErrorCollector;
import org.weborganic.berlioz.util.CollectedError.Level;

/**
 * An XSLT error listener will collect all the exceptions reported by the transformer.
 * 
 * @author Christophe Lauret
 * @version 30 June 2011
 */
public class XSLTErrorCollector extends ErrorCollector<TransformerException> implements ErrorListener {

  /**
   * The logger to use to report errors
   */
  private final Logger _logger;

  /**
   * Creates a new error collector.
   * 
   * @param logger A logger to report errors when the listener's methods are called.
   */
  public XSLTErrorCollector(Logger logger) {
    this._logger = logger;
  }

  /**
   * {@inheritDoc}
   */
  public void fatalError(TransformerException exception) throws TransformerException {
    this._logger.error(exception.getMessageAndLocation());
    collect(Level.FATAL, exception);
  }

  /**
   * {@inheritDoc}
   */
  public void error(TransformerException exception) throws TransformerException {
    this._logger.error(exception.getMessageAndLocation());
    collect(Level.ERROR, exception);
  }

  /**
   * {@inheritDoc}
   */
  public void warning(TransformerException exception) throws TransformerException {
    this._logger.warn(exception.getMessageAndLocation());
    collect(Level.WARNING, exception);
  }

}