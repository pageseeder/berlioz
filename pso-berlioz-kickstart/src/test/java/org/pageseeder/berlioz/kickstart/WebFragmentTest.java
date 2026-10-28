package org.pageseeder.berlioz.kickstart;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the security-relevant settings of the kickstart web fragment.
 */
class WebFragmentTest {

  @Test
  void sourceServletRequiresControl() throws Exception {
    Map<String, String> params = initParams("BerliozSource");
    assertEquals("true", params.get("requires-control"));
    assertEquals("IDENTITY", params.get("stylesheet"));
    assertTrue(params.get("cache-control").startsWith("private"), params.get("cache-control"));
  }

  @Test
  void otherServletsDoNotRequireControl() throws Exception {
    for (String name : new String[]{"BerliozHTML", "BerliozXML", "BerliozJSON"}) {
      assertNull(initParams(name).get("requires-control"), name);
    }
  }

  private static Map<String, String> initParams(String servletName) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    Document fragment;
    try (InputStream in = Objects.requireNonNull(
        WebFragmentTest.class.getResourceAsStream("/META-INF/web-fragment.xml"))) {
      fragment = factory.newDocumentBuilder().parse(in);
    }
    NodeList servlets = fragment.getElementsByTagNameNS("*", "servlet");
    for (int i = 0; i < servlets.getLength(); i++) {
      Element servlet = (Element) servlets.item(i);
      if (servletName.equals(text(servlet, "servlet-name"))) {
        Map<String, String> params = new HashMap<>();
        NodeList initParams = servlet.getElementsByTagNameNS("*", "init-param");
        for (int j = 0; j < initParams.getLength(); j++) {
          Element param = (Element) initParams.item(j);
          params.put(text(param, "param-name"), text(param, "param-value"));
        }
        return params;
      }
    }
    throw new AssertionError("Servlet not found: " + servletName);
  }

  private static String text(Element parent, String name) {
    return parent.getElementsByTagNameNS("*", name).item(0).getTextContent().trim();
  }
}
