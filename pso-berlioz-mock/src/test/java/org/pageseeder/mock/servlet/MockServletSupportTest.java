package org.pageseeder.mock.servlet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;

public class MockServletSupportTest {

  @Test
  public void servletConfigReturnsInitParameterNames() {
    MockServletConfig config = new MockServletConfig();
    config.setInitParameter("config", "WEB-INF/config.xml");

    Assertions.assertEquals(config.getInitParameter("config"), "WEB-INF/config.xml");
    Assertions.assertEquals(Collections.list(config.getInitParameterNames()).get(0), "config");
  }

  @Test
  public void servletOutputStreamStoresBytesAndIsReady() throws Exception {
    MockServletOutputstream stream = new MockServletOutputstream();

    stream.write('A');

    Assertions.assertTrue(stream.isReady());
    Assertions.assertArrayEquals(new byte[]{'A'}, stream.toByteArray());
  }

  @Test
  public void sessionInvalidationClearsAttributes() {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute("name", "value");

    session.invalidate();

    try {
      session.getAttribute("name");
      Assertions.fail("Invalidated session should reject access");
    } catch (IllegalStateException ex) {
      Assertions.assertEquals(ex.getMessage(), "HttpSession is no longer valid");
    }
  }
}
