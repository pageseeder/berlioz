/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.servlet;

import java.io.IOException;
import java.io.Serializable;

import javax.servlet.http.HttpServletRequest;

import org.weborganic.berlioz.content.PathInfo;

import com.topologi.diffx.xml.XMLWriter;

/**
 * An implementation of the path info based on HTTP servlets.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.13 - 21 January 2013
 */
public final class HttpPathInfo implements PathInfo, Serializable {

  /**
   * As per requirement for <code>Serializable</code>.
   */
  private static final long serialVersionUID = -2025665764421725251L;

  /**
   * The Web application context.
   */
  private final String _context;

  /**
   * The extension (may be <code>null</code>).
   */
  private final String _extension;

  /**
   * The prefix (may be <code>null</code>).
   */
  private final String _prefix;

  /**
   * The berlioz path.
   */
  private final String _path;

  /**
   * Creates a new PathInfo for HTTP.
   *
   * @param req the HTTP servlet request.
   *
   * @throws NullPointerException if the request is <code>null</code>
   */
  protected HttpPathInfo(HttpServletRequest req) {
    System.err.print("PATH-INFO");
    System.err.print("[context:"+req.getContextPath());
    System.err.print(", path-info:"+req.getPathInfo());
    System.err.print(", servlet-path:"+req.getServletPath());

    this._context = req.getContextPath();
    if (req.getPathInfo() != null) {
      // Try to get the path info (when mapped to '/prefix/*')
      this._path = req.getPathInfo();
      this._prefix = req.getServletPath();
      this._extension = "";
    } else {
      // Otherwise assume that it is mapped to '*.suffix'
      String path = req.getServletPath();
      int dot = path.lastIndexOf('.');
      this._path = (dot != -1)? path.substring(0, dot) : path;
      this._prefix = "";
      this._extension = (dot != -1)? path.substring(dot) : "";
    }
    System.err.print(", prefix:"+this._prefix);
    System.err.print(", prefix:"+this._prefix);
    System.err.print(", path:"+this._path);
    System.err.print(", extension:"+this._extension);
    System.err.print("]=>");
    System.err.println(toString());
  }

  @Override
  public String context() {
    return this._context;
  }

  @Override
  public String extension() {
    return this._extension;
  }

  @Override
  public String path() {
    return this._path;
  }

  @Override
  public String prefix() {
    return this._prefix;
  }

  @Override
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("path-info");
    if (this._context.length() > 0)
      xml.attribute("context", this._context);
    if (this._prefix.length() > 0)
      xml.attribute("prefix", this._prefix);
    xml.attribute("path", this._path);
    if (this._extension.length() > 0)
      xml.attribute("extension", this._extension);
    xml.closeElement();
  }

  @Override
  public String toString() {
    return this._context+this._prefix+this._path+this._extension;
  }
}
