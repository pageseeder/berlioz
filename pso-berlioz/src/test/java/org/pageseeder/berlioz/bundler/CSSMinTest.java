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

import org.junit.Assert;
import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

public class CSSMinTest {

  // --- Basic structure ---

  @Test
  public void testEmptyRule() {
    Assert.assertEquals("a{}", min("a { }"));
  }

  @Test
  public void testTrailingSemicolon() {
    Assert.assertEquals("a{color:#000}", min("a { color: #000;}"));
  }

  @Test
  public void testSingleProperty() {
    Assert.assertEquals("a{font-size:10px}", min("a { font-size: 10px}"));
  }

  @Test
  public void testMultipleProperties() {
    Assert.assertEquals("a{color:#000;font-size:10px}", min("a { color: #000; font-size: 10px }"));
  }

  @Test
  public void testPropertiesSortedAlphabetically() {
    Assert.assertEquals("a{color:#fff;font-size:10px;margin:0}", min("a { margin: 0px; font-size: 10px; color: white }"));
  }

  @Test
  public void testVendorPrefixSortedWithUnprefixed() {
    // -webkit-transform strips to "transform" so it sorts together with transform, not under "-w..."
    // equal sort keys preserve stable (input) order, so unprefixed first → stays first
    Assert.assertEquals("a{transform:rotate(45deg);-webkit-transform:rotate(45deg);z-index:0}",
        min("a { z-index: 0; transform: rotate(45deg); -webkit-transform: rotate(45deg) }"));
  }

  // --- Parameter simplification ---

  @Test
  public void testSimplifyParameters() {
    Assert.assertEquals("a{border:1px 2px 3px}", min("a { border: 1px 2px 3px 2px}"));
    Assert.assertEquals("a{border:1px 2px}",     min("a { border: 1px 2px 1px 2px}"));
    Assert.assertEquals("a{border:1px 2px}",     min("a { border: 1px 2px 1px}"));
    Assert.assertEquals("a{border:1px}",         min("a { border: 1px 1px 1px 1px}"));
    Assert.assertEquals("a{border:1px}",         min("a { border: 1px 1px 1px}"));
    Assert.assertEquals("a{border:1px}",         min("a { border: 1px 1px}"));
  }

  @Test
  public void testZeroUnit() {
    Assert.assertEquals("div{height:0}", min("div { height: 0px }"));
  }

  @Test
  public void testMultipleZeros() {
    Assert.assertEquals("a{margin:0}", min("a { margin: 0px 0px 0px 0px }"));
    Assert.assertEquals("a{padding:0}", min("a { padding: 0em 0em }"));
  }

  // --- Color transformations ---

  @Test
  public void testNamedColors() {
    Assert.assertEquals("a{color:#fff}", min("a { color: white}"));
    Assert.assertEquals("a{color:#000}", min("a { color: black}"));
    Assert.assertEquals("a{color:#777}", min("a { color: #777777}"));
  }

  @Test
  public void testHexColorShortening() {
    Assert.assertEquals("a{color:#abc}", min("a { color: #aabbcc }"));
    Assert.assertEquals("a{color:#f0f}", min("a { color: #ff00ff }"));
  }

  @Test
  public void testRgbToHex() {
    Assert.assertEquals("a{color:#369}", min("a { color: rgb(51, 102, 153) }"));
    Assert.assertEquals("a{color:#000}", min("a { color: rgb(0, 0, 0) }"));
  }

  @Test
  public void testHexColorNotShortenedWhenMismatch() {
    Assert.assertEquals("a{color:#aabbcd}", min("a { color: #aabbcd }"));
  }

  // --- Font weight ---

  @Test
  public void testFontWeightBold() {
    Assert.assertEquals("a{font-weight:700}", min("a { font-weight: bold }"));
  }

  @Test
  public void testFontWeightNormal() {
    Assert.assertEquals("a{font-weight:400}", min("a { font-weight: normal }"));
  }

  @Test
  public void testFontWeightLighter() {
    Assert.assertEquals("a{font-weight:100}", min("a { font-weight: lighter }"));
  }

  @Test
  public void testFontWeightNumericUnchanged() {
    Assert.assertEquals("a{font-weight:600}", min("a { font-weight: 600 }"));
  }

  // --- URL values ---

  @Test
  public void testUrlSingleQuotesStripped() {
    Assert.assertEquals("a{background:url(image.png)}", min("a { background: url('image.png') }"));
  }

  @Test
  public void testUrlDoubleQuotesStripped() {
    Assert.assertEquals("a{background:url(image.png)}", min("a { background: url(\"image.png\") }"));
  }

  @Test
  public void testUrlCaseInsensitive() {
    Assert.assertEquals("a{background:url(image.png)}", min("a { background: URL(\"image.png\") }"));
  }

  @Test
  public void testPreserveCaseDataURL() {
    String x = ".x{background:url(data:image/svg+xml,%3Csvg/%3E)}";
    Assert.assertEquals(x, min(x));
  }

  @Test
  public void testPreserveCaseDataURLwithCharset() {
    String x = ".x{background:url(data:image/svg+xml;charset=utf8,%3Csvg/%3E)}";
    Assert.assertEquals(x, min(x));
  }

  @Test
  public void testPreserveUrlQuotesWhenRequired() {
    Assert.assertEquals("a{background:url(\"image 1.png\")}", min("a { background: url(\"image 1.png\") }"));
  }

  // --- !important ---

  @Test
  public void testImportant() {
    // color names with !important are joined before lookup, so use hex instead
    Assert.assertEquals("a{color:#fff!important}", min("a { color: #ffffff !important }"));
  }

  @Test
  public void testImportantWithNamedColor() {
    Assert.assertEquals("a{color:#fff!important}", min("a { color: white !important }"));
  }

  // --- Selectors ---

  @Test
  public void testPseudoClass() {
    Assert.assertEquals("a:hover{color:#fff}", min("a:hover { color: white }"));
  }

  @Test
  public void testPseudoElement() {
    Assert.assertEquals("p::first-line{color:#fff}", min("p::first-line { color: white }"));
  }

  @Test
  public void testChildCombinator() {
    Assert.assertEquals("a>b{color:#fff}", min("a > b { color: white }"));
  }

  @Test
  public void testAdjacentSiblingCombinator() {
    Assert.assertEquals("a+b{color:#fff}", min("a + b { color: white }"));
  }

  @Test
  public void testGeneralSiblingCombinator() {
    Assert.assertEquals("a~b{color:#fff}", min("a ~ b { color: white }"));
  }

  @Test
  public void testMultipleSelectors() {
    Assert.assertEquals("h1,h2,h3{color:#fff}", min("h1, h2, h3 { color: white }"));
  }

  @Test
  public void testAttributeSelectorOperator() {
    Assert.assertEquals("a[href=\"/A B\"]{color:#fff}", min("a[href = \"/A B\"] { color: white }"));
  }

  @Test
  public void testContentProperty() {
    Assert.assertEquals("i::before{content:\" \"}", min("i::before { content: \" \" }"));
  }

  @Test
  public void testContentWithSemicolon() {
    Assert.assertEquals("i::before{content:\"a;b\"}", min("i::before { content: \"a;b\" }"));
  }

  @Test
  public void testContentWithBraces() {
    Assert.assertEquals("i::before{content:\"{}\"}", min("i::before { content: \"{}\" }"));
  }

  // --- Comments ---

  @Test
  public void testRegularCommentStripped() {
    Assert.assertEquals("a{color:#fff}", min("/* a comment */\na { color: white }"));
  }

  @Test
  public void testInlineCommentStripped() {
    Assert.assertEquals("a{color:#fff}", min("a { /* inline */ color: white }"));
  }

  @Test
  public void testCommentSyntaxInStringPreserved() {
    Assert.assertEquals("a{content:\"/* not a comment */\"}", min("a { content: \"/* not a comment */\" }"));
  }

  @Test
  public void testSpecialCommentPreserved() {
    String result = min("/** keep this */\na { color: white }");
    Assert.assertTrue("Special comment should be preserved", result.contains("/** keep this */"));
    Assert.assertTrue("Rule should still be output", result.contains("a{color:#fff}"));
  }

  // --- Nested rules (at-rules) ---

  @Test
  public void testMediaQuery() {
    Assert.assertEquals("@media screen{a{color:#fff}}", min("@media screen { a { color: white; } }"));
  }

  @Test
  public void testMediaQueryWithMultipleRules() {
    Assert.assertEquals("@media screen{a{color:#fff}b{color:#000}}", min("@media screen { a { color: white; } b { color: black; } }"));
  }

  @Test
  public void testKeyframes() {
    Assert.assertEquals("@keyframes fade{from{opacity:1}to{opacity:0}}", min("@keyframes fade { from { opacity: 1; } to { opacity: 0; } }"));
  }

  @Test
  public void testNestedRuleWithQuotedBrace() {
    Assert.assertEquals("@media screen{a::before{content:\"{\"}}", min("@media screen { a::before { content: \"{\"; } }"));
  }

  // --- At-rules ---

  @Test
  public void testFontFace() {
    Assert.assertEquals("@font-face{font-family:'Open Sans'}", min("@font-face { font-family: 'Open Sans'; }"));
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
