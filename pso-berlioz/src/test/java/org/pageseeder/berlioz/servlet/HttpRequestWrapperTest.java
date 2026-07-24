package org.pageseeder.berlioz.servlet;

import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.furi.URIPattern;
import org.pageseeder.berlioz.furi.URIResolveResult;
import org.pageseeder.berlioz.furi.URIResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HttpRequestWrapperTest {

  // getBerliozPath() — prefix mapping (pathInfo present)

  @Test
  void testGetBerliozPath_prefixMapping_returnsPathInfo() {
    HttpServletRequest req = ServletTestSupport.request()
        .servletPath("/html")
        .pathInfo("/home/index")
        .build();
    assertEquals("/home/index", HttpRequestWrapper.getBerliozPath(req));
  }

  @Test
  void testGetBerliozPath_prefixMapping_rootPathInfo() {
    HttpServletRequest req = ServletTestSupport.request()
        .servletPath("/api")
        .pathInfo("/")
        .build();
    assertEquals("/", HttpRequestWrapper.getBerliozPath(req));
  }

  // getBerliozPath() — suffix mapping (no pathInfo, strip extension)

  @Test
  void testGetBerliozPath_suffixMapping_stripsExtension() {
    HttpServletRequest req = ServletTestSupport.request()
        .servletPath("/home/index.html")
        .pathInfo(null)
        .build();
    assertEquals("/home/index", HttpRequestWrapper.getBerliozPath(req));
  }

  @Test
  void testGetBerliozPath_suffixMapping_noExtension() {
    HttpServletRequest req = ServletTestSupport.request()
        .servletPath("/home/index")
        .pathInfo(null)
        .build();
    assertEquals("/home/index", HttpRequestWrapper.getBerliozPath(req));
  }

  @Test
  void testGetBerliozPath_suffixMapping_xmlExtension() {
    HttpServletRequest req = ServletTestSupport.request()
        .servletPath("/service/data.xml")
        .pathInfo(null)
        .build();
    assertEquals("/service/data", HttpRequestWrapper.getBerliozPath(req));
  }

  @Test
  void testGetBerliozPath_suffixMapping_jsonExtension() {
    HttpServletRequest req = ServletTestSupport.request()
        .servletPath("/api/users.json")
        .pathInfo(null)
        .build();
    assertEquals("/api/users", HttpRequestWrapper.getBerliozPath(req));
  }

  @Test
  void testToParameters_nativeRepeatedBodyParameter_firstValueWins() {
    HttpServletRequest base = ServletTestSupport.request()
        .method("QUERY")
        .contentType("application/x-www-form-urlencoded")
        .body("q=raw-body-should-be-ignored")
        .build();
    HttpServletRequest req = new HttpServletRequestWrapper(base) {
      @Override
      public Map<String, String[]> getParameterMap() {
        return Map.of("q", new String[]{"first", "last"});
      }
    };
    URIPattern pattern = new URIPattern("/search");
    URIResolveResult result = new URIResolver("/search").resolve(pattern);

    assertEquals("first", HttpRequestWrapper.toParameters(req, result).get("q"));
  }
}
