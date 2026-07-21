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

import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.util.CollectedError;
import org.pageseeder.berlioz.util.CollectedError.Level;
import org.pageseeder.berlioz.util.ErrorCollector;
import org.slf4j.Logger;

/**
 * An XSLT error listener will collect all the exceptions reported by the transformer.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.0
 * @since 0.8
 */
public final class XsltErrorCollector extends ErrorCollector<TransformerException> implements ErrorListener {

  /**
   * The logger to use to report errors
   */
  private final Logger logger;

  /** The configured threshold at which diagnostics make the XSLT operation fail. */
  private final XsltErrorSensitivity sensitivity;

  /**
   * Creates a new error collector.
   *
   * @param logger A logger to report errors when the listener's methods are called.
   */
  public XsltErrorCollector(Logger logger) {
    this(logger, XsltErrorSensitivity.from(GlobalSettings.get(BerliozOption.XSLT_SENSITIVITY)));
  }

  XsltErrorCollector(Logger logger, XsltErrorSensitivity sensitivity) {
    this.logger = logger;
    this.sensitivity = sensitivity;
    setException(sensitivity.threshold());
    setErrorFlag(sensitivity.threshold());
  }

  /**
   * Throws the first collected diagnostic at or above the configured sensitivity.
   *
   * <p>This postcondition protects against processors that report a diagnostic to the listener but
   * nevertheless return normally from the JAXP operation.
   *
   * @throws TransformerException if a collected diagnostic reached the configured sensitivity
   */
  public void throwIfThresholdReached() throws TransformerException {
    if (!hasError()) return;
    for (CollectedError<TransformerException> item : getErrors()) {
      if (this.sensitivity.includes(item.level())) throw item.error();
    }
  }

  /**
   * @see ErrorListener#fatalError(TransformerException)
   *
   * @param exception A fatal error reported by the transformer.
   *
   * @throws TransformerException If thrown by the underlying {@link ErrorCollector}.
   */
  @Override
  public void fatalError(TransformerException exception) throws TransformerException {
    this.logger.error(exception.getMessageAndLocation());
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
    this.logger.error(exception.getMessageAndLocation());
    collect(Level.ERROR, exception);
  }

  /**
   * @see ErrorListener#warning(TransformerException)
   *
   * @param exception A warning reported by the transformer.
   *
   * @throws TransformerException If thrown by the underlying {@link ErrorCollector}.
   */
  @Override
  public void warning(TransformerException exception) throws TransformerException {
    this.logger.warn(exception.getMessageAndLocation());
    collect(Level.WARNING, exception);
  }

}
