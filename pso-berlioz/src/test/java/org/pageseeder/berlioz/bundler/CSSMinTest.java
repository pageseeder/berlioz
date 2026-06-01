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
    Assertions.assertEquals(min("a { }"), "a{}");
  }

  @Test
  void testTrailingSemicolon() {
    Assertions.assertEquals(min("a { color: #000;}"), "a{color:#000}");
  }

  @Test
  void testSingleProperty() {
    Assertions.assertEquals(min("a { font-size: 10px}"), "a{font-size:10px}");
  }

  @Test
  void testMultipleProperties() {
    Assertions.assertEquals(min("a { color: #000; font-size: 10px }"), "a{color:#000;font-size:10px}");
  }

  @Test
  void testPropertiesSortedAlphabetically() {
    Assertions.assertEquals(min("a { margin: 0px; font-size: 10px; color: white }"), "a{color:#fff;font-size:10px;margin:0}");
  }

  @Test
  void testVendorPrefixSortedWithUnprefixed() {
    // -webkit-transform strips to "transform" so it sorts together with transform, not under "-w..."
    // equal sort keys preserve stable (input) order, so unprefixed first → stays first
    Assertions.assertEquals(min("a { z-index: 0; transform: rotate(45deg); -webkit-transform: rotate(45deg) }"), "a{transform:rotate(45deg);-webkit-transform:rotate(45deg);z-index:0}");
  }

  // --- Parameter simplification ---

  @Test
  void testSimplifyParameters() {
    Assertions.assertEquals(min("a { border: 1px 2px 3px 2px}"), "a{border:1px 2px 3px}");
    Assertions.assertEquals(min("a { border: 1px 2px 1px 2px}"), "a{border:1px 2px}");
    Assertions.assertEquals(min("a { border: 1px 2px 1px}"), "a{border:1px 2px}");
    Assertions.assertEquals(min("a { border: 1px 1px 1px 1px}"), "a{border:1px}");
    Assertions.assertEquals(min("a { border: 1px 1px 1px}"), "a{border:1px}");
    Assertions.assertEquals(min("a { border: 1px 1px}"), "a{border:1px}");
  }

  @Test
  void testZeroUnit() {
    Assertions.assertEquals(min("div { height: 0px }"), "div{height:0}");
  }

  @Test
  void testMultipleZeros() {
    Assertions.assertEquals(min("a { margin: 0px 0px 0px 0px }"), "a{margin:0}");
    Assertions.assertEquals(min("a { padding: 0em 0em }"), "a{padding:0}");
  }

  // --- Color transformations ---

  @Test
  void testNamedColors() {
    Assertions.assertEquals(min("a { color: white}"), "a{color:#fff}");
    Assertions.assertEquals(min("a { color: black}"), "a{color:#000}");
    Assertions.assertEquals(min("a { color: #777777}"), "a{color:#777}");
  }

  @Test
  void testHexColorShortening() {
    Assertions.assertEquals(min("a { color: #aabbcc }"), "a{color:#abc}");
    Assertions.assertEquals(min("a { color: #ff00ff }"), "a{color:#f0f}");
  }

  @Test
  void testRgbToHex() {
    Assertions.assertEquals(min("a { color: rgb(51, 102, 153) }"), "a{color:#369}");
    Assertions.assertEquals(min("a { color: rgb(0, 0, 0) }"), "a{color:#000}");
  }

  @Test
  void testHexColorNotShortenedWhenMismatch() {
    Assertions.assertEquals(min("a { color: #aabbcd }"), "a{color:#aabbcd}");
  }

  // --- Font weight ---

  @Test
  void testFontWeightBold() {
    Assertions.assertEquals(min("a { font-weight: bold }"), "a{font-weight:700}");
  }

  @Test
  void testFontWeightNormal() {
    Assertions.assertEquals(min("a { font-weight: normal }"), "a{font-weight:400}");
  }

  @Test
  void testFontWeightLighter() {
    Assertions.assertEquals(min("a { font-weight: lighter }"), "a{font-weight:100}");
  }

  @Test
  void testFontWeightNumericUnchanged() {
    Assertions.assertEquals(min("a { font-weight: 600 }"), "a{font-weight:600}");
  }

  // --- URL values ---

  @Test
  void testUrlSingleQuotesStripped() {
    Assertions.assertEquals(min("a { background: url('image.png') }"), "a{background:url(image.png)}");
  }

  @Test
  void testUrlDoubleQuotesStripped() {
    Assertions.assertEquals(min("a { background: url(\"image.png\") }"), "a{background:url(image.png)}");
  }

  @Test
  void testUrlCaseInsensitive() {
    Assertions.assertEquals(min("a { background: URL(\"image.png\") }"), "a{background:url(image.png)}");
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
    Assertions.assertEquals(min("a { background: url(\"image 1.png\") }"), "a{background:url(\"image 1.png\")}");
  }

  // --- !important ---

  @Test
  void testImportant() {
    // color names with !important are joined before lookup, so use hex instead
    Assertions.assertEquals(min("a { color: #ffffff !important }"), "a{color:#fff!important}");
  }

  @Test
  void testImportantWithNamedColor() {
    Assertions.assertEquals(min("a { color: white !important }"), "a{color:#fff!important}");
  }

  // --- Selectors ---

  @Test
  void testPseudoClass() {
    Assertions.assertEquals(min("a:hover { color: white }"), "a:hover{color:#fff}");
  }

  @Test
  void testPseudoElement() {
    Assertions.assertEquals(min("p::first-line { color: white }"), "p::first-line{color:#fff}");
  }

  @Test
  void testChildCombinator() {
    Assertions.assertEquals(min("a > b { color: white }"), "a>b{color:#fff}");
  }

  @Test
  void testAdjacentSiblingCombinator() {
    Assertions.assertEquals(min("a + b { color: white }"), "a+b{color:#fff}");
  }

  @Test
  void testGeneralSiblingCombinator() {
    Assertions.assertEquals(min("a ~ b { color: white }"), "a~b{color:#fff}");
  }

  @Test
  void testMultipleSelectors() {
    Assertions.assertEquals(min("h1, h2, h3 { color: white }"), "h1,h2,h3{color:#fff}");
  }

  @Test
  void testAttributeSelectorOperator() {
    Assertions.assertEquals(min("a[href = \"/A B\"] { color: white }"), "a[href=\"/A B\"]{color:#fff}");
  }

  @Test
  void testContentProperty() {
    Assertions.assertEquals(min("i::before { content: \" \" }"), "i::before{content:\" \"}");
  }

  @Test
  void testContentWithSemicolon() {
    Assertions.assertEquals(min("i::before { content: \"a;b\" }"), "i::before{content:\"a;b\"}");
  }

  @Test
  void testContentWithBraces() {
    Assertions.assertEquals(min("i::before { content: \"{}\" }"), "i::before{content:\"{}\"}");
  }

  // --- Comments ---

  @Test
  void testRegularCommentStripped() {
    Assertions.assertEquals(min("/* a comment */\na { color: white }"), "a{color:#fff}");
  }

  @Test
  void testInlineCommentStripped() {
    Assertions.assertEquals(min("a { /* inline */ color: white }"), "a{color:#fff}");
  }

  @Test
  void testCommentSyntaxInStringPreserved() {
    Assertions.assertEquals(min("a { content: \"/* not a comment */\" }"), "a{content:\"/* not a comment */\"}");
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
    Assertions.assertEquals(min("@media screen { a { color: white; } }"), "@media screen{a{color:#fff}}");
  }

  @Test
  void testMediaQueryWithMultipleRules() {
    Assertions.assertEquals(min("@media screen { a { color: white; } b { color: black; } }"), "@media screen{a{color:#fff}b{color:#000}}");
  }

  @Test
  void testKeyframes() {
    Assertions.assertEquals(min("@keyframes fade { from { opacity: 1; } to { opacity: 0; } }"), "@keyframes fade{from{opacity:1}to{opacity:0}}");
  }

  @Test
  void testNestedRuleWithQuotedBrace() {
    Assertions.assertEquals(min("@media screen { a::before { content: \"{\"; } }"), "@media screen{a::before{content:\"{\"}}");
  }

  // --- At-rules ---

  @Test
  void testFontFace() {
    Assertions.assertEquals(min("@font-face { font-family: 'Open Sans'; }"), "@font-face{font-family:'Open Sans'}");
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
