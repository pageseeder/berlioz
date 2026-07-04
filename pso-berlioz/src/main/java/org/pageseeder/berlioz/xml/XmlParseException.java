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
package org.pageseeder.berlioz.xml;

/**
 * Thrown when an XML source could not be parsed because it is not well-formed (or could not be
 * read at all).
 *
 * <p>This is a narrower, unchecked alternative to catching a generic
 * {@link org.pageseeder.berlioz.BerliozException} when the caller specifically needs to react to
 * "the source was not well-formed" rather than any other kind of library failure.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.0
 * @since 0.14.0
 */
public final class XmlParseException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private static final int NO_LOCATION = -1;

  private final int line;

  private final int column;

  /**
   * Creates a new exception with no known source location.
   *
   * @param message The error message.
   * @param cause   The underlying cause (typically a wrapped {@code SAXException} or {@code IOException}).
   */
  public XmlParseException(String message, Throwable cause) {
    this(message, cause, NO_LOCATION, NO_LOCATION);
  }

  /**
   * Creates a new exception with a known source location.
   *
   * @param message The error message.
   * @param cause   The underlying cause.
   * @param line    The 1-based line number where parsing failed.
   * @param column  The 1-based column number where parsing failed.
   */
  public XmlParseException(String message, Throwable cause, int line, int column) {
    super(message, cause);
    this.line = line;
    this.column = column;
  }

  /**
   * @return {@code true} if a source location is available.
   */
  public boolean hasLocation() {
    return this.line >= 0;
  }

  /**
   * @return the 1-based line number where parsing failed, or -1 if not known.
   */
  public int getLine() {
    return this.line;
  }

  /**
   * @return the 1-based column number where parsing failed, or -1 if not known.
   */
  public int getColumn() {
    return this.column;
  }

}
