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
package org.pageseeder.berlioz.error;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.util.Errors;
import org.pageseeder.berlioz.output.OutputWriter;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.xml.sax.SAXParseException;

/**
 * A structured representation of a {@link Throwable} for use as an extension member in an
 * RFC 9457 problem details response.
 *
 * <p>Two verbosity levels are supported:</p>
 * <ul>
 *   <li><b>standard</b> ({@code includeStackTrace = false}) — exception class, optional type
 *       discriminator, clean message, and source location when available (line/column/system-id).
 *       Safe for staging environments.</li>
 *   <li><b>full</b> ({@code includeStackTrace = true}) — adds the safe stack trace and recursively
 *       includes the cause chain. Recommended for development only.</li>
 * </ul>
 *
 * <p>The XML serialization ({@link #toXml}) mirrors the structure produced by
 * {@link Errors#toXML(Throwable, org.pageseeder.xmlwriter.XMLWriter, boolean)} for the legacy
 * error format. The JSON serialization uses camelCase member names per
 * RFC 9457 JSON convention.</p>
 *
 * @author Christophe Lauret
 *
 * @version 0.13.5
 * @since 0.13.5
 */
@Beta
public final class ExceptionDetail implements ProblemExtension {

  private static final String NAME = "exception";

  private final String className;
  private final @Nullable String type;
  private final String message;
  private final @Nullable String stackTrace;
  private final @Nullable SourceLocation location;
  private final @Nullable ExceptionDetail cause;

  private ExceptionDetail(String className, @Nullable String type, String message,
      @Nullable String stackTrace, @Nullable SourceLocation location,
      @Nullable ExceptionDetail cause) {
    this.className = className;
    this.type = type;
    this.message = message;
    this.stackTrace = stackTrace;
    this.location = location;
    this.cause = cause;
  }

  /**
   * Creates an {@code ExceptionDetail} from the given throwable.
   *
   * <p>Recognises {@link SAXParseException} and {@link TransformerException} /
   * {@link TransformerConfigurationException} and extracts their source location
   * (line, column, system ID). All other throwables produce a location-free detail.</p>
   *
   * <p><b>Production guidance:</b> pass {@code false} in production generators. The stack trace
   * and cause chain are sent verbatim to API clients and reveal internal class names, method
   * names, and file paths regardless of the {@code ERROR_DETAIL} Berlioz option. Use {@code true}
   * only when the calling code explicitly checks that the configured
   * {@link org.pageseeder.berlioz.error.DetailLevel} is {@code FULL}.</p>
   *
   * @param ex                 the throwable to describe
   * @param includeStackTrace  {@code true} for full verbosity (stack trace + cause chain) —
   *                           do not hardcode {@code true} in production generators;
   *                           {@code false} for standard verbosity (class, message, location only)
   * @return a new {@code ExceptionDetail}
   */
  public static ExceptionDetail of(Throwable ex, boolean includeStackTrace) {
    ex = unwrapTransportException(ex);
    String stackTrace = stackTrace(ex, includeStackTrace);
    ExceptionDetail cause = cause(ex, includeStackTrace);

    if (ex instanceof SAXParseException) {
      return fromSAXParseException((SAXParseException) ex, stackTrace, cause);
    }

    if (ex instanceof TransformerException) {
      return fromTransformerException((TransformerException) ex, stackTrace, cause);
    }

    return fromThrowable(ex, stackTrace, cause);
  }

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public OutputWriter writeTo(OutputWriter out) {
    out.startObject(NAME);
    writeFieldsTo(out);
    return out.endObject();
  }

  private void writeFieldsTo(OutputWriter out) {
    out.field("class", this.className);
    out.optionalField("type", this.type);
    out.field("message", this.message, OutputWriter.FieldOption.XML_ELEMENT);
    if (this.location != null) this.location.writeTo(out);
    out.optionalField("stack-trace", this.stackTrace, OutputWriter.FieldOption.XML_ELEMENT);
    if (this.cause != null) {
      out.startObject("cause");
      this.cause.writeFieldsTo(out);
      out.endObject();
    }
  }

  // --- Private helpers -------------------------------------------------------------------------

  private static Throwable unwrapTransportException(Throwable ex) {
    // BerliozException is a framework transport wrapper; surface the real cause.
    if (ex instanceof org.pageseeder.berlioz.BerliozException && ex.getCause() != null) {
      return ex.getCause();
    }
    return ex;
  }

  private static @Nullable String stackTrace(Throwable ex, boolean includeStackTrace) {
    return includeStackTrace ? Errors.getStackTrace(ex, true) : null;
  }

  private static @Nullable ExceptionDetail cause(Throwable ex, boolean includeStackTrace) {
    return includeStackTrace && ex.getCause() != null ? of(ex.getCause(), true) : null;
  }

  private static ExceptionDetail fromSAXParseException(SAXParseException ex,
      @Nullable String stackTrace, @Nullable ExceptionDetail cause) {
    return new ExceptionDetail(className(ex), "SAXParseException", message(ex), stackTrace,
        SourceLocation.of(ex), cause);
  }

  private static ExceptionDetail fromTransformerException(TransformerException ex,
      @Nullable String stackTrace, @Nullable ExceptionDetail cause) {
    return new ExceptionDetail(className(ex), transformerType(ex), message(ex), stackTrace,
        SourceLocation.of(ex.getLocator()), cause);
  }

  private static ExceptionDetail fromThrowable(Throwable ex,
      @Nullable String stackTrace, @Nullable ExceptionDetail cause) {
    return new ExceptionDetail(className(ex), null, message(ex), stackTrace, null, cause);
  }

  private static String className(Throwable ex) {
    return ex.getClass().getName();
  }

  private static String message(Throwable ex) {
    return Errors.cleanMessage(ex);
  }

  private static String transformerType(TransformerException ex) {
    return ex instanceof TransformerConfigurationException
        ? "TransformerConfigurationException" : "TransformerException";
  }

}
