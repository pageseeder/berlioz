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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

class JSMinTest {

  // --- Empty and trivial input ---

  @Test
  void testEmptyInput() throws Exception {
    Assertions.assertEquals("", min(""));
  }

  @Test
  void testWhitespaceOnly() throws Exception {
    Assertions.assertEquals("", min("   \n   \n   "));
  }

  // --- Comment removal ---

  @Test
  void testLineCommentOnly() throws Exception {
    Assertions.assertEquals("", min("// this is a comment\n"));
  }

  @Test
  void testBlockCommentOnly() throws Exception {
    Assertions.assertEquals("", min("/* this is a comment */"));
  }

  @Test
  void testLineCommentBeforeCode() throws Exception {
    Assertions.assertEquals("var x=1;", min("// comment\nvar x = 1;"));
  }

  @Test
  void testBlockCommentBetweenTokens() throws Exception {
    Assertions.assertEquals("var x=1;", min("var /* comment */ x = 1;"));
  }

  @Test
  void testBlockCommentAtEnd() throws Exception {
    Assertions.assertEquals("var x=1;", min("var x = 1; /* trailing comment */"));
  }

  @Test
  void testMultiLineBlockComment() throws Exception {
    Assertions.assertEquals("var x=1;", min("var x = 1; /*\n * multi-line\n * comment\n */"));
  }

  // --- Whitespace compression ---

  @Test
  void testSimpleAssignment() throws Exception {
    Assertions.assertEquals("var x=1;", min("var x = 1;"));
  }

  @Test
  void testExtraSpacesCompressed() throws Exception {
    Assertions.assertEquals("var x=1;", min("var  x  =  1;"));
  }

  @Test
  void testSpaceBetweenKeywordsPreserved() throws Exception {
    Assertions.assertEquals("var x=1;", min("var x=1;"));
  }

  @Test
  void testNewlinesBetweenStatementsDropped() throws Exception {
    Assertions.assertEquals("var a=1;var b=2;", min("var a = 1;\n\n\nvar b = 2;"));
  }

  @Test
  void testTabsConvertedToSpaces() throws Exception {
    Assertions.assertEquals("var x=1;", min("var\tx\t=\t1;"));
  }

  @Test
  void testWindowsNewlines() throws Exception {
    Assertions.assertEquals("var a=1;var b=2;", min("var a = 1;\r\nvar b = 2;"));
  }

  // --- String literal preservation ---

  @Test
  void testDoubleQuotedString() throws Exception {
    Assertions.assertEquals("var s=\"hello world\";", min("var s = \"hello world\";"));
  }

  @Test
  void testSingleQuotedString() throws Exception {
    Assertions.assertEquals("var s='hello world';", min("var s = 'hello world';"));
  }

  @Test
  void testStringContainingLineCommentSyntax() throws Exception {
    Assertions.assertEquals("var s=\"// not a comment\";", min("var s = \"// not a comment\";"));
  }

  @Test
  void testStringContainingBlockCommentSyntax() throws Exception {
    Assertions.assertEquals("var s=\"/* not a comment */\";", min("var s = \"/* not a comment */\";"));
  }

  @Test
  void testStringWithEscapedQuote() throws Exception {
    Assertions.assertEquals("var s=\"say \\\"hi\\\"\";", min("var s = \"say \\\"hi\\\"\";"));
  }

  @Test
  void testEmptyString() throws Exception {
    Assertions.assertEquals("var s=\"\";", min("var s = \"\";"));
  }

  @Test
  void testTemplateLiteralContainingCommentSyntax() throws Exception {
    Assertions.assertEquals("const s=`/* not a comment */`;", min("const s = `/* not a comment */`;"));
  }

  // --- Operators and punctuation ---

  @Test
  void testAdditionExpression() throws Exception {
    Assertions.assertEquals("var z=x+y;", min("var z = x + y;"));
  }

  @Test
  void testComparisonOperator() throws Exception {
    Assertions.assertEquals("if(a===b){}", min("if (a === b) {}"));
  }

  @Test
  void testTernaryOperator() throws Exception {
    Assertions.assertEquals("var x=a?b:c;", min("var x = a ? b : c;"));
  }

  // --- Regex literals ---

  @Test
  void testRegexAfterEquals() throws Exception {
    Assertions.assertEquals("var r=/[a-z]+/;", min("var r = /[a-z]+/;"));
  }

  @Test
  void testRegexAfterOpenParen() throws Exception {
    Assertions.assertEquals("if(/abc/.test(s)){}", min("if (/abc/.test(s)) {}"));
  }

  @Test
  void testRegexWithFlags() throws Exception {
    Assertions.assertEquals("var r=/pattern/gi;", min("var r = /pattern/gi;"));
  }

  @Test
  void testRegexWithSlashInCharacterClass() throws Exception {
    Assertions.assertEquals("var r=/[/]/;", min("var r = /[/]/;"));
  }

  // --- Functions and blocks ---

  @Test
  void testFunctionDeclaration() throws Exception {
    Assertions.assertEquals("function f(x){return x+1;}", min("function f(x) {\n  return x + 1;\n}"));
  }

  @Test
  void testObjectLiteral() throws Exception {
    Assertions.assertEquals("var o={a:1,b:2};", min("var o = { a: 1, b: 2 };"));
  }

  @Test
  void testArrayLiteral() throws Exception {
    Assertions.assertEquals("var a=[1,2,3];", min("var a = [1, 2, 3];"));
  }

  @Test
  void testDivisionExpression() throws Exception {
    Assertions.assertEquals("var x=a/b;", min("var x = a / b;"));
  }

  // --- Real-world snippets ---

  @Test
  void testIIFE() throws Exception {
    String input = "(function() {\n  var x = 1;\n}());";
    String expected = "(function(){var x=1;}());";
    Assertions.assertEquals(expected, min(input));
  }

  @Test
  void testReturnStatement() throws Exception {
    Assertions.assertEquals("function f(){return true;}", min("function f() {\n  return true;\n}"));
  }

  @Test
  void testIncrementDecrement() throws Exception {
    Assertions.assertEquals("i++;j--;", min("i++; j--;"));
  }

  // --- Error conditions ---

  @Test
  void testUnterminatedBlockComment() {
    Assertions.assertThrows(JSMin.UnterminatedCommentException.class, () -> min("/* unterminated comment"));
  }

  @Test
  void testUnterminatedBlockCommentAfterCode() {
    Assertions.assertThrows(JSMin.UnterminatedCommentException.class, () -> min("var x = 1; /* forgot to close"));
  }

  @Test
  void testUnterminatedDoubleQuotedString() {
    Assertions.assertThrows(JSMin.UnterminatedStringLiteralException.class, () -> min("var s = \"unterminated\nvar x = 1;"));
  }

  @Test
  void testUnterminatedSingleQuotedString() {
    Assertions.assertThrows(JSMin.UnterminatedStringLiteralException.class, () -> min("var s = 'unterminated\nvar x = 1;"));
  }

  // --- Helper ---

  private static String min(String js) throws IOException, ParsingException {
    ByteArrayInputStream in = new ByteArrayInputStream(js.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    new JSMin(in, out).jsmin();
    return out.toString(StandardCharsets.UTF_8).trim();
  }
}
