/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.content;

import java.util.Date;
import java.util.Enumeration;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;

/**
 * Provides a generic and uniform mechanism for the content generator to access parameters 
 * and attributes from a request. 
 * 
 * All of the methods will return a <code>NullPointerException</code> if the specified
 * parameter name, attribute name or object name is <code>null</code>. 
 * 
 * @author Tu Tak Tran (Allette Systems)
 * @author Christophe Lauret (Weborganic)
 * @version 9 October 2009
 */
public interface ContentRequest {

  /**
   * Returns the specified parameter value or <code>null</code>.
   * 
   * <p>This method guarantees that the returned value is not equal to an empty string.
   *
   * @param name The name of the requested parameter.
   *
   * @return A <code>String</code> or <code>null</code>.
   */
  String getParameter(String name);

  /**
   * Returns the path information of this request.
   *
   * @return The path information of this request.
   */
  String getPathInfo();

  /**
   * Returns the specified parameter value or the specified default if <code>null</code>.
   * 
   * <p>This method guarantees that a value is returned.
   *
   * @param name The name of the URL requested parameter.
   * @param def  A default value if the value is <code>null</code> or empty string.
   *
   * @return A value of the parameter or the default value if missing.
   */
  String getURLParameter(String name, String def);

  /**
   * Returns the specified parameter value or the specified default if <code>null</code>.
   * 
   * <p>This method guarantees that a value is returned.
   *
   * @param name The name of the requested parameter.
   * @param def  A default value if the value is <code>null</code> or empty string.
   *
   * @return A value of the parameter or the default value if missing.
   */
  String getParameter(String name, String def);

  /**
   * Returns the specified parameter value.
   * 
   * <p>This method guarantees that a value is returned.
   *
   * @param name The name of the requested parameter.
   * @param def  A default value if the value is <code>null</code> or empty string.
   *
   * @return A value of the parameter or the default value if missing or could not be parsed.
   */
  int getIntParameter(String name, int def);

  /**
   * Returns an array of String objects containing all of the values the given request parameter
   * has, or <code>null</code> if the parameter does not exist.
   *
   * <p>If the parameter has a single value, the array has a length of 1.
   *
   * @param name A String containing the name of the parameter whose value is requested
   * 
   * @return An array of String objects containing the parameter's values
   */
  String[] getParameterValues(String name);

  /**
   * Returns an <code>Enumeration</code> of <code>String</code> objects containing the names of 
   * the parameters contained in this request. 
   * 
   * <p>If the request has no parameters, the method returns an empty Enumeration.
   *
   * @return An <code>Enumeration</code> of the names of each parameters as <code>String</code>s;
   *          or an empty <code>Enumeration</code> if the request has no parameters.
   */
  Enumeration<String> getParameterNames();

  /**
   * Returns the specified attribute object or <code>null</code>.
   * 
   * @param name The name of the attribute.
   * 
   * @return the specified attribute object or <code>null</code>.
   */
  Object getAttribute(String name);

  /**
   * Returns the specified file item object or <code>null</code>.
   * 
   * @param name The name of the file item.
   * 
   * @return the specified file item object or <code>null</code>.
   */
  FileItem getFileItem(String name);

  /**
   * Sets the specified attribute object or <code>null</code>.
   * 
   * @param name The name of the attribute.
   * @param o    The object for this attribute.
   */
  void setAttribute(String name, Object o);

  /**
   * Returns a <code>Date</code> instance from the specified parameter.
   * 
   * <p>Parses dates as 'dd MMM yyyy'.
   * 
   * @param name The name of the parameter.
   * 
   * @return A <code>Date</code> instance or <code>null</code> if not specified.
   */
  Date getDateParameter(String name);

  /**
   * Returns an array containing all of the Cookie objects the client sent with this request.
   * 
   * This method returns <code>null</code> if no cookies were sent.
   *
   * @return An array of all the Cookies included with this request,
   *         or <code>null</code> if the request has no cookies
   */
  Cookie[] getCookies();

  /**
   * Returns the session of the wrapped HTTP servlet request.
   * 
   * @return The session of the HTTP servlet request.
   */
  HttpSession getSession();

  /**
   * Returns the environment of the request.
   * 
   * @return The environment of the request.
   */
  Environment getEnvironment();
  
  
  
  /**
   * Requests that the answer be interpreted as a page not found error.
   */
  void returnNotFound();

}
