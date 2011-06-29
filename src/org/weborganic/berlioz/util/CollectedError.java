package org.weborganic.berlioz.util;

import org.weborganic.berlioz.Beta;

/**
 * A simple class to associate an error collected by a parser to a level or seriousness.
 * 
 * <p>This class is designed to be used with an {@link org.xml.sax.ErrorHandler.ErrorHandler} 
 * or an {@link javax.xml.transform.ErrorListener} so that errors can be collected in a simple list.
 * 
 * @param <T> The type of error collected.
 * 
 * @author Christophe Lauret
 * @version 29 June 2011
 */
@Beta public final class CollectedError<T> {

  /**
   * The level of collected error.
   */
  public enum Level {

    /** Error was considered fatal by the underlying process. */
    FATAL,

    /** For normal non-fatal errors reported by the underlying process. */
    ERROR, 

    /** Warning reported by the underlying process. */
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
