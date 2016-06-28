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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.pageseeder.berlioz.util.CollectedError.Level;

/**
 * An error listener wrapping the XSLT engines default listener and recording occurring errors
 * as XML so that they can be used.
 *
 * @param <T> The class of exception that is collected.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.32 - 29 January 2015
 * @since Berlioz 0.8.1
 */
public class ErrorCollector<T extends Throwable>  {

  /**
   * Errors are collected here.
   */
  private final List<CollectedError<T>> _collected = new ArrayList<CollectedError<T>>();

  /**
   * The threshold level for the collect method to throw an exception.
   */
  private Level _exception = Level.FATAL;

  /**
   * The threshold level for the collect method to set the error flag to <code>true</code>.
   */
  private Level _flag = Level.ERROR;

  /**
   * Indicates whether an error has been reported during parsing.
   *
   * <p>This flag is used to indicate that there is no point in processing the file any further
   * because they could cause exceptions.
   */
  private boolean _hasError = false;

  /**
   * Creates a new error collector.
   */
  public ErrorCollector() {
  }

  /**
   * Set the threshold to throw an exception during the next collect operation.
   *
   * @param threshold the level at which the next collect call will throw an exception.
   * @throws NullPointerException If the threshold argument is <code>null</code>.
   */
  public final void setException(Level threshold) {
    this._exception = Objects.requireNonNull(threshold, "Specified threshold is null");
  }

  /**
   * Set the threshold to set to rise the error flag.
   *
   * @param threshold the level at which the next collect call set the error flag to <code>true</code>.
   * @throws NullPointerException If the threshold argument is <code>null</code>.
   */
  public final void setErrorFlag(Level threshold) {
    this._flag = Objects.requireNonNull(threshold, "Specified threshold is null");;
  }

  /**
   * Collect an error reported by an underlying process such as a parser or a transformer.
   *
   * <p>Note: this method should generally be called at the end of a function at it can throw an exception if
   * the threshold was reached.
   *
   * @param level     The level of the exception.
   * @param exception The exception thrown by the underlying process.
   *
   * @throws T If the exception threshold has been reached, the exception passed as argument is thrown.
   * @throws NullPointerException If either argument is <code>null</code>.
   */
  public final void collect(Level level, T exception) throws T {
    this._collected.add(new CollectedError<T>(level, exception));
    if (this._flag.compareTo(level) <= 0) {
      this._hasError = true;
    }
    if (this._exception.compareTo(level) <= 0) throw exception;
  }

  /**
   * Collect an error reported by an underlying process such as a parser or a transformer without
   * throwing any exception.
   *
   * @param level     The level of the exception.
   * @param exception The exception thrown by the underlying process.
   *
   * @throws NullPointerException If either argument is <code>null</code>.
   */
  public final void collectQuietly(Level level, T exception) {
    this._collected.add(new CollectedError<T>(level, exception));
    if (this._flag.compareTo(level) <= 0) {
      this._hasError = true;
    }
  }

  /**
   * Returns the list of collected errors.
   *
   * @return the list of collected errors.
   */
  public final List<CollectedError<T>> getErrors() {
    return this._collected;
  }

  /**
   * Indicate whether any error was recorded by this error collector.
   *
   * @return <code>true</code> if any error was reported;
   *         <code>false</code> otherwise.
   */
  public final boolean hasError() {
    return this._hasError;
  }

}
