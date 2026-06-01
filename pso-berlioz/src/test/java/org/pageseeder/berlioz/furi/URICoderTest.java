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

import org.junit.Assert;
import org.junit.Test;

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
    Assert.assertEquals("", URICoder.encode(""));
  }

  @Test
  public void testEncode_Unreserved_Alpha() {
    Assert.assertEquals(ALPHA, URICoder.encode(ALPHA));
  }

  @Test
  public void testEncode_Unreserved_Digit() {
    Assert.assertEquals(DIGIT, URICoder.encode(DIGIT));
  }

  @Test
  public void testEncode_Unreserved_Punctuation() {
    // hyphen, dot, underscore, tilde are all unreserved per RFC 3986
    Assert.assertEquals("-_.~", URICoder.encode("-_.~"));
  }

  @Test
  public void testEncode_AllASCII() {
    // Every ASCII char is either passed through (if unreserved) or percent-encoded uppercase
    for (char c = 0; c < 0x80; c++) {
      String s = String.valueOf(c);
      String enc = URICoder.encode(s);
      if (UNRESERVED.indexOf(c) >= 0) {
        Assert.assertEquals("char 0x" + Integer.toHexString(c) + " should pass through", s, enc);
      } else {
        String hex = Integer.toHexString(c);
        if (hex.length() == 1) hex = "0" + hex;
        Assert.assertEquals("char 0x" + Integer.toHexString(c) + " should be percent-encoded",
            "%" + hex.toUpperCase(), enc);
      }
    }
  }

  @Test
  public void testEncode_UppercaseHex() {
    // Percent-encoding must use uppercase hex digits (RFC 3986 §2.1 recommends uppercase)
    Assert.assertEquals("%2F", URICoder.encode("/"));
    Assert.assertEquals("%3F", URICoder.encode("?"));
    Assert.assertEquals("%23", URICoder.encode("#"));
    Assert.assertEquals("%20", URICoder.encode(" "));
  }

  @Test
  public void testEncode_Space_As_Percent20() {
    // Spaces must become %20, not + (which is a form-encoding convention)
    Assert.assertEquals("%20", URICoder.encode(" "));
    Assert.assertEquals("hello%20world", URICoder.encode("hello world"));
  }

  @Test
  public void testEncode_Plus_Encoded() {
    // + is a reserved char in URIs and must be encoded
    Assert.assertEquals("%2B", URICoder.encode("+"));
  }

  @Test
  public void testEncode_Slash_Encoded() {
    Assert.assertEquals("%2F", URICoder.encode("/"));
    Assert.assertEquals("a%2Fb", URICoder.encode("a/b"));
  }

  @Test
  public void testEncode_ReservedChars_AllEncoded() {
    // RFC 3986 gen-delims: : / ? # [ ] @
    Assert.assertEquals("%3A", URICoder.encode(":"));
    Assert.assertEquals("%2F", URICoder.encode("/"));
    Assert.assertEquals("%3F", URICoder.encode("?"));
    Assert.assertEquals("%23", URICoder.encode("#"));
    Assert.assertEquals("%5B", URICoder.encode("["));
    Assert.assertEquals("%5D", URICoder.encode("]"));
    Assert.assertEquals("%40", URICoder.encode("@"));
    // RFC 3986 sub-delims: ! $ & ' ( ) * + , ; =
    Assert.assertEquals("%21", URICoder.encode("!"));
    Assert.assertEquals("%24", URICoder.encode("$"));
    Assert.assertEquals("%26", URICoder.encode("&"));
    Assert.assertEquals("%27", URICoder.encode("'"));
    Assert.assertEquals("%28", URICoder.encode("("));
    Assert.assertEquals("%29", URICoder.encode(")"));
    Assert.assertEquals("%2A", URICoder.encode("*"));
    Assert.assertEquals("%2B", URICoder.encode("+"));
    Assert.assertEquals("%2C", URICoder.encode(","));
    Assert.assertEquals("%3B", URICoder.encode(";"));
    Assert.assertEquals("%3D", URICoder.encode("="));
  }

  @Test
  public void testEncode_PercentSign_Encoded() {
    // A literal % in input must itself be encoded to avoid malformed sequences
    Assert.assertEquals("%25", URICoder.encode("%"));
    Assert.assertEquals("%2525", URICoder.encode("%25"));
  }

  @Test
  public void testEncode_NonASCII_CafeAccent() {
    // é (U+00E9) → UTF-8 bytes 0xC3 0xA9 → %C3%A9
    Assert.assertEquals("Caf%C3%A9", URICoder.encode("Café"));
  }

  @Test
  public void testEncode_NonASCII_WithReservedChar() {
    Assert.assertEquals("Caf%C3%A9%3F", URICoder.encode("Café?"));
  }

  @Test
  public void testEncode_NonASCII_CJK() {
    // 中 (U+4E2D) → UTF-8 bytes 0xE4 0xB8 0xAD → %E4%B8%AD
    Assert.assertEquals("%E4%B8%AD", URICoder.encode("中"));
  }

  @Test
  public void testEncode_NonASCII_ArabicWord() {
    // م (U+0645) → UTF-8 bytes 0xD9 0x85
    Assert.assertEquals("%D9%85", URICoder.encode("م"));
  }

  // ---------------------------------------------------------------------------
  // encode(String) — Unicode normalisation (NFKC)
  // ---------------------------------------------------------------------------

  @Test
  public void testEncode_Normalization_GreekUpsilon() {
    // U+03D3 GREEK UPSILON WITH HOOK AND ACUTE (single codepoint)
    // U+03D2 + U+0301 (decomposed) — both normalise to the same NFKC form
    Assert.assertEquals("%CE%8E", URICoder.encode("ϓ"));
    Assert.assertEquals("%CE%8E", URICoder.encode("ϓ"));
  }

  @Test
  public void testEncode_Normalization_DotAbove() {
    // Long-s (U+017F) + combining dot above (U+0307) → ṡ (U+1E61)
    // Regular s (U+0073) + combining dot above (U+0307) → same NFKC result
    Assert.assertEquals("%E1%B9%A1", URICoder.encode("ẛ"));
    Assert.assertEquals("%E1%B9%A1", URICoder.encode("ṡ"));
  }

  @Test
  public void testEncode_Normalization_EquivalentFormsProduceSameOutput() {
    // NFC and NFD of "é" should both produce the same encoding
    String nfc = "é";           // é as precomposed
    String nfd = "é";          // e + combining acute accent
    Assert.assertEquals(URICoder.encode(nfc), URICoder.encode(nfd));
  }

  // ---------------------------------------------------------------------------
  // encode(String, char) — extra character passthrough
  // ---------------------------------------------------------------------------

  @Test
  public void testEncodeWithChar_EmptyString() {
    Assert.assertEquals("", URICoder.encode("", '/'));
  }

  @Test
  public void testEncodeWithChar_ExtraCharPassesThrough() {
    // The second argument is an extra char that should not be encoded
    Assert.assertEquals("a/b/c", URICoder.encode("a/b/c", '/'));
    Assert.assertEquals("a?b", URICoder.encode("a?b", '?'));
    Assert.assertEquals("a@b", URICoder.encode("a@b", '@'));
  }

  @Test
  public void testEncodeWithChar_OtherReservedCharsStillEncoded() {
    // Only the specified extra char bypasses encoding; others are still encoded
    Assert.assertEquals("a/b%3Fc", URICoder.encode("a/b?c", '/'));
    Assert.assertEquals("a%2Fb%3Fc", URICoder.encode("a/b?c", '@'));
  }

  @Test
  public void testEncodeWithChar_UnreservedCharsAlwaysPassThrough() {
    Assert.assertEquals("abc-._~", URICoder.encode("abc-._~", '/'));
  }

  @Test
  public void testEncodeWithChar_NonASCII_WithSlash() {
    // Non-ASCII chars still get encoded even with the extra-char override
    Assert.assertEquals("/Caf%C3%A9/menu", URICoder.encode("/Café/menu", '/'));
  }

  @Test
  public void testEncodeWithChar_UnreservedChar_AsExtraChar_NoChange() {
    // Passing an unreserved char as the extra char has no effect since it's already unencoded
    Assert.assertEquals("abc", URICoder.encode("abc", 'a'));
  }

  // ---------------------------------------------------------------------------
  // minimalEncode(String)
  // ---------------------------------------------------------------------------

  @Test
  public void testMinimalEncode_EmptyString() {
    Assert.assertEquals("", URICoder.minimalEncode(""));
  }

  @Test
  public void testMinimalEncode_UnreservedChars_PassThrough() {
    Assert.assertEquals(UNRESERVED, URICoder.minimalEncode(UNRESERVED));
  }

  @Test
  public void testMinimalEncode_RFC3986ReservedChars_PassThrough() {
    // gen-delims and sub-delims are "legal" in a URI and must NOT be encoded by minimalEncode
    String reserved = ":/?#[]@!$&'()*+,;=";
    Assert.assertEquals(reserved, URICoder.minimalEncode(reserved));
  }

  @Test
  public void testMinimalEncode_Slash_PassThrough() {
    Assert.assertEquals("a/b/c", URICoder.minimalEncode("a/b/c"));
  }

  @Test
  public void testMinimalEncode_Query_PassThrough() {
    Assert.assertEquals("/search?q=hello&lang=en", URICoder.minimalEncode("/search?q=hello&lang=en"));
  }

  @Test
  public void testMinimalEncode_IllegalChars_Encoded() {
    // Characters that are illegal in any URI component must be encoded
    Assert.assertEquals("%3C", URICoder.minimalEncode("<"));    // less-than
    Assert.assertEquals("%3E", URICoder.minimalEncode(">"));    // greater-than
    Assert.assertEquals("%5C", URICoder.minimalEncode("\\"));   // backslash
    Assert.assertEquals("%5E", URICoder.minimalEncode("^"));    // caret
    Assert.assertEquals("%60", URICoder.minimalEncode("`"));    // backtick
    Assert.assertEquals("%7B", URICoder.minimalEncode("{"));    // left brace
    Assert.assertEquals("%7C", URICoder.minimalEncode("|"));    // vertical bar
    Assert.assertEquals("%7D", URICoder.minimalEncode("}"));    // right brace
  }

  @Test
  public void testMinimalEncode_Space_Encoded() {
    Assert.assertEquals("%20", URICoder.minimalEncode(" "));
  }

  @Test
  public void testMinimalEncode_Percent_Encoded() {
    Assert.assertEquals("%25", URICoder.minimalEncode("%"));
  }

  @Test
  public void testMinimalEncode_DEL_Encoded() {
    Assert.assertEquals("%7F", URICoder.minimalEncode(""));
  }

  @Test
  public void testMinimalEncode_ControlChars_Encoded() {
    Assert.assertEquals("%00", URICoder.minimalEncode(" "));
    Assert.assertEquals("%01", URICoder.minimalEncode(""));
    Assert.assertEquals("%1F", URICoder.minimalEncode(""));
  }

  @Test
  public void testMinimalEncode_NonASCII_Encoded() {
    Assert.assertEquals("%C3%A9", URICoder.minimalEncode("é"));
  }

  @Test
  public void testMinimalEncode_Normalization() {
    // NFKC normalisation applies in minimalEncode just as in encode
    Assert.assertEquals("%CE%8E", URICoder.minimalEncode("ϓ"));
    Assert.assertEquals("%CE%8E", URICoder.minimalEncode("ϓ"));
  }

  @Test
  public void testMinimalEncode_UrlWithIllegalChars() {
    // A URL-like string with both legal and illegal characters
    Assert.assertEquals("/path/to?q=hello%3Cworld%3E",
        URICoder.minimalEncode("/path/to?q=hello<world>"));
  }

  @Test
  public void testMinimalEncode_Vs_Encode_ReservedCharsAreDifferent() {
    // encode() encodes reserved chars; minimalEncode() does not
    String reserved = "/?#@";
    String encoded = URICoder.encode(reserved);
    String minEncoded = URICoder.minimalEncode(reserved);
    Assert.assertNotEquals(encoded, minEncoded);
    Assert.assertEquals(reserved, minEncoded);  // reserved chars preserved
    Assert.assertEquals("%2F%3F%23%40", encoded); // all encoded
  }

  // ---------------------------------------------------------------------------
  // decode(String) — basic contract
  // ---------------------------------------------------------------------------

  @Test
  public void testDecode_EmptyString() {
    Assert.assertEquals("", URICoder.decode(""));
  }

  @Test
  public void testDecode_StringWithoutPercentOrPlus_ReturnsSame() {
    // Fast-path: no decoding needed; input is returned unchanged
    String s = "hello-world_foo.bar~baz";
    Assert.assertEquals(s, URICoder.decode(s));
  }

  @Test
  public void testDecode_Unreserved_Unchanged() {
    Assert.assertEquals(ALPHA, URICoder.decode(ALPHA));
    Assert.assertEquals(DIGIT, URICoder.decode(DIGIT));
    Assert.assertEquals(PUNC,  URICoder.decode(PUNC));
  }

  @Test
  public void testDecode_Plus_DecodesToSpace() {
    Assert.assertEquals(" ", URICoder.decode("+"));
  }

  @Test
  public void testDecode_Percent20_DecodesToSpace() {
    Assert.assertEquals(" ", URICoder.decode("%20"));
  }

  @Test
  public void testDecode_Space_BothRepresentations() {
    Assert.assertEquals("Café $1", URICoder.decode("Caf%C3%A9+$1"));
  }

  @Test
  public void testDecode_PercentEncoded_Slash() {
    Assert.assertEquals("/", URICoder.decode("%2F"));
    Assert.assertEquals("a/b", URICoder.decode("a%2Fb"));
  }

  @Test
  public void testDecode_LowercaseHex_Accepted() {
    // Decoders must accept both uppercase and lowercase hex digits per RFC 3986 §2.1
    Assert.assertEquals("/", URICoder.decode("%2f"));
    Assert.assertEquals("?", URICoder.decode("%3f"));
  }

  @Test
  public void testDecode_MixedCaseHex_Accepted() {
    Assert.assertEquals("Café", URICoder.decode("Caf%c3%a9"));
    Assert.assertEquals("Café", URICoder.decode("Caf%C3%A9"));
  }

  @Test
  public void testDecode_PercentB_DecodesCorrectly() {
    // %2B is + (should not become space, which + does)
    Assert.assertEquals("+", URICoder.decode("%2B"));
    Assert.assertEquals("+", URICoder.decode("%2b"));
  }

  @Test
  public void testDecode_AllASCII_RoundTrip() {
    // encode then decode must recover the original char for every ASCII input
    for (char c = 0; c < 0x80; c++) {
      String original = String.valueOf(c);
      String encoded = URICoder.encode(original);
      String decoded = URICoder.decode(encoded);
      Assert.assertEquals("round-trip failed for char 0x" + Integer.toHexString(c), original, decoded);
    }
  }

  @Test
  public void testDecode_NonASCII_CafeAccent() {
    Assert.assertEquals("Café", URICoder.decode("Caf%C3%A9"));
    Assert.assertEquals("Café?", URICoder.decode("Caf%C3%A9%3F"));
  }

  @Test
  public void testDecode_NonASCII_CJK() {
    Assert.assertEquals("中", URICoder.decode("%E4%B8%AD"));
  }

  @Test
  public void testDecode_NonASCII_RoundTrip() {
    String original = "Café — menu";
    String encoded = URICoder.encode(original);
    Assert.assertEquals(original, URICoder.decode(encoded));
  }

  @Test
  public void testDecode_IncompletePercent_AtEnd_Ignored() {
    // A % at the very end of the string has no following hex digits; it is silently dropped
    Assert.assertEquals("abc", URICoder.decode("abc%"));
  }

  @Test
  public void testDecode_IncompletePercent_OneDigit_PercentDroppedTrailingKept() {
    // When % has only one following hex digit (i.e., at the second-to-last position),
    // the % is silently dropped (TODO in source) but the trailing char is kept as a literal.
    Assert.assertEquals("abc2", URICoder.decode("abc%2"));
  }

  @Test
  public void testDecode_IncompletePercent_MidString() {
    // Valid sequences around the malformed one are still decoded
    Assert.assertEquals("a/c", URICoder.decode("a%2Fc"));
  }

  @Test
  public void testDecode_NullDecodeDoesNotOccur_PlusInMiddle() {
    Assert.assertEquals("hello world today", URICoder.decode("hello+world+today"));
  }

  // ---------------------------------------------------------------------------
  // URICoder vs java.net.URLEncoder — documented behavioural differences
  // ---------------------------------------------------------------------------

  @Test
  public void testVsURLEncoder_Space_EncodedDifferently() {
    String urlEncoded = URLEncoder.encode(" ", StandardCharsets.UTF_8);
    String uriEncoded = URICoder.encode(" ");
    Assert.assertEquals("+",   urlEncoded); // form-data convention
    Assert.assertEquals("%20", uriEncoded); // RFC 3986 percent-encoding
    Assert.assertNotEquals(urlEncoded, uriEncoded);
  }

  @Test
  public void testVsURLEncoder_Tilde_EncodedDifferently() {
    // URLEncoder encodes ~ because it predates RFC 3986 recognising ~ as unreserved
    String urlEncoded = URLEncoder.encode("~", StandardCharsets.UTF_8);
    String uriEncoded = URICoder.encode("~");
    Assert.assertEquals("%7E", urlEncoded); // URLEncoder encodes tilde
    Assert.assertEquals("~",   uriEncoded); // URICoder leaves tilde (RFC 3986 unreserved)
    Assert.assertNotEquals(urlEncoded, uriEncoded);
  }

  @Test
  public void testVsURLEncoder_Asterisk_EncodedDifferently() {
    // URLEncoder treats * as safe (legacy); URICoder encodes it (not RFC 3986 unreserved)
    String urlEncoded = URLEncoder.encode("*", StandardCharsets.UTF_8);
    String uriEncoded = URICoder.encode("*");
    Assert.assertEquals("*",   urlEncoded); // URLEncoder leaves asterisk
    Assert.assertEquals("%2A", uriEncoded); // URICoder percent-encodes it
    Assert.assertNotEquals(urlEncoded, uriEncoded);
  }

  @Test
  public void testVsURLEncoder_Plus_EncodedSame() {
    // Both encode + as %2B (URLEncoder: + means space, so literal + must be encoded)
    Assert.assertEquals(URLEncoder.encode("+", StandardCharsets.UTF_8), URICoder.encode("+"));
    Assert.assertEquals("%2B", URICoder.encode("+"));
  }

  @Test
  public void testVsURLEncoder_AlphaDigit_EncodedSame() {
    // Both leave letters and digits unencoded
    Assert.assertEquals(URLEncoder.encode(ALPHA, StandardCharsets.UTF_8), URICoder.encode(ALPHA));
    Assert.assertEquals(URLEncoder.encode(DIGIT, StandardCharsets.UTF_8), URICoder.encode(DIGIT));
  }

  @Test
  public void testVsURLEncoder_Hyphen_Dot_Underscore_EncodedSame() {
    // Both leave - . _ unencoded (all are unreserved in both RFC 3986 and form-encoding)
    Assert.assertEquals(URLEncoder.encode("-._", StandardCharsets.UTF_8), URICoder.encode("-._"));
  }

  @Test
  public void testVsURLEncoder_NonASCII_SameBytes_DifferentSpaceEncoding() {
    // For non-ASCII content both use UTF-8 bytes, but space handling differs
    String urlEncoded = URLEncoder.encode("Café", StandardCharsets.UTF_8);
    String uriEncoded = URICoder.encode("Café");
    Assert.assertEquals("Caf%C3%A9", urlEncoded);
    Assert.assertEquals("Caf%C3%A9", uriEncoded);
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
    Assert.assertNotEquals("URLEncoder: NFC and NFD differ", urlNfc, urlNfd);

    String uriNfc = URICoder.encode(nfc);
    String uriNfd = URICoder.encode(nfd);
    Assert.assertEquals("URICoder: NFC and NFD normalise to same output", uriNfc, uriNfd);
  }

  @Test
  public void testVsURLEncoder_NoEquivalent_MinimalEncode() {
    // URLEncoder has no equivalent to minimalEncode: it always encodes reserved chars.
    String path = "/search?q=hello world";
    String urlEncoded = URLEncoder.encode(path, StandardCharsets.UTF_8);
    String minEncoded = URICoder.minimalEncode(path);

    // URLEncoder encodes / ? = and space
    Assert.assertEquals("%2Fsearch%3Fq%3Dhello+world", urlEncoded);
    // minimalEncode preserves / ? = and encodes space as %20
    Assert.assertEquals("/search?q=hello%20world", minEncoded);
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
