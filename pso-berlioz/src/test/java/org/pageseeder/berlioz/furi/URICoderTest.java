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
public final class URICoderTest {

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
  public void testEncode_EmptyString() {
    Assertions.assertEquals(URICoder.encode(""), "");
  }

  @Test
  public void testEncode_Unreserved_Alpha() {
    Assertions.assertEquals(ALPHA, URICoder.encode(ALPHA));
  }

  @Test
  public void testEncode_Unreserved_Digit() {
    Assertions.assertEquals(DIGIT, URICoder.encode(DIGIT));
  }

  @Test
  public void testEncode_Unreserved_Punctuation() {
    // hyphen, dot, underscore, tilde are all unreserved per RFC 3986
    Assertions.assertEquals(URICoder.encode("-_.~"), "-_.~");
  }

  @Test
  public void testEncode_AllASCII() {
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
  public void testEncode_UppercaseHex() {
    // Percent-encoding must use uppercase hex digits (RFC 3986 §2.1 recommends uppercase)
    Assertions.assertEquals(URICoder.encode("/"), "%2F");
    Assertions.assertEquals(URICoder.encode("?"), "%3F");
    Assertions.assertEquals(URICoder.encode("#"), "%23");
    Assertions.assertEquals(URICoder.encode(" "), "%20");
  }

  @Test
  public void testEncode_Space_As_Percent20() {
    // Spaces must become %20, not + (which is a form-encoding convention)
    Assertions.assertEquals(URICoder.encode(" "), "%20");
    Assertions.assertEquals(URICoder.encode("hello world"), "hello%20world");
  }

  @Test
  public void testEncode_Plus_Encoded() {
    // + is a reserved char in URIs and must be encoded
    Assertions.assertEquals(URICoder.encode("+"), "%2B");
  }

  @Test
  public void testEncode_Slash_Encoded() {
    Assertions.assertEquals(URICoder.encode("/"), "%2F");
    Assertions.assertEquals(URICoder.encode("a/b"), "a%2Fb");
  }

  @Test
  public void testEncode_ReservedChars_AllEncoded() {
    // RFC 3986 gen-delims: : / ? # [ ] @
    Assertions.assertEquals(URICoder.encode(":"), "%3A");
    Assertions.assertEquals(URICoder.encode("/"), "%2F");
    Assertions.assertEquals(URICoder.encode("?"), "%3F");
    Assertions.assertEquals(URICoder.encode("#"), "%23");
    Assertions.assertEquals(URICoder.encode("["), "%5B");
    Assertions.assertEquals(URICoder.encode("]"), "%5D");
    Assertions.assertEquals(URICoder.encode("@"), "%40");
    // RFC 3986 sub-delims: ! $ & ' ( ) * + , ; =
    Assertions.assertEquals(URICoder.encode("!"), "%21");
    Assertions.assertEquals(URICoder.encode("$"), "%24");
    Assertions.assertEquals(URICoder.encode("&"), "%26");
    Assertions.assertEquals(URICoder.encode("'"), "%27");
    Assertions.assertEquals(URICoder.encode("("), "%28");
    Assertions.assertEquals(URICoder.encode(")"), "%29");
    Assertions.assertEquals(URICoder.encode("*"), "%2A");
    Assertions.assertEquals(URICoder.encode("+"), "%2B");
    Assertions.assertEquals(URICoder.encode(","), "%2C");
    Assertions.assertEquals(URICoder.encode(";"), "%3B");
    Assertions.assertEquals(URICoder.encode("="), "%3D");
  }

  @Test
  public void testEncode_PercentSign_Encoded() {
    // A literal % in input must itself be encoded to avoid malformed sequences
    Assertions.assertEquals(URICoder.encode("%"), "%25");
    Assertions.assertEquals(URICoder.encode("%25"), "%2525");
  }

  @Test
  public void testEncode_NonASCII_CafeAccent() {
    // é (U+00E9) → UTF-8 bytes 0xC3 0xA9 → %C3%A9
    Assertions.assertEquals(URICoder.encode("Café"), "Caf%C3%A9");
  }

  @Test
  public void testEncode_NonASCII_WithReservedChar() {
    Assertions.assertEquals(URICoder.encode("Café?"), "Caf%C3%A9%3F");
  }

  @Test
  public void testEncode_NonASCII_CJK() {
    // 中 (U+4E2D) → UTF-8 bytes 0xE4 0xB8 0xAD → %E4%B8%AD
    Assertions.assertEquals(URICoder.encode("中"), "%E4%B8%AD");
  }

  @Test
  public void testEncode_NonASCII_ArabicWord() {
    // م (U+0645) → UTF-8 bytes 0xD9 0x85
    Assertions.assertEquals(URICoder.encode("م"), "%D9%85");
  }

  // ---------------------------------------------------------------------------
  // encode(String) — Unicode normalisation (NFKC)
  // ---------------------------------------------------------------------------

  @Test
  public void testEncode_Normalization_GreekUpsilon() {
    // U+03D3 GREEK UPSILON WITH HOOK AND ACUTE (single codepoint)
    // U+03D2 + U+0301 (decomposed) — both normalise to the same NFKC form
    Assertions.assertEquals(URICoder.encode("ϓ"), "%CE%8E");
    Assertions.assertEquals(URICoder.encode("ϓ"), "%CE%8E");
  }

  @Test
  public void testEncode_Normalization_DotAbove() {
    // Long-s (U+017F) + combining dot above (U+0307) → ṡ (U+1E61)
    // Regular s (U+0073) + combining dot above (U+0307) → same NFKC result
    Assertions.assertEquals(URICoder.encode("ẛ"), "%E1%B9%A1");
    Assertions.assertEquals(URICoder.encode("ṡ"), "%E1%B9%A1");
  }

  @Test
  public void testEncode_Normalization_EquivalentFormsProduceSameOutput() {
    // NFC and NFD of "é" should both produce the same encoding
    String nfc = "é";           // é as precomposed
    String nfd = "é";          // e + combining acute accent
    Assertions.assertEquals(URICoder.encode(nfc), URICoder.encode(nfd));
  }

  // ---------------------------------------------------------------------------
  // encode(String, char) — extra character passthrough
  // ---------------------------------------------------------------------------

  @Test
  public void testEncodeWithChar_EmptyString() {
    Assertions.assertEquals(URICoder.encode("", '/'), "");
  }

  @Test
  public void testEncodeWithChar_ExtraCharPassesThrough() {
    // The second argument is an extra char that should not be encoded
    Assertions.assertEquals(URICoder.encode("a/b/c", '/'), "a/b/c");
    Assertions.assertEquals(URICoder.encode("a?b", '?'), "a?b");
    Assertions.assertEquals(URICoder.encode("a@b", '@'), "a@b");
  }

  @Test
  public void testEncodeWithChar_OtherReservedCharsStillEncoded() {
    // Only the specified extra char bypasses encoding; others are still encoded
    Assertions.assertEquals(URICoder.encode("a/b?c", '/'), "a/b%3Fc");
    Assertions.assertEquals(URICoder.encode("a/b?c", '@'), "a%2Fb%3Fc");
  }

  @Test
  public void testEncodeWithChar_UnreservedCharsAlwaysPassThrough() {
    Assertions.assertEquals(URICoder.encode("abc-._~", '/'), "abc-._~");
  }

  @Test
  public void testEncodeWithChar_NonASCII_WithSlash() {
    // Non-ASCII chars still get encoded even with the extra-char override
    Assertions.assertEquals(URICoder.encode("/Café/menu", '/'), "/Caf%C3%A9/menu");
  }

  @Test
  public void testEncodeWithChar_UnreservedChar_AsExtraChar_NoChange() {
    // Passing an unreserved char as the extra char has no effect since it's already unencoded
    Assertions.assertEquals(URICoder.encode("abc", 'a'), "abc");
  }

  // ---------------------------------------------------------------------------
  // minimalEncode(String)
  // ---------------------------------------------------------------------------

  @Test
  public void testMinimalEncode_EmptyString() {
    Assertions.assertEquals(URICoder.minimalEncode(""), "");
  }

  @Test
  public void testMinimalEncode_UnreservedChars_PassThrough() {
    Assertions.assertEquals(UNRESERVED, URICoder.minimalEncode(UNRESERVED));
  }

  @Test
  public void testMinimalEncode_RFC3986ReservedChars_PassThrough() {
    // gen-delims and sub-delims are "legal" in a URI and must NOT be encoded by minimalEncode
    String reserved = ":/?#[]@!$&'()*+,;=";
    Assertions.assertEquals(reserved, URICoder.minimalEncode(reserved));
  }

  @Test
  public void testMinimalEncode_Slash_PassThrough() {
    Assertions.assertEquals(URICoder.minimalEncode("a/b/c"), "a/b/c");
  }

  @Test
  public void testMinimalEncode_Query_PassThrough() {
    Assertions.assertEquals(URICoder.minimalEncode("/search?q=hello&lang=en"), "/search?q=hello&lang=en");
  }

  @Test
  public void testMinimalEncode_IllegalChars_Encoded() {
    // Characters that are illegal in any URI component must be encoded
    Assertions.assertEquals(URICoder.minimalEncode("<"), "%3C");    // less-than
    Assertions.assertEquals(URICoder.minimalEncode(">"), "%3E");    // greater-than
    Assertions.assertEquals(URICoder.minimalEncode("\\"), "%5C");   // backslash
    Assertions.assertEquals(URICoder.minimalEncode("^"), "%5E");    // caret
    Assertions.assertEquals(URICoder.minimalEncode("`"), "%60");    // backtick
    Assertions.assertEquals(URICoder.minimalEncode("{"), "%7B");    // left brace
    Assertions.assertEquals(URICoder.minimalEncode("|"), "%7C");    // vertical bar
    Assertions.assertEquals(URICoder.minimalEncode("}"), "%7D");    // right brace
  }

  @Test
  public void testMinimalEncode_Space_Encoded() {
    Assertions.assertEquals(URICoder.minimalEncode(" "), "%20");
  }

  @Test
  public void testMinimalEncode_Percent_Encoded() {
    Assertions.assertEquals(URICoder.minimalEncode("%"), "%25");
  }

  @Test
  public void testMinimalEncode_DEL_Encoded() {
    Assertions.assertEquals(URICoder.minimalEncode(""), "%7F");
  }

  @Test
  public void testMinimalEncode_ControlChars_Encoded() {
    Assertions.assertEquals(URICoder.minimalEncode(" "), "%00");
    Assertions.assertEquals(URICoder.minimalEncode(""), "%01");
    Assertions.assertEquals(URICoder.minimalEncode(""), "%1F");
  }

  @Test
  public void testMinimalEncode_NonASCII_Encoded() {
    Assertions.assertEquals(URICoder.minimalEncode("é"), "%C3%A9");
  }

  @Test
  public void testMinimalEncode_Normalization() {
    // NFKC normalisation applies in minimalEncode just as in encode
    Assertions.assertEquals(URICoder.minimalEncode("ϓ"), "%CE%8E");
    Assertions.assertEquals(URICoder.minimalEncode("ϓ"), "%CE%8E");
  }

  @Test
  public void testMinimalEncode_UrlWithIllegalChars() {
    // A URL-like string with both legal and illegal characters
    Assertions.assertEquals(URICoder.minimalEncode("/path/to?q=hello<world>"), "/path/to?q=hello%3Cworld%3E");
  }

  @Test
  public void testMinimalEncode_Vs_Encode_ReservedCharsAreDifferent() {
    // encode() encodes reserved chars; minimalEncode() does not
    String reserved = "/?#@";
    String encoded = URICoder.encode(reserved);
    String minEncoded = URICoder.minimalEncode(reserved);
    Assertions.assertNotEquals(encoded, minEncoded);
    Assertions.assertEquals(reserved, minEncoded);  // reserved chars preserved
    Assertions.assertEquals(encoded, "%2F%3F%23%40"); // all encoded
  }

  // ---------------------------------------------------------------------------
  // decode(String) — basic contract
  // ---------------------------------------------------------------------------

  @Test
  public void testDecode_EmptyString() {
    Assertions.assertEquals(URICoder.decode(""), "");
  }

  @Test
  public void testDecode_StringWithoutPercentOrPlus_ReturnsSame() {
    // Fast-path: no decoding needed; input is returned unchanged
    String s = "hello-world_foo.bar~baz";
    Assertions.assertEquals(s, URICoder.decode(s));
  }

  @Test
  public void testDecode_Unreserved_Unchanged() {
    Assertions.assertEquals(ALPHA, URICoder.decode(ALPHA));
    Assertions.assertEquals(DIGIT, URICoder.decode(DIGIT));
    Assertions.assertEquals(PUNC,  URICoder.decode(PUNC));
  }

  @Test
  public void testDecode_Plus_DecodesToSpace() {
    Assertions.assertEquals(URICoder.decode("+"), " ");
  }

  @Test
  public void testDecode_Percent20_DecodesToSpace() {
    Assertions.assertEquals(URICoder.decode("%20"), " ");
  }

  @Test
  public void testDecode_Space_BothRepresentations() {
    Assertions.assertEquals(URICoder.decode("Caf%C3%A9+$1"), "Café $1");
  }

  @Test
  public void testDecode_PercentEncoded_Slash() {
    Assertions.assertEquals(URICoder.decode("%2F"), "/");
    Assertions.assertEquals(URICoder.decode("a%2Fb"), "a/b");
  }

  @Test
  public void testDecode_LowercaseHex_Accepted() {
    // Decoders must accept both uppercase and lowercase hex digits per RFC 3986 §2.1
    Assertions.assertEquals(URICoder.decode("%2f"), "/");
    Assertions.assertEquals(URICoder.decode("%3f"), "?");
  }

  @Test
  public void testDecode_MixedCaseHex_Accepted() {
    Assertions.assertEquals(URICoder.decode("Caf%c3%a9"), "Café");
    Assertions.assertEquals(URICoder.decode("Caf%C3%A9"), "Café");
  }

  @Test
  public void testDecode_PercentB_DecodesCorrectly() {
    // %2B is + (should not become space, which + does)
    Assertions.assertEquals(URICoder.decode("%2B"), "+");
    Assertions.assertEquals(URICoder.decode("%2b"), "+");
  }

  @Test
  public void testDecode_AllASCII_RoundTrip() {
    // encode then decode must recover the original char for every ASCII input
    for (char c = 0; c < 0x80; c++) {
      String original = String.valueOf(c);
      String encoded = URICoder.encode(original);
      String decoded = URICoder.decode(encoded);
      Assertions.assertEquals(original, decoded, "round-trip failed for char 0x" + Integer.toHexString(c));
    }
  }

  @Test
  public void testDecode_NonASCII_CafeAccent() {
    Assertions.assertEquals(URICoder.decode("Caf%C3%A9"), "Café");
    Assertions.assertEquals(URICoder.decode("Caf%C3%A9%3F"), "Café?");
  }

  @Test
  public void testDecode_NonASCII_CJK() {
    Assertions.assertEquals(URICoder.decode("%E4%B8%AD"), "中");
  }

  @Test
  public void testDecode_NonASCII_RoundTrip() {
    String original = "Café — menu";
    String encoded = URICoder.encode(original);
    Assertions.assertEquals(original, URICoder.decode(encoded));
  }

  @Test
  public void testDecode_IncompletePercent_AtEnd_Ignored() {
    // A % at the very end of the string has no following hex digits; it is silently dropped
    Assertions.assertEquals(URICoder.decode("abc%"), "abc");
  }

  @Test
  public void testDecode_IncompletePercent_OneDigit_PercentDroppedTrailingKept() {
    // When % has only one following hex digit (i.e., at the second-to-last position),
    // the % is silently dropped but the trailing char is kept as a literal.
    Assertions.assertEquals(URICoder.decode("abc%2"), "abc2");
  }

  @Test
  public void testDecode_IncompletePercent_MidString() {
    // Valid sequences around the malformed one are still decoded
    Assertions.assertEquals(URICoder.decode("a%2Fc"), "a/c");
  }

  @Test
  public void testDecode_NullDecodeDoesNotOccur_PlusInMiddle() {
    Assertions.assertEquals(URICoder.decode("hello+world+today"), "hello world today");
  }

  // ---------------------------------------------------------------------------
  // URICoder vs java.net.URLEncoder — documented behavioural differences
  // ---------------------------------------------------------------------------

  @Test
  public void testVsURLEncoder_Space_EncodedDifferently() {
    String urlEncoded = URLEncoder.encode(" ", StandardCharsets.UTF_8);
    String uriEncoded = URICoder.encode(" ");
    Assertions.assertEquals(urlEncoded, "+"); // form-data convention
    Assertions.assertEquals(uriEncoded, "%20"); // RFC 3986 percent-encoding
    Assertions.assertNotEquals(urlEncoded, uriEncoded);
  }

  @Test
  public void testVsURLEncoder_Tilde_EncodedDifferently() {
    // URLEncoder encodes ~ because it predates RFC 3986 recognising ~ as unreserved
    String urlEncoded = URLEncoder.encode("~", StandardCharsets.UTF_8);
    String uriEncoded = URICoder.encode("~");
    Assertions.assertEquals(urlEncoded, "%7E"); // URLEncoder encodes tilde
    Assertions.assertEquals(uriEncoded, "~"); // URICoder leaves tilde (RFC 3986 unreserved)
    Assertions.assertNotEquals(urlEncoded, uriEncoded);
  }

  @Test
  public void testVsURLEncoder_Asterisk_EncodedDifferently() {
    // URLEncoder treats * as safe (legacy); URICoder encodes it (not RFC 3986 unreserved)
    String urlEncoded = URLEncoder.encode("*", StandardCharsets.UTF_8);
    String uriEncoded = URICoder.encode("*");
    Assertions.assertEquals(urlEncoded, "*"); // URLEncoder leaves asterisk
    Assertions.assertEquals(uriEncoded, "%2A"); // URICoder percent-encodes it
    Assertions.assertNotEquals(urlEncoded, uriEncoded);
  }

  @Test
  public void testVsURLEncoder_Plus_EncodedSame() {
    // Both encode + as %2B (URLEncoder: + means space, so literal + must be encoded)
    Assertions.assertEquals(URLEncoder.encode("+", StandardCharsets.UTF_8), URICoder.encode("+"));
    Assertions.assertEquals(URICoder.encode("+"), "%2B");
  }

  @Test
  public void testVsURLEncoder_AlphaDigit_EncodedSame() {
    // Both leave letters and digits unencoded
    Assertions.assertEquals(URLEncoder.encode(ALPHA, StandardCharsets.UTF_8), URICoder.encode(ALPHA));
    Assertions.assertEquals(URLEncoder.encode(DIGIT, StandardCharsets.UTF_8), URICoder.encode(DIGIT));
  }

  @Test
  public void testVsURLEncoder_Hyphen_Dot_Underscore_EncodedSame() {
    // Both leave - . _ unencoded (all are unreserved in both RFC 3986 and form-encoding)
    Assertions.assertEquals(URLEncoder.encode("-._", StandardCharsets.UTF_8), URICoder.encode("-._"));
  }

  @Test
  public void testVsURLEncoder_NonASCII_SameBytes_DifferentSpaceEncoding() {
    // For non-ASCII content both use UTF-8 bytes, but space handling differs
    String urlEncoded = URLEncoder.encode("Café", StandardCharsets.UTF_8);
    String uriEncoded = URICoder.encode("Café");
    Assertions.assertEquals(urlEncoded, "Caf%C3%A9");
    Assertions.assertEquals(uriEncoded, "Caf%C3%A9");
  }

  @Test
  public void testVsURLEncoder_UnicodeNormalisation_DifferenceMayAppear() {
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
  public void testVsURLEncoder_NoEquivalent_MinimalEncode() {
    // URLEncoder has no equivalent to minimalEncode: it always encodes reserved chars.
    String path = "/search?q=hello world";
    String urlEncoded = URLEncoder.encode(path, StandardCharsets.UTF_8);
    String minEncoded = URICoder.minimalEncode(path);

    // URLEncoder encodes / ? = and space
    Assertions.assertEquals(urlEncoded, "%2Fsearch%3Fq%3Dhello+world");
    // minimalEncode preserves / ? = and encodes space as %20
    Assertions.assertEquals(minEncoded, "/search?q=hello%20world");
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
