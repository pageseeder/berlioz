package org.pageseeder.mock.berlioz;

import org.junit.Assert;
import org.junit.Test;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.content.Environment;
import org.pageseeder.berlioz.servlet.HttpEnvironment;
import org.pageseeder.mock.servlet.MockHttpServletRequest;

import java.io.File;
import java.net.URI;

public class MockContentRequestTest {

  @Test
  public void berliozPathUsesServletPathWithoutExtension() {
    MockHttpServletRequest http = new MockHttpServletRequest(URI.create("http://localhost:8080/articles/one.html"), "GET");
    MockContentRequest request = new MockContentRequest(http);

    Assert.assertEquals("/articles/one", request.getBerliozPath());
  }

  @Test
  public void berliozPathPrefersPathInfo() {
    MockHttpServletRequest http = new MockHttpServletRequest();
    http.setServletPath("/html");
    http.setPathInfo("/articles/one");
    MockContentRequest request = new MockContentRequest(http);

    Assert.assertEquals("/articles/one", request.getBerliozPath());
  }

  @Test
  public void environmentCanBeOverridden() {
    MockContentRequest request = new MockContentRequest();
    Environment environment = new HttpEnvironment(new File("public"), new File("private"), "no-cache");

    request.setEnvironment(environment);

    Assert.assertSame(environment, request.getEnvironment());
  }

  @Test
  public void redirectStatusAndUrlAreCaptured() {
    MockContentRequest request = new MockContentRequest();

    request.setRedirect("/elsewhere", ContentStatus.FOUND);

    Assert.assertEquals(ContentStatus.FOUND, request.getStatus());
    Assert.assertEquals("/elsewhere", request.getRedirectURL());
  }
}
