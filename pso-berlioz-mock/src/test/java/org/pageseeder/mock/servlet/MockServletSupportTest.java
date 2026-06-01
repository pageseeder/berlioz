package org.pageseeder.mock.servlet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;

class MockServletSupportTest {

  @Test
  void servletConfigReturnsInitParameterNames() {
    MockServletConfig config = new MockServletConfig();
    config.setInitParameter("config", "WEB-INF/config.xml");

    Assertions.assertEquals("WEB-INF/config.xml", config.getInitParameter("config"));
    Assertions.assertEquals("config", Collections.list(config.getInitParameterNames()).get(0));
  }

  @Test
  void servletOutputStreamStoresBytesAndIsReady() throws Exception {
    MockServletOutputstream stream = new MockServletOutputstream();

    stream.write('A');

    Assertions.assertTrue(stream.isReady());
    Assertions.assertArrayEquals(new byte[]{'A'}, stream.toByteArray());
  }

  @Test
  void sessionInvalidationClearsAttributes() {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute("name", "value");

    session.invalidate();

    try {
      session.getAttribute("name");
      Assertions.fail("Invalidated session should reject access");
    } catch (IllegalStateException ex) {
      Assertions.assertEquals("HttpSession is no longer valid", ex.getMessage());
    }
  }
}
