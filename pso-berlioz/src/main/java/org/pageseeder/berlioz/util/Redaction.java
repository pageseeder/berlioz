package org.pageseeder.berlioz.util;

import java.util.Locale;
import java.util.Set;

/**
 * Utility class for redacting sensitive values before logging or serialization.
 *
 * <p>Names are normalized (stripped of {@code -}, {@code _}, {@code .} separators and lowercased)
 * before keyword matching, so {@code api-key}, {@code api_key}, and {@code apikey} all match.
 */
public final class Redaction {

  public static final String REDACTED = "[REDACTED]";

  /** HTTP header names that are unconditionally redacted regardless of keyword matching. */
  private static final Set<String> EXACT_HEADER_NAMES = Set.of(
      "authorization",
      "proxy-authorization",
      "cookie",
      "set-cookie"
  );

  private static final String[] SENSITIVE_KEYWORDS = {
      "password", "passwd", "pwd",
      "secret", "apikey", "token",
      "credential", "privatekey",
      "session", "auth",
      "assertion", "saml", "jwt", "csrf"
  };

  private Redaction() {}

  /**
   * Returns {@link #REDACTED} if the HTTP header name is sensitive, otherwise the original value.
   */
  public static String redactHeader(String name, String value) {
    return isSensitiveHeader(name) ? REDACTED : value;
  }

  /**
   * Returns {@link #REDACTED} if the parameter or property name is sensitive, otherwise the original value.
   */
  public static String redact(String name, String value) {
    return isSensitiveName(name) ? REDACTED : value;
  }

  /**
   * Returns {@code true} if the HTTP header name is considered sensitive.
   *
   * <p>Checks exact header names first ({@code authorization}, {@code cookie}, etc.),
   * then falls back to keyword matching.
   */
  public static boolean isSensitiveHeader(String name) {
    String lower = name.toLowerCase(Locale.ROOT);
    return EXACT_HEADER_NAMES.contains(lower) || containsKeyword(normalize(lower));
  }

  /**
   * Returns {@code true} if the parameter or property name is considered sensitive.
   */
  public static boolean isSensitiveName(String name) {
    return containsKeyword(normalize(name.toLowerCase(Locale.ROOT)));
  }

  private static boolean containsKeyword(String normalized) {
    for (String keyword : SENSITIVE_KEYWORDS) {
      if (normalized.contains(keyword)) return true;
    }
    return false;
  }

  private static String normalize(String name) {
    return name.replace("-", "").replace("_", "").replace(".", "");
  }

}
