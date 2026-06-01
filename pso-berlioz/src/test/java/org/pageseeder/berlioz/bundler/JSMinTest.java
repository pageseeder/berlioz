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

public class JSMinTest {

  // --- Empty and trivial input ---

  @Test
  public void testEmptyInput() throws Exception {
    Assertions.assertEquals(min(""), "");
  }

  @Test
  public void testWhitespaceOnly() throws Exception {
    Assertions.assertEquals(min("   \n   \n   "), "");
  }

  // --- Comment removal ---

  @Test
  public void testLineCommentOnly() throws Exception {
    Assertions.assertEquals(min("// this is a comment\n"), "");
  }

  @Test
  public void testBlockCommentOnly() throws Exception {
    Assertions.assertEquals(min("/* this is a comment */"), "");
  }

  @Test
  public void testLineCommentBeforeCode() throws Exception {
    Assertions.assertEquals(min("// comment\nvar x = 1;"), "var x=1;");
  }

  @Test
  public void testBlockCommentBetweenTokens() throws Exception {
    Assertions.assertEquals(min("var /* comment */ x = 1;"), "var x=1;");
  }

  @Test
  public void testBlockCommentAtEnd() throws Exception {
    Assertions.assertEquals(min("var x = 1; /* trailing comment */"), "var x=1;");
  }

  @Test
  public void testMultiLineBlockComment() throws Exception {
    Assertions.assertEquals(min("var x = 1; /*\n * multi-line\n * comment\n */"), "var x=1;");
  }

  // --- Whitespace compression ---

  @Test
  public void testSimpleAssignment() throws Exception {
    Assertions.assertEquals(min("var x = 1;"), "var x=1;");
  }

  @Test
  public void testExtraSpacesCompressed() throws Exception {
    Assertions.assertEquals(min("var  x  =  1;"), "var x=1;");
  }

  @Test
  public void testSpaceBetweenKeywordsPreserved() throws Exception {
    Assertions.assertEquals(min("var x=1;"), "var x=1;");
  }

  @Test
  public void testNewlinesBetweenStatementsDropped() throws Exception {
    Assertions.assertEquals(min("var a = 1;\n\n\nvar b = 2;"), "var a=1;var b=2;");
  }

  @Test
  public void testTabsConvertedToSpaces() throws Exception {
    Assertions.assertEquals(min("var\tx\t=\t1;"), "var x=1;");
  }

  @Test
  public void testWindowsNewlines() throws Exception {
    Assertions.assertEquals(min("var a = 1;\r\nvar b = 2;"), "var a=1;var b=2;");
  }

  // --- String literal preservation ---

  @Test
  public void testDoubleQuotedString() throws Exception {
    Assertions.assertEquals(min("var s = \"hello world\";"), "var s=\"hello world\";");
  }

  @Test
  public void testSingleQuotedString() throws Exception {
    Assertions.assertEquals(min("var s = 'hello world';"), "var s='hello world';");
  }

  @Test
  public void testStringContainingLineCommentSyntax() throws Exception {
    Assertions.assertEquals(min("var s = \"// not a comment\";"), "var s=\"// not a comment\";");
  }

  @Test
  public void testStringContainingBlockCommentSyntax() throws Exception {
    Assertions.assertEquals(min("var s = \"/* not a comment */\";"), "var s=\"/* not a comment */\";");
  }

  @Test
  public void testStringWithEscapedQuote() throws Exception {
    Assertions.assertEquals(min("var s = \"say \\\"hi\\\"\";"), "var s=\"say \\\"hi\\\"\";");
  }

  @Test
  public void testEmptyString() throws Exception {
    Assertions.assertEquals(min("var s = \"\";"), "var s=\"\";");
  }

  @Test
  public void testTemplateLiteralContainingCommentSyntax() throws Exception {
    Assertions.assertEquals(min("const s = `/* not a comment */`;"), "const s=`/* not a comment */`;");
  }

  // --- Operators and punctuation ---

  @Test
  public void testAdditionExpression() throws Exception {
    Assertions.assertEquals(min("var z = x + y;"), "var z=x+y;");
  }

  @Test
  public void testComparisonOperator() throws Exception {
    Assertions.assertEquals(min("if (a === b) {}"), "if(a===b){}");
  }

  @Test
  public void testTernaryOperator() throws Exception {
    Assertions.assertEquals(min("var x = a ? b : c;"), "var x=a?b:c;");
  }

  // --- Regex literals ---

  @Test
  public void testRegexAfterEquals() throws Exception {
    Assertions.assertEquals(min("var r = /[a-z]+/;"), "var r=/[a-z]+/;");
  }

  @Test
  public void testRegexAfterOpenParen() throws Exception {
    Assertions.assertEquals(min("if (/abc/.test(s)) {}"), "if(/abc/.test(s)){}");
  }

  @Test
  public void testRegexWithFlags() throws Exception {
    Assertions.assertEquals(min("var r = /pattern/gi;"), "var r=/pattern/gi;");
  }

  @Test
  public void testRegexWithSlashInCharacterClass() throws Exception {
    Assertions.assertEquals(min("var r = /[/]/;"), "var r=/[/]/;");
  }

  // --- Functions and blocks ---

  @Test
  public void testFunctionDeclaration() throws Exception {
    Assertions.assertEquals(min("function f(x) {\n  return x + 1;\n}"), "function f(x){return x+1;}");
  }

  @Test
  public void testObjectLiteral() throws Exception {
    Assertions.assertEquals(min("var o = { a: 1, b: 2 };"), "var o={a:1,b:2};");
  }

  @Test
  public void testArrayLiteral() throws Exception {
    Assertions.assertEquals(min("var a = [1, 2, 3];"), "var a=[1,2,3];");
  }

  @Test
  public void testDivisionExpression() throws Exception {
    Assertions.assertEquals(min("var x = a / b;"), "var x=a/b;");
  }

  // --- Real-world snippets ---

  @Test
  public void testIIFE() throws Exception {
    String input = "(function() {\n  var x = 1;\n}());";
    String expected = "(function(){var x=1;}());";
    Assertions.assertEquals(expected, min(input));
  }

  @Test
  public void testReturnStatement() throws Exception {
    Assertions.assertEquals(min("function f() {\n  return true;\n}"), "function f(){return true;}");
  }

  @Test
  public void testIncrementDecrement() throws Exception {
    Assertions.assertEquals(min("i++; j--;"), "i++;j--;");
  }

  // --- Error conditions ---

  @Test
  public void testUnterminatedBlockComment() throws Exception {
    Assertions.assertThrows(JSMin.UnterminatedCommentException.class, () -> min("/* unterminated comment"));
  }

  @Test
  public void testUnterminatedBlockCommentAfterCode() throws Exception {
    Assertions.assertThrows(JSMin.UnterminatedCommentException.class, () -> min("var x = 1; /* forgot to close"));
  }

  @Test
  public void testUnterminatedDoubleQuotedString() throws Exception {
    Assertions.assertThrows(JSMin.UnterminatedStringLiteralException.class, () -> min("var s = \"unterminated\nvar x = 1;"));
  }

  @Test
  public void testUnterminatedSingleQuotedString() throws Exception {
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
