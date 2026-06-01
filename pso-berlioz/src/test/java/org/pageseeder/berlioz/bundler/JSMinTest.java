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
    Assertions.assertEquals(min(""), "");
  }

  @Test
  void testWhitespaceOnly() throws Exception {
    Assertions.assertEquals(min("   \n   \n   "), "");
  }

  // --- Comment removal ---

  @Test
  void testLineCommentOnly() throws Exception {
    Assertions.assertEquals(min("// this is a comment\n"), "");
  }

  @Test
  void testBlockCommentOnly() throws Exception {
    Assertions.assertEquals(min("/* this is a comment */"), "");
  }

  @Test
  void testLineCommentBeforeCode() throws Exception {
    Assertions.assertEquals(min("// comment\nvar x = 1;"), "var x=1;");
  }

  @Test
  void testBlockCommentBetweenTokens() throws Exception {
    Assertions.assertEquals(min("var /* comment */ x = 1;"), "var x=1;");
  }

  @Test
  void testBlockCommentAtEnd() throws Exception {
    Assertions.assertEquals(min("var x = 1; /* trailing comment */"), "var x=1;");
  }

  @Test
  void testMultiLineBlockComment() throws Exception {
    Assertions.assertEquals(min("var x = 1; /*\n * multi-line\n * comment\n */"), "var x=1;");
  }

  // --- Whitespace compression ---

  @Test
  void testSimpleAssignment() throws Exception {
    Assertions.assertEquals(min("var x = 1;"), "var x=1;");
  }

  @Test
  void testExtraSpacesCompressed() throws Exception {
    Assertions.assertEquals(min("var  x  =  1;"), "var x=1;");
  }

  @Test
  void testSpaceBetweenKeywordsPreserved() throws Exception {
    Assertions.assertEquals(min("var x=1;"), "var x=1;");
  }

  @Test
  void testNewlinesBetweenStatementsDropped() throws Exception {
    Assertions.assertEquals(min("var a = 1;\n\n\nvar b = 2;"), "var a=1;var b=2;");
  }

  @Test
  void testTabsConvertedToSpaces() throws Exception {
    Assertions.assertEquals(min("var\tx\t=\t1;"), "var x=1;");
  }

  @Test
  void testWindowsNewlines() throws Exception {
    Assertions.assertEquals(min("var a = 1;\r\nvar b = 2;"), "var a=1;var b=2;");
  }

  // --- String literal preservation ---

  @Test
  void testDoubleQuotedString() throws Exception {
    Assertions.assertEquals(min("var s = \"hello world\";"), "var s=\"hello world\";");
  }

  @Test
  void testSingleQuotedString() throws Exception {
    Assertions.assertEquals(min("var s = 'hello world';"), "var s='hello world';");
  }

  @Test
  void testStringContainingLineCommentSyntax() throws Exception {
    Assertions.assertEquals(min("var s = \"// not a comment\";"), "var s=\"// not a comment\";");
  }

  @Test
  void testStringContainingBlockCommentSyntax() throws Exception {
    Assertions.assertEquals(min("var s = \"/* not a comment */\";"), "var s=\"/* not a comment */\";");
  }

  @Test
  void testStringWithEscapedQuote() throws Exception {
    Assertions.assertEquals(min("var s = \"say \\\"hi\\\"\";"), "var s=\"say \\\"hi\\\"\";");
  }

  @Test
  void testEmptyString() throws Exception {
    Assertions.assertEquals(min("var s = \"\";"), "var s=\"\";");
  }

  @Test
  void testTemplateLiteralContainingCommentSyntax() throws Exception {
    Assertions.assertEquals(min("const s = `/* not a comment */`;"), "const s=`/* not a comment */`;");
  }

  // --- Operators and punctuation ---

  @Test
  void testAdditionExpression() throws Exception {
    Assertions.assertEquals(min("var z = x + y;"), "var z=x+y;");
  }

  @Test
  void testComparisonOperator() throws Exception {
    Assertions.assertEquals(min("if (a === b) {}"), "if(a===b){}");
  }

  @Test
  void testTernaryOperator() throws Exception {
    Assertions.assertEquals(min("var x = a ? b : c;"), "var x=a?b:c;");
  }

  // --- Regex literals ---

  @Test
  void testRegexAfterEquals() throws Exception {
    Assertions.assertEquals(min("var r = /[a-z]+/;"), "var r=/[a-z]+/;");
  }

  @Test
  void testRegexAfterOpenParen() throws Exception {
    Assertions.assertEquals(min("if (/abc/.test(s)) {}"), "if(/abc/.test(s)){}");
  }

  @Test
  void testRegexWithFlags() throws Exception {
    Assertions.assertEquals(min("var r = /pattern/gi;"), "var r=/pattern/gi;");
  }

  @Test
  void testRegexWithSlashInCharacterClass() throws Exception {
    Assertions.assertEquals(min("var r = /[/]/;"), "var r=/[/]/;");
  }

  // --- Functions and blocks ---

  @Test
  void testFunctionDeclaration() throws Exception {
    Assertions.assertEquals(min("function f(x) {\n  return x + 1;\n}"), "function f(x){return x+1;}");
  }

  @Test
  void testObjectLiteral() throws Exception {
    Assertions.assertEquals(min("var o = { a: 1, b: 2 };"), "var o={a:1,b:2};");
  }

  @Test
  void testArrayLiteral() throws Exception {
    Assertions.assertEquals(min("var a = [1, 2, 3];"), "var a=[1,2,3];");
  }

  @Test
  void testDivisionExpression() throws Exception {
    Assertions.assertEquals(min("var x = a / b;"), "var x=a/b;");
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
    Assertions.assertEquals(min("function f() {\n  return true;\n}"), "function f(){return true;}");
  }

  @Test
  void testIncrementDecrement() throws Exception {
    Assertions.assertEquals(min("i++; j--;"), "i++;j--;");
  }

  // --- Error conditions ---

  @Test
  void testUnterminatedBlockComment() throws Exception {
    Assertions.assertThrows(JSMin.UnterminatedCommentException.class, () -> min("/* unterminated comment"));
  }

  @Test
  void testUnterminatedBlockCommentAfterCode() throws Exception {
    Assertions.assertThrows(JSMin.UnterminatedCommentException.class, () -> min("var x = 1; /* forgot to close"));
  }

  @Test
  void testUnterminatedDoubleQuotedString() throws Exception {
    Assertions.assertThrows(JSMin.UnterminatedStringLiteralException.class, () -> min("var s = \"unterminated\nvar x = 1;"));
  }

  @Test
  void testUnterminatedSingleQuotedString() throws Exception {
    Assertions.assertThrows(JSMin.UnterminatedStringLiteralException.class, () -> min("var s = 'unterminated\nvar x = 1;"));
  }

  // --- Helper ---

  private static String min(String js) throws IOException, ParsingException {
    ByteArrayInputStream in = new ByteArrayInputStream(js.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    new JSMin(in, out).jsmin();
    return out.toString(StandardCharsets.UTF_8.name()).trim();
  }
}
