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
 * @version 1 July 2011
 */
public final class XSLTErrorCollector extends ErrorCollector<TransformerException> implements ErrorListener {

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
   * @see ErrorListener#fatalError(TransformerException)
   * 
   * @param exception An fatal error reported by the transformer.
   * 
   * @throws TransformerException If thrown by the underlying {@link ErrorCollector}.
   */
  public void fatalError(TransformerException exception) throws TransformerException {
    this._logger.error(exception.getMessageAndLocation());
    collect(Level.FATAL, exception);
  }

  /**
   * @see ErrorListener#error(TransformerException)
   *
   * @param exception An error reported by the transformer.
   * 
   * @throws TransformerException If thrown by the underlying {@link ErrorCollector}.
   */
  public void error(TransformerException exception) throws TransformerException {
    this._logger.error(exception.getMessageAndLocation());
    collect(Level.ERROR, exception);
  }

  /**
   * @see ErrorListener#warning(TransformerException)
   * 
   * @param exception An warning reported by the transformer.
   * 
   * @throws TransformerException If thrown by the underlying {@link ErrorCollector}.
   */
  public void warning(TransformerException exception) throws TransformerException {
    this._logger.warn(exception.getMessageAndLocation());
    collect(Level.WARNING, exception);
  }

}
