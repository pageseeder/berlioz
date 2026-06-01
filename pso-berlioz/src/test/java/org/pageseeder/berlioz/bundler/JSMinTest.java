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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class JSMinTest {

  // --- Empty and trivial input ---

  @Test
  public void testEmptyInput() throws Exception {
    Assert.assertEquals("", min(""));
  }

  @Test
  public void testWhitespaceOnly() throws Exception {
    Assert.assertEquals("", min("   \n   \n   "));
  }

  // --- Comment removal ---

  @Test
  public void testLineCommentOnly() throws Exception {
    Assert.assertEquals("", min("// this is a comment\n"));
  }

  @Test
  public void testBlockCommentOnly() throws Exception {
    Assert.assertEquals("", min("/* this is a comment */"));
  }

  @Test
  public void testLineCommentBeforeCode() throws Exception {
    Assert.assertEquals("var x=1;", min("// comment\nvar x = 1;"));
  }

  @Test
  public void testBlockCommentBetweenTokens() throws Exception {
    Assert.assertEquals("var x=1;", min("var /* comment */ x = 1;"));
  }

  @Test
  public void testBlockCommentAtEnd() throws Exception {
    Assert.assertEquals("var x=1;", min("var x = 1; /* trailing comment */"));
  }

  @Test
  public void testMultiLineBlockComment() throws Exception {
    Assert.assertEquals("var x=1;", min("var x = 1; /*\n * multi-line\n * comment\n */"));
  }

  // --- Whitespace compression ---

  @Test
  public void testSimpleAssignment() throws Exception {
    Assert.assertEquals("var x=1;", min("var x = 1;"));
  }

  @Test
  public void testExtraSpacesCompressed() throws Exception {
    Assert.assertEquals("var x=1;", min("var  x  =  1;"));
  }

  @Test
  public void testSpaceBetweenKeywordsPreserved() throws Exception {
    Assert.assertEquals("var x=1;", min("var x=1;"));
  }

  @Test
  public void testNewlinesBetweenStatementsDropped() throws Exception {
    Assert.assertEquals("var a=1;var b=2;", min("var a = 1;\n\n\nvar b = 2;"));
  }

  @Test
  public void testTabsConvertedToSpaces() throws Exception {
    Assert.assertEquals("var x=1;", min("var\tx\t=\t1;"));
  }

  @Test
  public void testWindowsNewlines() throws Exception {
    Assert.assertEquals("var a=1;var b=2;", min("var a = 1;\r\nvar b = 2;"));
  }

  // --- String literal preservation ---

  @Test
  public void testDoubleQuotedString() throws Exception {
    Assert.assertEquals("var s=\"hello world\";", min("var s = \"hello world\";"));
  }

  @Test
  public void testSingleQuotedString() throws Exception {
    Assert.assertEquals("var s='hello world';", min("var s = 'hello world';"));
  }

  @Test
  public void testStringContainingLineCommentSyntax() throws Exception {
    Assert.assertEquals("var s=\"// not a comment\";", min("var s = \"// not a comment\";"));
  }

  @Test
  public void testStringContainingBlockCommentSyntax() throws Exception {
    Assert.assertEquals("var s=\"/* not a comment */\";", min("var s = \"/* not a comment */\";"));
  }

  @Test
  public void testStringWithEscapedQuote() throws Exception {
    Assert.assertEquals("var s=\"say \\\"hi\\\"\";", min("var s = \"say \\\"hi\\\"\";"));
  }

  @Test
  public void testEmptyString() throws Exception {
    Assert.assertEquals("var s=\"\";", min("var s = \"\";"));
  }

  @Test
  public void testTemplateLiteralContainingCommentSyntax() throws Exception {
    Assert.assertEquals("const s=`/* not a comment */`;", min("const s = `/* not a comment */`;"));
  }

  // --- Operators and punctuation ---

  @Test
  public void testAdditionExpression() throws Exception {
    Assert.assertEquals("var z=x+y;", min("var z = x + y;"));
  }

  @Test
  public void testComparisonOperator() throws Exception {
    Assert.assertEquals("if(a===b){}", min("if (a === b) {}"));
  }

  @Test
  public void testTernaryOperator() throws Exception {
    Assert.assertEquals("var x=a?b:c;", min("var x = a ? b : c;"));
  }

  // --- Regex literals ---

  @Test
  public void testRegexAfterEquals() throws Exception {
    Assert.assertEquals("var r=/[a-z]+/;", min("var r = /[a-z]+/;"));
  }

  @Test
  public void testRegexAfterOpenParen() throws Exception {
    Assert.assertEquals("if(/abc/.test(s)){}", min("if (/abc/.test(s)) {}"));
  }

  @Test
  public void testRegexWithFlags() throws Exception {
    Assert.assertEquals("var r=/pattern/gi;", min("var r = /pattern/gi;"));
  }

  @Test
  public void testRegexWithSlashInCharacterClass() throws Exception {
    Assert.assertEquals("var r=/[/]/;", min("var r = /[/]/;"));
  }

  // --- Functions and blocks ---

  @Test
  public void testFunctionDeclaration() throws Exception {
    Assert.assertEquals("function f(x){return x+1;}", min("function f(x) {\n  return x + 1;\n}"));
  }

  @Test
  public void testObjectLiteral() throws Exception {
    Assert.assertEquals("var o={a:1,b:2};", min("var o = { a: 1, b: 2 };"));
  }

  @Test
  public void testArrayLiteral() throws Exception {
    Assert.assertEquals("var a=[1,2,3];", min("var a = [1, 2, 3];"));
  }

  @Test
  public void testDivisionExpression() throws Exception {
    Assert.assertEquals("var x=a/b;", min("var x = a / b;"));
  }

  // --- Real-world snippets ---

  @Test
  public void testIIFE() throws Exception {
    String input = "(function() {\n  var x = 1;\n}());";
    String expected = "(function(){var x=1;}());";
    Assert.assertEquals(expected, min(input));
  }

  @Test
  public void testReturnStatement() throws Exception {
    Assert.assertEquals("function f(){return true;}", min("function f() {\n  return true;\n}"));
  }

  @Test
  public void testIncrementDecrement() throws Exception {
    Assert.assertEquals("i++;j--;", min("i++; j--;"));
  }

  // --- Error conditions ---

  @Test(expected = JSMin.UnterminatedCommentException.class)
  public void testUnterminatedBlockComment() throws Exception {
    min("/* unterminated comment");
  }

  @Test(expected = JSMin.UnterminatedCommentException.class)
  public void testUnterminatedBlockCommentAfterCode() throws Exception {
    min("var x = 1; /* forgot to close");
  }

  @Test(expected = JSMin.UnterminatedStringLiteralException.class)
  public void testUnterminatedDoubleQuotedString() throws Exception {
    min("var s = \"unterminated\nvar x = 1;");
  }

  @Test(expected = JSMin.UnterminatedStringLiteralException.class)
  public void testUnterminatedSingleQuotedString() throws Exception {
    min("var s = 'unterminated\nvar x = 1;");
  }

  // --- Helper ---

  private static String min(String js) throws IOException, ParsingException {
    ByteArrayInputStream in = new ByteArrayInputStream(js.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    new JSMin(in, out).jsmin();
    return out.toString(StandardCharsets.UTF_8.name()).trim();
  }
}
