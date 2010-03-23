/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.servlet;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.weborganic.berlioz.content.ContentRequest;
import org.weborganic.berlioz.logging.ZLogger;
import org.weborganic.berlioz.logging.ZLoggerFactory;

/**
 * Wraps a {@link javax.servlet.ServletRequest} instance and provide methods
 * to access the parameters and attributes in a consistent manner.
 *
 * @author Christophe Lauret (Weborganic)
 * @author Tu Tak Tran (Allette Systems)
 * 
 * @version 29 October 2009
 */
public final class HttpRequestWrapper implements ContentRequest {

  /**
   * Displays debug information.
   */
  private static final ZLogger LOGGER = ZLoggerFactory.getLogger(HttpRequestWrapper.class);

  /**
   * A parser for date parameters
   */
  private static final DateFormat DATE_PARSER = new SimpleDateFormat("dd MMM yyyy");

  /**
   * A file servlet upload handler for uploaded files.
   */
  private static final ServletFileUpload UPLOAD = new ServletFileUpload(new DiskFileItemFactory());

  /**
   * The wrapped {@link javax.servlet.ServletRequest}.
   */
  private final HttpServletRequest req;

  /**
   * The wrapped {@link javax.servlet.ServletResponse}.
   */
  private final HttpServletResponse res;

  /**
   * The list of file items if needed.
   */
  private List<FileItem> items;

// sole constructor -------------------------------------------------------------------------------

  /**
   * Creates a new wrapper around the specified HTTP servlet request.
   * 
   * @param req The request to wrap.
   * @param res The response to wrap.
   * 
   * @throws IllegalArgumentException If the request is <code>null</code>.
   */
  public HttpRequestWrapper(HttpServletRequest req, HttpServletResponse res) throws IllegalArgumentException {
    if (req == null)
      throw new IllegalArgumentException("Cannot construct wrapper around null request.");
    this.req = req;
    // Handling multipart requests
    boolean isMultipart = this.isMultipartContent();
    if (isMultipart) {
      LOGGER.info("Parsing multipart content...");
      try {
        this.items = UPLOAD.parseRequest(req);
      } catch (Exception ex) {
        LOGGER.warn("Error whilst parsing the multiplart request.", ex);
      }
    }
    this.res = res;
  }

// generic parameter methods ----------------------------------------------------------------------

  /**
   * {@inheritDoc}
   */
  public String getURLParameter(String name, String def) {
    // TODO: implement using URI templates
    return null;
  };

  /**
   * {@inheritDoc}
   */
  public String getParameter(String name) {
    String value = this.req.getParameter(name);
    return ("".equals(value))? null : value;
  }

  /**
   * {@inheritDoc}
   */
  public String getParameter(String name, String def) {
    String value = this.req.getParameter(name);
    return (value == null || "".equals(value))? def : value;
  }

  /**
   * {@inheritDoc}
   */
  public String[] getParameterValues(String name) {
    return this.req.getParameterValues(name);
  }

  /**
   * {@inheritDoc}
   */
  public Enumeration getParameterNames() {
    return this.req.getParameterNames();
  }

// specific methods ---------------------------------------------------------------------

  /**
   * {@inheritDoc}
   */
  public int getIntParameter(String name, int def) {
    String value = this.req.getParameter(name);
    if (value == null || "".equals(value)) return def;
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException ex) {
      LOGGER.error("Unable to parse value "+value+" for "+name+" parameter.", ex);
      return def;
    }
  }

  /**
   * {@inheritDoc}
   */
  public Date getDateParameter(String name) {
    try {
      return DATE_PARSER.parse(this.getParameter(name));
    } catch (Exception ex) {
      LOGGER.warn("The date parameter cannot be parsed :"+this.req.getParameter(name), ex);
      return null;
    }
  }

  /**
   * Returns the path information (what comes after servlet path).
   * 
   * @return The path information (what comes after servlet path).
   */
  public String getPathInfo() {
    return this.req.getPathInfo();
  }

  /**
   * Returns the specified file item if it matches the given field name and its size is greater
   * than zero.
   * 
   * {@inheritDoc}
   */
  public FileItem getFileItem(String name) {
    if (this.items == null) return null;
    for (FileItem item : this.items) {
      if (name.equals(item.getFieldName()) && item.getSize() > 0)
        return item;
    }
    return null;
  }

  /**
   * {@inheritDoc} 
   */
  public Cookie[] getCookies() {
    return this.req.getCookies();
  }

  /**
   * {@inheritDoc}
   */
  public void returnNotFound() {
    this.res.setStatus(HttpServletResponse.SC_NOT_FOUND);
  }

// attributes -------------------------------------------------------------------------------------

  /**
   * {@inheritDoc}
   */
  public Object getAttribute(String name) {
    return this.req.getAttribute(name);
  }

  /**
   * {@inheritDoc}
   */
  public void setAttribute(String name, Object o) {
    this.req.setAttribute(name, o);
  }

  /**
   * Returns the wrapped HTTP servlet request.
   * 
   * <p>This effectively enables the content generator to bypass the clean and simple 
   * methods of the content request, use wisely...
   * 
   * @return The wrapped HTTP servlet request.
   */
  public HttpServletRequest getHttpRequest() {
    return this.req;
  }

  /**
   * Returns the attached HTTP servlet response.
   * 
   * <p>This effectively enables the content generator to make use of the HTTP servlet
   * response, use wisely...
   * 
   * @return The attached HTTP servlet response.
   */
  public HttpServletResponse getHttpResponse() {
    return this.res;
  }

  /**
   * Utility method that determines whether this request contains multipart content.
   *
   * @return <code>true</code> if the request is multipart;
   *         <code>false</code> otherwise.
   */
  public boolean isMultipartContent() {
    if (!"post".equals(this.req.getMethod().toLowerCase())) {
      return false;
    }
    String contentType = this.req.getContentType();
    if (contentType == null) {
      return false;
    }
    if (contentType.toLowerCase().startsWith("multipart/")) {
      return true;
    }
    return false;
  }

  /**
   * Returns the session of the wrapped HTTP servlet request.
   * 
   * @return The session of the HTTP servlet request.
   */
  public HttpSession getSession() {
    return this.req.getSession();
  }

}
