/*
 * Copyright 2015 Allette Systems (Australia)
 * http://www.allette.com.au
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pageseeder.berlioz.servlet;

import java.io.IOException;
import java.io.Serializable;

import javax.servlet.http.HttpServletRequest;

import org.pageseeder.berlioz.content.PathInfo;
import org.pageseeder.xmlwriter.XMLWriter;

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
    xml.openElement("path");
    if (this._context.length() > 0) {
      xml.attribute("context", this._context);
    }
    if (this._prefix.length() > 0) {
      xml.attribute("prefix", this._prefix);
    }
    xml.attribute("info", this._path);
    if (this._extension.length() > 0) {
      xml.attribute("extension", this._extension);
    }
    xml.closeElement();
  }

  @Override
  public String toString() {
    return this._context+this._prefix+this._path+this._extension;
  }
}
