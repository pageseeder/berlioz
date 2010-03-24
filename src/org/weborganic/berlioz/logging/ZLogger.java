package org.weborganic.berlioz.logging;

/**
 * An interface for loggers used in the Berlioz.
 *
 * <p>The {@link ZLoggerFactory} produces logger implementations for the different logging frameworks.
 *
 * <p>The purpose of this interface is simply to allow Berlioz to use different logging frameworks by using a
 * consistent interface. The aim is not to provide a universal logging interface.
 *
 * <p>Only the following logging levels are supported: DEBUG, INFO, WARN, ERROR.
 *
 * <p>The methods using a format and object arrays are more efficient than strings if string concatenation is required.
 *
 * <p>Note: this interface borrows heavily from <a href="http://www.slf4j.org/">http://www.slf4j.org/</a>.
 *
 * @author Christophe Lauret (Weborganic)
 * @version 26 November 2009
 */
public interface ZLogger {

  /**
   * Return the name of this Logger instance.
   *
   * @return the name of this Logger instance.
   */
  String getName();

  /**
   * Indicates whether the logger is for the specified class.
   * 
   * <p>Prevents the creation of unnecessary loggers.
   * 
   * @param c The class.
   * 
   * @return <code>true</code> if usable for the specified class; <code>false</code> otherwise.
   */
  public boolean isFor(Class<?> c);

// DEBUG ------------------------------------------------------------------------------------------

  /**
   * Is the logger instance enabled for the DEBUG level?
   *
   * @return <code>true</code> if this Logger is enabled for the DEBUG level; <code>false</code> otherwise.
   */
  boolean isDebugEnabled();

  /**
   * Log a message at the DEBUG level.
   *
   * @param msg the message string to be logged
   */
  void debug(String msg);

  /**
   * Log a message at the DEBUG level according to the specified format and argument.
   *
   * @param format the format string
   * @param arg    the argument
   */
  void debug(String format, Object arg);

  /**
   * Log a message at the DEBUG level according to the specified format and arguments.
   *
   * @param format the format string
   * @param args   an array of arguments
   */
  void debug(String format, Object[] args);

  /**
   * Log an exception (throwable) at the DEBUG level with an accompanying message.
   *
   * @param msg the message accompanying the exception
   * @param t   the exception (throwable) to log
   */
  void debug(String msg, Throwable t);

// INFO -------------------------------------------------------------------------------------------

  /**
   * Is the logger instance enabled for the INFO level?
   *
   * @return <code>true</code> if this Logger is enabled for the INFO level; <code>false</code> otherwise.
   */
  boolean isInfoEnabled();

  /**
   * Log a message at the INFO level.
   *
   * @param msg the message string to be logged
   */
  void info(String msg);

  /**
   * Log a message at the INFO level according to the specified format and argument.
   *
   * @param format the format string
   * @param arg    the argument
   */
  void info(String format, Object arg);

  /**
   * Log a message at the INFO level according to the specified format and arguments.
   *
   * @param format the format string
   * @param args   an array of arguments
   */
  void info(String format, Object[] args);

  /**
   * Log an exception (throwable) at the INFO level with an accompanying message.
   *
   * @param msg the message accompanying the exception
   * @param t   the exception (throwable) to log
   */
  void info(String msg, Throwable t);

// WARN -------------------------------------------------------------------------------------------

  /**
   * Is the logger instance enabled for the WARN level?
   *
   * @return <code>true</code> if this Logger is enabled for the WARN level; <code>false</code> otherwise.
   */
  boolean isWarnEnabled();

  /**
   * Log a message at the WARN level.
   *
   * @param msg the message string to be logged
   */
  void warn(String msg);

  /**
   * Log a message at the WARN level according to the specified format and argument.
   *
   * @param format the format string
   * @param arg    the argument
   */
  void warn(String format, Object arg);

  /**
   * Log a message at the WARN level according to the specified format and arguments.
   *
   * @param format the format string
   * @param args   an array of arguments
   */
  void warn(String format, Object[] args);

  /**
   * Log an exception (throwable) at the WARN level with an accompanying message.
   *
   * @param msg the message accompanying the exception
   * @param t   the exception (throwable) to log
   */
  void warn(String msg, Throwable t);

// ERROR ------------------------------------------------------------------------------------------

  /**
   * Is the logger instance enabled for the ERROR level?
   *
   * @return <code>true</code> if this Logger is enabled for the ERROR level; <code>false</code> otherwise.
   */
  boolean isErrorEnabled();

  /**
   * Log a message at the ERROR level.
   *
   * @param msg the message string to be logged
   */
  void error(String msg);

  /**
   * Log a message at the ERROR level according to the specified format and argument.
   *
   * @param format the format string
   * @param arg    the argument
   */
  void error(String format, Object arg);

  /**
   * Log a message at the ERROR level according to the specified format and arguments.
   *
   * @param format the format string
   * @param args   an array of arguments
   */
  void error(String format, Object[] args);

  /**
   * Log an exception (throwable) at the ERROR level with an accompanying message.
   *
   * @param msg the message accompanying the exception
   * @param t   the exception (throwable) to log
   */
  void error(String msg, Throwable t);

}
