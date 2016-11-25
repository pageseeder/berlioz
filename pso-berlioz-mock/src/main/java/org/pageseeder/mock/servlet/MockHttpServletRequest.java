/*
 * Copyright 2016 Allette Systems (Australia)
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
package org.pageseeder.mock.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

/**
 * An HTTP servlet request implementation mocking a servlet request sent by a
 * servlet container.
 *
 * <p>This class provides default settings and setters to modify any aspect of
 * the request for testing.
 *
 */
public class MockHttpServletRequest implements HttpServletRequest {

  private URI url;

  private String method;

  private Map<String, Object> attributes = new HashMap<>();

  private Map<String, String[]> parameters = new HashMap<>();

  private Map<String, List<String>> headers = new HashMap<>();

  private String remoteAddr = "127.0.0.1";

  private String remoteHost = "127.0.0.1";

  private MockHttpSession session = null;

  private String characterEncoding;

  private int contentLength = 0;

  private String contentType;

  private byte[] data = new byte[]{};

  public MockHttpServletRequest() {
    this.url = URI.create("http://localhost:8080/");
    this.method = "GET";
  }

  public MockHttpServletRequest(URI url, String method) {
    this.url = url;
    this.method = method;
  }

  @Override
  public Object getAttribute(String name) {
    return this.attributes.get(name);
  }

  @Override
  public Enumeration<String> getAttributeNames() {
    return Collections.enumeration(this.attributes.keySet());
  }

  @Override
  public String getCharacterEncoding() {
    return this.characterEncoding;
  }

  @Override
  public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
    this.characterEncoding = env;
  }

  @Override
  public int getContentLength() {
    return this.contentLength;
  }

  @Override
  public long getContentLengthLong() {
    return this.contentLength;
  }

  @Override
  public String getContentType() {
    return this.contentType;
  }

  @Override
  public ServletInputStream getInputStream() throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getParameter(String name) {
    String[] values = this.parameters.get(name);
    return values == null || values.length == 0? null : values[0];
  }

  @Override
  public Enumeration<String> getParameterNames() {
    return Collections.enumeration(this.parameters.keySet());
  }

  @Override
  public String[] getParameterValues(String name) {
    return this.parameters.get(name);
  }

  @Override
  public Map<String, String[]> getParameterMap() {
    return this.parameters;
  }

  @Override
  public String getProtocol() {
    // TODO Auto-generated method stub
    return getScheme().startsWith("http")? "HTTP/1.1" : getScheme();
  }

  @Override
  public String getScheme() {
    return this.url.getScheme();
  }

  @Override
  public String getServerName() {
    return this.url.getHost();
  }

  @Override
  public int getServerPort() {
    return this.url.getPort();
  }

  @Override
  public BufferedReader getReader() throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getRemoteAddr() {
    return this.remoteAddr;
  }

  @Override
  public String getRemoteHost() {
    return this.remoteHost;
  }

  @Override
  public void setAttribute(String name, Object o) {
    this.attributes.put(name, o);
  }

  @Override
  public void removeAttribute(String name) {
    this.attributes.remove(name);
  }

  @Override
  public Locale getLocale() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Enumeration<Locale> getLocales() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean isSecure() {
    return "https".equals(this.url.getScheme());
  }

  @Override
  public RequestDispatcher getRequestDispatcher(String path) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getRealPath(String path) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getRemotePort() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public String getLocalName() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getLocalAddr() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getLocalPort() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public ServletContext getServletContext() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public AsyncContext startAsync() throws IllegalStateException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
      throws IllegalStateException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean isAsyncStarted() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isAsyncSupported() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public AsyncContext getAsyncContext() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public DispatcherType getDispatcherType() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getAuthType() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Cookie[] getCookies() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public long getDateHeader(String name) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public String getHeader(String name) {
    List<String> values = this.headers.get(name);
    return values != null && values.size() > 0? values.get(0) : null;
  }

  @Override
  public Enumeration<String> getHeaders(String name) {
    List<String> values = this.headers.get(name);
    if (values == null) return  Collections.emptyEnumeration();
    return Collections.enumeration(values);
  }

  @Override
  public Enumeration<String> getHeaderNames() {
    return Collections.enumeration(this.headers.keySet());
  }

  @Override
  public int getIntHeader(String name) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public String getMethod() {
    return this.method;
  }

  @Override
  public String getPathInfo() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getPathTranslated() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getContextPath() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getQueryString() {
    return this.url.getQuery();
  }

  @Override
  public String getRemoteUser() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean isUserInRole(String role) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public Principal getUserPrincipal() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getRequestedSessionId() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getRequestURI() {
    return this.url.getPath();
  }

  @Override
  public StringBuffer getRequestURL() {
    return new StringBuffer(this.url.toString());
  }

  @Override
  public String getServletPath() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public HttpSession getSession(boolean create) {
    if (create && this.session == null) {
      this.session = new MockHttpSession();
    }
    return this.session;
  }

  @Override
  public HttpSession getSession() {
    return this.session;
  }

//  @Override
//  public String changeSessionId() {
//    if (this.session == null) throw new IllegalStateException();
// // TODO Auto-generated method stub
//    return null;
//  }

  @Override
  public boolean isRequestedSessionIdValid() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isRequestedSessionIdFromCookie() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isRequestedSessionIdFromURL() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isRequestedSessionIdFromUrl() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void login(String username, String password) throws ServletException {
    // TODO Auto-generated method stub

  }

  @Override
  public void logout() throws ServletException {
    // TODO Auto-generated method stub
  }

  @Override
  public Collection<Part> getParts() throws IOException, ServletException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Part getPart(String name) throws IOException, ServletException {
    // TODO Auto-generated method stub
    return null;
  }

//  @Override
//  public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
//    // TODO Auto-generated method stub
//    throw new UnsupportedOperationException();
//  }

  // Setters which aren't part of Servlet API
  //

  public void addHeader(String name, String value) {
    List<String> values = this.headers.get(name);
    if (values == null) {
      values = new ArrayList<>();
    }
    values.add(value);
    this.headers.put(name, values);
  }

  public void setHeader(String name, String value) {
    List<String> values = new ArrayList<>();
    values.add(value);
    this.headers.put(name, values);
  }

  public void setParameterValues(String name, String[] values) {
    this.parameters.put(name, values);
  }

  public void addParameter(String name, String value) {
    String[] values = this.parameters.get(name);
    values = Arrays.copyOf(values, values.length+1);
    values[values.length-1] = value;
    this.parameters.put(name, values);
  }

  public void setParameter(String name, String value) {
    this.parameters.put(name, new String[]{value});
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  @Override
  public String changeSessionId() {
    if (this.session != null) return this.session.changeId();
    throw new IllegalStateException("there is no session associated with the request");
  }

  /**
   * This method is not supported.
   */
  @Override
  public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
    throw new UnsupportedOperationException();
  }


  // Factory method
  public static MockHttpServletRequest create(String url, String method) {
    return create(URI.create(url), method);
  }

  public static MockHttpServletRequest create(URI url, String method) {
    MockHttpServletRequest req = new MockHttpServletRequest(url, method);
    // Extract parameters from the query
    String query = url.getQuery();
    String[] pairs = query.split("&");
    for (String p : pairs) {
      int equal = p.indexOf('=');
      if (equal > 0) {
        req.addParameter(p.substring(0, equal), p.substring(equal+1));
      } else {
        req.addParameter(p, "");
      }
    }
    req.setHeader("Host", "chris-pc.ad.allette.com.au:8443");
    req.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:44.0) Gecko/20100101 Firefox/44.0");
    req.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
    req.setHeader("Accept-Language", "en-US,en;q=0.5");
    req.setHeader("Accept-Encoding", "gzip, deflate, br");
    return req;
  }

}
