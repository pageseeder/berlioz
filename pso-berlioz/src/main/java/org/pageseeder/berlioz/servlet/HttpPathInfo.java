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
 * @version Berlioz 0.11.2
 */
public final class HttpPathInfo implements PathInfo, Serializable {

  /**
   * As per requirement for <code>Serializable</code>.
   */
  private static final long serialVersionUID = -2025665764421725251L;

  /**
   * The Web application context.
   */
  private final String context;

  /**
   * The extension (might be <code>null</code>).
   */
  private final String extension;

  /**
   * The prefix (might be <code>null</code>).
   */
  private final String prefix;

  /**
   * The berlioz path.
   */
  private final String path;

  /**
   * Creates a new PathInfo for HTTP.
   *
   * @param req the HTTP servlet request.
   *
   * @throws NullPointerException if the request is <code>null</code>
   */
  protected HttpPathInfo(HttpServletRequest req) {
    this.context = req.getContextPath();
    if (req.getPathInfo() != null) {
      // Try to get the path info (when mapped to '/prefix/*')
      String pathInfo = req.getPathInfo();
      this.path = pathInfo != null? pathInfo : "";
      this.prefix = req.getServletPath();
      this.extension = "";
    } else {
      // Otherwise assume that it is mapped to '*.suffix'
      String servletPath = req.getServletPath();
      int dot = servletPath.lastIndexOf('.');
      this.path = (dot != -1)? servletPath.substring(0, dot) : servletPath;
      this.prefix = "";
      this.extension = (dot != -1)? servletPath.substring(dot) : "";
    }
  }

  @Override
  public String context() {
    return this.context;
  }

  @Override
  public String extension() {
    return this.extension;
  }

  @Override
  public String path() {
    return this.path;
  }

  @Override
  public String prefix() {
    return this.prefix;
  }

  @Override
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("path");
    if (!this.context.isEmpty()) {
      xml.attribute("context", this.context);
    }
    if (!this.prefix.isEmpty()) {
      xml.attribute("prefix", this.prefix);
    }
    xml.attribute("info", this.path);
    if (!this.extension.isEmpty()) {
      xml.attribute("extension", this.extension);
    }
    xml.closeElement();
  }

  @Override
  public String toString() {
    return this.context +this.prefix +this.path +this.extension;
  }
}
