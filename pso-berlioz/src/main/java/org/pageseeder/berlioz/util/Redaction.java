package org.pageseeder.berlioz.util;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for redacting sensitive values before logging or serialization.
 *
 * <p>A value is redacted by its name, then by its content:
 * <ul>
 *   <li>a sensitive name: the whole value is {@link #REDACTED};</li>
 *   <li>the user information of a URL anywhere in the value (after {@code ://}, or {@code //} at the
 *   start): its password, or all of it when it has no {@code :};</li>
 *   <li>the value of a parameter with a sensitive name, at the start of the value or after {@code ?},
 *   {@code &}, {@code ;} or whitespace.</li>
 * </ul>
 * So {@code jdbc:postgresql://app:s3cret@db/app?ssl=true&password=s3cret} is written
 * {@code jdbc:postgresql://app:[REDACTED]@db/app?ssl=true&password=[REDACTED]}.
 *
 * <p>Names are normalized (stripped of {@code -}, {@code _}, {@code .} separators and lowercased)
 * before keyword matching, so {@code api-key}, {@code api_key}, and {@code apikey} all match.
 *
 * <p>A secret in a value that matches none of these rules is not detected.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.6
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
      "credential", "privatekey", "key",
      "session", "auth",
      "assertion", "saml", "jwt", "csrf"
  };

  /**
   * The user information of a URL: after {@code ://} (or {@code //} at the start), up to the last
   * {@code @} of the authority, which ends with {@code /}, {@code ?}, {@code #} or whitespace.
   */
  private static final Pattern USER_INFO = Pattern.compile("(^//|://)([^/?#\\s]*)@");

  /** A parameter at the start of the value, or after '?', '&', ';' or whitespace. */
  private static final Pattern PARAMETER = Pattern.compile("(^|[?&;\\s])([^=?&;#\\s]+)=([^&;#\\s]*)");

  private Redaction() {}

  /**
   * Returns {@link #REDACTED} if the HTTP header name is sensitive, otherwise the value with
   * its sensitive parts redacted ({@link #redactValue(String)}).
   *
   * @param name  the HTTP header name
   * @param value the HTTP header value
   * @return the redacted value
   */
  public static String redactHeader(String name, String value) {
    return isSensitiveHeader(name) ? REDACTED : redactValue(value);
  }

  /**
   * Returns {@link #REDACTED} if the parameter or property name is sensitive, otherwise the value
   * with its sensitive parts redacted ({@link #redactValue(String)}).
   *
   * @param name  the parameter or property name
   * @param value the parameter or property value
   * @return the redacted value
   */
  public static String redact(String name, String value) {
    return isSensitiveName(name) ? REDACTED : redactValue(value);
  }

  /**
   * Redacts the parts of a value that hold a credential whatever the name of the value: the user
   * information of a URL, and the values of parameters with a sensitive name.
   *
   * @param value any value
   * @return the value with its sensitive parts replaced by {@link #REDACTED}
   *
   * @since 0.14.6
   */
  public static String redactValue(String value) {
    return redactParameters(redactUserInfo(value));
  }

  /**
   * Redacts a URI pattern, such as the target of a redirect rule.
   *
   * <p>Stricter than {@link #redactValue(String)}, since the name of a parameter does not reveal
   * a credential in a URL: the value of every query parameter is replaced, unless it is only one
   * URI template expression such as {@code {q}}. The user information is redacted as by
   * {@link #redactValue(String)}. Template expressions, which can contain {@code ?}, {@code &}
   * and {@code #}, and the fragment are kept.
   *
   * @param pattern the URI pattern, as text
   * @return the redacted pattern
   *
   * @since 0.14.6
   */
  public static String redactUriPattern(String pattern) {
    String to = redactUserInfo(pattern);
    int query = indexOutsideExpressions(to, '?', 0);
    int fragment = indexOutsideExpressions(to, '#', 0);
    // No query, or a '?' in the fragment
    if (query < 0 || fragment >= 0 && fragment < query) return to;
    int end = fragment < 0 ? to.length() : fragment;
    StringBuilder out = new StringBuilder(to.length()).append(to, 0, query + 1);
    int start = query + 1;
    while (true) {
      int amp = indexOutsideExpressions(to, '&', start);
      int stop = amp < 0 || amp > end ? end : amp;
      out.append(redactPatternParameter(to.substring(start, stop)));
      if (stop == end) break;
      out.append('&');
      start = stop + 1;
    }
    return out.append(to, end, to.length()).toString();
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

  /** @return the value without the password of a URL, or its user information without password */
  private static String redactUserInfo(String value) {
    if (!value.contains("//")) return value;
    Matcher m = USER_INFO.matcher(value);
    StringBuilder out = new StringBuilder(value.length());
    while (m.find()) {
      String info = m.group(2);
      int colon = info.indexOf(':');
      String user = colon >= 0 ? info.substring(0, colon + 1) : "";
      String redacted = info.isEmpty() ? m.group() : m.group(1) + user + REDACTED + '@';
      m.appendReplacement(out, Matcher.quoteReplacement(redacted));
    }
    m.appendTail(out);
    return out.toString();
  }

  /** @return the value without the values of the parameters with a sensitive name */
  private static String redactParameters(String value) {
    if (value.indexOf('=') < 0) return value;
    Matcher m = PARAMETER.matcher(value);
    StringBuilder out = new StringBuilder(value.length());
    while (m.find()) {
      String name = m.group(2);
      String replaced = isSensitiveName(name) && !m.group(3).isEmpty()
          ? m.group(1) + name + '=' + REDACTED
          : m.group();
      m.appendReplacement(out, Matcher.quoteReplacement(replaced));
    }
    m.appendTail(out);
    return out.toString();
  }

  private static String redactPatternParameter(String parameter) {
    int equals = parameter.indexOf('=');
    if (equals < 0) return parameter;
    String value = parameter.substring(equals + 1);
    boolean expression = value.length() > 2 && value.charAt(0) == '{' && value.indexOf('}') == value.length() - 1;
    return expression ? parameter : parameter.substring(0, equals + 1) + REDACTED;
  }

  /** @return the index of the character outside template expressions, from the index, or -1 */
  private static int indexOutsideExpressions(String s, char c, int from) {
    int depth = 0;
    // Scan from the start to know the depth of expressions at 'from'
    for (int i = 0; i < s.length(); i++) {
      char x = s.charAt(i);
      if (x == '{') depth++;
      else if (x == '}' && depth > 0) depth--;
      else if (x == c && depth == 0 && i >= from) return i;
    }
    return -1;
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
