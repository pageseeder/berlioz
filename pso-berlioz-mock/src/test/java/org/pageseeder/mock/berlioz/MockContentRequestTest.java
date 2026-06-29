package org.pageseeder.mock.berlioz;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.content.Environment;
import org.pageseeder.berlioz.content.TypedParameter;
import org.pageseeder.berlioz.error.InvalidParameterException;
import org.pageseeder.berlioz.servlet.HttpEnvironment;
import org.pageseeder.mock.servlet.MockHttpServletRequest;

import java.io.File;
import java.net.URI;
import java.time.LocalDate;
import java.time.Month;
import java.util.Collection;
import java.util.List;

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

  // --- parameterNames() -----------------------------------------------------

  @Test
  void parameterNames_emptyWhenNoParameters() {
    MockContentRequest request = new MockContentRequest();

    Collection<String> names = request.parameterNames();

    Assertions.assertTrue(names.isEmpty());
  }

  @Test
  void parameterNames_containsSetParameters() {
    MockContentRequest request = new MockContentRequest();
    request.setParameter("page", "2");
    request.setParameter("sort", "date");

    Collection<String> names = request.parameterNames();

    Assertions.assertTrue(names.contains("page"));
    Assertions.assertTrue(names.contains("sort"));
    Assertions.assertEquals(2, names.size());
  }

  // --- parameterValues() ----------------------------------------------------

  @Test
  void parameterValues_emptyListWhenAbsent() {
    MockContentRequest request = new MockContentRequest();

    List<String> values = request.parameterValues("missing");

    Assertions.assertTrue(values.isEmpty());
  }

  @Test
  void parameterValues_singleValueWhenSet() {
    MockContentRequest request = new MockContentRequest();
    request.setParameter("q", "hello");

    List<String> values = request.parameterValues("q");

    Assertions.assertEquals(List.of("hello"), values);
  }

  // --- cookies() ------------------------------------------------------------

  @Test
  void cookies_emptyListWhenNoCookies() {
    MockContentRequest request = new MockContentRequest();

    List<?> cookies = request.cookies();

    Assertions.assertTrue(cookies.isEmpty());
  }

  // --- parameter() fluent API -----------------------------------------------

  @Test
  void parameter_asInt_defaultValue() {
    MockContentRequest request = new MockContentRequest();
    request.setParameter("page", "3");

    int page = request.parameter("page").asInt().defaultValue(1);

    Assertions.assertEquals(3, page);
  }

  @Test
  void parameter_asInt_absent_usesDefault() {
    MockContentRequest request = new MockContentRequest();

    int page = request.parameter("page").asInt().defaultValue(1);

    Assertions.assertEquals(1, page);
  }

  @Test
  void parameter_asLocalDate_required() {
    MockContentRequest request = new MockContentRequest();
    request.setParameter("from", "2024-06-01");

    LocalDate from = request.parameter("from").asLocalDate().required();

    Assertions.assertEquals(LocalDate.of(2024, Month.JUNE, 1), from);
  }

  @Test
  void parameter_required_absent_throws() {
    MockContentRequest request = new MockContentRequest();
    TypedParameter<?> p = request.parameter("from").asLocalDate();
    Assertions.assertThrows(InvalidParameterException.class, p::required);
  }

  @Test
  void parameter_oneOf_validValue() {
    MockContentRequest request = new MockContentRequest();
    request.setParameter("sort", "date");

    String sort = request.parameter("sort").oneOf("name", "date", "title").defaultValue("name");

    Assertions.assertEquals("date", sort);
  }

  @Test
  void parameter_oneOf_invalidValue_throws() {
    MockContentRequest request = new MockContentRequest();
    request.setParameter("sort", "score");
    TypedParameter<?> p = request.parameter("sort").oneOf("name", "date", "title");
    Assertions.assertThrows(InvalidParameterException.class, p::required);
  }

}
