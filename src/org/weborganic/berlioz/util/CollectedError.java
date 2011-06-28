package org.weborganic.berlioz.util;

import org.weborganic.berlioz.Beta;

/**
 * A simple class to associate an error collected by a parser to a level or seriousness. 
 * 
 * @param <T> The type of error collected.
 * 
 * @author Christophe Lauret
 * @version 28 June 2011
 */
@Beta public final class CollectedError<T> {

  /**
   * The level of collected error.
   */
  public enum Level {

    /** Error was considered fatal by the process. */
    FATAL,

    /** Error was considered fatal by the process. */
    ERROR, 

    /** Error was considered fatal by the process. */
    WARNING
  };

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
   */
  public CollectedError(Level level, T error) {
    this._level = level;
    this._error = error;
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
    return _error;
  }
}
