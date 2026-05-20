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
package org.pageseeder.berlioz.xml;

import org.pageseeder.berlioz.util.CollectedError.Level;
import org.pageseeder.berlioz.util.ErrorCollector;
import org.slf4j.Logger;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

/**
 * A SAX error handler will collect all the exceptions reported by the SAX parser.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.8.5 - 15 August 2011
 * @since Berlioz 0.8
 */
public final class SAXErrorCollector extends ErrorCollector<SAXParseException> implements ErrorHandler {

  private static final String MESSAGE_PATTERN = "{} (line: {})";

  /**
   * The logger to use to report errors
   */
  private final Logger logger;

  /**
   * Creates a new Berlioz error handler.
   *
   * @param logger A logger to report errors when the SAX methods are called.
   */
  public SAXErrorCollector(Logger logger) {
    this.logger = logger;
  }

  /**
   * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
   *
   * @param exception A SAX parse fatal reported by the SAX parser.
   *
   * @throws SAXParseException If thrown by the underlying {@link ErrorCollector}.
   */
  @Override
  public void fatalError(SAXParseException exception) throws SAXParseException {
    this.logger.error(MESSAGE_PATTERN, exception.getMessage(), exception.getLineNumber());
    collect(Level.FATAL, exception);
  }

  /**
   * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
   *
   * @param exception A SAX parse error reported by the SAX parser.
   *
   * @throws SAXParseException If thrown by the underlying {@link ErrorCollector}.
   */
  @Override
  public void error(SAXParseException exception) throws SAXParseException {
    this.logger.error(MESSAGE_PATTERN, exception.getMessage(), exception.getLineNumber());
    collect(Level.ERROR, exception);
  }

  /**
   * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
   *
   * @param exception A SAX parse warning reported by the SAX parser.
   *
   * @throws SAXParseException If thrown by the underlying {@link ErrorCollector}.
   */
  @Override
  public void warning(SAXParseException exception) throws SAXParseException {
    this.logger.warn(MESSAGE_PATTERN, exception.getMessage(), exception.getLineNumber());
    collect(Level.WARNING, exception);
  }

}
