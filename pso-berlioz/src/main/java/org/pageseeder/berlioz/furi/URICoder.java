/*
 * Copyright 2015 Allette Systems (Australia)
 * http://www.allette.com.au
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pageseeder.berlioz.furi;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.text.Normalizer.Form;

/**
 * An encoder/decoder for use by URI templates, following RFC 3986.
 *
 * <p>Only unreserved characters do not need to be encoded within a URI variable:
 *
 * <pre>{@code
 * unreserved = ALPHA / DIGIT / '-' / '.' / '_' / '&tilde;'
 * }</pre>
 *
 * <p>Two encoding modes are provided:
 * <ul>
 *   <li>{@link #encode(String)} — encodes everything except unreserved characters. Use this
 *       for URI template variable values.</li>
 *   <li>{@link #minimalEncode(String)} — additionally preserves RFC 3986 reserved characters
 *       ({@code : / ? # [ ] @ ! $ &amp; ' ( ) * + , ; =}) and only encodes characters that are
 *       outright illegal in any URI. Use this when the value already contains structural URI
 *       characters that must be kept intact.</li>
 * </ul>
 *
 * <h2>Differences from {@link java.net.URLEncoder} / {@link java.net.URLDecoder}</h2>
 *
 * <p>{@code java.net.URLEncoder} targets the {@code application/x-www-form-urlencoded} media
 * type used by HTML forms, not RFC 3986. The two differ in several important ways:
 *
 * <table>
 *   <caption>Encoding comparison</caption>
 *   <tr><th>Input</th><th>URICoder.encode</th><th>URLEncoder</th></tr>
 *   <tr><td>space</td><td>{@code %20}</td>
 *       <td>{@code +} — form-data convention, not valid in a URI path</td></tr>
 *   <tr><td>{@code ~}</td><td>{@code ~} — left as-is (RFC 3986 unreserved)</td>
 *       <td>{@code %7E} — encoded (predates RFC 3986 adding {@code ~} to unreserved)</td></tr>
 *   <tr><td>{@code *}</td><td>{@code %2A} — encoded (not RFC 3986 unreserved)</td>
 *       <td>{@code *} — left as-is (legacy safe set)</td></tr>
 *   <tr><td>{@code +}</td><td>{@code %2B}</td><td>{@code %2B} — same</td></tr>
 *   <tr><td>{@code -._}</td><td>unchanged</td><td>unchanged — same</td></tr>
 *   <tr><td>non-ASCII</td><td>UTF-8 percent-encoded, after NFKC normalisation</td>
 *       <td>UTF-8 percent-encoded, no normalization (NFC and NFD may differ)</td></tr>
 * </table>
 *
 * <p>{@link #decode(String)} accepts both {@code %20} and {@code +} as space, so it can handle
 * input from either encoding convention. {@code java.net.URLDecoder} behaves the same way on
 * input, but callers should prefer this class when working with URI templates.
 *
 * <p>Strings that contain only unreserved characters (the common case for well-formed path
 * segments) are returned unchanged without any allocation.
 *
 * @see <a href="http://tools.ietf.org/html/rfc3986">RFC 3986 – Uniform Resource Identifier (URI):
 *      Generic Syntax</a>
 * @see <a href="http://tools.ietf.org/html/rfc3986#appendix-A">RFC 3986 Appendix A – Collected
 *      ABNF for URI</a>
 * @see <a href="http://www.unicode.org/unicode/reports/tr15/tr15-23.html#Specification">UAX #15:
 *      Unicode Normalization</a>
 * @see java.net.URLEncoder
 * @see java.net.URLDecoder
 *
 * @author Christophe Lauret
 *
 * @version 0.13.0
 * @since 0.9.32
 */
public final class URICoder {

  /**
   * The hexadecimal digits for use by the encoder.
   */
  private static final char[] HEX_DIGITS = {
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
  };

  /**
   * Prevents creation of instances.
   */
  private URICoder() {
  }

  // Encoder
  // ==========================================================================

  /**
   * Encodes a string for use as a URI template variable value.
   *
   * <p>All characters except RFC 3986 unreserved characters ({@code ALPHA / DIGIT / - . _ ~})
   * are percent-encoded. In particular:
   * <ul>
   *   <li>Space is encoded as {@code %20}, not {@code +}.</li>
   *   <li>{@code ~} is left as-is (unreserved per RFC 3986).</li>
   *   <li>{@code *} is encoded as {@code %2A} (not unreserved).</li>
   *   <li>Non-ASCII text is normalized to NFKC, then encoded as UTF-8 byte sequences.</li>
   * </ul>
   *
   * @param s The string to encode.
   *
   * @return The percent-encoded string.
   */
  public static String encode(String s) {
    // '0' is unreserved, so passing it as the extra passthrough char has no effect
    return encode(s, '0');
  }

  /**
   * Encodes a string for use as a URI template variable value, with one extra passthrough character.
   *
   * <p>Behaves identically to {@link #encode(String)} except that the ASCII character {@code c}
   * is also left unencoded. This is useful for operators that allow one structural character
   * to appear literally in the expansion (e.g. {@code '/'} for path segments).
   *
   * @param s The string to encode.
   * @param c An ASCII character that should not be encoded if found in the string.
   *
   * @return The percent-encoded string.
   */
  public static String encode(String s, char c) {
    if (s.isEmpty())
      return s;
    return isASCII(s) ? encodeASCII(s, c) : encodeUTF8(s, c);
  }

  /**
   * Minimally encodes a string so that it is safe to embed in a URI.
   *
   * <p>Unlike {@link #encode(String)}, this method preserves RFC 3986 reserved characters
   * ({@code : / ? # [ ] @ ! $ &amp; ' ( ) * + , ; =}) so that structural URI syntax is not
   * destroyed. Only characters that are outright illegal in any URI component are
   * percent-encoded: space, {@code %}, {@code "}, {@code <}, {@code >}, {@code \},
   * {@code ^}, {@code `}, {@code {}, {@code |}, {@code }}, and control characters.
   *
   * <p>Use this method when the input may already contain a valid URI structure (e.g. a full
   * path-and-query string) and only unsafe characters need to be escaped.
   *
   * @param s The string to encode.
   *
   * @return The minimally percent-encoded string.
   */
  public static String minimalEncode(String s) {
    if (s.isEmpty())
      return s;
    return isASCII(s) ? minimalEncodeASCII(s) : minimalEncodeUTF8(s);
  }

  /**
   * Encodes an ASCII-only string, leaving unreserved characters and {@code e} as-is.
   *
   * @param s The string to encode (caller guarantees ASCII-only content).
   * @param e An extra character that bypasses encoding.
   */
  private static String encodeASCII(String s, char e) {
    int len = s.length();
    // Scan for the first character that needs encoding; return s unchanged on the hot path.
    int i = 0;
    while (i < len && (isUnreserved(s.charAt(i)) || s.charAt(i) == e)) {
      i++;
    }
    if (i == len) return s;
    StringBuilder sb = new StringBuilder(len);
    sb.append(s, 0, i);
    while (i < len) {
      char c = s.charAt(i++);
      if (isUnreserved(c) || c == e) {
        sb.append(c);
      } else {
        appendEscape(sb, c);
      }
    }
    return sb.toString();
  }

  /**
   * Minimally encodes an ASCII-only string, leaving legal URI characters as-is.
   *
   * @param s The string to encode (caller guarantees ASCII-only content).
   */
  private static String minimalEncodeASCII(String s) {
    int len = s.length();
    int i = 0;
    while (i < len && isLegal(s.charAt(i))) {
      i++;
    }
    if (i == len) return s;
    StringBuilder sb = new StringBuilder(len);
    sb.append(s, 0, i);
    while (i < len) {
      char c = s.charAt(i++);
      if (isLegal(c)) {
        sb.append(c);
      } else {
        appendEscape(sb, c);
      }
    }
    return sb.toString();
  }

  /**
   * Encodes a string containing non-ASCII characters, normalizing to NFKC first.
   *
   * @param s The string to encode (may contain non-ASCII characters).
   * @param e An extra character that bypasses encoding.
   */
  private static String encodeUTF8(String s, char e) {
    String n = Normalizer.isNormalized(s, Form.NFKC) ? s : Normalizer.normalize(s, Form.NFKC);
    ByteBuffer bb = StandardCharsets.UTF_8.encode(n);
    StringBuilder sb = new StringBuilder(n.length());
    while (bb.hasRemaining()) {
      int b = bb.get() & 0xff;
      if (isUnreserved(b) || b == e) {
        sb.append((char) b);
      } else {
        appendEscape(sb, b);
      }
    }
    return sb.toString();
  }

  /**
   * Minimally encodes a string containing non-ASCII characters, normalizing to NFKC first.
   *
   * @param s The string to encode (may contain non-ASCII characters).
   */
  private static String minimalEncodeUTF8(String s) {
    String n = Normalizer.isNormalized(s, Form.NFKC) ? s : Normalizer.normalize(s, Form.NFKC);
    ByteBuffer bb = StandardCharsets.UTF_8.encode(n);
    StringBuilder sb = new StringBuilder(n.length());
    while (bb.hasRemaining()) {
      int b = bb.get() & 0xff;
      if (isLegal(b)) {
        sb.append((char) b);
      } else {
        appendEscape(sb, b);
      }
    }
    return sb.toString();
  }

  // Decoder
  // ==========================================================================

  /**
   * Decodes a percent-encoded URI string.
   *
   * <p>Both {@code %XX} sequences and {@code +} are treated as space, so input from either
   * URI percent-encoding ({@code %20}) or HTML form encoding ({@code +}) is handled correctly.
   * {@code %2B} decodes to a literal {@code +}. Hex digits in {@code %XX} sequences may be
   * upper- or lowercase.
   *
   * <p>Strings that contain no {@code %} or {@code +} characters are returned as-is without
   * any allocation. Malformed {@code %} sequences (fewer than two following hex digits, or
   * non-hex digits) are silently dropped.
   *
   * @param s The string to decode.
   *
   * @return The decoded string.
   */
  public static String decode(String s) {
    if (s.isEmpty() || (s.indexOf('%') < 0 && s.indexOf('+') < 0))
      return s;
    return isEncodedASCII(s) ? decodeASCII(s) : decodeUTF8(s);
  }

  /**
   * Decodes a percent-encoded string whose decoded bytes are all in the ASCII range.
   */
  private static String decodeASCII(String s) {
    int len = s.length();
    StringBuilder sb = new StringBuilder(len);
    int i = 0;
    while (i < len) {
      char c = s.charAt(i);
      if (c == '%') {
        if (i + 2 < len) {
          int hi = hexToInt(s.charAt(i + 1));
          int lo = hexToInt(s.charAt(i + 2));
          if (hi >= 0 && lo >= 0) {
            sb.append((char) ((hi << 4) | lo));
            i += 3;
          } else {
            // Non-hex digits after %: drop the % and reprocess the following characters.
            i++;
          }
        } else {
          // Fewer than two characters remain after %: drop the truncated sequence.
          i++;
        }
      } else if (c == '+') {
        sb.append(' ');
        i++;
      } else {
        sb.append(c);
        i++;
      }
    }
    return sb.toString();
  }

  /**
   * Decodes a percent-encoded string that contains multibyte UTF-8 sequences.
   */
  private static String decodeUTF8(String s) {
    int len = s.length();
    // Each input character produces at most one output byte for well-formed encoded input
    // (%XX → 1 byte from 3 chars; ASCII literal → 1 byte; + → 1 byte).
    byte[] bytes = new byte[len];
    int pos = 0;
    int i = 0;
    while (i < len) {
      char c = s.charAt(i);
      if (c == '%') {
        if (i + 2 < len) {
          int hi = hexToInt(s.charAt(i + 1));
          int lo = hexToInt(s.charAt(i + 2));
          if (hi >= 0 && lo >= 0) {
            bytes[pos++] = (byte) ((hi << 4) | lo);
            i += 3;
          } else {
            // Non-hex digits after %: drop the % and reprocess the following characters.
            i++;
          }
        } else {
          // Fewer than two characters remain after %: drop the truncated sequence.
          i++;
        }
      } else if (c == '+') {
        bytes[pos++] = (byte) ' ';
        i++;
      } else {
        // For valid percent-encoded input this is always an ASCII character.
        // Non-ASCII literals are not expected here; only the low byte is stored.
        bytes[pos++] = (byte) c;
        i++;
      }
    }
    return new String(bytes, 0, pos, StandardCharsets.UTF_8);
  }

  // Helpers
  // ==========================================================================

  /**
   * Appends a percent-escape sequence for a single byte value to the builder.
   *
   * @param sb The builder to append to.
   * @param b  The byte value (0–255) to escape.
   */
  private static void appendEscape(StringBuilder sb, int b) {
    sb.append('%');
    sb.append(HEX_DIGITS[(b >> 4) & 0x0f]);
    sb.append(HEX_DIGITS[b & 0x0f]);
  }

  /**
   * Converts a single hexadecimal character to its integer value.
   *
   * @param c The character to convert ({@code 0–9}, {@code A–F}, or {@code a–f}).
   *
   * @return The integer value (0–15), or {@code -1} if {@code c} is not a valid hex digit.
   */
  private static int hexToInt(char c) {
    if (c >= '0' && c <= '9') return c - '0';
    if (c >= 'A' && c <= 'F') return c - 'A' + 10;
    if (c >= 'a' && c <= 'f') return c - 'a' + 10;
    return -1;
  }

  /**
   * Returns {@code true} if the character is an RFC 3986 unreserved character.
   *
   * @param c The character to test.
   *
   * @return {@code true} if unreserved; {@code false} otherwise.
   */
  private static boolean isUnreserved(int c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
        || c == '-' || c == '.' || c == '_' || c == '~';
  }

  /**
   * Returns {@code true} if the character is legal in a URI (i.e. not required to be
   * percent-encoded by {@link #minimalEncode}).
   *
   * @param c The character to test.
   *
   * @return {@code true} if legal; {@code false} otherwise.
   */
  private static boolean isLegal(int c) {
    if (c < '&') return c == '!' || c == '#' || c == '$';
    if (c >= '{') return c == '~';
    return c != '`' && c != '<' && c != '>' && c != '\\' && c != '^';
  }

  /**
   * Returns {@code true} if the string contains only ASCII characters (code points {@code < 0x80}).
   */
  private static boolean isASCII(String s) {
    for (int i = 0; i < s.length(); i++) {
      if (s.charAt(i) >= 0x80) return false;
    }
    return true;
  }

  /**
   * Returns {@code true} if the string contains no percent-encoded non-ASCII bytes.
   *
   * <p>A {@code %XX} sequence encodes a non-ASCII byte when the first hex digit is greater than
   * {@code '7'} (i.e. the byte value is {@code >= 0x80}), which indicates the start of a
   * multibyte UTF-8 sequence. If any such sequence is found, the UTF-8 decoder must be used.
   */
  private static boolean isEncodedASCII(String s) {
    for (int i = 0; i < s.length(); i++) {
      if (s.charAt(i) == '%' && i < s.length() - 1 && s.charAt(i + 1) > '7') return false;
    }
    return true;
  }

}
