/*
 * Copyright (c) 1999-2012 weborganic systems pty. ltd.
 */
package org.weborganic.berlioz.util;

import java.lang.reflect.Method;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weborganic.berlioz.Beta;

/**
 * @author Christophe Lauret
 * @version 18 November 2013
 */
public final class Aeson {

  /**
   * Displays debug information.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(Aeson.class);

  /**
   * Indicates whether we have checked whether Aeson is available.
   */
  private static volatile boolean checkedAeson = false;

  /**
   * The method to invoke if Aeson is available.
   */
  private static volatile Method method = null;

  /** Utility class */
  private Aeson() {
  }

  /**
   *
   * @param transformer
   * @param result
   * @return
   */
  @Beta
  public static Result updateResultIfPossible(Transformer transformer, StreamResult result) {
    Result r = result;
    // Fetch the method
    if (!checkedAeson) {
      method = getMethod();
    }
    // Invoke the method
    if (method != null) {
      try {
        r = (Result)method.invoke(null, transformer, result);
      } catch (Exception ex) {
        LOGGER.debug("Unable to generate JSON Result", ex.getMessage(), ex);
        method = null;
      }
    }
    return r;
  }

  /**
   *
   * @return
   */
  private static synchronized Method getMethod() {
    LOGGER.debug("Checking if Aeson is available");
    Method m = null;
    try {
      Class<?> c = Class.forName("org.weborganic.aeson.JSONResult");
      m = c.getMethod("newInstanceIfSupported", Transformer.class, StreamResult.class);
      // TODO Check return type class
    } catch (Exception ex) {
      LOGGER.info("Aeson not available {}", ex.getMessage(), ex);
    }
    checkedAeson = true;
    return m;
  }

}
