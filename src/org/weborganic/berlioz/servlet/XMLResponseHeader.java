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
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import org.weborganic.berlioz.GlobalSettings;
import org.weborganic.berlioz.content.Service;
import org.weborganic.furi.URIResolveResult;

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
 * 
 * @since Berlioz 0.6.0
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
   * The group of the service provided.
   */
  private final String _group;

  /**
   * The results of URI resolution.
   */
  private URIResolveResult _results;

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
    this._group = "default";
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
    this._group = "default";
  }

  /**
   * Creates a new XML response header.
   * 
   * @param request  The HTTP request.
   * @param service  The service object.
   */
  public XMLResponseHeader(HttpServletRequest request, Service service) {
    this._request = request;
    this._service = service.id();
    this._group = service.group();
  }

  /**
   * Creates a new XML response header.
   * 
   * @param request  The HTTP request.
   * @param service  The service object.
   * @param results  The result of URI resolution.
   */
  public XMLResponseHeader(HttpServletRequest request, Service service, URIResolveResult results) {
    this._request = request;
    this._service = service.id();
    this._group = service.group();
    this._results = results;
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
   * <pre class="xml">{@code
   *   <header>
   *     <group>[service group name]</group>
   *     <service>[service name]</service>
   *     <path-info>[berlioz path]</path-info>
   *     <context-path>[servlet context path]</context-path>
   *     <host>[remote host]</host>
   *     <port>[remote port]</port>
   *     <url>[remote url]</url>
   *     <query-string>[remote port]</query-string>
   *     <http-parameters>
   *       <parameter name="[name-A]">[value-A]</parameter>
   *       <parameter name="[name-B]">[value-B1]</parameter>
   *       <parameter name="[name-B]">[value-B2]</parameter>
   *       <parameter name="[name-C]">[value-C]</parameter>
   *       <parameter name="[name-D]">[value-D]</parameter>
   *       <!-- ... -->
   *     </http-parameters>
   *     <uri-parameters>
   *       <parameter name="[name-X]">[value-X]</parameter>
   *       <parameter name="[name-Y]">[value-Y]</parameter>
   *       <!-- ... -->
   *     </uri-parameters>
   *   </header>
   * }</pre>
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
    xml.element("group", this._group);
    xml.element("service", this._service);
    xml.element("path-info", HttpRequestWrapper.getBerliozPath(this._request));
    xml.element("context-path", this._request.getContextPath());
    xml.element("host", this._request.getServerName());
    int port = GlobalSettings.get("xmlport", this._request.getServerPort());
    xml.element("port", Integer.toString(port));
    xml.element("url", this._request.getRequestURL().toString());
    xml.element("query-string", this._request.getQueryString());

    // Write the http parameters
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

    // Write the URI parameters
    if (this._results != null) {
      Set<String> unames = this._results.names();
      xml.openElement("uri-parameters", !unames.isEmpty());
      for (String name : unames) {
        Object value = this._results.get(name);
        xml.openElement("parameter", false);
        xml.attribute("name", name);
        xml.writeText(value != null? value.toString() : "");
        xml.closeElement();
      }
      xml.closeElement();
    }

    // Include Berlioz version
    xml.openElement("berlioz");
    xml.attribute("version", GlobalSettings.getVersion());
    xml.closeElement();

    xml.closeElement(); // close header
  }

}
