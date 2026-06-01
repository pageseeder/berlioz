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
/*
 * JSMin.java 2006-02-13
 *
 * Copyright (c) 2006 John Reilly (www.inconspicuous.org)
 *
 * This work is a translation from C to Java of jsmin.c published by Douglas Crockford. Permission is hereby granted to
 * use the Java version under the same conditions as the jsmin.c on which it is based.
 *
 * jsmin.c 2003-04-21
 *
 * Copyright (c) 2002 Douglas Crockford (www.crockford.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * The Software shall be used for Good, not Evil.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.pageseeder.berlioz.bundler;

import java.io.*;
import java.nio.file.Files;

/**
 * A JavaScript minimiser.
 *
 * <p>This class is a slightly modified version of the work done by John Reilly who initially
 * adapted Douglas Crockford's C version of his JavaScript minimiser.
 *
 * @author Christophe Lauret
 *
 * @version 0.11.2
 * @since 0.9.32
 */
public final class JSMin {

  /**
   * End of file marker.
   */
  private static final int EOF = -1;

  /** What to do with the byte: Output A. Copy B to A. Get the next B */
  private static final int WRITE = 1;

  /** What to do with the byte: Copy B to A. Get the next B. (Delete A).  */
  private static final int COPY = 2;

  /** What to do with the byte: Get the next B. (Delete B) */
  private static final int NEXT = 3;

  /**
   * The script to read.
   */
  private final PushbackInputStream in;

  /**
   * The minimised version.
   */
  private final OutputStream out;

  /** What to do with byte A. */
  private int theA;

  /** What to do with byte B. */
  private int theB;

  /**
   * Tracks the current line being processed.
   */
  private int line;

  /**
   * Tracks the current column being processed.
   */
  private int column;

  /**
   * Creates a new JavaScript minimiser for the specified I/O.
   *
   * @param in  The JavaScript to minimise.
   * @param out The minimised script.
   */
  public JSMin(InputStream in, OutputStream out) {
    this.in = new PushbackInputStream(in);
    this.out = out;
    this.line = 0;
    this.column = 0;
  }

  /**
   * @param c character to evaluate.
   * @return <code>true</code> if the character is a letter, digit, underscore, dollar sign, or non-ASCII character.
   */
  private static boolean isAlphanum(int c) {
    final int lastPrintableAscii = 126;
    return ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z')
          || c == '_' || c == '$' || c == '\\' || c > lastPrintableAscii);
  }

  /**
   * Returns the next character from the input.
   *
   * <p>Watch out for lookahead. If the character is a control character, translate it to a space
   * or linefeed.
   *
   * @return the next character from the input.
   *
   * @throws IOException should an error occur while reading the input
   */
  int get() throws IOException {
    int c = this.in.read();

    if (c == EOF) return EOF;

    if (c == '\r') {
      int next = this.in.read();
      if (next != '\n' && next != EOF) {
        this.in.unread(next);
      }
      this.line++;
      this.column = 0;
      return '\n';
    }

    if (c == '\n') {
      this.line++;
      this.column = 0;
    } else {
      this.column++;
    }

    return c >= ' ' || c == '\n' ? c : ' ';
  }

  /**
   * Get the next character without getting it.
   *
   * @return the next character.
   * @throws IOException should an error occur while reading the input
   */
  int peek() throws IOException {
    int lookaheadChar = this.in.read();
    if (lookaheadChar != EOF) {
      this.in.unread(lookaheadChar);
    }
    return lookaheadChar;
  }

  /**
   * Get the next character, excluding comments.
   *
   * <p><code>peek()</code> is used to see if a '/' is followed by a '/' or '*'.
   *
   * @return the next character.
   * @throws IOException If thrown while reading the input
   * @throws UnterminatedCommentException If the end of the file is reading before the comment ends.
   */
  int next() throws IOException, UnterminatedCommentException {
    int c = get();
    if (c != '/') {
      return c;
    }
    int next = peek();
    if (next == '/') {
      return skipLineComment();
    }
    if (next == '*') {
      return skipBlockComment();
    }
    return c;
  }

  /**
   * Skips a JavaScript line comment and returns the line terminator or EOF.
   */
  private int skipLineComment() throws IOException {
    for (;;) {
      int c = get();
      if (c <= '\n') return c;
    }
  }

  /**
   * Skips a JavaScript block comment.
   */
  private int skipBlockComment() throws IOException, UnterminatedCommentException {
    get();
    for (;;) {
      int c = get();
      if (c == '*' && peek() == '/') {
        get();
        return ' ';
      }
      if (c == EOF) {
        throw new UnterminatedCommentException(this.line, this.column);
      }
    }
  }

  /**
   * Do something!
   *
   * <p>What you do is determined by the argument:
   * <ol>
   *   <li>1 Output A. Copy B to A. Get the next B.</li>
   *   <li>2 Copy B to A. Get the next B. (Delete A).</li>
   *   <li>3 Get the next B. (Delete B).</li>
   * </ol>
   *
   * <p>This method treats a string as a single character. It also recognizes a regular expression
   * if it is preceded by ( or , or =.
   *
   * @param action what to do
   *
   * @throws IOException      Should any IO error occur
   * @throws UnterminatedRegExpLiteralException Thrown when a regular expression does not terminate properly
   * @throws UnterminatedCommentException       Thrown when a comment does not terminate properly
   * @throws UnterminatedStringLiteralException Thrown when a string does not terminate properly
   */
  private void process(int action) throws IOException, UnterminatedRegExpLiteralException, UnterminatedCommentException, UnterminatedStringLiteralException {
    if (action < WRITE || action > NEXT) {
      return;
    }
    if (action == WRITE) {
      this.out.write(this.theA);
    }
    if (action <= COPY) {
      copyBToA();
    }
    readNextB();
  }

  /**
   * Copies B to A, preserving string-like literals as a single token.
   */
  private void copyBToA() throws IOException, UnterminatedStringLiteralException {
    this.theA = this.theB;
    if (this.theA == '\'' || this.theA == '"' || this.theA == '`') {
      writeStringLiteral(this.theA);
    }
  }

  /**
   * Reads the next B token, preserving regular expression literals as a single token.
   */
  private void readNextB() throws IOException, UnterminatedRegExpLiteralException, UnterminatedCommentException {
    this.theB = next();
    if (isRegExpLiteralStart()) {
      writeRegExpLiteral();
    }
  }

  /**
   * @return <code>true</code> if the current A/B pair starts a regular expression literal.
   */
  private boolean isRegExpLiteralStart() {
    if (this.theB != '/') {
      return false;
    }
    switch (this.theA) {
      case '(':
      case ',':
      case '=':
      case ':':
      case '[':
      case '!':
      case '&':
      case '|':
      case '?':
      case '{':
      case '}':
      case ';':
      case '\n':
        return true;
      default:
        return false;
    }
  }

  /**
   * Writes a regular expression literal as-is, preserving escaped characters and character classes.
   */
  private void writeRegExpLiteral() throws IOException, UnterminatedRegExpLiteralException, UnterminatedCommentException {
    this.out.write(this.theA);
    this.out.write(this.theB);
    boolean inCharacterClass = false;
    for (;;) {
      this.theA = get();
      if (this.theA == '/' && !inCharacterClass) {
        break;
      }
      inCharacterClass = processRegExpCharacter(inCharacterClass);
      this.out.write(this.theA);
    }
    this.theB = next();
  }

  /**
   * Handles a single character within a regular expression literal.
   */
  private boolean processRegExpCharacter(boolean inCharacterClass) throws IOException, UnterminatedRegExpLiteralException {
    if (this.theA == '\\') {
      this.out.write(this.theA);
      this.theA = get();
      return inCharacterClass;
    }
    if (this.theA == '[') {
      return true;
    }
    if (this.theA == ']') {
      return false;
    }
    if (this.theA <= '\n') {
      throw new UnterminatedRegExpLiteralException(this.line, this.column);
    }
    return inCharacterClass;
  }

  /**
   * Writes a string-like literal as-is, preserving escaped characters and template literal newlines.
   */
  private void writeStringLiteral(int delimiter) throws IOException, UnterminatedStringLiteralException {
    for (;;) {
      this.out.write(this.theA);
      this.theA = get();
      if (this.theA == delimiter) {
        break;
      }
      if (this.theA == EOF || (delimiter != '`' && this.theA <= '\n')) {
        throw new UnterminatedStringLiteralException(this.line, this.column);
      }
      if (this.theA == '\\') {
        this.out.write(this.theA);
        this.theA = get();
        if (this.theA == EOF) {
          throw new UnterminatedStringLiteralException(this.line, this.column);
        }
      }
    }
  }

  /**
   * Main JSMin method.
   *
   * <p>Copy the input to the output, deleting the characters which are insignificant to JavaScript:
   * <ul>
   *   <li>Comments will be removed.</li>
   *   <li>Tabs will be replaced with spaces.</li>
   *   <li>Carriage returns will be replaced with line feeds.</li>
   *   <li>Most spaces and line feeds will be removed.</li>
   * </ul>
   *
   * @throws IOException If an error occurs while reading the input or writing on the output.
   * @throws ParsingException If an error occurs while parsing the JavaScript (minimizing is not possible then).
   */
  public void jsmin() throws IOException, ParsingException {
    this.theA = '\n';
    process(NEXT);
    while (this.theA != EOF) {
      process(nextAction());
    }
    this.out.flush();
  }

  /**
   * @return the next action to perform for the current A/B pair.
   */
  private int nextAction() {
    if (this.theA == ' ') {
      return isAlphanum(this.theB) ? WRITE : COPY;
    }
    if (this.theA == '\n') {
      return actionAfterLineBreak();
    }
    if (this.theB == ' ') {
      return isAlphanum(this.theA) ? WRITE : NEXT;
    }
    if (this.theB == '\n') {
      return actionBeforeLineBreak();
    }
    return WRITE;
  }

  /**
   * @return the next action when A is a line break.
   */
  private int actionAfterLineBreak() {
    switch (this.theB) {
      case '{':
      case '[':
      case '(':
      case '+':
      case '-':
        return WRITE;
      case ' ':
        return NEXT;
      default:
        return isAlphanum(this.theB) ? WRITE : COPY;
    }
  }

  /**
   * @return the next action when B is a line break.
   */
  private int actionBeforeLineBreak() {
    switch (this.theA) {
      case '}':
      case ']':
      case ')':
      case '+':
      case '-':
      case '"':
      case '\'':
        return WRITE;
      default:
        return isAlphanum(this.theA) ? WRITE : NEXT;
    }
  }

  // Predefined Exceptions
  // ----------------------------------------------------------------------------------------------

  /**
   * A comment that does not terminate properly.
   */
  public static class UnterminatedCommentException extends ParsingException {

    /**
     * @param line   Current line number.
     * @param column Current column number.
     */
    public UnterminatedCommentException(int line, int column) {
      super("Unterminated comment at line", line, column);
    }
  }

  /**
   * A string that does not terminate properly.
   */
  public static class UnterminatedStringLiteralException extends ParsingException {

    /**
     * @param line   Current line number.
     * @param column Current column number.
     */
    public UnterminatedStringLiteralException(int line, int column) {
      super("Unterminated string literal", line, column);
    }
  }

  /**
   * A regular expression that does not terminate properly.
   */
  public static class UnterminatedRegExpLiteralException extends ParsingException {

    /**
     * @param line   Current line number.
     * @param column Current column number.
     */
    public UnterminatedRegExpLiteralException(int line, int column) {
      super("Unterminated regular expression", line, column);
    }
  }

  /**
   * To invoke the minimizer on the command line.
   *
   * @param args first argument is path to file to minimize
   *
   * @throws IOException if an input/output error occurs.
   */
  @SuppressWarnings("java:S106")
  public static void main(String[] args) throws IOException {
    if (args.length < 1) {
      System.err.println("Usage: ");
      System.err.println("JSMin [filepath]");
      return;
    }
    try {
      File file = new File(args[0]).getCanonicalFile();
      String currentPath = new File(".").getCanonicalPath();
      if (!file.toPath().startsWith(currentPath) || !file.exists() || file.isDirectory())
        throw new IllegalArgumentException("Illegal filepath argument");
      JSMin jsmin = new JSMin(Files.newInputStream(file.toPath()), System.out);
      jsmin.jsmin();
    } catch (ParsingException | IOException | IllegalArgumentException ex) {
      System.err.println(ex.getMessage());
      System.exit(1);
    }
  }

}
