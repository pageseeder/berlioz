/*
 * Copyright 2026 Allette Systems (Australia)
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

import java.io.FileNotFoundException;
import java.nio.file.NoSuchFileException;
import java.util.Objects;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.pageseeder.berlioz.BerliozErrorID;
import org.pageseeder.berlioz.BerliozException;
import org.xml.sax.SAXParseException;

/**
 * A structured failure from application stylesheet loading, source preparation, or execution.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.0
 * @since 0.14.0
 */
public final class XsltTransformException extends BerliozException {

  private static final long serialVersionUID = 8508866905853577647L;

  private static final int MAX_CAUSE_DEPTH = 100;

  /** The stage of transformation that failed. */
  public enum Phase {
    /** Loading or compiling the application stylesheet. */
    STYLESHEET,
    /** Preparing or parsing the generated XML source. */
    SOURCE_XML,
    /** Executing a compiled stylesheet. */
    EXECUTION
  }

  private final Phase phase;
  private final TransformerException transformerException;

  private XsltTransformException(Phase phase, TransformerException cause, BerliozErrorID id) {
    super(cause.getMessageAndLocation(), cause, id);
    this.phase = phase;
    this.transformerException = cause;
  }

  /**
   * Creates a structured failure for the supplied phase and transformer exception.
   *
   * @param phase The failed processing phase.
   * @param cause The original JAXP failure.
   * @return the structured failure.
   */
  public static XsltTransformException of(Phase phase, TransformerException cause) {
    Objects.requireNonNull(phase, "phase");
    Objects.requireNonNull(cause, "cause");
    return new XsltTransformException(phase, cause, identify(phase, cause));
  }

  /**
   * Creates an execution failure, changing the phase to source XML when a SAX parse error is found.
   *
   * @param cause The original JAXP failure.
   * @return the structured failure.
   */
  public static XsltTransformException duringExecution(TransformerException cause) {
    Phase phase = hasCause(cause, SAXParseException.class) ? Phase.SOURCE_XML : Phase.EXECUTION;
    return of(phase, cause);
  }

  /** @return the processing phase in which transformation failed. */
  public Phase phase() {
    return this.phase;
  }

  /**
   * Returns the exception in its original wrapper so collected XSLT diagnostics remain available.
   *
   * @return the original transformer exception or diagnostic wrapper.
   */
  public TransformerException transformerException() {
    return this.transformerException;
  }

  private static BerliozErrorID identify(Phase phase, TransformerException exception) {
    TransformerException actual = unwrap(exception);
    if (hasCause(actual, NoSuchFileException.class) || hasCause(actual, FileNotFoundException.class)) {
      return BerliozErrorID.TRANSFORM_NOT_FOUND;
    }
    if (hasCause(actual, SAXParseException.class)) return BerliozErrorID.TRANSFORM_MALFORMED_SOURCE_XML;
    if (phase == Phase.STYLESHEET || actual instanceof TransformerConfigurationException) {
      return BerliozErrorID.TRANSFORM_INVALID;
    }
    return BerliozErrorID.TRANSFORM_DYNAMIC_ERROR;
  }

  private static TransformerException unwrap(TransformerException exception) {
    if (exception instanceof XsltExceptionWrapper) {
      Throwable wrapped = exception.getException();
      if (wrapped instanceof TransformerException) return (TransformerException) wrapped;
    }
    return exception;
  }

  private static boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
    Throwable current = throwable;
    for (int depth = 0; current != null && depth < MAX_CAUSE_DEPTH; depth++) {
      if (type.isInstance(current)) return true;
      current = current.getCause();
    }
    return false;
  }
}
