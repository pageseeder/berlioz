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
package org.pageseeder.berlioz.util;

import java.io.IOException;
import java.util.Objects;

import org.pageseeder.xmlwriter.XMLWritable;
import org.pageseeder.xmlwriter.XMLWriter;

/**
 * A simple class to associate an error collected by a parser to a level or seriousness.
 *
 * <p>This class is designed to be used with an {@link org.xml.sax.ErrorHandler}
 * or an {@link javax.xml.transform.ErrorListener} so that errors can be collected in a simple list.
 *
 * @param <T> The type of error collected.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.32 - 29 January 2015
 * @since Berlioz 0.8.1
 */
public final class CollectedError<T extends Throwable> implements XMLWritable {

  /**
   * The level of collected error.
   *
   * <p>Note: the ordinal of this enumeration constant (its position in its enum declaration)
   * is significant as it is used to compare levels.
   */
  public enum Level {

    /** Warning reported by the underlying process. */
    WARNING,

    /** For normal non-fatal errors reported by the underlying process. */
    ERROR,

    /** Error was considered fatal by the underlying process. */
    FATAL;

    @Override
    public String toString() {
      return name().toLowerCase();
    }
  }

  /**
   * The seriousness of the error.
   */
  private final Level _level;

  /**
   * The actual error (may be an exception, message as a string, etc...)
   */
  private final T _error;

  /**
   * Creates a new collected error.
   *
   * @param level The seriousness of the error.
   * @param error The error itself.
   *
   * @throws NullPointerException If either argument is <code>null</code>.
   */
  public CollectedError(Level level, T error) {
    this._level = Objects.requireNonNull(level, "The level is required");
    this._error = Objects.requireNonNull(error, "The error is required");
  }

  /**
   * The seriousness of the error.
   *
   * @return The captured error.
   */
  public Level level() {
    return this._level;
  }

  /**
   * The captured error.
   *
   * @return The captured error.
   */
  public T error() {
    return this._error;
  }

  /**
   * Returns the source locator as XML.
   *
   * <p>Does nothing if the locator is <code>null</code>.
   *
   * @param xml     the XML writer.
   *
   * @throws IOException If thrown by the XML writer.
   */
  @Override
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("collected");
    xml.attribute("level", this._level.toString());
    Errors.toXML(this._error, xml, false);
    xml.closeElement();
  }

}
