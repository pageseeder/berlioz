package org.pageseeder.berlioz.servlet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.content.*;
import org.pageseeder.berlioz.http.HttpMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class HttpContentRequestTest {

  private static final File WEB_INF = new File("./src/test/resources/org/pageseeder/berlioz");

  @TempDir
  static Path tmp;

  private HttpContentRequest request;
  private BerliozGenerator generator;
  private Service service;

  @BeforeAll
  static void setupServices() throws BerliozException {
    GlobalSettings.setup(WEB_INF);
    ServiceLoader.getInstance().load(new File(WEB_INF, "config/services.xml"));
  }

  @BeforeEach
  void setup() {
    MatchingService match = ServiceLoader.getInstance().getDefaultRegistry()
        .get("/home", HttpMethod.GET);
    assertNotNull(match, "Expected 'home' service to be loaded");
    service = match.service();
    generator = service.generators().get(0);

    File pub  = tmp.resolve("public").toFile();
    File priv = tmp.resolve("private").toFile();
    pub.mkdirs();
    priv.mkdirs();
    HttpEnvironment env = new HttpEnvironment(pub, priv, "no-cache");

    HttpServletRequest req = ServletTestSupport.request()
        .scheme("http").host("localhost").port(80)
        .uri("/home.html").contextPath("").servletPath("/home.html").pathInfo(null)
        .build();
    HttpServletResponse res = ServletTestSupport.response().build();

    CoreHttpRequest core = new CoreHttpRequest(req, res, env);
    request = new HttpContentRequest(core, Collections.emptyMap(), generator, service, 0);
  }

  // Initial state

  @Test
  void testInitialStatus_isOK() {
    assertEquals(ContentStatus.OK, request.getStatus());
  }

  @Test
  void testInitialRedirectURL_isNull() {
    assertNull(request.getRedirectURL());
  }

  @Test
  void testGenerator_returnsConstructorArg() {
    assertSame(generator, request.generator());
  }

  @Test
  void testGetService_returnsConstructorArg() {
    assertSame(service, request.getService());
  }

  @Test
  void testOrder_returnsZero() {
    assertEquals(0, request.order());
  }

  // setStatus()

  @Test
  void testSetStatus_notFound() {
    request.setStatus(ContentStatus.NOT_FOUND);
    assertEquals(ContentStatus.NOT_FOUND, request.getStatus());
  }

  @Test
  void testSetStatus_null_throws() {
    assertThrows(NullPointerException.class, () -> request.setStatus(null));
  }

  @Test
  void testSetStatus_redirectStatus_throws() {
    assertThrows(IllegalArgumentException.class,
        () -> request.setStatus(ContentStatus.MOVED_PERMANENTLY));
  }

  @Test
  void testSetStatus_temporaryRedirect_throws() {
    assertThrows(IllegalArgumentException.class,
        () -> request.setStatus(ContentStatus.TEMPORARY_REDIRECT));
  }

  // setRedirect()

  @Test
  void testSetRedirect_nullStatus_defaultsToTemporaryRedirect() {
    request.setRedirect("/new-url", null);
    assertEquals(ContentStatus.TEMPORARY_REDIRECT, request.getStatus());
    assertEquals("/new-url", request.getRedirectURL());
  }

  @Test
  void testSetRedirect_explicitMovedPermanently() {
    request.setRedirect("/new-url", ContentStatus.MOVED_PERMANENTLY);
    assertEquals(ContentStatus.MOVED_PERMANENTLY, request.getStatus());
    assertEquals("/new-url", request.getRedirectURL());
  }

  @Test
  void testSetRedirect_nullUrl_throws() {
    assertThrows(NullPointerException.class,
        () -> request.setRedirect(null, null));
  }

  @Test
  void testSetRedirect_nonRedirectStatus_throws() {
    assertThrows(IllegalArgumentException.class,
        () -> request.setRedirect("/new", ContentStatus.NOT_FOUND));
  }

  // profiling fields

  @Test
  void testProfileEtag_defaultZero() {
    assertEquals(0L, request.getProfileEtag());
  }

  @Test
  void testSetProfileEtag() {
    request.setProfileEtag(12345L);
    assertEquals(12345L, request.getProfileEtag());
  }

  // getBerliozPath() via ContentRequest interface

  @Test
  void testGetBerliozPath_fromServletPath() {
    assertEquals("/home", request.getBerliozPath());
  }
}
