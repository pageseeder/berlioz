/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.servlet;

import java.io.IOException;
import java.util.Enumeration;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import org.weborganic.berlioz.GlobalSettings;
import org.weborganic.berlioz.content.ContentGenerator;

import com.topologi.diffx.xml.XMLWritable;
import com.topologi.diffx.xml.XMLWriter;

/**
 * The XML header common to all Berlioz responses.
 * 
 * <p>This class is used to produce consistent headers for all the servlets.
 * 
 * <p>The <i>area name</i> and <i>service name</i>, must be specified by the servlet.
 * 
 * <p>The HTTP parameters are the parameters attached with the HTTP request. They are returned
 * in the order in which they are given by the HTTP request. Values for parameters with multiple
 * values are returned in order as separate parameters with the same name.
 * 
 * <p>The <var>servlet path info</var> is the result of
 * {@link javax.servlet.http.HttpServletRequest#getPathInfo()}.
 * 
 * <p>The <var>servlet context path</var> is the result of
 * {@link javax.servlet.http.HttpServletRequest#getContextPath()}.
 *
 * <p>The <var>remote host</var> is the result of
 * {@link javax.servlet.http.ServletRequest#getRemoteHost()}.
 *
 * <p>The <var>remote port</var> is the result of
 * {@link javax.servlet.http.ServletRequest#getRemotePort()}.
 *
 * @author Christophe Lauret (Weborganic)
 * @version 19 November 2009
 */
public final class XMLResponseHeader implements XMLWritable {

  /**
   * The HTTP servlet request object.
   */
  private final HttpServletRequest _request;

  /**
   * The name of the service provided.
   */
  private final String _service;

  /**
   * The name of the service provided.
   */
  private final String _area;

  /**
   * Creates a new XML response header using the path info to generate the service name.
   * 
   * <p>For example, if the servlet is configured for URL pattern <code>/df/*</code>,
   * and a given URL is <code>/df/something/do</code>, the name of the service will be
   * <code>do-something</code>.
   * 
   * @param request The HTTP request.
   */
  public XMLResponseHeader(HttpServletRequest request) {
    this._request = request;
    this._service = toServiceName(request);
    this._area = "default";
  }

  /**
   * Creates a new XML response header.
   * 
   * @param request The HTTP request.
   * @param service The service that is being provided.
   */
  public XMLResponseHeader(HttpServletRequest request, String service) {
    this._request = request;
    this._service = service;
    this._area = "default";
  }

  /**
   * Creates a new XML response header.
   * 
   * @param request   The HTTP request.
   * @param generator The content generator used.
   */
  public XMLResponseHeader(HttpServletRequest request, ContentGenerator generator) {
    this._request = request;
    this._service = generator.getService();
    this._area = generator.getArea();
  }

  /**
   * Converts the path info from the servlet request to the name of the service.
   * 
   * @param request The servlet request.
   * 
   * @return The name of the service.
   */
  public static String toServiceName(HttpServletRequest request) {
    String pathInfo = request.getPathInfo();
    if (pathInfo == null) return "";
    StringBuffer name = new StringBuffer();
    StringTokenizer st = new StringTokenizer(pathInfo, "/");
    while (st.hasMoreTokens()) {
      if (name.length() > 0) name.insert(0, '-');
      name.insert(0, st.nextToken());
    }
    return name.toString();
  }

  /**
   * Writes the XML response for this header.
   * 
   * <pre class="xml">
   *   &lt;header&gt;
   *     &lt;area&gt;[area name]&lt;/area&gt;
   *     &lt;service&gt;[service name]&lt;/service&gt;
   *     &lt;path-info&gt;[servlet path info]&lt;/path-info&gt;
   *     &lt;context-path&gt;[servlet context path]&lt;/context-path&gt;
   *     &lt;host&gt;[remote host]&lt;/host&gt;
   *     &lt;port&gt;[remote port]&lt;/port&gt;
   *     &lt;url&gt;[remote port]&lt;/url&gt;
   *     &lt;query-string&gt;[remote port]&lt;/query-string&gt;
   *     &lt;http-parameters&gt;
   *       &lt;parameter name="[name-A]"&gt;[value-A]&lt;/parameter&gt;
   *       &lt;parameter name="[name-B]"&gt;[value-B1]&lt;/parameter&gt;
   *       &lt;parameter name="[name-B]"&gt;[value-B2]&lt;/parameter&gt;
   *       &lt;parameter name="[name-C]"&gt;[value-C]&lt;/parameter&gt;
   *       &lt;parameter name="[name-D]"&gt;[value-D]&lt;/parameter&gt;
   *       <code class="comment">&lt;!-- ... --&gt;</code>
   *     &lt;/http-parameters&gt;
   *   &lt;/header&gt;
   * </pre>
   * 
   * @see XMLWritable#toXML(com.topologi.diffx.xml.XMLWriter)
   * 
   * @param xml The XML Writer to use.
   * 
   * @throws IOException If thrown by the underlying XML Writer.
   */
  public void toXML(XMLWriter xml) throws IOException {
    // start serialising
    xml.openElement("header", true);
    xml.element("area", this._area);
    xml.element("service", this._service);
    xml.element("path-info", this._request.getPathInfo());
    xml.element("context-path", this._request.getContextPath());
    xml.element("host", this._request.getServerName());
    int port = GlobalSettings.get("xmlport", this._request.getServerPort());
    xml.element("port", Integer.toString(port));
    xml.element("url", this._request.getRequestURL().toString());
    xml.element("query-string", this._request.getQueryString());
    // Include the configuration
    xml.element("configuration", GlobalSettings.get("configuration", ""));

    // write the http parameters
    xml.openElement("http-parameters", true);
    Enumeration<?> names = this._request.getParameterNames();
    while (names.hasMoreElements()) {
      String paramName = (String)names.nextElement();
      String[] values = this._request.getParameterValues(paramName);
      for (int i = 0; i < values.length; i++) {
        xml.openElement("parameter", false);
        xml.attribute("name", paramName);
        xml.writeText(values[i]);
        xml.closeElement();
      }
    }
    xml.closeElement(); // close http-parameters
    xml.closeElement(); // close header
  }

}
