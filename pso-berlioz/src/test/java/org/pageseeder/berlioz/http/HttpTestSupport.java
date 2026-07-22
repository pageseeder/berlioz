package org.pageseeder.berlioz.http;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


final class HttpTestSupport {

  private HttpTestSupport() {
  }

  static RequestBuilder request() {
    return new RequestBuilder();
  }

  static ResponseRecorder response() {
    return new ResponseRecorder();
  }

  static final class RequestBuilder {

    private final Map<String, String> headers = new LinkedHashMap<>();
    private final Map<String, Long> dateHeaders = new LinkedHashMap<>();
    private final Map<String, String[]> parameterMap = new LinkedHashMap<>();
    private String method = "GET";
    private String serverName = "example.org";
    private int serverPort = 80;
    private String scheme = null;
    private String requestURI = "/";
    private String queryString = null;
    private String contentType = null;
    private byte[] body = new byte[0];

    RequestBuilder method(String value) {
      this.method = value;
      return this;
    }

    RequestBuilder uri(String value) {
      this.requestURI = value;
      return this;
    }

    RequestBuilder queryString(String value) {
      this.queryString = value;
      return this;
    }

    RequestBuilder parameter(String name, String value) {
      this.parameterMap.put(name, new String[]{value});
      return this;
    }

    RequestBuilder contentType(String value) {
      this.contentType = value;
      return this;
    }

    RequestBuilder body(String value) {
      this.body = value.getBytes(StandardCharsets.UTF_8);
      return this;
    }

    RequestBuilder server(String name, int port) {
      this.serverName = name;
      this.serverPort = port;
      return this;
    }

    RequestBuilder scheme(String value) {
      this.scheme = value;
      return this;
    }

    RequestBuilder header(String name, String value) {
      this.headers.put(name, value);
      return this;
    }

    RequestBuilder dateHeader(String name, long value) {
      this.dateHeaders.put(name, value);
      return this;
    }

    HttpServletRequest build() {
      InvocationHandler handler = (proxy, m, args) -> {
        String name = m.getName();
        if ("getHeader".equals(name)) return this.headers.get(args[0]);
        if ("getDateHeader".equals(name)) return this.dateHeaders.getOrDefault(args[0], -1L);
        if ("getMethod".equals(name)) return this.method;
        if ("getScheme".equals(name)) return this.scheme;
        if ("getServerName".equals(name)) return this.serverName;
        if ("getServerPort".equals(name)) return this.serverPort;
        if ("getRequestURI".equals(name)) return this.requestURI;
        if ("getQueryString".equals(name)) return this.queryString;
        if ("getParameterMap".equals(name)) return this.parameterMap;
        if ("getContentType".equals(name)) return this.contentType;
        if ("getContentLength".equals(name)) return this.body.length;
        if ("getInputStream".equals(name)) return new ServletInputStream() {
          private final ByteArrayInputStream in = new ByteArrayInputStream(body);
          @Override public boolean isFinished() { return this.in.available() == 0; }
          @Override public boolean isReady() { return true; }
          @Override public void setReadListener(ReadListener readListener) { throw new UnsupportedOperationException(); }
          @Override public int read() { return this.in.read(); }
        };
        if ("toString".equals(name)) return "RequestBuilder";
        if ("hashCode".equals(name)) return System.identityHashCode(proxy);
        if ("equals".equals(name)) return proxy == args[0];
        return defaultValue(m.getReturnType());
      };
      return (HttpServletRequest) Proxy.newProxyInstance(
          HttpServletRequest.class.getClassLoader(),
          new Class<?>[]{HttpServletRequest.class},
          handler);
    }
  }

  static final class ResponseRecorder {

    private final Map<String, List<String>> headers = new LinkedHashMap<>();
    private int status = HttpServletResponse.SC_OK;
    private int contentLength = -1;
    private boolean errorSent = false;

    HttpServletResponse build() {
      InvocationHandler handler = (proxy, method, args) -> {
        String name = method.getName();
        if ("setStatus".equals(name)) {
          this.status = (Integer) args[0];
          return null;
        }
        if ("sendError".equals(name)) {
          this.status = (Integer) args[0];
          this.errorSent = true;
          return null;
        }
        if ("setHeader".equals(name)) {
          List<String> values = new ArrayList<>(1);
          values.add((String) args[1]);
          this.headers.put((String) args[0], values);
          return null;
        }
        if ("addHeader".equals(name)) {
          this.headers.computeIfAbsent((String) args[0], key -> new ArrayList<>()).add((String) args[1]);
          return null;
        }
        if ("setContentLength".equals(name)) {
          this.contentLength = (Integer) args[0];
          return null;
        }
        if ("getStatus".equals(name)) return this.status;
        if ("getHeader".equals(name)) return header((String) args[0]);
        if ("getHeaders".equals(name)) return headers((String) args[0]);
        if ("toString".equals(name)) return "ResponseRecorder";
        if ("hashCode".equals(name)) return System.identityHashCode(proxy);
        if ("equals".equals(name)) return proxy == args[0];
        return defaultValue(method.getReturnType());
      };
      return (HttpServletResponse) Proxy.newProxyInstance(
          HttpServletResponse.class.getClassLoader(),
          new Class<?>[]{HttpServletResponse.class},
          handler);
    }

    int status() {
      return this.status;
    }

    int contentLength() {
      return this.contentLength;
    }

    boolean errorSent() {
      return this.errorSent;
    }

    String header(String name) {
      List<String> values = this.headers.get(name);
      return values == null || values.isEmpty() ? null : values.get(0);
    }

    Collection<String> headers(String name) {
      List<String> values = this.headers.get(name);
      return values == null ? new ArrayList<>() : values;
    }
  }

  private static Object defaultValue(Class<?> type) {
    if (!type.isPrimitive()) return null;
    if (boolean.class.equals(type)) return false;
    if (byte.class.equals(type)) return (byte) 0;
    if (short.class.equals(type)) return (short) 0;
    if (int.class.equals(type)) return 0;
    if (long.class.equals(type)) return 0L;
    if (float.class.equals(type)) return 0f;
    if (double.class.equals(type)) return 0d;
    if (char.class.equals(type)) return (char) 0;
    throw new IllegalStateException("Unsupported primitive type: " + type.getName());
  }

}
