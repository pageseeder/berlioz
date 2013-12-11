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

import org.weborganic.berlioz.BerliozOption;
import org.weborganic.berlioz.GlobalSettings;
import org.weborganic.berlioz.content.Location;
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
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.25 - 11 December 2013
 * @since Berlioz 0.6.0
 */
public final class XMLResponseHeader implements XMLWritable {

  /**
   * The core HTTP details.
   */
  private final CoreHttpRequest _core;

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
   * Creates a new XML response header.
   *
   * @param core     The core HTTP info.
   * @param service  The service object.
   * @param results  The result of URI resolution.
   */
  protected XMLResponseHeader(CoreHttpRequest core, Service service, URIResolveResult results) {
    this._core = core;
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
      if (name.length() > 0) {
        name.insert(0, '-');
      }
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
   *     <host>[server host]</host>
   *     <port>[server port]</port>
   *     <url>[url (up to query)]</url>
   *     <query-string>[query string]</query-string>
   *     <location scheme="[http|https]"
   *                 host="[hostname]"
   *                 port="[post]"
   *                 path="[path]"
   *                query="[query]">[full url]</location>
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
  @Override
  public void toXML(XMLWriter xml) throws IOException {
    HttpServletRequest req = this._core.request();

    // start serialising
    xml.openElement("header", true);
    xml.element("group", this._group);
    xml.element("service", this._service);
    xml.element("path-info", HttpRequestWrapper.getBerliozPath(req));
    xml.element("context-path", req.getContextPath());

    // Deprecated from 1.0
    if (GlobalSettings.has(BerliozOption.XML_HEADER_COMPATIBILITY)) {
      xml.writeComment("Elements 'scheme', 'host', 'port', 'url' and 'query-string' will be deprecated in Berlioz 1.0, use 'location' instead");
      xml.element("scheme", req.getScheme());
      xml.element("host", req.getServerName());
      xml.element("port", Integer.toString(req.getServerPort()));
      xml.element("url", req.getRequestURL().toString());
      xml.element("query-string", req.getQueryString());
    }

    // New location info
    Location location = this._core.location();
    if (location != null) location.toXML(xml);

    // Write the http parameters
    xml.openElement("http-parameters", true);
    Enumeration<?> names = req.getParameterNames();
    while (names.hasMoreElements()) {
      String paramName = (String)names.nextElement();
      String[] values = req.getParameterValues(paramName);
      for (String value : values) {
        xml.openElement("parameter", false);
        xml.attribute("name", paramName);
        xml.writeText(value);
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
