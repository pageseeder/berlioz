package org.weborganic.berlioz.logging;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * A basic implementation wrapping a log4j logger.
 */
public class ZLoggerLog4j implements ZLogger {

  /**
   * The wrapped logger.
   */
  private final Logger logger;

  /**
   * Creates a new logger for the specified class.
   * 
   * @param c The class requiring a logger.
   */
  public ZLoggerLog4j(Class<?> c) {
    this.logger = Logger.getLogger(c);
  }

  /**
   * Returns true if this logger is current for the specified class.
   * 
   * @param c The class.
   * 
   * @return <code>true</code> if current; <code>false</code> otherwise.
   */
  public boolean isFor(Class<?> c) {
    return Logger.getLogger(c) == this.logger;
  }

  /**
   * {@inheritDoc}
   */
  public String getName() {
    return this.logger.getName();
  }

  /**
   * {@inheritDoc}
   */
  public boolean isDebugEnabled() {
    return this.logger.isDebugEnabled();
  }

  /**
   * {@inheritDoc}
   */
  public void debug(String msg) {
    this.logger.debug(msg);
  }

  /**
   * {@inheritDoc}
   */
  public void debug(String format, Object arg) {
    // TODO Auto-generated method stub
  }

  /**
   * {@inheritDoc}
   */
  public void debug(String format, Object[] args) {
    // TODO Auto-generated method stub
  }

  /**
   * {@inheritDoc}
   */
  public void debug(String msg, Throwable t) {
    this.logger.debug(msg, t);
  }

  /**
   * {@inheritDoc}
   */
  public boolean isInfoEnabled() {
    return this.logger.isInfoEnabled();
  }

  /**
   * {@inheritDoc}
   */
  public void info(String msg) {
    this.logger.info(msg);
  }

  /**
   * {@inheritDoc}
   */
  public void info(String format, Object arg) {
    this.logger.info(format);
  }

  /**
   * {@inheritDoc}
   */
  public void info(String format, Object[] args) {
    // TODO Auto-generated method stub
  }

  /**
   * {@inheritDoc}
   */
  public void info(String msg, Throwable t) {
    // TODO Auto-generated method stub
  }

  /**
   * {@inheritDoc}
   */
  public boolean isWarnEnabled() {
    return this.logger.isEnabledFor(Level.WARN);
  }

  /**
   * {@inheritDoc}
   */
  public void warn(String msg) {
    this.logger.info(msg);
  }

  /**
   * {@inheritDoc}
   */
  public void warn(String format, Object arg) {
    this.logger.info(format);
  }

  /**
   * {@inheritDoc}
   */
  public void warn(String format, Object[] args) {
    // TODO Auto-generated method stub
  }

  /**
   * {@inheritDoc}
   */
  public void warn(String msg, Throwable t) {
    // TODO Auto-generated method stub
  }

  /**
   * {@inheritDoc}
   */
  public boolean isErrorEnabled() {
    return this.logger.isEnabledFor(Level.ERROR);
  }

  /**
   * {@inheritDoc}
   */
  public void error(String msg) {
    this.logger.error(msg);
  }

  /**
   * {@inheritDoc}
   */
  public void error(String format, Object arg) {
    this.logger.error(format);
  }

  /**
   * {@inheritDoc}
   */
  public void error(String format, Object[] args) {
    // TODO Auto-generated method stub
  }

  /**
   * {@inheritDoc}
   */
  public void error(String msg, Throwable t) {
    // TODO Auto-generated method stub
  }

}
