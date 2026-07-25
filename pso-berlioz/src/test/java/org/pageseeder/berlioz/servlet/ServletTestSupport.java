package org.pageseeder.berlioz.servlet;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ReadListener;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ServletTestSupport {

  private ServletTestSupport() {}

  public static RequestBuilder request() {
    return new RequestBuilder();
  }

  static ResponseRecorder response() {
    return new ResponseRecorder();
  }

  static FilterChain recordingChain(boolean[] invoked) {
    return (req, res) -> invoked[0] = true;
  }

  static FilterConfig filterConfig(String contextRealPath, Map<String, String> initParams) {
    ServletContext ctx = (ServletContext) Proxy.newProxyInstance(
        ServletContext.class.getClassLoader(),
        new Class<?>[]{ServletContext.class},
        (proxy, m, args) -> {
          if ("getRealPath".equals(m.getName())) return contextRealPath;
          return defaultValue(m.getReturnType());
        });
    return (FilterConfig) Proxy.newProxyInstance(
        FilterConfig.class.getClassLoader(),
        new Class<?>[]{FilterConfig.class},
        (proxy, m, args) -> {
          if ("getServletContext".equals(m.getName())) return ctx;
          if ("getInitParameter".equals(m.getName())) return initParams.get(args[0]);
          return defaultValue(m.getReturnType());
        });
  }

  public static final class RequestBuilder {
    private String method = "GET";
    private String scheme = "http";
    private String host = "localhost";
    private int port = 80;
    private String contextPath = "";
    private String servletPath = "";
    private String pathInfo = null;
    private String requestURI = "/";
    private String queryString = null;
    private String characterEncoding = null;
    private String remoteAddr = "127.0.0.1";
    private String contentType = null;
    private byte[] body = new byte[0];
    private ServletContext servletContext = null;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private final Map<String, String> parameters = new LinkedHashMap<>();
    private final Map<String, Object> attributes = new LinkedHashMap<>();
    private RequestDispatcher dispatcher = null;

    public RequestBuilder method(String v)      { this.method = v; return this; }
    public RequestBuilder scheme(String v)      { this.scheme = v; return this; }
    public RequestBuilder host(String v)        { this.host = v; return this; }
    public RequestBuilder port(int v)           { this.port = v; return this; }
    public RequestBuilder contextPath(String v) { this.contextPath = v; return this; }
    public RequestBuilder servletPath(String v) { this.servletPath = v; return this; }
    public RequestBuilder pathInfo(String v)    { this.pathInfo = v; return this; }
    public RequestBuilder uri(String v)         { this.requestURI = v; return this; }
    public RequestBuilder query(String v)       { this.queryString = v; return this; }
    public RequestBuilder remoteAddr(String v)  { this.remoteAddr = v; return this; }
    public RequestBuilder servletContext(ServletContext v) { this.servletContext = v; return this; }
    public RequestBuilder header(String n, String v)    { this.headers.put(n, v); return this; }
    public RequestBuilder parameter(String n, String v) { this.parameters.put(n, v); return this; }
    public RequestBuilder attribute(String n, Object v) { this.attributes.put(n, v); return this; }
    public RequestBuilder dispatcher(RequestDispatcher d) { this.dispatcher = d; return this; }
    public RequestBuilder contentType(String v) { this.contentType = v; return this; }
    public RequestBuilder body(String v)        { this.body = v.getBytes(StandardCharsets.UTF_8); return this; }

    public HttpServletRequest build() {
      InvocationHandler h = (proxy, m, args) -> {
        switch (m.getName()) {
          case "getMethod":       return method;
          case "getScheme":       return scheme;
          case "getServerName":   return host;
          case "getServerPort":   return port;
          case "getContextPath":  return contextPath;
          case "getServletPath":  return servletPath;
          case "getPathInfo":     return pathInfo;
          case "getRequestURI":   return requestURI;
          case "getRequestURL":   return requestURL();
          case "getQueryString":  return queryString;
          case "getRemoteAddr":   return remoteAddr;
          case "getServletContext": return servletContext;
          case "getHeader":       return headers.get(args[0]);
          case "getDateHeader":   return dateHeader((String) args[0]);
          case "setCharacterEncoding": characterEncoding = (String) args[0]; return null;
          case "getCharacterEncoding": return characterEncoding;
          case "getHeaders": {
            String val = headers.get(args[0]);
            return Collections.enumeration(val != null ? List.of(val) : Collections.emptyList());
          }
          case "getHeaderNames":  return Collections.enumeration(headers.keySet());
          case "getParameter":    return parameters.get(args[0]);
          case "getParameterNames": return Collections.enumeration(parameters.keySet());
          case "getParameterValues": {
            String val = parameters.get(args[0]);
            return val != null ? new String[]{val} : null;
          }
          case "getParameterMap": {
            Map<String, String[]> map = new LinkedHashMap<>();
            parameters.forEach((k, v) -> map.put(k, new String[]{v}));
            return map;
          }
          case "getContentType":       return contentType;
          case "getContentLength":     return body.length;
          case "getContentLengthLong": return (long) body.length;
          case "getInputStream":       return new ServletInputStream() {
            private final ByteArrayInputStream in = new ByteArrayInputStream(body);
            @Override public boolean isFinished() { return in.available() == 0; }
            @Override public boolean isReady() { return true; }
            @Override public void setReadListener(ReadListener readListener) { throw new UnsupportedOperationException(); }
            @Override public int read() { return in.read(); }
          };
          case "getAttribute":    return attributes.get(args[0]);
          case "setAttribute":    attributes.put((String) args[0], args[1]); return null;
          case "getAttributeNames": return Collections.enumeration(attributes.keySet());
          case "getRequestDispatcher": return dispatcher;
          case "toString":   return "Request[" + method + " " + requestURI + "]";
          case "hashCode":   return System.identityHashCode(proxy);
          case "equals":     return proxy == args[0];
          default:           return defaultValue(m.getReturnType());
        }
      };
      return (HttpServletRequest) Proxy.newProxyInstance(
          HttpServletRequest.class.getClassLoader(),
          new Class<?>[]{HttpServletRequest.class}, h);
    }

    private StringBuffer requestURL() {
      StringBuilder url = new StringBuilder();
      url.append(scheme).append("://").append(host);
      if (("http".equals(scheme) && port != 80) || ("https".equals(scheme) && port != 443)) {
        url.append(':').append(port);
      }
      url.append(requestURI);
      return new StringBuffer(url.toString());
    }

    private long dateHeader(String name) {
      String value = headers.get(name);
      if (value == null) return -1L;
      try {
        return ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli();
      } catch (DateTimeParseException ex) {
        throw new IllegalArgumentException("Invalid date header: " + name, ex);
      }
    }
  }

  static final class ResponseRecorder {
    int status = 200;
    boolean resetCalled = false;
    boolean committed = false;
    String errorMessage = null;
    String contentType = null;
    String characterEncoding = null;
    final StringWriter body = new StringWriter();
    final PrintWriter writer = new PrintWriter(body);
    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    final Map<String, String> headers = new LinkedHashMap<>();

    String header(String name) { return headers.get(name); }
    String content() {
      writer.flush();
      if (body.getBuffer().length() > 0) return body.toString();
      return bytes.toString(StandardCharsets.UTF_8);
    }

    HttpServletResponse build() {
      InvocationHandler h = (proxy, m, args) -> {
        switch (m.getName()) {
          case "setStatus":            status = (Integer) args[0]; return null;
          case "setHeader":
          case "addHeader":            headers.put((String) args[0], (String) args[1]); return null;
          case "setDateHeader":
          case "setIntHeader":         headers.put((String) args[0], String.valueOf(args[1])); return null;
          case "reset":                reset(); return null;
          case "sendError":            status = (Integer) args[0]; errorMessage = args.length > 1 ? (String) args[1] : null; return null;
          case "setContentLength":
          case "setContentLengthLong":  headers.put("Content-Length", String.valueOf(args[0])); return null;
          case "setCharacterEncoding":  setCharacterEncoding(String.valueOf(args[0])); return null;
          case "flushBuffer":          committed = true; return null;
          case "isCommitted":          return committed;
          case "setContentType":       setContentType((String) args[0]); return null;
          case "getStatus":            return status;
          case "getWriter":            return writer;
          case "getOutputStream":       return new ServletOutputStream() {
            @Override public void write(int b) { bytes.write(b); }
            @Override public boolean isReady() { return true; }
            @Override public void setWriteListener(WriteListener writeListener) { throw new UnsupportedOperationException(); }
          };
          case "getHeader":            return headers.get(args[0]);
          case "encodeRedirectURL":     return args[0];
          case "encodeURL":             return args[0];
          case "toString":             return "ResponseRecorder";
          case "hashCode":             return System.identityHashCode(proxy);
          case "equals":               return proxy == args[0];
          default:                     return defaultValue(m.getReturnType());
        }
      };
      return (HttpServletResponse) Proxy.newProxyInstance(
          HttpServletResponse.class.getClassLoader(),
          new Class<?>[]{HttpServletResponse.class}, h);
    }

    private void reset() {
      resetCalled = true;
      headers.clear();
      body.getBuffer().setLength(0);
      bytes.reset();
      contentType = null;
      characterEncoding = null;
    }

    private void setContentType(String value) {
      String type = mediaType(value);
      String charset = charset(value);
      if (charset != null) {
        characterEncoding = charset;
      }
      contentType = withCharset(type);
    }

    private void setCharacterEncoding(String value) {
      characterEncoding = value;
      if (contentType != null) {
        contentType = withCharset(mediaType(contentType));
      }
    }

    private String withCharset(String type) {
      return characterEncoding != null ? type + ";charset=" + characterEncoding : type;
    }

    private static String mediaType(String value) {
      int semicolon = value.indexOf(';');
      return semicolon >= 0 ? value.substring(0, semicolon) : value;
    }

    private static String charset(String value) {
      String[] parts = value.split(";");
      for (int i = 1; i < parts.length; i++) {
        String part = parts[i].trim();
        int equals = part.indexOf('=');
        if (equals > 0 && "charset".equalsIgnoreCase(part.substring(0, equals).trim())) {
          return part.substring(equals + 1).trim();
        }
      }
      return null;
    }
  }

  static Object defaultValue(Class<?> type) {
    if (!type.isPrimitive()) return null;
    if (boolean.class.equals(type)) return false;
    if (byte.class.equals(type))    return (byte) 0;
    if (short.class.equals(type))   return (short) 0;
    if (int.class.equals(type))     return 0;
    if (long.class.equals(type))    return 0L;
    if (float.class.equals(type))   return 0f;
    if (double.class.equals(type))  return 0d;
    if (char.class.equals(type))    return (char) 0;
    throw new IllegalStateException("Unsupported primitive type: " + type.getName());
  }
}
