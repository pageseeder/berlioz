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

import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
import javax.servlet.ReadListener;

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

  private Map<String, RequestDispatcher> dispatchers = new HashMap<>();

  private String remoteAddr = "127.0.0.1";

  private String remoteHost = "127.0.0.1";

  private int remotePort = 0;

  private String localAddr = "127.0.0.1";

  private String localName = "localhost";

  private int localPort = 0;

  private MockHttpSession session = null;

  private String requestedSessionId;

  private String characterEncoding;

  private int contentLength = 0;

  private String contentType;

  private byte[] data = new byte[]{};

  private Locale locale = Locale.getDefault();

  private List<Locale> locales = new ArrayList<>();

  private ServletContext servletContext;

  private String contextPath = "";

  private String servletPath;

  private String pathInfo;

  private String pathTranslated;

  private String remoteUser;

  private String authType;

  private Principal principal;

  private List<Cookie> cookies = new ArrayList<>();

  private Collection<Part> parts = new ArrayList<>();

  public MockHttpServletRequest() {
    this.url = URI.create("http://localhost:8080/");
    this.method = "GET";
    this.servletPath = this.url.getPath();
  }

  public MockHttpServletRequest(URI url, String method) {
    this.url = url;
    this.method = method;
    this.servletPath = url.getPath();
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
    return new MockServletInputStream(this.data);
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
    return Collections.unmodifiableMap(this.parameters);
  }

  @Override
  public String getProtocol() {
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
    int port = this.url.getPort();
    if (port >= 0) return port;
    return isSecure()? 443 : 80;
  }

  @Override
  public BufferedReader getReader() throws IOException {
    return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(this.data), getCharset()));
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
    return this.locale;
  }

  @Override
  public Enumeration<Locale> getLocales() {
    if (this.locales.isEmpty()) return Collections.enumeration(Collections.singletonList(this.locale));
    return Collections.enumeration(this.locales);
  }

  @Override
  public boolean isSecure() {
    return "https".equals(this.url.getScheme());
  }

  @Override
  public RequestDispatcher getRequestDispatcher(String path) {
    return this.dispatchers.get(path);
  }

  @Override
  public String getRealPath(String path) {
    return this.servletContext != null? this.servletContext.getRealPath(path) : path;
  }

  @Override
  public int getRemotePort() {
    return this.remotePort;
  }

  @Override
  public String getLocalName() {
    return this.localName;
  }

  @Override
  public String getLocalAddr() {
    return this.localAddr;
  }

  @Override
  public int getLocalPort() {
    return this.localPort != 0? this.localPort : getServerPort();
  }

  @Override
  public ServletContext getServletContext() {
    return this.servletContext;
  }

  @Override
  public AsyncContext startAsync() throws IllegalStateException {
    throw new IllegalStateException("Async processing is not supported by this mock request");
  }

  @Override
  public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
      throws IllegalStateException {
    throw new IllegalStateException("Async processing is not supported by this mock request");
  }

  @Override
  public boolean isAsyncStarted() {
    return false;
  }

  @Override
  public boolean isAsyncSupported() {
    return false;
  }

  @Override
  public AsyncContext getAsyncContext() {
    throw new IllegalStateException("Async processing has not been started");
  }

  @Override
  public DispatcherType getDispatcherType() {
    return DispatcherType.REQUEST;
  }

  @Override
  public String getAuthType() {
    return this.authType;
  }

  @Override
  public Cookie[] getCookies() {
    return this.cookies.isEmpty()? null : this.cookies.toArray(new Cookie[this.cookies.size()]);
  }

  @Override
  public long getDateHeader(String name) {
    String value = getHeader(name);
    if (value == null) return -1;
    try {
      return java.time.ZonedDateTime.parse(value, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME)
          .toInstant().toEpochMilli();
    } catch (java.time.format.DateTimeParseException ex) {
      throw new IllegalArgumentException("Header "+name+" cannot be parsed as an HTTP date", ex);
    }
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
    String value = getHeader(name);
    if (value == null) return -1;
    return Integer.parseInt(value);
  }

  @Override
  public String getMethod() {
    return this.method;
  }

  @Override
  public String getPathInfo() {
    return this.pathInfo;
  }

  @Override
  public String getPathTranslated() {
    return this.pathTranslated;
  }

  @Override
  public String getContextPath() {
    return this.contextPath;
  }

  @Override
  public String getQueryString() {
    return this.url.getQuery();
  }

  @Override
  public String getRemoteUser() {
    return this.remoteUser;
  }

  @Override
  public boolean isUserInRole(String role) {
    return false;
  }

  @Override
  public Principal getUserPrincipal() {
    return this.principal;
  }

  @Override
  public String getRequestedSessionId() {
    return this.requestedSessionId;
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
    return this.servletPath;
  }

  @Override
  public HttpSession getSession(boolean create) {
    if (create && this.session == null) {
      this.session = new MockHttpSession();
      this.requestedSessionId = this.session.getId();
      return this.session;
    }
    if (this.session != null) this.session.access();
    return this.session;
  }

  @Override
  public HttpSession getSession() {
    return getSession(true);
  }

  @Override
  public boolean isRequestedSessionIdValid() {
    return this.session != null && this.session.getId().equals(this.requestedSessionId);
  }

  @Override
  public boolean isRequestedSessionIdFromCookie() {
    return this.requestedSessionId != null;
  }

  @Override
  public boolean isRequestedSessionIdFromURL() {
    return false;
  }

  @Override
  public boolean isRequestedSessionIdFromUrl() {
    return false;
  }

  @Override
  public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
    return this.principal != null;
  }

  @Override
  public void login(String username, String password) throws ServletException {
    this.remoteUser = username;
    this.authType = "FORM";
    this.principal = () -> username;
  }

  @Override
  public void logout() throws ServletException {
    this.remoteUser = null;
    this.authType = null;
    this.principal = null;
  }

  @Override
  public Collection<Part> getParts() throws IOException, ServletException {
    return Collections.unmodifiableCollection(this.parts);
  }

  @Override
  public Part getPart(String name) throws IOException, ServletException {
    for (Part part : this.parts) {
      if (part.getName().equals(name)) return part;
    }
    return null;
  }

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
    if (values == null) values = new String[0];
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

  public void setContent(byte[] data) {
    this.data = data != null? Arrays.copyOf(data, data.length) : new byte[]{};
    this.contentLength = this.data.length;
  }

  public void setContent(String content) {
    setContent(content.getBytes(getCharset()));
  }

  public void setCookie(Cookie cookie) {
    this.cookies.clear();
    addCookie(cookie);
  }

  public void addCookie(Cookie cookie) {
    this.cookies.add(cookie);
  }

  public void setLocale(Locale locale) {
    this.locale = locale;
    this.locales.clear();
    this.locales.add(locale);
  }

  public void addLocale(Locale locale) {
    if (this.locales.isEmpty()) this.locales.add(this.locale);
    this.locales.add(locale);
  }

  public void setRequestDispatcher(String path, RequestDispatcher dispatcher) {
    this.dispatchers.put(path, dispatcher);
  }

  public void setServletContext(ServletContext servletContext) {
    this.servletContext = servletContext;
  }

  public void setContextPath(String contextPath) {
    this.contextPath = contextPath;
  }

  public void setServletPath(String servletPath) {
    this.servletPath = servletPath;
  }

  public void setPathInfo(String pathInfo) {
    this.pathInfo = pathInfo;
  }

  public void setPathTranslated(String pathTranslated) {
    this.pathTranslated = pathTranslated;
  }

  public void setRemoteAddr(String remoteAddr) {
    this.remoteAddr = remoteAddr;
  }

  public void setRemoteHost(String remoteHost) {
    this.remoteHost = remoteHost;
  }

  public void setRemotePort(int remotePort) {
    this.remotePort = remotePort;
  }

  public void setLocalAddr(String localAddr) {
    this.localAddr = localAddr;
  }

  public void setLocalName(String localName) {
    this.localName = localName;
  }

  public void setLocalPort(int localPort) {
    this.localPort = localPort;
  }

  public void setParts(Collection<Part> parts) {
    this.parts = parts != null? new ArrayList<>(parts) : new ArrayList<>();
  }

  @Override
  public String changeSessionId() {
    if (this.session != null) {
      String id = this.session.changeId();
      this.requestedSessionId = id;
      return id;
    }
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
    if (query != null && !query.isEmpty()) {
      String[] pairs = query.split("&");
      for (String p : pairs) {
        int equal = p.indexOf('=');
        if (equal > 0) {
          req.addParameter(p.substring(0, equal), p.substring(equal+1));
        } else {
          req.addParameter(p, "");
        }
      }
    }
    req.setHeader("Host", "chris-pc.ad.allette.com.au:8443");
    req.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:44.0) Gecko/20100101 Firefox/44.0");
    req.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
    req.setHeader("Accept-Language", "en-US,en;q=0.5");
    req.setHeader("Accept-Encoding", "gzip, deflate, br");
    return req;
  }

  private Charset getCharset() {
    if (this.characterEncoding == null) return StandardCharsets.UTF_8;
    return Charset.forName(this.characterEncoding);
  }

  private static final class MockServletInputStream extends ServletInputStream {

    private final ByteArrayInputStream in;

    private ReadListener listener;

    private MockServletInputStream(byte[] data) {
      this.in = new ByteArrayInputStream(data);
    }

    @Override
    public int read() throws IOException {
      return this.in.read();
    }

    @Override
    public boolean isFinished() {
      return this.in.available() == 0;
    }

    @Override
    public boolean isReady() {
      return true;
    }

    @Override
    public void setReadListener(ReadListener readListener) {
      this.listener = readListener;
    }

    @SuppressWarnings("unused")
    ReadListener getReadListener() {
      return this.listener;
    }
  }

}
