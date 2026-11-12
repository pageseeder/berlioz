package org.pageseeder.berlioz.util;

import java.time.Duration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RedactionTest {

  // redact() — parameter/property names
  // ---------------------------------------------------------------------------

  @Test
  void testRedactSensitiveKeywords() {
    Assertions.assertEquals(Redaction.REDACTED, Redaction.redact("password", "secret123"));
    Assertions.assertEquals(Redaction.REDACTED, Redaction.redact("passwd", "secret123"));
    Assertions.assertEquals(Redaction.REDACTED, Redaction.redact("pwd", "secret123"));
    Assertions.assertEquals(Redaction.REDACTED, Redaction.redact("secret", "value"));
    Assertions.assertEquals(Redaction.REDACTED, Redaction.redact("apikey", "value"));
    Assertions.assertEquals(Redaction.REDACTED, Redaction.redact("token", "value"));
    Assertions.assertEquals(Redaction.REDACTED, Redaction.redact("credential", "value"));
    Assertions.assertEquals(Redaction.REDACTED, Redaction.redact("privatekey", "value"));
    Assertions.assertEquals(Redaction.REDACTED, Redaction.redact("session", "value"));
    Assertions.assertEquals(Redaction.REDACTED, Redaction.redact("auth", "value"));
    Assertions.assertEquals(Redaction.REDACTED, Redaction.redact("assertion", "value"));
    Assertions.assertEquals(Redaction.REDACTED, Redaction.redact("saml", "value"));
    Assertions.assertEquals(Redaction.REDACTED, Redaction.redact("jwt", "value"));
    Assertions.assertEquals(Redaction.REDACTED, Redaction.redact("csrf", "value"));
    Assertions.assertEquals(Redaction.REDACTED, Redaction.redact("key", "value"));
  }

  @Test
  void testRedactControlKey() {
    Assertions.assertEquals(Redaction.REDACTED, Redaction.redact("berlioz.control.key", "s3cr3t"));
  }

  @Test
  void testRedactNormalizesSeparators() {
    Assertions.assertEquals(Redaction.REDACTED, Redaction.redact("api-key", "value"));
    Assertions.assertEquals(Redaction.REDACTED, Redaction.redact("api_key", "value"));
    Assertions.assertEquals(Redaction.REDACTED, Redaction.redact("api.key", "value"));
    Assertions.assertEquals(Redaction.REDACTED, Redaction.redact("private-key", "value"));
    Assertions.assertEquals(Redaction.REDACTED, Redaction.redact("private_key", "value"));
    Assertions.assertEquals(Redaction.REDACTED, Redaction.redact("db.password", "value"));
    Assertions.assertEquals(Redaction.REDACTED, Redaction.redact("user_password", "value"));
  }

  @Test
  void testRedactCaseInsensitive() {
    Assertions.assertEquals(Redaction.REDACTED, Redaction.redact("PASSWORD", "value"));
    Assertions.assertEquals(Redaction.REDACTED, Redaction.redact("ApiKey", "value"));
    Assertions.assertEquals(Redaction.REDACTED, Redaction.redact("SECRET_KEY", "value"));
  }

  @Test
  void testRedactSafeNames() {
    Assertions.assertEquals("value", Redaction.redact("username", "value"));
    Assertions.assertEquals("value", Redaction.redact("email", "value"));
    Assertions.assertEquals("value", Redaction.redact("host", "value"));
    Assertions.assertEquals("value", Redaction.redact("port", "value"));
    Assertions.assertEquals("value", Redaction.redact("timeout", "value"));
  }

  // redactHeader() — HTTP header names
  // ---------------------------------------------------------------------------

  @Test
  void testRedactHeaderExactNames() {
    Assertions.assertEquals(Redaction.REDACTED, Redaction.redactHeader("authorization", "Bearer token"));
    Assertions.assertEquals(Redaction.REDACTED, Redaction.redactHeader("Authorization", "Bearer token"));
    Assertions.assertEquals(Redaction.REDACTED, Redaction.redactHeader("proxy-authorization", "value"));
    Assertions.assertEquals(Redaction.REDACTED, Redaction.redactHeader("cookie", "session=abc"));
    Assertions.assertEquals(Redaction.REDACTED, Redaction.redactHeader("Cookie", "session=abc"));
    Assertions.assertEquals(Redaction.REDACTED, Redaction.redactHeader("set-cookie", "id=xyz"));
  }

  @Test
  void testRedactHeaderKeywordMatching() {
    Assertions.assertEquals(Redaction.REDACTED, Redaction.redactHeader("x-auth-token", "value"));
    Assertions.assertEquals(Redaction.REDACTED, Redaction.redactHeader("x-api-key", "value"));
    Assertions.assertEquals(Redaction.REDACTED, Redaction.redactHeader("x-csrf-token", "value"));
    Assertions.assertEquals(Redaction.REDACTED, Redaction.redactHeader("x-session-id", "value"));
  }

  @Test
  void testRedactHeaderSafeNames() {
    Assertions.assertEquals("text/html", Redaction.redactHeader("content-type", "text/html"));
    Assertions.assertEquals("gzip", Redaction.redactHeader("content-encoding", "gzip"));
    Assertions.assertEquals("keep-alive", Redaction.redactHeader("connection", "keep-alive"));
    Assertions.assertEquals("example.com", Redaction.redactHeader("host", "example.com"));
  }

  // redactValue() — values whatever their name
  // ---------------------------------------------------------------------------

  @ParameterizedTest
  @CsvSource(delimiter = '|', value = {
      "plain value                                      | plain value",
      "https://example.org/x?q=1                        | https://example.org/x?q=1",
      "jdbc:postgresql://app:s3cret@db/app              | jdbc:postgresql://app:[REDACTED]@db/app",
      "https://ghp_token@github.com/org/repo.git        | https://[REDACTED]@github.com/org/repo.git",
      "https://user:p@ss@host/x                         | https://user:[REDACTED]@host/x",
      "//user:pw@host                                   | //user:[REDACTED]@host",
      "https://@host/x                                  | https://@host/x",
      "https://host/a@b                                 | https://host/a@b",
      "a=http://u:p@h1/ b=ftp://u2:p2@h2/               | a=http://u:[REDACTED]@h1/ b=ftp://u2:[REDACTED]@h2/",
      "jdbc:mysql://db/app?user=app&password=s3cret     | jdbc:mysql://db/app?user=app&password=[REDACTED]",
      "https://host/x?access_token=abc#top              | https://host/x?access_token=[REDACTED]#top",
      "jdbc:sqlserver://db;user=app;password=s3cret     | jdbc:sqlserver://db;user=app;password=[REDACTED]",
      "host=db user=app password=s3cret                 | host=db user=app password=[REDACTED]",
      "token=abc                                        | token=[REDACTED]",
      "https://host/x?password=                         | https://host/x?password=",
      "[REDACTED]                                       | [REDACTED]"})
  void testRedactValue(String value, String expected) {
    Assertions.assertEquals(expected, Redaction.redactValue(value));
  }

  @Test
  void testRedactAppliesValueRules() {
    Assertions.assertEquals("jdbc:postgresql://app:[REDACTED]@db/app",
        Redaction.redact("db.url", "jdbc:postgresql://app:s3cret@db/app"));
    Assertions.assertEquals("https://example.org/?token=[REDACTED]",
        Redaction.redactHeader("referer", "https://example.org/?token=abc"));
  }

  // redactUriPattern() — URI patterns such as redirect targets
  // ---------------------------------------------------------------------------

  @ParameterizedTest
  @CsvSource(delimiter = '|', value = {
      "/a                                  | /a",
      "/a?token=secret                     | /a?token=[REDACTED]",
      "/a?token=secret&q={q}&flag#frag     | /a?token=[REDACTED]&q={q}&flag#frag",
      "/a?x=1&y=2                          | /a?x=[REDACTED]&y=[REDACTED]",
      "/a?x={q}{r}                         | /a?x=[REDACTED]",
      "/a?x=                               | /a?x=[REDACTED]",
      "/a?                                 | /a?",
      "/a?x=1&                             | /a?x=[REDACTED]&",
      "/a?x=1{&y}                          | /a?x=[REDACTED]",
      "/search{?q}                         | /search{?q}",
      "/search{?q}&x=1                     | /search{?q}&x=1",
      "/a{#f}?x=1                          | /a{#f}?x=[REDACTED]",
      "/a#f?x=1                            | /a#f?x=1",
      "https://user:pass@example.org/x?a=1 | https://user:[REDACTED]@example.org/x?a=[REDACTED]",
      "https://user@example.org/x          | https://[REDACTED]@example.org/x",
      "//alice:secret@example.com/p?token=s | //alice:[REDACTED]@example.com/p?token=[REDACTED]",
      "//alice@example.com                 | //[REDACTED]@example.com",
      "//example.com/a@b                   | //example.com/a@b",
      "https://example.org/x?u=a@b         | https://example.org/x?u=[REDACTED]",
      "https://{host}/x                    | https://{host}/x",
      "/{+path}                            | /{+path}"})
  void testRedactUriPattern(String pattern, String expected) {
    Assertions.assertEquals(expected, Redaction.redactUriPattern(pattern));
  }

  @Test
  void testRedactUriPatternLargeQueryWithExpressions() {
    int count = 50_000;
    StringBuilder pattern = new StringBuilder("/path?");
    StringBuilder expected = new StringBuilder("/path?");
    for (int i = 0; i < count; i++) {
      pattern.append("q=value&e={x}&t={?a,b&c}&");
      expected.append("q=[REDACTED]&e={x}&t={?a,b&c}&");
    }
    pattern.append("last=1{#f}#frag?x=1&y=2");
    expected.append("last=[REDACTED]#frag?x=1&y=2");
    // Linear in the length of the pattern: rescanning it for each parameter took seconds
    String redacted = Assertions.assertTimeoutPreemptively(Duration.ofSeconds(2),
        () -> Redaction.redactUriPattern(pattern.toString()));
    Assertions.assertEquals(expected.toString(), redacted);
  }

  // isSensitiveName() and isSensitiveHeader()
  // ---------------------------------------------------------------------------

  @Test
  void testIsSensitiveName() {
    Assertions.assertTrue(Redaction.isSensitiveName("password"));
    Assertions.assertTrue(Redaction.isSensitiveName("api_key"));
    Assertions.assertFalse(Redaction.isSensitiveName("username"));
    Assertions.assertFalse(Redaction.isSensitiveName("host"));
  }

  @Test
  void testIsSensitiveHeader() {
    Assertions.assertTrue(Redaction.isSensitiveHeader("cookie"));
    Assertions.assertTrue(Redaction.isSensitiveHeader("set-cookie"));
    Assertions.assertTrue(Redaction.isSensitiveHeader("authorization"));
    Assertions.assertTrue(Redaction.isSensitiveHeader("x-api-key"));
    Assertions.assertFalse(Redaction.isSensitiveHeader("content-type"));
    Assertions.assertFalse(Redaction.isSensitiveHeader("host"));
  }

}
