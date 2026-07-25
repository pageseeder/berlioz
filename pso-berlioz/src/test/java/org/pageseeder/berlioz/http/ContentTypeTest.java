package org.pageseeder.berlioz.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ContentTypeTest {

  @Test
  void testParse_mediaTypeWithoutParameters() {
    ContentType contentType = ContentType.parse("text/html");

    assertEquals("text/html", contentType.mediaType());
    assertNull(contentType.parameter("charset"));
    assertNull(contentType.charset());
  }

  @Test
  void testParse_trimsOptionalWhitespace() {
    ContentType contentType = ContentType.parse(" \tApplication/JSON \t");

    assertEquals("Application/JSON", contentType.mediaType());
    assertTrue(contentType.is("application/json"));
    assertFalse(contentType.is("application/xml"));
  }

  @Test
  void testParameter_namesAreCaseInsensitiveAndAllowWhitespace() {
    ContentType contentType = ContentType.parse("text/plain; Charset = UTF-8");

    assertEquals("UTF-8", contentType.parameter("charset"));
    assertEquals("UTF-8", contentType.parameter("CHARSET"));
    assertEquals(StandardCharsets.UTF_8, contentType.charset());
  }

  @Test
  void testParameter_quotedValueIsUnquoted() {
    ContentType contentType = ContentType.parse("text/plain;charset=\"UTF-8\"");

    assertEquals("UTF-8", contentType.parameter("charset"));
    assertEquals(StandardCharsets.UTF_8, contentType.charset());
  }

  @Test
  void testParameter_quotedSemicolonAndEscapesArePreserved() {
    ContentType contentType = ContentType.parse(
        "text/plain;note=\"semi; quote=\\\" and slash=\\\\\"");

    assertEquals("semi; quote=\" and slash=\\", contentType.parameter("note"));
  }

  @Test
  void testParameter_charsetTextInAnotherParameterIsIgnored() {
    ContentType contentType = ContentType.parse(
        "text/html;note=\"charset=ISO-8859-1\";x-charset=UTF-16");

    assertNull(contentType.parameter("charset"));
    assertNull(contentType.charset());
  }

  @Test
  void testCharset_aliasIsResolved() {
    ContentType contentType = ContentType.parse("text/plain;charset=utf8");

    assertEquals(StandardCharsets.UTF_8, contentType.charset());
  }

  @Test
  void testCharset_unsupportedNameThrowsWhenAccessed() {
    ContentType contentType = ContentType.parse("text/plain;charset=not-a-real-charset");

    assertEquals("text/plain", contentType.mediaType());
    assertThrows(UnsupportedCharsetException.class, contentType::charset);
  }

  @ParameterizedTest
  @MethodSource("malformedValues")
  void testParse_malformedValueThrows(String value) {
    assertThrows(IllegalArgumentException.class, () -> ContentType.parse(value));
  }

  private static Stream<String> malformedValues() {
    return Stream.of(
        "",
        "text",
        "/plain",
        "text/",
        "text /plain",
        "text/plain;charset",
        "text/plain;charset=",
        "text/plain;charset=\"UTF-8",
        "text/plain;charset=\"UTF-8\"junk",
        "text/plain;charset=UTF-8;CHARSET=UTF-8"
    );
  }
}
