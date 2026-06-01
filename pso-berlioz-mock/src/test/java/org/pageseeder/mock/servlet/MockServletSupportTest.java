package org.pageseeder.mock.servlet;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class MockServletSupportTest {

  @Test
  public void servletConfigReturnsInitParameterNames() {
    MockServletConfig config = new MockServletConfig();
    config.setInitParameter("config", "WEB-INF/config.xml");

    Assert.assertEquals("WEB-INF/config.xml", config.getInitParameter("config"));
    Assert.assertEquals("config", Collections.list(config.getInitParameterNames()).get(0));
  }

  @Test
  public void servletOutputStreamStoresBytesAndIsReady() throws Exception {
    MockServletOutputstream stream = new MockServletOutputstream();

    stream.write('A');

    Assert.assertTrue(stream.isReady());
    Assert.assertArrayEquals(new byte[]{'A'}, stream.toByteArray());
  }

  @Test
  public void sessionInvalidationClearsAttributes() {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute("name", "value");

    session.invalidate();

    try {
      session.getAttribute("name");
      Assert.fail("Invalidated session should reject access");
    } catch (IllegalStateException ex) {
      Assert.assertEquals("HttpSession is no longer valid", ex.getMessage());
    }
  }
}
