package org.weborganic.berlioz.logging;

import java.util.Hashtable;

/**
 * A simple factory for loggers in order to reduce the dependency on external libraries.
 * 
 * <p>This factory is only intended for Berlioz classes 
 * 
 * @author Christophe Lauret (Weborganic)
 * @version 26 November 2009
 */
public final class ZLoggerFactory {

  /**
   * The loggers used in Berlioz: one per class name.
   */
  private final static Hashtable<String, ZLogger> LOGGERS = new Hashtable<String, ZLogger>();

  /**
   * Keep it private for now. 
   */
  private ZLoggerFactory() {
  }

  /**
   * Returns a logger for the specified class.
   * 
   * @param c the class for which the logger is needed.
   * @return The corresponding logger. 
   */
  public static ZLogger getLogger(Class<?> c) {
    ZLogger z = LOGGERS.get(c.getName());
    if (z == null || !z.isFor(c)) {
      z = new ZLoggerLog4j(c);
      LOGGERS.put(c.getName(), z);
    }
    return z;
  }

}
