package org.pageseeder.berlioz.http;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private String method = "GET";
    private String serverName = "example.org";
    private int serverPort = 80;

    RequestBuilder method(String value) {
      this.method = value;
      return this;
    }

    RequestBuilder server(String name, int port) {
      this.serverName = name;
      this.serverPort = port;
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
      InvocationHandler handler = (proxy, method, args) -> {
        String name = method.getName();
        if ("getHeader".equals(name)) return this.headers.get(args[0]);
        if ("getDateHeader".equals(name)) return this.dateHeaders.getOrDefault(args[0], -1L);
        if ("getMethod".equals(name)) return this.method;
        if ("getServerName".equals(name)) return this.serverName;
        if ("getServerPort".equals(name)) return this.serverPort;
        if ("toString".equals(name)) return "RequestBuilder";
        if ("hashCode".equals(name)) return System.identityHashCode(proxy);
        if ("equals".equals(name)) return proxy == args[0];
        return defaultValue(method.getReturnType());
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
