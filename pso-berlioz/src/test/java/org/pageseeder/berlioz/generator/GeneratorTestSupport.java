package org.pageseeder.berlioz.generator;

import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.content.Environment;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

final class GeneratorTestSupport {

  private GeneratorTestSupport() {}

  static RequestBuilder request() {
    return new RequestBuilder();
  }

  static final class RequestBuilder {

    final Map<String, Object> attributes = new LinkedHashMap<>();
    final Map<String, String[]> parameters = new LinkedHashMap<>();
    Environment environment = null;
    ContentStatus capturedStatus = null;

    RequestBuilder attribute(String name, Object value) {
      this.attributes.put(name, value);
      return this;
    }

    RequestBuilder parameter(String name, String value) {
      this.parameters.put(name, new String[]{value});
      return this;
    }

    RequestBuilder multiParameter(String name, String... values) {
      this.parameters.put(name, values);
      return this;
    }

    RequestBuilder environment(Environment env) {
      this.environment = env;
      return this;
    }

    ContentRequest build() {
      RequestBuilder self = this;
      return (ContentRequest) Proxy.newProxyInstance(
          ContentRequest.class.getClassLoader(),
          new Class<?>[]{ContentRequest.class},
          (proxy, m, args) -> {
            switch (m.getName()) {
              case "getAttribute":       return self.attributes.get(args[0]);
              case "setAttribute":       self.attributes.put((String) args[0], args[1]); return null;
              case "getParameterNames":  return Collections.enumeration(self.parameters.keySet());
              case "getParameterValues": return self.parameters.get(args[0]);
              case "getParameter": {
                String[] vals = self.parameters.get(args[0]);
                String first = (vals != null && vals.length > 0 && !vals[0].isEmpty()) ? vals[0] : null;
                if (args.length == 1) return first;
                return first != null ? first : args[1];  // getParameter(name, def)
              }
              case "getIntParameter": {
                String[] vals = self.parameters.get(args[0]);
                String v = (vals != null && vals.length > 0) ? vals[0] : null;
                if (v == null || v.isEmpty()) return args[1];
                try { return Integer.parseInt(v); } catch (NumberFormatException e) { return args[1]; }
              }
              case "getLongParameter": {
                String[] vals = self.parameters.get(args[0]);
                String v = (vals != null && vals.length > 0) ? vals[0] : null;
                if (v == null || v.isEmpty()) return args[1];
                try { return Long.parseLong(v); } catch (NumberFormatException e) { return args[1]; }
              }
              case "getEnvironment":  return self.environment;
              case "setStatus":       self.capturedStatus = (ContentStatus) args[0]; return null;
              case "setRedirect":     return null;
              case "getLocation":     return null;
              case "getBerliozPath":  return null;
              case "getCookies":      return null;
              case "getSession":      return null;
              case "getDateParameter": return null;
              case "toString":        return "ContentRequestStub";
              case "hashCode":        return System.identityHashCode(proxy);
              case "equals":          return proxy == args[0];
              default:                return null;
            }
          });
    }
  }
}
