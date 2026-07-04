package org.pageseeder.berlioz.util;

import java.util.Locale;
import java.util.Set;

/**
 * Utility class for redacting sensitive values before logging or serialization.
 *
 * <p>Names are normalized (stripped of {@code -}, {@code _}, {@code .} separators and lowercased)
 * before keyword matching, so {@code api-key}, {@code api_key}, and {@code apikey} all match.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.0
 * @since 0.14.0
 */
public final class Redaction {

  /**
   * The redacted value.
   */
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
   *
   * @param name  the HTTP header name
   * @param value the HTTP header value
   * @return the original value, or {@link #REDACTED} if the header name is sensitive
   */
  public static String redactHeader(String name, String value) {
    return isSensitiveHeader(name) ? REDACTED : value;
  }

  /**
   * Returns {@link #REDACTED} if the parameter or property name is sensitive, otherwise the original value.
   *
   * @param name  the parameter or property name
   * @param value the parameter or property value
   * @return the original value, or {@link #REDACTED} if the name is sensitive
   */
  public static String redact(String name, String value) {
    return isSensitiveName(name) ? REDACTED : value;
  }

  /**
   * Returns {@code true} if the HTTP header name is considered sensitive.
   *
   * <p>Checks exact header names first ({@code authorization}, {@code cookie}, etc.),
   * then falls back to keyword matching.
   *
   * @param name the HTTP header name (case-insensitive)
   * @return {@code true} if the header value should be redacted
   */
  public static boolean isSensitiveHeader(String name) {
    String lower = name.toLowerCase(Locale.ROOT);
    return EXACT_HEADER_NAMES.contains(lower) || containsKeyword(normalize(lower));
  }

  /**
   * Returns {@code true} if the parameter or property name is considered sensitive.
   *
   * @param name the parameter or property name (case-insensitive)
   * @return {@code true} if values for this name should be redacted
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
