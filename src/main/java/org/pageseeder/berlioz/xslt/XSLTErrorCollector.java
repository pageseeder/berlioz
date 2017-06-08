/*
 * Copyright 2015 Allette Systems (Australia)
 * http://www.allette.com.au
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pageseeder.berlioz.xslt;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

import org.pageseeder.berlioz.util.CollectedError.Level;
import org.pageseeder.berlioz.util.ErrorCollector;
import org.slf4j.Logger;

/**
 * An XSLT error listener will collect all the exceptions reported by the transformer.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.11.2
 * @since Berlioz 0.8
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
  @Override
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
  @Override
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
  @Override
  public void warning(TransformerException exception) throws TransformerException {
    this._logger.warn(exception.getMessageAndLocation());
    collect(Level.WARNING, exception);
  }

}
