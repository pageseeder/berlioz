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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;

/**
 * A JavaScript minimiser.
 *
 * <p>This class is a slightly modified version of the work done by John Reilly who initially
 * adapted Douglas Crockford's C version of his JavaScript minimiser.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.11.2
 * @since Berlioz 0.9.32
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
  private final PushbackInputStream _in;

  /**
   * The minimised version.
   */
  private OutputStream out;

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
    this._in = new PushbackInputStream(in);
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
    int c = this._in.read();

    if (c == '\n') {
      this.line++;
      this.column = 0;
    } else {
      this.column++;
    }

    if (c >= ' ' || c == '\n' || c == EOF) return c;

    if (c == '\r') {
      this.column = 0;
      return '\n';
    }

    return ' ';
  }

  /**
   * Get the next character without getting it.
   *
   * @return the next character.
   * @throws IOException should an error occur while reading the input
   */
  int peek() throws IOException {
    int lookaheadChar = this._in.read();
    this._in.unread(lookaheadChar);
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
    if (c == '/') {
      switch (peek()) {
        case '/':
          for (;;) {
            c = get();
            if (c <= '\n') return c;
          }

        case '*':
          get();
          for (;;) {
            switch (get()) {
              case '*':
                if (peek() == '/') {
                  get();
                  return ' ';
                }
                break;
              case EOF:
                throw new UnterminatedCommentException(this.line, this.column);
              default:
            }
          }

        // fall through
        default:
          return c;
      }

    }
    return c;
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
    switch (action) {
      case WRITE:
        this.out.write(this.theA);

      // fall through
      case COPY:
        this.theA = this.theB;
        if (this.theA == '\'' || this.theA == '"') {
          for (;;) {
            this.out.write(this.theA);
            this.theA = get();
            if (this.theA == this.theB) {
              break;
            }
            if (this.theA <= '\n') throw new UnterminatedStringLiteralException(this.line, this.column);
            if (this.theA == '\\') {
              this.out.write(this.theA);
              this.theA = get();
            }
          }
        }

      // fall through
      case NEXT:
        this.theB = next();
        if (this.theB == '/'
            && (this.theA == '(' || this.theA == ',' || this.theA == '=' || this.theA == ':' || this.theA == '[' || this.theA == '!' || this.theA == '&'
                || this.theA == '|' || this.theA == '?' || this.theA == '{' || this.theA == '}' || this.theA == ';' || this.theA == '\n')) {

          this.out.write(this.theA);
          this.out.write(this.theB);
          for (;;) {
            this.theA = get();
            if (this.theA == '/') {
              break;
            } else if (this.theA == '\\') {
              this.out.write(this.theA);
              this.theA = get();
            } else if (this.theA <= '\n') throw new UnterminatedRegExpLiteralException(this.line, this.column);
            this.out.write(this.theA);
          }
          this.theB = next();
        }

      // fall through
      default:
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
      switch (this.theA) {
        case ' ':
          if (isAlphanum(this.theB)) {
            process(WRITE);
          } else {
            process(COPY);
          }
          break;
        case '\n':
          switch (this.theB) {
            case '{':
            case '[':
            case '(':
            case '+':
            case '-':
              process(WRITE);
              break;
            case ' ':
              process(NEXT);
              break;
            default:
              if (isAlphanum(this.theB)) {
                process(WRITE);
              } else {
                process(COPY);
              }
          }
          break;
        default:
          switch (this.theB) {
            case ' ':
              if (isAlphanum(this.theA)) {
                process(WRITE);
                break;
              }
              process(NEXT);
              break;
            case '\n':
              switch (this.theA) {
                case '}':
                case ']':
                case ')':
                case '+':
                case '-':
                case '"':
                case '\'':
                  process(WRITE);
                  break;
                default:
                  if (isAlphanum(this.theA)) {
                    process(WRITE);
                  } else {
                    process(NEXT);
                  }
              }
              break;
            default:
              process(WRITE);
              break;
          }
      }
    }
    this.out.flush();
  }

  // Predefined Exceptions
  // ----------------------------------------------------------------------------------------------

  /**
   * A comment that does not terminate properly.
   */
  @SuppressWarnings("serial")
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
  @SuppressWarnings("serial")
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
  @SuppressWarnings("serial")
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
   * @param arg name of file to minimize
   */
  public static void main(String[] arg) {
    try {
      JSMin jsmin = new JSMin(new FileInputStream(arg[0]), System.out);
      jsmin.jsmin();
    } catch (FileNotFoundException ex) {
      ex.printStackTrace();
    } catch (ArrayIndexOutOfBoundsException ex) {
      ex.printStackTrace();
    } catch (IOException ex) {
      ex.printStackTrace();
    } catch (ParsingException ex) {
      ex.printStackTrace();
    }
  }

}
