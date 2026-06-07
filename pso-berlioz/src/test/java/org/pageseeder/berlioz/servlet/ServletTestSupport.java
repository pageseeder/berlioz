package org.pageseeder.berlioz.servlet;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ServletTestSupport {

  private ServletTestSupport() {}

  static RequestBuilder request() {
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

  static final class RequestBuilder {
    private String method = "GET";
    private String scheme = "http";
    private String host = "localhost";
    private int port = 80;
    private String contextPath = "";
    private String servletPath = "";
    private String pathInfo = null;
    private String requestURI = "/";
    private String queryString = null;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private final Map<String, String> parameters = new LinkedHashMap<>();
    private final Map<String, Object> attributes = new LinkedHashMap<>();
    private RequestDispatcher dispatcher = null;

    RequestBuilder method(String v)      { this.method = v; return this; }
    RequestBuilder scheme(String v)      { this.scheme = v; return this; }
    RequestBuilder host(String v)        { this.host = v; return this; }
    RequestBuilder port(int v)           { this.port = v; return this; }
    RequestBuilder contextPath(String v) { this.contextPath = v; return this; }
    RequestBuilder servletPath(String v) { this.servletPath = v; return this; }
    RequestBuilder pathInfo(String v)    { this.pathInfo = v; return this; }
    RequestBuilder uri(String v)         { this.requestURI = v; return this; }
    RequestBuilder query(String v)       { this.queryString = v; return this; }
    RequestBuilder header(String n, String v)    { this.headers.put(n, v); return this; }
    RequestBuilder parameter(String n, String v) { this.parameters.put(n, v); return this; }
    RequestBuilder attribute(String n, Object v) { this.attributes.put(n, v); return this; }
    RequestBuilder dispatcher(RequestDispatcher d) { this.dispatcher = d; return this; }

    HttpServletRequest build() {
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
          case "getQueryString":  return queryString;
          case "getHeader":       return headers.get(args[0]);
          case "getHeaders": {
            String val = headers.get(args[0]);
            return Collections.enumeration(val != null ? List.of(val) : Collections.emptyList());
          }
          case "getHeaderNames":  return Collections.enumeration(headers.keySet());
          case "getParameter":    return parameters.get(args[0]);
          case "getParameterMap": {
            Map<String, String[]> map = new LinkedHashMap<>();
            parameters.forEach((k, v) -> map.put(k, new String[]{v}));
            return map;
          }
          case "getAttribute":    return attributes.get(args[0]);
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
  }

  static final class ResponseRecorder {
    int status = 200;
    boolean resetCalled = false;
    String contentType = null;
    final StringWriter body = new StringWriter();
    final PrintWriter writer = new PrintWriter(body);
    final Map<String, String> headers = new LinkedHashMap<>();

    String header(String name) { return headers.get(name); }
    String content() { return body.toString(); }

    HttpServletResponse build() {
      InvocationHandler h = (proxy, m, args) -> {
        switch (m.getName()) {
          case "setStatus":            status = (Integer) args[0]; return null;
          case "setHeader":
          case "addHeader":            headers.put((String) args[0], (String) args[1]); return null;
          case "reset":                resetCalled = true; headers.clear(); return null;
          case "setContentLength":
          case "setCharacterEncoding":
          case "setIntHeader":
          case "flushBuffer":          return null;
          case "setContentType":       contentType = (String) args[0]; return null;
          case "getStatus":            return status;
          case "getWriter":            return writer;
          case "getHeader":            return headers.get(args[0]);
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
