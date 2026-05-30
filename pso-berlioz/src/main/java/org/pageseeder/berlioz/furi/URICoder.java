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
 * <pre>
 * unreserved = ALPHA / DIGIT / '-' / '.' / '_' / '&tilde;'
 * </pre>
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
 *       <td>UTF-8 percent-encoded, no normalisation (NFC and NFD may differ)</td></tr>
 * </table>
 *
 * <p>{@link #decode(String)} accepts both {@code %20} and {@code +} as space, so it can handle
 * input from either encoding convention. {@code java.net.URLDecoder} behaves the same way on
 * input, but callers should prefer this class when working with URI templates.
 *
 * <p>Strings that contain only unreserved characters (the common case for well-formed path
 * segments) are processed without allocating a {@code StringBuilder}, keeping the hot path fast.
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
 * @version Berlioz 0.13.0
 * @since Berlioz 0.9.32
 */
public final class URICoder {

  /**
   * The hexadecimal digits for use by the encoder.
   */
  private static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A',
      'B', 'C', 'D', 'E', 'F' };

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
   *   <li>Non-ASCII text is normalised to NFKC, then encoded as UTF-8 byte sequences.</li>
   * </ul>
   *
   * @param s The string to encode.
   *
   * @return The percent-encoded string.
   */
  public static String encode(String s) {
    // invoke encode method with character that we know does not require encoding
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
    // Check whether we need to use UTF-8 encoder
    boolean ascii = isASCII(s);
    return ascii ? encodeASCII(s, c) : encodeUTF8(s, c);
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
   * <p>Use this method when the input may already contain valid URI structure (e.g. a full
   * path-and-query string) and only unsafe characters need to be escaped.
   *
   * @param s The string to encode.
   *
   * @return The minimally percent-encoded string.
   */
  public static String minimalEncode(String s) {
    if (s.isEmpty())
      return s;
    // Check whether we need to use UTF-8 encoder
    boolean ascii = isASCII(s);
    return ascii ? minimalEncodeASCII(s) : minimalEncodeUTF8(s);
  }

  /**
   * Encodes a string containing only ASCII characters.
   *
   * @param s The string the encode (assuming ASCII characters only)
   * @param e A character that does not require encoding if found in the string.
   */
  private static String encodeASCII(String s, char e) {
    StringBuilder sb = new StringBuilder();
    for (char c : s.toCharArray()) {
      if (isUnreserved(c) || c == e) {
        sb.append(c);
      } else {
        appendEscape(sb, c);
      }
    }
    return sb.toString();
  }

  /**
   * Encodes a string containing only ASCII characters.
   *
   * @param s The string the encode (assuming ASCII characters only)
   */
  private static String minimalEncodeASCII(String s) {
    StringBuilder sb = new StringBuilder();
    for (char c : s.toCharArray()) {
      if (isLegal(c)) {
        sb.append(c);
      } else {
        appendEscape(sb, c);
      }
    }
    return sb.toString();
  }

  /**
   * Encodes a string containing non ASCII characters using an UTF-8 encoder.
   *
   * @param s The string the encode (assuming ASCII characters only)
   * @param e A character that does not require encoding if found in the string.
   */
  private static String encodeUTF8(String s, char e) {
    String n = (Normalizer.isNormalized(s, Form.NFKC)) ? s : Normalizer.normalize(s, Form.NFKC);
    // convert String to UTF-8
    ByteBuffer bb = StandardCharsets.UTF_8.encode(n);
    // URI encode
    StringBuilder sb = new StringBuilder();
    while (bb.hasRemaining()) {
      int b = bb.get() & 0xff;
      if (isUnreserved(b) || b == e) {
        sb.append((char) b);
      } else {
        appendEscape(sb, (byte) b);
      }
    }
    return sb.toString();
  }

  /**
   * Encodes a string containing non ASCII characters using an UTF-8 encoder.
   *
   * @param s The string the encode (assuming ASCII characters only)
   */
  private static String minimalEncodeUTF8(String s) {
    String n = (Normalizer.isNormalized(s, Form.NFKC)) ? s : Normalizer.normalize(s, Form.NFKC);
    // convert String to UTF-8
    ByteBuffer bb = StandardCharsets.UTF_8.encode(n);
    // URI encode
    StringBuilder sb = new StringBuilder();
    while (bb.hasRemaining()) {
      int b = bb.get() & 0xff;
      if (isLegal(b)) {
        sb.append((char) b);
      } else {
        appendEscape(sb, (byte) b);
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
   * any allocation. Malformed {@code %} sequences (fewer than two following hex digits) are
   * silently dropped.
   *
   * @param s The string to decode.
   *
   * @return The decoded string.
   */
  public static String decode(String s) {
    if (s.isEmpty() || (s.indexOf('%') < 0 && s.indexOf('+') < 0))
      return s;
    // Check whether we need to convert to UTF-8 encoder
    boolean ascii = isEncodedASCII(s);
    return ascii ? decodeASCII(s) : decodeUTF8(s);
  }

  /**
   * Decodes a string containing only ASCII characters.
   */
  private static String decodeASCII(String s) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '%') {
        if (i < s.length() - 2) {
          String hex = String.copyValueOf(new char[] { s.charAt(++i), s.charAt(++i) });
          char x = (char) Integer.parseInt(hex, 16);
          sb.append(x);
        }
        // TODO: handle error condition
      } else if (c == '+') {
        sb.append(' ');
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  /**
   * Decodes a string containing non ASCII characters using an UTF-8 decoder.
   */
  private static String decodeUTF8(String s) {
    // URI decode
    ByteBuffer bb = ByteBuffer.allocate(s.length());
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '%') {
        if (i < s.length() - 2) {
          String hex = "" + s.charAt(++i) + s.charAt(++i);
          byte b = (byte) (Integer.parseInt(hex, 16));
          bb.put(b);
        }
      } else if (c == '+') {
        bb.put((byte)' ');
      } else {
        // TODO: could there be also non-ASCII characters that should have been encoded?
        bb.put((byte) c);
      }
    }
    bb.limit(bb.position());
    bb.position(0);
    return StandardCharsets.UTF_8.decode(bb).toString();
  }

  /**
   * Appends the escape sequence for the given byte to the specified string buffer.
   *
   * @param sb The string buffer.
   * @param b The byte to escape.
   */
  private static void appendEscape(StringBuilder sb, byte b) {
    sb.append('%');
    sb.append(HEX_DIGITS[(b >> 4) & 0x0f]);
    sb.append(HEX_DIGITS[(b) & 0x0f]);
  }

  /**
   * Appends the escape sequence for the given byte to the specified string buffer.
   *
   * @param sb The string buffer.
   * @param c The char to escape.
   */
  private static void appendEscape(StringBuilder sb, char c) {
    sb.append('%');
    sb.append(HEX_DIGITS[(c >> 4) & 0x0f]);
    sb.append(HEX_DIGITS[(c) & 0x0f]);
  }

  /**
   * Indicates whether the character is unreserved of not.
   *
   * @param c The character to test.
   *
   * @return <code>true</code> if it is unreserved; <code>false</code> otherwise.
   */
  private static boolean isUnreserved(int c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
        || c == '.' || c == '_' || c == '-' || c == '~';
  }

  /**
   * Indicates whether the character is unreserved of not.
   *
   * @param c The character to test.
   *
   * @return <code>true</code> if it is unreserved; <code>false</code> otherwise.
   */
  private static boolean isLegal(int c) {
    if (c < '&') return c == '!' || c == '#' || c == '$';
    if (c >= '{') return c == '~';
    return c != '`' && c != '<' && c != '>' && c != '\\' && c != '^';
  }

  /**
   * Indicates whether the string contains non-ASCII characters.
   */
  private static boolean isASCII(String s) {
    for (int i = 0; i < s.length(); i++) {
      if (s.charAt(i) >= 0x80)
        return false;
    }
    return true;
  }

  /**
   * Indicates whether the encoded string contains non-ASCII characters.
   */
  private static boolean isEncodedASCII(String s) {
    for (int i = 0; i < s.length(); i++) {
      if (s.charAt(i) == '%' && i < s.length() - 1 && s.charAt(i + 1) > '7')
        return false;
    }
    return true;
  }

}
