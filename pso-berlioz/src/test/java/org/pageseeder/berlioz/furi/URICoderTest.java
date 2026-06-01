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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Tests for {@link URICoder}, including a section comparing its behaviour to
 * {@link java.net.URLEncoder}.
 *
 * <h2>URICoder vs URLEncoder — key differences</h2>
 * <ul>
 *   <li><b>Space</b>: URICoder encodes as {@code %20}; URLEncoder encodes as {@code +}.</li>
 *   <li><b>Tilde {@code ~}</b>: URICoder leaves it unencoded (RFC 3986 unreserved);
 *       URLEncoder encodes it as {@code %7E}.</li>
 *   <li><b>Asterisk {@code *}</b>: URICoder encodes as {@code %2A} (not RFC 3986 unreserved);
 *       URLEncoder leaves it as {@code *}.</li>
 *   <li><b>Unicode normalisation</b>: URICoder applies NFKC before encoding;
 *       URLEncoder applies no normalisation.</li>
 *   <li><b>Charset</b>: URICoder always uses UTF-8, no parameter needed;
 *       URLEncoder requires an explicit charset.</li>
 *   <li><b>Purpose</b>: URICoder targets RFC 3986 URI template variables;
 *       URLEncoder targets {@code application/x-www-form-urlencoded} (HTML forms).</li>
 *   <li><b>minimalEncode</b>: URICoder offers a second mode that preserves all RFC 3986 reserved
 *       characters ({@code /}, {@code ?}, {@code #}, {@code @}, etc.) and only encodes characters
 *       that are outright illegal in a URI. URLEncoder has no equivalent.</li>
 * </ul>
 *
 * @see <a href="http://tools.ietf.org/html/rfc3986#appendix-A">RFC 3986 Appendix A</a>
 */
final class URICoderTest {

  /** ALPHA characters as defined in RFC 3986. */
  private static final String ALPHA = range('a', 'z') + range('A', 'Z');

  /** DIGIT characters as defined in RFC 3986. */
  private static final String DIGIT = range('0', '9');

  /** Unreserved punctuation as defined in RFC 3986. */
  private static final String PUNC = "-_.~";

  /** All unreserved characters as defined in RFC 3986. */
  private static final String UNRESERVED = ALPHA + DIGIT + PUNC;

  // ---------------------------------------------------------------------------
  // encode(String) — basic contract
  // ---------------------------------------------------------------------------

  @Test
  void testEncode_EmptyString() {
    Assertions.assertEquals("", URICoder.encode(""));
  }

  @Test
  void testEncode_Unreserved_Alpha() {
    Assertions.assertEquals(ALPHA, URICoder.encode(ALPHA));
  }

  @Test
  void testEncode_Unreserved_Digit() {
    Assertions.assertEquals(DIGIT, URICoder.encode(DIGIT));
  }

  @Test
  void testEncode_Unreserved_Punctuation() {
    // hyphen, dot, underscore, tilde are all unreserved per RFC 3986
    Assertions.assertEquals("-_.~", URICoder.encode("-_.~"));
  }

  @Test
  void testEncode_AllASCII() {
    // Every ASCII char is either passed through (if unreserved) or percent-encoded uppercase
    for (char c = 0; c < 0x80; c++) {
      String s = String.valueOf(c);
      String enc = URICoder.encode(s);
      if (UNRESERVED.indexOf(c) >= 0) {
        Assertions.assertEquals(s, enc, "char 0x" + Integer.toHexString(c) + " should pass through");
      } else {
        String hex = Integer.toHexString(c);
        if (hex.length() == 1) hex = "0" + hex;
        Assertions.assertEquals("%" + hex.toUpperCase(), enc, "char 0x" + Integer.toHexString(c) + " should be percent-encoded");
      }
    }
  }

  @Test
  void testEncode_UppercaseHex() {
    // Percent-encoding must use uppercase hex digits (RFC 3986 §2.1 recommends uppercase)
    Assertions.assertEquals("%2F", URICoder.encode("/"));
    Assertions.assertEquals("%3F", URICoder.encode("?"));
    Assertions.assertEquals("%23", URICoder.encode("#"));
    Assertions.assertEquals("%20", URICoder.encode(" "));
  }

  @Test
  void testEncode_Space_As_Percent20() {
    // Spaces must become %20, not + (which is a form-encoding convention)
    Assertions.assertEquals("%20", URICoder.encode(" "));
    Assertions.assertEquals("hello%20world", URICoder.encode("hello world"));
  }

  @Test
  void testEncode_Plus_Encoded() {
    // + is a reserved char in URIs and must be encoded
    Assertions.assertEquals("%2B", URICoder.encode("+"));
  }

  @Test
  void testEncode_Slash_Encoded() {
    Assertions.assertEquals("%2F", URICoder.encode("/"));
    Assertions.assertEquals("a%2Fb", URICoder.encode("a/b"));
  }

  @Test
  void testEncode_ReservedChars_AllEncoded() {
    // RFC 3986 gen-delims: : / ? # [ ] @
    Assertions.assertEquals("%3A", URICoder.encode(":"));
    Assertions.assertEquals("%2F", URICoder.encode("/"));
    Assertions.assertEquals("%3F", URICoder.encode("?"));
    Assertions.assertEquals("%23", URICoder.encode("#"));
    Assertions.assertEquals("%5B", URICoder.encode("["));
    Assertions.assertEquals("%5D", URICoder.encode("]"));
    Assertions.assertEquals("%40", URICoder.encode("@"));
    // RFC 3986 sub-delims: ! $ & ' ( ) * + , ; =
    Assertions.assertEquals("%21", URICoder.encode("!"));
    Assertions.assertEquals("%24", URICoder.encode("$"));
    Assertions.assertEquals("%26", URICoder.encode("&"));
    Assertions.assertEquals("%27", URICoder.encode("'"));
    Assertions.assertEquals("%28", URICoder.encode("("));
    Assertions.assertEquals("%29", URICoder.encode(")"));
    Assertions.assertEquals("%2A", URICoder.encode("*"));
    Assertions.assertEquals("%2B", URICoder.encode("+"));
    Assertions.assertEquals("%2C", URICoder.encode(","));
    Assertions.assertEquals("%3B", URICoder.encode(";"));
    Assertions.assertEquals("%3D", URICoder.encode("="));
  }

  @Test
  void testEncode_PercentSign_Encoded() {
    // A literal % in input must itself be encoded to avoid malformed sequences
    Assertions.assertEquals("%25", URICoder.encode("%"));
    Assertions.assertEquals("%2525", URICoder.encode("%25"));
  }

  @Test
  void testEncode_NonASCII_CafeAccent() {
    // é (U+00E9) → UTF-8 bytes 0xC3 0xA9 → %C3%A9
    Assertions.assertEquals("Caf%C3%A9", URICoder.encode("Café"));
  }

  @Test
  void testEncode_NonASCII_WithReservedChar() {
    Assertions.assertEquals("Caf%C3%A9%3F", URICoder.encode("Café?"));
  }

  @Test
  void testEncode_NonASCII_CJK() {
    // 中 (U+4E2D) → UTF-8 bytes 0xE4 0xB8 0xAD → %E4%B8%AD
    Assertions.assertEquals("%E4%B8%AD", URICoder.encode("中"));
  }

  @Test
  void testEncode_NonASCII_ArabicWord() {
    // م (U+0645) → UTF-8 bytes 0xD9 0x85
    Assertions.assertEquals("%D9%85", URICoder.encode("م"));
  }

  // ---------------------------------------------------------------------------
  // encode(String) — Unicode normalisation (NFKC)
  // ---------------------------------------------------------------------------

  @Test
  void testEncode_Normalization_GreekUpsilon() {
    // U+03D3 GREEK UPSILON WITH HOOK AND ACUTE (single codepoint)
    // U+03D2 + U+0301 (decomposed) — both normalise to the same NFKC form
    Assertions.assertEquals("%CE%8E", URICoder.encode("ϓ"));
    Assertions.assertEquals("%CE%8E", URICoder.encode("ϓ"));
  }

  @Test
  void testEncode_Normalization_DotAbove() {
    // Long-s (U+017F) + combining dot above (U+0307) → ṡ (U+1E61)
    // Regular s (U+0073) + combining dot above (U+0307) → same NFKC result
    Assertions.assertEquals("%E1%B9%A1", URICoder.encode("ẛ"));
    Assertions.assertEquals("%E1%B9%A1", URICoder.encode("ṡ"));
  }

  @Test
  void testEncode_Normalization_EquivalentFormsProduceSameOutput() {
    // NFC and NFD of "é" should both produce the same encoding
    String nfc = "é";           // é as precomposed
    String nfd = "é";          // e + combining acute accent
    Assertions.assertEquals(URICoder.encode(nfc), URICoder.encode(nfd));
  }

  // ---------------------------------------------------------------------------
  // encode(String, char) — extra character passthrough
  // ---------------------------------------------------------------------------

  @Test
  void testEncodeWithChar_EmptyString() {
    Assertions.assertEquals("", URICoder.encode("", '/'));
  }

  @Test
  void testEncodeWithChar_ExtraCharPassesThrough() {
    // The second argument is an extra char that should not be encoded
    Assertions.assertEquals("a/b/c", URICoder.encode("a/b/c", '/'));
    Assertions.assertEquals("a?b", URICoder.encode("a?b", '?'));
    Assertions.assertEquals("a@b", URICoder.encode("a@b", '@'));
  }

  @Test
  void testEncodeWithChar_OtherReservedCharsStillEncoded() {
    // Only the specified extra char bypasses encoding; others are still encoded
    Assertions.assertEquals("a/b%3Fc", URICoder.encode("a/b?c", '/'));
    Assertions.assertEquals("a%2Fb%3Fc", URICoder.encode("a/b?c", '@'));
  }

  @Test
  void testEncodeWithChar_UnreservedCharsAlwaysPassThrough() {
    Assertions.assertEquals("abc-._~", URICoder.encode("abc-._~", '/'));
  }

  @Test
  void testEncodeWithChar_NonASCII_WithSlash() {
    // Non-ASCII chars still get encoded even with the extra-char override
    Assertions.assertEquals("/Caf%C3%A9/menu", URICoder.encode("/Café/menu", '/'));
  }

  @Test
  void testEncodeWithChar_UnreservedChar_AsExtraChar_NoChange() {
    // Passing an unreserved char as the extra char has no effect since it's already unencoded
    Assertions.assertEquals("abc", URICoder.encode("abc", 'a'));
  }

  // ---------------------------------------------------------------------------
  // minimalEncode(String)
  // ---------------------------------------------------------------------------

  @Test
  void testMinimalEncode_EmptyString() {
    Assertions.assertEquals("", URICoder.minimalEncode(""));
  }

  @Test
  void testMinimalEncode_UnreservedChars_PassThrough() {
    Assertions.assertEquals(UNRESERVED, URICoder.minimalEncode(UNRESERVED));
  }

  @Test
  void testMinimalEncode_RFC3986ReservedChars_PassThrough() {
    // gen-delims and sub-delims are "legal" in a URI and must NOT be encoded by minimalEncode
    String reserved = ":/?#[]@!$&'()*+,;=";
    Assertions.assertEquals(reserved, URICoder.minimalEncode(reserved));
  }

  @Test
  void testMinimalEncode_Slash_PassThrough() {
    Assertions.assertEquals("a/b/c", URICoder.minimalEncode("a/b/c"));
  }

  @Test
  void testMinimalEncode_Query_PassThrough() {
    Assertions.assertEquals("/search?q=hello&lang=en", URICoder.minimalEncode("/search?q=hello&lang=en"));
  }

  @Test
  void testMinimalEncode_IllegalChars_Encoded() {
    // Characters that are illegal in any URI component must be encoded
    Assertions.assertEquals("%3C", URICoder.minimalEncode("<"));    // less-than
    Assertions.assertEquals("%3E", URICoder.minimalEncode(">"));    // greater-than
    Assertions.assertEquals("%5C", URICoder.minimalEncode("\\"));   // backslash
    Assertions.assertEquals("%5E", URICoder.minimalEncode("^"));    // caret
    Assertions.assertEquals("%60", URICoder.minimalEncode("`"));    // backtick
    Assertions.assertEquals("%7B", URICoder.minimalEncode("{"));    // left brace
    Assertions.assertEquals("%7C", URICoder.minimalEncode("|"));    // vertical bar
    Assertions.assertEquals("%7D", URICoder.minimalEncode("}"));    // right brace
  }

  @Test
  void testMinimalEncode_Space_Encoded() {
    Assertions.assertEquals("%20", URICoder.minimalEncode(" "));
  }

  @Test
  void testMinimalEncode_Percent_Encoded() {
    Assertions.assertEquals("%25", URICoder.minimalEncode("%"));
  }

  @Test
  void testMinimalEncode_DEL_Encoded() {
    Assertions.assertEquals("%7F", URICoder.minimalEncode(""));
  }

  @Test
  void testMinimalEncode_ControlChars_Encoded() {
    Assertions.assertEquals("%00", URICoder.minimalEncode(" "));
    Assertions.assertEquals("%01", URICoder.minimalEncode(""));
    Assertions.assertEquals("%1F", URICoder.minimalEncode(""));
  }

  @Test
  void testMinimalEncode_NonASCII_Encoded() {
    Assertions.assertEquals("%C3%A9", URICoder.minimalEncode("é"));
  }

  @Test
  void testMinimalEncode_Normalization() {
    // NFKC normalisation applies in minimalEncode just as in encode
    Assertions.assertEquals("%CE%8E", URICoder.minimalEncode("ϓ"));
    Assertions.assertEquals("%CE%8E", URICoder.minimalEncode("ϓ"));
  }

  @Test
  void testMinimalEncode_UrlWithIllegalChars() {
    // A URL-like string with both legal and illegal characters
    Assertions.assertEquals("/path/to?q=hello%3Cworld%3E", URICoder.minimalEncode("/path/to?q=hello<world>"));
  }

  @Test
  void testMinimalEncode_Vs_Encode_ReservedCharsAreDifferent() {
    // encode() encodes reserved chars; minimalEncode() does not
    String reserved = "/?#@";
    String encoded = URICoder.encode(reserved);
    String minEncoded = URICoder.minimalEncode(reserved);
    Assertions.assertNotEquals(encoded, minEncoded);
    Assertions.assertEquals(reserved, minEncoded);  // reserved chars preserved
    Assertions.assertEquals("%2F%3F%23%40", encoded); // all encoded
  }

  // ---------------------------------------------------------------------------
  // decode(String) — basic contract
  // ---------------------------------------------------------------------------

  @Test
  void testDecode_EmptyString() {
    Assertions.assertEquals("", URICoder.decode(""));
  }

  @Test
  void testDecode_StringWithoutPercentOrPlus_ReturnsSame() {
    // Fast-path: no decoding needed; input is returned unchanged
    String s = "hello-world_foo.bar~baz";
    Assertions.assertEquals(s, URICoder.decode(s));
  }

  @Test
  void testDecode_Unreserved_Unchanged() {
    Assertions.assertEquals(ALPHA, URICoder.decode(ALPHA));
    Assertions.assertEquals(DIGIT, URICoder.decode(DIGIT));
    Assertions.assertEquals(PUNC,  URICoder.decode(PUNC));
  }

  @Test
  void testDecode_Plus_DecodesToSpace() {
    Assertions.assertEquals(" ", URICoder.decode("+"));
  }

  @Test
  void testDecode_Percent20_DecodesToSpace() {
    Assertions.assertEquals(" ", URICoder.decode("%20"));
  }

  @Test
  void testDecode_Space_BothRepresentations() {
    Assertions.assertEquals("Café $1", URICoder.decode("Caf%C3%A9+$1"));
  }

  @Test
  void testDecode_PercentEncoded_Slash() {
    Assertions.assertEquals("/", URICoder.decode("%2F"));
    Assertions.assertEquals("a/b", URICoder.decode("a%2Fb"));
  }

  @Test
  void testDecode_LowercaseHex_Accepted() {
    // Decoders must accept both uppercase and lowercase hex digits per RFC 3986 §2.1
    Assertions.assertEquals("/", URICoder.decode("%2f"));
    Assertions.assertEquals("?", URICoder.decode("%3f"));
  }

  @Test
  void testDecode_MixedCaseHex_Accepted() {
    Assertions.assertEquals("Café", URICoder.decode("Caf%c3%a9"));
    Assertions.assertEquals("Café", URICoder.decode("Caf%C3%A9"));
  }

  @Test
  void testDecode_PercentB_DecodesCorrectly() {
    // %2B is + (should not become space, which + does)
    Assertions.assertEquals("+", URICoder.decode("%2B"));
    Assertions.assertEquals("+", URICoder.decode("%2b"));
  }

  @Test
  void testDecode_AllASCII_RoundTrip() {
    // encode then decode must recover the original char for every ASCII input
    for (char c = 0; c < 0x80; c++) {
      String original = String.valueOf(c);
      String encoded = URICoder.encode(original);
      String decoded = URICoder.decode(encoded);
      Assertions.assertEquals(original, decoded, "round-trip failed for char 0x" + Integer.toHexString(c));
    }
  }

  @Test
  void testDecode_NonASCII_CafeAccent() {
    Assertions.assertEquals("Café", URICoder.decode("Caf%C3%A9"));
    Assertions.assertEquals("Café?", URICoder.decode("Caf%C3%A9%3F"));
  }

  @Test
  void testDecode_NonASCII_CJK() {
    Assertions.assertEquals("中", URICoder.decode("%E4%B8%AD"));
  }

  @Test
  void testDecode_NonASCII_RoundTrip() {
    String original = "Café — menu";
    String encoded = URICoder.encode(original);
    Assertions.assertEquals(original, URICoder.decode(encoded));
  }

  @Test
  void testDecode_IncompletePercent_AtEnd_Ignored() {
    // A % at the very end of the string has no following hex digits; it is silently dropped
    Assertions.assertEquals("abc", URICoder.decode("abc%"));
  }

  @Test
  void testDecode_IncompletePercent_OneDigit_PercentDroppedTrailingKept() {
    // When % has only one following hex digit (i.e., at the second-to-last position),
    // the % is silently dropped but the trailing char is kept as a literal.
    Assertions.assertEquals("abc2", URICoder.decode("abc%2"));
  }

  @Test
  void testDecode_IncompletePercent_MidString() {
    // Valid sequences around the malformed one are still decoded
    Assertions.assertEquals("a/c", URICoder.decode("a%2Fc"));
  }

  @Test
  void testDecode_NullDecodeDoesNotOccur_PlusInMiddle() {
    Assertions.assertEquals("hello world today", URICoder.decode("hello+world+today"));
  }

  // ---------------------------------------------------------------------------
  // URICoder vs java.net.URLEncoder — documented behavioural differences
  // ---------------------------------------------------------------------------

  @Test
  void testVsURLEncoder_Space_EncodedDifferently() {
    String urlEncoded = URLEncoder.encode(" ", StandardCharsets.UTF_8);
    String uriEncoded = URICoder.encode(" ");
    Assertions.assertEquals("+", urlEncoded); // form-data convention
    Assertions.assertEquals("%20", uriEncoded); // RFC 3986 percent-encoding
    Assertions.assertNotEquals(urlEncoded, uriEncoded);
  }

  @Test
  void testVsURLEncoder_Tilde_EncodedDifferently() {
    // URLEncoder encodes ~ because it predates RFC 3986 recognising ~ as unreserved
    String urlEncoded = URLEncoder.encode("~", StandardCharsets.UTF_8);
    String uriEncoded = URICoder.encode("~");
    Assertions.assertEquals("%7E", urlEncoded); // URLEncoder encodes tilde
    Assertions.assertEquals("~", uriEncoded); // URICoder leaves tilde (RFC 3986 unreserved)
    Assertions.assertNotEquals(urlEncoded, uriEncoded);
  }

  @Test
  void testVsURLEncoder_Asterisk_EncodedDifferently() {
    // URLEncoder treats * as safe (legacy); URICoder encodes it (not RFC 3986 unreserved)
    String urlEncoded = URLEncoder.encode("*", StandardCharsets.UTF_8);
    String uriEncoded = URICoder.encode("*");
    Assertions.assertEquals("*", urlEncoded); // URLEncoder leaves asterisk
    Assertions.assertEquals("%2A", uriEncoded); // URICoder percent-encodes it
    Assertions.assertNotEquals(urlEncoded, uriEncoded);
  }

  @Test
  void testVsURLEncoder_Plus_EncodedSame() {
    // Both encode + as %2B (URLEncoder: + means space, so literal + must be encoded)
    Assertions.assertEquals(URLEncoder.encode("+", StandardCharsets.UTF_8), URICoder.encode("+"));
    Assertions.assertEquals("%2B", URICoder.encode("+"));
  }

  @Test
  void testVsURLEncoder_AlphaDigit_EncodedSame() {
    // Both leave letters and digits unencoded
    Assertions.assertEquals(URLEncoder.encode(ALPHA, StandardCharsets.UTF_8), URICoder.encode(ALPHA));
    Assertions.assertEquals(URLEncoder.encode(DIGIT, StandardCharsets.UTF_8), URICoder.encode(DIGIT));
  }

  @Test
  void testVsURLEncoder_Hyphen_Dot_Underscore_EncodedSame() {
    // Both leave - . _ unencoded (all are unreserved in both RFC 3986 and form-encoding)
    Assertions.assertEquals(URLEncoder.encode("-._", StandardCharsets.UTF_8), URICoder.encode("-._"));
  }

  @Test
  void testVsURLEncoder_NonASCII_SameBytes_DifferentSpaceEncoding() {
    // For non-ASCII content both use UTF-8 bytes, but space handling differs
    String urlEncoded = URLEncoder.encode("Café", StandardCharsets.UTF_8);
    String uriEncoded = URICoder.encode("Café");
    Assertions.assertEquals("Caf%C3%A9", urlEncoded);
    Assertions.assertEquals("Caf%C3%A9", uriEncoded);
  }

  @Test
  void testVsURLEncoder_UnicodeNormalisation_DifferenceMayAppear() {
    // NFC and NFD of "é": URLEncoder encodes the raw bytes without normalisation,
    // so NFC and NFD inputs produce different byte sequences.
    // URICoder normalises to NFKC first, so both inputs produce the same output.
    String nfc = "é";      // precomposed é
    String nfd = "é";     // e + combining acute

    String urlNfc = URLEncoder.encode(nfc, StandardCharsets.UTF_8);
    String urlNfd = URLEncoder.encode(nfd, StandardCharsets.UTF_8);
    Assertions.assertNotEquals(urlNfc, urlNfd, "URLEncoder: NFC and NFD differ");

    String uriNfc = URICoder.encode(nfc);
    String uriNfd = URICoder.encode(nfd);
    Assertions.assertEquals(uriNfc, uriNfd, "URICoder: NFC and NFD normalise to same output");
  }

  @Test
  void testVsURLEncoder_NoEquivalent_MinimalEncode() {
    // URLEncoder has no equivalent to minimalEncode: it always encodes reserved chars.
    String path = "/search?q=hello world";
    String urlEncoded = URLEncoder.encode(path, StandardCharsets.UTF_8);
    String minEncoded = URICoder.minimalEncode(path);

    // URLEncoder encodes / ? = and space
    Assertions.assertEquals("%2Fsearch%3Fq%3Dhello+world", urlEncoded);
    // minimalEncode preserves / ? = and encodes space as %20
    Assertions.assertEquals("/search?q=hello%20world", minEncoded);
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private static String range(int from, int to) {
    StringBuilder sb = new StringBuilder(to - from + 1);
    for (int i = from; i <= to; i++) sb.append((char) i);
    return sb.toString();
  }

}
