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
 * A utility class allowing Berlioz to produce JSON from the Aeson JSON library.
 *
 * <p>DO NOT USE THIS CLASS DIRECTLY AS IT IS SUBJECT TO CHANGE
 *
 * @author Christophe Lauret
 * @version 18 November 2013
 */
@Beta
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
   * Updates the Result.
   *
   * @param transformer The XSLT transformer
   * @param result      The stream result to use
   *
   * @return A JSON Result instance to use for the transformation instead.
   */
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
   * @return the method to invoke whenever JSON results are needed.
   */
  private static synchronized Method getMethod() {
    LOGGER.debug("Checking if Aeson is available");
    Method m = null;
    try {
      Class<?> c = Class.forName("org.weborganic.aeson.JSONResult");
      m = c.getMethod("newInstanceIfSupported", Transformer.class, StreamResult.class);
      // TODO Check return type class
    } catch (Exception ex) {
      LOGGER.info("Aeson not available {}", ex.getMessage());
    }
    checkedAeson = true;
    return m;
  }

}
