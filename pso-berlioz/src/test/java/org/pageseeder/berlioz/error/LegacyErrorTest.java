package org.pageseeder.berlioz.error;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.BerliozErrorID;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.servlet.ErrorHandlerServlet;
import org.pageseeder.berlioz.xml.XmlStringBuilder;

@SuppressWarnings("deprecation") // LegacyError is removed in 1.0
class LegacyErrorTest {

  @Test
  void of_usesBerliozErrorExceptionWhenStandardAttributeIsAbsent() {
    // Berlioz sent the error to the container (errors.handle=false): only its own attribute is set
    BerliozException ex = new BerliozException("Not found", BerliozErrorID.SERVICES_NOT_FOUND);
    HttpServletRequest req = request(Map.of(
        RequestDispatcher.ERROR_STATUS_CODE, 404,
        ErrorHandlerServlet.BERLIOZ_ERROR_EXCEPTION, ex));

    XmlStringBuilder xml = new XmlStringBuilder();
    LegacyError.of(req).toXml(xml);

    String id = BerliozErrorID.SERVICES_NOT_FOUND.id();
    assertTrue(xml.toString().contains("id=\"" + id + "\""), xml.toString());
  }

  private static HttpServletRequest request(Map<String, Object> attributes) {
    return (HttpServletRequest) Proxy.newProxyInstance(
        HttpServletRequest.class.getClassLoader(),
        new Class<?>[]{HttpServletRequest.class},
        (proxy, m, args) -> {
          if ("getAttribute".equals(m.getName())) return attributes.get(args[0]);
          if ("getRequestURI".equals(m.getName())) return "/missing.html";
          return null;
        });
  }
}
