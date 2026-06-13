package org.pageseeder.berlioz.servlet;

import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.BerliozErrorID;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.content.BerliozGenerator;
import org.pageseeder.berlioz.content.InvalidParameterException;
import org.pageseeder.berlioz.content.UpstreamException;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.generator.NoContent;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GeneratorDispatchTest {

  // FRAMEWORK_HEADERS ----------------------------------------------------------------------------

  @Test
  void frameworkHeaders_containsKnownHeaders() {
    assertTrue(GeneratorDispatch.FRAMEWORK_HEADERS.contains("Location"));
    assertTrue(GeneratorDispatch.FRAMEWORK_HEADERS.contains("ETag"));
    assertTrue(GeneratorDispatch.FRAMEWORK_HEADERS.contains("Cache-Control"));
    assertTrue(GeneratorDispatch.FRAMEWORK_HEADERS.contains("Set-Cookie"));
    assertTrue(GeneratorDispatch.FRAMEWORK_HEADERS.contains("Date"));
  }

  @Test
  void frameworkHeaders_isCaseInsensitive() {
    assertTrue(GeneratorDispatch.FRAMEWORK_HEADERS.contains("location"));
    assertTrue(GeneratorDispatch.FRAMEWORK_HEADERS.contains("ETAG"));
    assertTrue(GeneratorDispatch.FRAMEWORK_HEADERS.contains("cache-control"));
  }

  @Test
  void frameworkHeaders_doesNotContainCustomHeader() {
    assertFalse(GeneratorDispatch.FRAMEWORK_HEADERS.contains("X-Custom-Header"));
    assertFalse(GeneratorDispatch.FRAMEWORK_HEADERS.contains("Content-Type"));
  }

  // toBerliozException ---------------------------------------------------------------------------

  @Test
  void toBerliozException_berliozExceptionPassThrough() {
    BerliozException original = new BerliozException("test", BerliozErrorID.GENERATOR_ERROR_UNFORCED);
    BerliozException result = GeneratorDispatch.toBerliozException(original);
    assertSame(original, result);
  }

  @Test
  void toBerliozException_berliozExceptionWithNullId_assignsDefault() {
    BerliozException original = new BerliozException("test", (BerliozErrorID) null);
    BerliozException result = GeneratorDispatch.toBerliozException(original);
    assertSame(original, result);
    assertEquals(BerliozErrorID.GENERATOR_ERROR_UNFORCED, result.id());
  }

  @Test
  void toBerliozException_invalidParameterException_wrapsWithCorrectId() {
    InvalidParameterException ipe = InvalidParameterException.required("myParam");
    BerliozException result = GeneratorDispatch.toBerliozException(ipe);
    assertNotNull(result);
    assertEquals(BerliozErrorID.INVALID_PARAMETER, result.id());
    assertTrue(result.getMessage().contains("myParam"));
    assertSame(ipe, result.getCause());
  }

  @Test
  void toBerliozException_upstreamException_wrapsWithUpstreamErrorId() {
    UpstreamException ue = new UpstreamException("connection refused");
    BerliozException result = GeneratorDispatch.toBerliozException(ue);
    assertNotNull(result);
    assertEquals(BerliozErrorID.UPSTREAM_ERROR, result.id());
    assertSame(ue, result.getCause());
    assertTrue(result.getMessage().contains("connection refused"));
  }

  @Test
  void toBerliozException_upstreamExceptionWithService_includesServiceName() {
    UpstreamException ue = new UpstreamException("timeout", "search-api");
    BerliozException result = GeneratorDispatch.toBerliozException(ue);
    assertNotNull(result);
    assertEquals(BerliozErrorID.UPSTREAM_ERROR, result.id());
    assertTrue(result.getMessage().contains("search-api"));
  }

  @Test
  void toBerliozException_genericException_wrapsWithUncheckedId() {
    RuntimeException ex = new RuntimeException("boom");
    BerliozException result = GeneratorDispatch.toBerliozException(ex);
    assertNotNull(result);
    assertEquals(BerliozErrorID.GENERATOR_ERROR_UNCHECKED, result.id());
    assertSame(ex, result.getCause());
  }

  // accumulateHeaders ----------------------------------------------------------------------------

  @Test
  void accumulateHeaders_nonFrameworkHeader_isAdded() {
    BerliozGenerator generator = new NoContent();
    Response response = Response.ok().header("X-Custom", "value");
    Map<String, String> target = new HashMap<>();

    GeneratorDispatch.accumulateHeaders(generator, response, target);

    assertEquals("value", target.get("X-Custom"));
  }

  @Test
  void accumulateHeaders_frameworkHeader_isDropped() {
    BerliozGenerator generator = new NoContent();
    Response response = Response.ok().header("Location", "/somewhere");
    Map<String, String> target = new HashMap<>();

    GeneratorDispatch.accumulateHeaders(generator, response, target);

    assertFalse(target.containsKey("Location"));
  }

  @Test
  void accumulateHeaders_frameworkHeaderCaseInsensitive_isDropped() {
    BerliozGenerator generator = new NoContent();
    Response response = Response.ok().header("cache-control", "no-cache");
    Map<String, String> target = new HashMap<>();

    GeneratorDispatch.accumulateHeaders(generator, response, target);

    assertFalse(target.containsKey("cache-control"));
  }

  @Test
  void accumulateHeaders_emptyResponse_targetUnchanged() {
    BerliozGenerator generator = new NoContent();
    Response response = Response.ok();
    Map<String, String> target = new HashMap<>();

    GeneratorDispatch.accumulateHeaders(generator, response, target);

    assertTrue(target.isEmpty());
  }

  @Test
  void accumulateHeaders_mixedHeaders_onlyCustomHeadersAdded() {
    BerliozGenerator generator = new NoContent();
    Response response = Response.ok()
        .header("X-App-Version", "1.0")
        .header("ETag", "abc123")
        .header("X-Request-Id", "xyz");
    Map<String, String> target = new HashMap<>();

    GeneratorDispatch.accumulateHeaders(generator, response, target);

    assertEquals("1.0", target.get("X-App-Version"));
    assertEquals("xyz", target.get("X-Request-Id"));
    assertFalse(target.containsKey("ETag"));
  }
}
