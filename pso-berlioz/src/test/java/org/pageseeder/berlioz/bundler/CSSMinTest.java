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
package org.pageseeder.berlioz.bundler;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

class CSSMinTest {

  // --- Basic structure ---

  @Test
  void testEmptyRule() {
    Assertions.assertEquals("a{}", min("a { }"));
  }

  @Test
  void testTrailingSemicolon() {
    Assertions.assertEquals("a{color:#000}", min("a { color: #000;}"));
  }

  @Test
  void testSingleProperty() {
    Assertions.assertEquals("a{font-size:10px}", min("a { font-size: 10px}"));
  }

  @Test
  void testMultipleProperties() {
    Assertions.assertEquals("a{color:#000;font-size:10px}", min("a { color: #000; font-size: 10px }"));
  }

  @Test
  void testPropertiesSortedAlphabetically() {
    Assertions.assertEquals("a{color:#fff;font-size:10px;margin:0}", min("a { margin: 0px; font-size: 10px; color: white }"));
  }

  @Test
  void testVendorPrefixSortedWithUnprefixed() {
    // -webkit-transform strips to "transform" so it sorts together with transform, not under "-w..."
    // equal sort keys preserve stable (input) order, so unprefixed first → stays first
    Assertions.assertEquals("a{transform:rotate(45deg);-webkit-transform:rotate(45deg);z-index:0}", min("a { z-index: 0; transform: rotate(45deg); -webkit-transform: rotate(45deg) }"));
  }

  // --- Parameter simplification ---

  @Test
  void testSimplifyParameters() {
    Assertions.assertEquals("a{border:1px 2px 3px}", min("a { border: 1px 2px 3px 2px}"));
    Assertions.assertEquals("a{border:1px 2px}", min("a { border: 1px 2px 1px 2px}"));
    Assertions.assertEquals("a{border:1px 2px}", min("a { border: 1px 2px 1px}"));
    Assertions.assertEquals("a{border:1px}", min("a { border: 1px 1px 1px 1px}"));
    Assertions.assertEquals("a{border:1px}", min("a { border: 1px 1px 1px}"));
    Assertions.assertEquals("a{border:1px}", min("a { border: 1px 1px}"));
  }

  @Test
  void testZeroUnit() {
    Assertions.assertEquals("div{height:0}", min("div { height: 0px }"));
  }

  @Test
  void testMultipleZeros() {
    Assertions.assertEquals("a{margin:0}", min("a { margin: 0px 0px 0px 0px }"));
    Assertions.assertEquals("a{padding:0}", min("a { padding: 0em 0em }"));
  }

  // --- Color transformations ---

  @Test
  void testNamedColors() {
    Assertions.assertEquals("a{color:#fff}", min("a { color: white}"));
    Assertions.assertEquals("a{color:#000}", min("a { color: black}"));
    Assertions.assertEquals("a{color:#777}", min("a { color: #777777}"));
  }

  @Test
  void testHexColorShortening() {
    Assertions.assertEquals("a{color:#abc}", min("a { color: #aabbcc }"));
    Assertions.assertEquals("a{color:#f0f}", min("a { color: #ff00ff }"));
  }

  @Test
  void testRgbToHex() {
    Assertions.assertEquals("a{color:#369}", min("a { color: rgb(51, 102, 153) }"));
    Assertions.assertEquals("a{color:#000}", min("a { color: rgb(0, 0, 0) }"));
  }

  @Test
  void testHexColorNotShortenedWhenMismatch() {
    Assertions.assertEquals("a{color:#aabbcd}", min("a { color: #aabbcd }"));
  }

  // --- Font weight ---

  @Test
  void testFontWeightBold() {
    Assertions.assertEquals("a{font-weight:700}", min("a { font-weight: bold }"));
  }

  @Test
  void testFontWeightNormal() {
    Assertions.assertEquals("a{font-weight:400}", min("a { font-weight: normal }"));
  }

  @Test
  void testFontWeightLighter() {
    Assertions.assertEquals("a{font-weight:100}", min("a { font-weight: lighter }"));
  }

  @Test
  void testFontWeightNumericUnchanged() {
    Assertions.assertEquals("a{font-weight:600}", min("a { font-weight: 600 }"));
  }

  // --- URL values ---

  @Test
  void testUrlSingleQuotesStripped() {
    Assertions.assertEquals("a{background:url(image.png)}", min("a { background: url('image.png') }"));
  }

  @Test
  void testUrlDoubleQuotesStripped() {
    Assertions.assertEquals("a{background:url(image.png)}", min("a { background: url(\"image.png\") }"));
  }

  @Test
  void testUrlCaseInsensitive() {
    Assertions.assertEquals("a{background:url(image.png)}", min("a { background: URL(\"image.png\") }"));
  }

  @Test
  void testPreserveCaseDataURL() {
    String x = ".x{background:url(data:image/svg+xml,%3Csvg/%3E)}";
    Assertions.assertEquals(x, min(x));
  }

  @Test
  void testPreserveCaseDataURLwithCharset() {
    String x = ".x{background:url(data:image/svg+xml;charset=utf8,%3Csvg/%3E)}";
    Assertions.assertEquals(x, min(x));
  }

  @Test
  void testPreserveUrlQuotesWhenRequired() {
    Assertions.assertEquals("a{background:url(\"image 1.png\")}", min("a { background: url(\"image 1.png\") }"));
  }

  // --- !important ---

  @Test
  void testImportant() {
    // color names with !important are joined before lookup, so use hex instead
    Assertions.assertEquals("a{color:#fff!important}", min("a { color: #ffffff !important }"));
  }

  @Test
  void testImportantWithNamedColor() {
    Assertions.assertEquals("a{color:#fff!important}", min("a { color: white !important }"));
  }

  // --- Selectors ---

  @Test
  void testPseudoClass() {
    Assertions.assertEquals("a:hover{color:#fff}", min("a:hover { color: white }"));
  }

  @Test
  void testPseudoElement() {
    Assertions.assertEquals("p::first-line{color:#fff}", min("p::first-line { color: white }"));
  }

  @Test
  void testChildCombinator() {
    Assertions.assertEquals("a>b{color:#fff}", min("a > b { color: white }"));
  }

  @Test
  void testAdjacentSiblingCombinator() {
    Assertions.assertEquals("a+b{color:#fff}", min("a + b { color: white }"));
  }

  @Test
  void testGeneralSiblingCombinator() {
    Assertions.assertEquals("a~b{color:#fff}", min("a ~ b { color: white }"));
  }

  @Test
  void testMultipleSelectors() {
    Assertions.assertEquals("h1,h2,h3{color:#fff}", min("h1, h2, h3 { color: white }"));
  }

  @Test
  void testAttributeSelectorOperator() {
    Assertions.assertEquals("a[href=\"/A B\"]{color:#fff}", min("a[href = \"/A B\"] { color: white }"));
  }

  @Test
  void testContentProperty() {
    Assertions.assertEquals("i::before{content:\" \"}", min("i::before { content: \" \" }"));
  }

  @Test
  void testContentWithSemicolon() {
    Assertions.assertEquals("i::before{content:\"a;b\"}", min("i::before { content: \"a;b\" }"));
  }

  @Test
  void testContentWithBraces() {
    Assertions.assertEquals("i::before{content:\"{}\"}", min("i::before { content: \"{}\" }"));
  }

  // --- Comments ---

  @Test
  void testRegularCommentStripped() {
    Assertions.assertEquals("a{color:#fff}", min("/* a comment */\na { color: white }"));
  }

  @Test
  void testInlineCommentStripped() {
    Assertions.assertEquals("a{color:#fff}", min("a { /* inline */ color: white }"));
  }

  @Test
  void testCommentSyntaxInStringPreserved() {
    Assertions.assertEquals("a{content:\"/* not a comment */\"}", min("a { content: \"/* not a comment */\" }"));
  }

  @Test
  void testSpecialCommentPreserved() {
    String result = min("/** keep this */\na { color: white }");
    Assertions.assertTrue(result.contains("/** keep this */"), "Special comment should be preserved");
    Assertions.assertTrue(result.contains("a{color:#fff}"), "Rule should still be output");
  }

  // --- Nested rules (at-rules) ---

  @Test
  void testMediaQuery() {
    Assertions.assertEquals("@media screen{a{color:#fff}}", min("@media screen { a { color: white; } }"));
  }

  @Test
  void testMediaQueryWithMultipleRules() {
    Assertions.assertEquals("@media screen{a{color:#fff}b{color:#000}}", min("@media screen { a { color: white; } b { color: black; } }"));
  }

  @Test
  void testKeyframes() {
    Assertions.assertEquals("@keyframes fade{from{opacity:1}to{opacity:0}}", min("@keyframes fade { from { opacity: 1; } to { opacity: 0; } }"));
  }

  @Test
  void testNestedRuleWithQuotedBrace() {
    Assertions.assertEquals("@media screen{a::before{content:\"{\"}}", min("@media screen { a::before { content: \"{\"; } }"));
  }

  // --- At-rules ---

  @Test
  void testFontFace() {
    Assertions.assertEquals("@font-face{font-family:'Open Sans'}", min("@font-face { font-family: 'Open Sans'; }"));
  }

  // --- Helper ---

  private static String min(String css) {
    StringReader r = new StringReader(css);
    StringWriter w = new StringWriter();
    PrintWriter out = new PrintWriter(w);
    CSSMin.minimize(r, out);
    return w.toString().trim();
  }
}
