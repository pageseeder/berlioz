package org.pageseeder.berlioz.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
