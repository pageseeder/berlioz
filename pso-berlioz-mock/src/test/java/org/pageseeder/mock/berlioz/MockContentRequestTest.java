package org.pageseeder.mock.berlioz;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.content.Environment;
import org.pageseeder.berlioz.servlet.HttpEnvironment;
import org.pageseeder.mock.servlet.MockHttpServletRequest;

import java.io.File;
import java.net.URI;

class MockContentRequestTest {

  @Test
  void berliozPathUsesServletPathWithoutExtension() {
    MockHttpServletRequest http = new MockHttpServletRequest(URI.create("http://localhost:8080/articles/one.html"), "GET");
    MockContentRequest request = new MockContentRequest(http);

    Assertions.assertEquals("/articles/one", request.getBerliozPath());
  }

  @Test
  void berliozPathPrefersPathInfo() {
    MockHttpServletRequest http = new MockHttpServletRequest();
    http.setServletPath("/html");
    http.setPathInfo("/articles/one");
    MockContentRequest request = new MockContentRequest(http);

    Assertions.assertEquals("/articles/one", request.getBerliozPath());
  }

  @Test
  void environmentCanBeOverridden() {
    MockContentRequest request = new MockContentRequest();
    Environment environment = new HttpEnvironment(new File("public"), new File("private"), "no-cache");

    request.setEnvironment(environment);

    Assertions.assertSame(environment, request.getEnvironment());
  }

  @Test
  void redirectStatusAndUrlAreCaptured() {
    MockContentRequest request = new MockContentRequest();

    request.setRedirect("/elsewhere", ContentStatus.FOUND);

    Assertions.assertEquals(ContentStatus.FOUND, request.getStatus());
    Assertions.assertEquals("/elsewhere", request.getRedirectURL());
  }
}
