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
package org.pageseeder.berlioz.content;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.Beta;

/**
 * Thrown when a request parameter fails a type or constraint check.
 *
 * <p>This exception is a flow-control signal, not a programming error. Stack trace
 * capture is suppressed so it can be thrown on every bad request without the cost
 * of walking the call stack.
 *
 * <p>The servlet layer catches this exception and maps it to a {@code 400 Bad Request}
 * response instead of the default {@code 500 Internal Server Error}.
 *
 * @author Christophe Lauret
 *
 * @version 0.13.1
 * @since 0.13.1
 */
@Beta
public final class InvalidParameterException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * The reason the parameter was rejected.
   */
  public enum Reason {
    /** The parameter was required but absent. */
    REQUIRED,
    /** The parameter value could not be parsed into the requested type. */
    INVALID_FORMAT,
    /** The parameter value falls outside the allowed numeric or date range. */
    OUT_OF_RANGE,
    /** The parameter value is not in the set of allowed values. */
    NOT_ALLOWED
  }

  private final String parameterName;
  private final @Nullable String parameterValue;
  private final Reason reason;

  private InvalidParameterException(String message, String parameterName, @Nullable String parameterValue, Reason reason) {
    super(message);
    this.parameterName = parameterName;
    this.parameterValue = parameterValue;
    this.reason = reason;
  }

  /**
   * Suppresses stack trace capture — this exception is a request-level signal, not a bug.
   */
  @Override
  public synchronized Throwable fillInStackTrace() {
    return this;
  }

  /**
   * @return the name of the parameter that failed validation.
   */
  public String getParameterName() {
    return this.parameterName;
  }

  /**
   * @return the raw string value that was submitted, or {@code null} if the parameter was absent.
   */
  public @Nullable String getParameterValue() {
    return this.parameterValue;
  }

  /**
   * @return the reason the parameter was rejected.
   */
  public Reason getReason() {
    return this.reason;
  }

  // --- Factory methods -------------------------------------------------------

  /**
   * Creates an exception for a required parameter that was absent.
   *
   * @param name the parameter name
   * @return the exception
   */
  public static InvalidParameterException required(String name) {
    return new InvalidParameterException("Parameter '" + name + "' is required", name, null, Reason.REQUIRED);
  }

  /**
   * Creates an exception for a parameter whose value could not be parsed.
   *
   * @param name        the parameter name
   * @param value       the raw value that could not be parsed
   * @param targetType  a short description of the expected type (e.g. {@code "integer"}, {@code "date"})
   * @return the exception
   */
  public static InvalidParameterException invalidFormat(String name, String value, String targetType) {
    return new InvalidParameterException(
        "Parameter '" + name + "' value '" + value + "' is not a valid " + targetType,
        name, value, Reason.INVALID_FORMAT);
  }

  /**
   * Creates an exception for a parameter whose value is outside the allowed range.
   *
   * @param name    the parameter name
   * @param value   the raw value that was out of range
   * @param detail  a short description of the constraint (e.g. {@code "must be >= 1"})
   * @return the exception
   */
  public static InvalidParameterException outOfRange(String name, String value, String detail) {
    return new InvalidParameterException(
        "Parameter '" + name + "' value '" + value + "': " + detail,
        name, value, Reason.OUT_OF_RANGE);
  }

  /**
   * Creates an exception for a parameter whose value is not in the allowed set.
   *
   * @param name    the parameter name
   * @param value   the raw value that was not allowed
   * @param allowed the set of permitted values
   * @return the exception
   */
  public static InvalidParameterException notAllowed(String name, String value, String... allowed) {
    return new InvalidParameterException(
        "Parameter '" + name + "' value '" + value + "' must be one of: " + String.join(", ", allowed),
        name, value, Reason.NOT_ALLOWED);
  }

}
