package org.weborganic.berlioz.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A basic implementation wrapping a Java Util Logging logger.
 * 
 * @author Christophe Lauret (Weborganic)
 * @version 26 November 2009
 */
public class ZLoggerJUL implements ZLogger {

  /**
   * The wrapped logger.
   */
  private final Logger logger;

  /**
   * Creates a new logger for the specified class.
   * 
   * @param c The class requiring a logger. 
   */
  public ZLoggerJUL(Class<?> c) {
    this.logger = Logger.getLogger(c.getName());
  }

  /**
   * Returns true if this logger is current for the specified class.
   * 
   * @param c The class.
   * 
   * @return <code>true</code> if current; <code>false</code> otherwise.
   */
  public boolean isFor(Class<?> c) {
    return Logger.getLogger(c.getName()) == this.logger;
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
    return this.logger.isLoggable(Level.FINE);
  }

  /**
   * {@inheritDoc}
   */
  public void debug(String msg) {
    this.logger.fine(msg);
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
    this.logger.log(Level.FINE, msg, t);
  }

  /**
   * {@inheritDoc}
   */
  public boolean isInfoEnabled() {
    return this.logger.isLoggable(Level.INFO);
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
    return this.logger.isLoggable(Level.WARNING);
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
    this.logger.log(Level.WARNING, msg, t);
  }

  /**
   * {@inheritDoc}
   */
  public boolean isErrorEnabled() {
    return this.logger.isLoggable(Level.SEVERE);
  }

  /**
   * {@inheritDoc}
   */
  public void error(String msg) {
    this.logger.log(Level.SEVERE, msg);
  }

  /**
   * {@inheritDoc}
   */
  public void error(String format, Object arg) {
 // TODO Auto-generated method stub
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
    this.logger.log(Level.SEVERE, msg, t);
  }

}

