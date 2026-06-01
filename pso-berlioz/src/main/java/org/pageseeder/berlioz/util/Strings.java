package org.pageseeder.berlioz.util;

import org.jspecify.annotations.Nullable;

/**
 * Utility methods for strings
 *
 * @version Berlioz 0.13.0
 * @since Berlioz 0.13.0
 */
public final class Strings {

  private Strings() {}

  /**
   * @param value The value to inspect.
   * @param delimiter The delimiter to find.
   * @return substring after the first occurrence of the delimiter, or the original string if the
   * delimiter is not found; empty string if the value is empty or delimiter is null
   */
  public static String substringAfter(String value, @Nullable String delimiter) {
    if (value.isEmpty() || delimiter == null) return "";
    if (delimiter.isEmpty()) return value;
    int i = value.indexOf(delimiter);
    return i < 0 ? value : value.substring(i + delimiter.length());
  }

  /**
   * @param value The value to inspect.
   * @param delimiter The delimiter to find.
   * @return substring after the first occurrence of the delimiter, or the original string if the
   * delimiter is not found; empty string if the value is empty
   */
  public static String substringAfter(String value, char delimiter) {
    if (value.isEmpty()) return "";
    int i = value.indexOf(delimiter);
    return i < 0 ? value : value.substring(i + 1);
  }

  /**
   * @param value The value to inspect.
   * @param delimiter The delimiter to find.
   * @return substring before the first occurrence of the delimiter, or the original string if the
   * delimiter is not found; empty string if the value is empty or delimiter is null
   */
  public static String substringBefore(String value, @Nullable String delimiter) {
    if (value.isEmpty() || delimiter == null) return "";
    if (delimiter.isEmpty()) return value;
    int i = value.indexOf(delimiter);
    return i < 0 ? value : value.substring(0, i);
  }

  /**
   * @param value The value to inspect.
   * @param delimiter The delimiter to find.
   * @return substring before the first occurrence of the delimiter, or the original string if the
   * delimiter is not found; empty string if the value is empty
   */
  public static String substringBefore(String value, char delimiter) {
    if (value.isEmpty()) return "";
    int i = value.indexOf(delimiter);
    return i < 0 ? value : value.substring(0, i);
  }

  /**
   * Converts a PascalCase or camelCase identifier to kebab-case, treating consecutive
   * uppercase letters as a single block (e.g. {@code MyHTTPClient} → {@code my-http-client}).
   * Characters outside {@code [a-z0-9]} are replaced with a hyphen; consecutive hyphens
   * are collapsed, and leading/trailing hyphens are stripped.
   *
   * <p>{@link Class#getSimpleName()} returns an empty string for anonymous classes and
   * lambda expressions (which have no source-level name). Pass a non-empty {@code fallback}
   * to receive a safe value in that case.
   *
   * @param name     the identifier to convert (e.g. the value of {@link Class#getSimpleName()})
   * @param fallback returned as-is when {@code name} is empty or produces an empty result
   * @return a non-empty string matching {@code [a-z0-9][a-z0-9-]*}, or {@code fallback}
   */
  public static String toKebabCase(String name, String fallback) {
    if (name.isEmpty()) return fallback;
    char[] chars = name.toCharArray();
    StringBuilder sb = new StringBuilder(chars.length + 4);
    for (int i = 0; i < chars.length; i++) {
      char c = chars[i];
      if (Character.isUpperCase(c)) {
        if (i > 0 && needsHyphenBefore(chars, i)) appendHyphen(sb);
        sb.append(Character.toLowerCase(c));
      } else if (Character.isLowerCase(c) || Character.isDigit(c)) {
        sb.append(c);
      } else {
        appendHyphen(sb);
      }
    }
    // strip trailing hyphen left by a non-alphanumeric at the end
    int len = sb.length();
    if (len > 0 && sb.charAt(len - 1) == '-') sb.deleteCharAt(len - 1);
    return sb.length() == 0 ? fallback : sb.toString();
  }

  /**
   * Returns {@code true} when a hyphen should be inserted before the uppercase character
   * at position {@code i}: either the previous character was a lowercase letter or digit,
   * or this uppercase is the last in a run and the next character is lowercase.
   */
  private static boolean needsHyphenBefore(char[] chars, int i) {
    boolean prevLowerOrDigit = Character.isLowerCase(chars[i - 1]) || Character.isDigit(chars[i - 1]);
    boolean endOfUpperRun = Character.isUpperCase(chars[i - 1])
        && i + 1 < chars.length && Character.isLowerCase(chars[i + 1]);
    return prevLowerOrDigit || endOfUpperRun;
  }

  /**
   * Appends a hyphen to {@code sb} unless it is empty or already ends with a hyphen,
   * preventing leading or consecutive hyphens.
   */
  private static void appendHyphen(StringBuilder sb) {
    if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '-') sb.append('-');
  }
}
