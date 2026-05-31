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
 * CSSMin Copyright License Agreement (BSD License)
 *
 * Copyright (c) 2011, Barry van Oudtshoorn
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * * Neither the name of Barryvan nor the names of its
 *   contributors may be used to endorse or promote products
 *   derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.pageseeder.berlioz.bundler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CSSMin takes in well-formed, human-readable CSS and reduces its size substantially.
 *
 * <p>It removes unnecessary whitespace and comments.
 *
 * <p>Originally by Barry van Oudtshoorn and released under BSD licence, with bug
 * reports, fixes, and contributions by
 * <ul>
 *   <li>Kevin de Groote</li>
 *   <li>Pedro Pinheiro</li>
 *   <li>Asier Lostal</li>
 * </ul>
 * Portions of the code is based on the YUI CssCompressor code, by Julien Lecomte.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.13.0
 * @since Berlioz 0.9.32
 */
public final class CSSMin {

  /**
   * Logger to know what is going on in the class.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(CSSMin.class);

  /**
   * Font weights
   */
  private static final Map<String, String> FONT_WEIGHTS = Map.of(
    "normal",  "400",
    "bold",    "700",
    "bolder",  "700",
    "lighter", "100"
  );

  /** CSS priority marker. */
  private static final String IMPORTANT = "!important";

  /** Utility class. */
  private CSSMin() {
  }

  /**
   * Process a file from a filename.
   *
   * @param file The file of the CSS file to process.
   * @param out Where to send the result
   */
  static void minimize(File file, OutputStream out) {
    try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
      minimize(reader, out);
    } catch (IOException ex) {
      LOGGER.debug("Unable to read file", ex);
    }
  }

  /**
   * Process input from a reader.
   *
   * @param input Where to read the CSS from
   * @param out   Where to send the result
   */
  static void minimize(Reader input, OutputStream out) {
    try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
      minimize(input, writer);
    }
  }

  /**
   * Minify CSS from a reader to a printstream.
   *
   * @param input Where to read the CSS from
   * @param min   Where to write the result to
   */
  public static void minimize(Reader input, PrintWriter min) {
    String original = "";
    try {
      StringBuilder buffer = toBuffer(input);
      original = buffer.toString();
      String comment = stripComments(buffer);
      LOGGER.debug("Parsing and processing selectors.");

      List<Rule> rules = parseRules(buffer);

      StringBuilder minified = new StringBuilder(buffer.length());
      if (!comment.isEmpty()) minified.append(comment).append('\n');
      int countRules = 0;
      for (Rule rule : rules) {
        if (countRules % 10 == 0 || !rule.subrules.isEmpty()) {
          minified.append('\n');
        }
        rule.append(minified);
        countRules++;
      }
      minified.append('\n');
      min.print(minified);
      min.flush();

      LOGGER.debug("Process completed successfully.");

    } catch (IOException ex) {
      LOGGER.error("Unable to minimize CSS", ex);
    } catch (ParsingException | RuntimeException ex) {
      LOGGER.warn("Unable to minimize CSS, writing original content", ex);
      min.write(original);
      min.flush();
    }
  }

  /**
   * Loads the content of the styles as CSS trimming empty lines (to preserve line count)
   *
   * @param input the CSS code to read.
   * @return the buffer.
   *
   * @throws IOException Should an error occur while reading the file.
   */
   private static StringBuilder toBuffer(Reader input) throws IOException {
     StringBuilder buffer = new StringBuilder();
     try (BufferedReader br = new BufferedReader(input)) {
       String s;
       while ((s = br.readLine()) != null) {
         if (!s.isBlank()) {
           buffer.append(s);
         }
         buffer.append('\n');
       }
     }
     return buffer;
   }

   /**
    * Strips comments from the buffer.
    *
    * @param buffer the buffer to strip the comments from.
    * @return the updated buffer.
    *
    * @throws ParsingException Should an error occur while reading the file.
    */
  @SuppressWarnings("java:S3776")
  private static String stripComments(StringBuilder buffer) throws ParsingException {
    StringBuilder css = new StringBuilder(buffer.length());
    StringBuilder comments = new StringBuilder();
    char quote = 0;
    boolean escaped = false;
    int i = 0;
    while (i < buffer.length()) {
      char c = buffer.charAt(i);
      if (quote != 0) {
        css.append(c);
        if (escaped) {
          escaped = false;
        } else if (c == '\\') {
          escaped = true;
        } else if (c == quote) {
          quote = 0;
        }
        i++;
      } else if (c == '\'' || c == '"') {
        quote = c;
        css.append(c);
        i++;
      } else if (c == '/' && i + 1 < buffer.length() && buffer.charAt(i + 1) == '*') {
        int end = buffer.indexOf("*/", i + 2);
        if (end == -1) throw new ParsingException("Unterminated comment. Aborting.", -1, -1);
        if (i + 2 < buffer.length() && (buffer.charAt(i + 2) == '*' || buffer.charAt(i + 2) == '!')) {
          comments.append(buffer, i, end + 2);
        }
        appendNewLines(buffer, i, end, css);
        i = end + 2;
      } else {
        css.append(c);
        i++;
      }
    }
    buffer.setLength(0);
    buffer.append(css);
    return cleanComment(comments.toString());
  }

  /**
   * Appends new lines found in a removed section so downstream line numbers remain useful.
   */
  private static void appendNewLines(CharSequence source, int from, int to, StringBuilder target) {
    for (int i = from; i < to; i++) {
      if (source.charAt(i) == '\n') {
        target.append('\n');
      }
    }
  }

  /**
   * Clean the comment by removing unnecessary white space.
   *
   * @param comment the comment string to clean.
   *
   * @return the clean comment.
   */
  private static String cleanComment(String comment) {
    return comment.replace("\n * ", " ").replace("\n", "");
  }

  /**
   * Parses CSS rules from the specified input.
   */
  @SuppressWarnings("java:S3776") // Splitting this method would help
  private static List<Rule> parseRules(CharSequence css) throws ParsingException {
    List<Rule> rules = new ArrayList<>();
    int start = 0;
    int line = 0;
    ScanState state = new ScanState();
    int i = 0;
    while (i < css.length()) {
      char c = css.charAt(i);
      if (c == '\n') {
        line++;
      }
      if (state.accept(c)) {
        i++;
      } else if (c == '}') {
        throw new ParsingException("Unbalanced braces!", line, -1);
      } else if (c == '{') {
        String selector = css.subSequence(start, i).toString().trim();
        int end = findMatchingBrace(css, i, line);
        if (!selector.isEmpty()) {
          try {
            rules.add(new Rule(selector, css.subSequence(i + 1, end).toString()));
          } catch (ParsingException ex) {
            LOGGER.warn("{} L:{}", ex.getMessage(), line);
          }
        }
        line += countNewLines(css, i + 1, end);
        i = end + 1;
        start = end + 1;
        state.reset();
      } else {
        i++;
      }
    }
    if (!css.subSequence(start, css.length()).toString().isBlank()) {
      LOGGER.debug("Ignoring CSS text without a rule block.");
    }
    return rules;
  }

  /**
   * Counts new line characters in the given source range.
   */
  private static int countNewLines(CharSequence source, int from, int to) {
    int count = 0;
    for (int i = from; i < to; i++) {
      if (source.charAt(i) == '\n') {
        count++;
      }
    }
    return count;
  }

  /**
   * Finds the closing brace matching the opening brace at the specified position.
   */
  private static int findMatchingBrace(CharSequence css, int open, int line) throws ParsingException {
    int depth = 1;
    ScanState state = new ScanState();
    for (int i = open + 1; i < css.length(); i++) {
      char c = css.charAt(i);
      if (c == '\n') {
        line++;
      }
      if (state.accept(c)) {
        continue;
      }
      if (c == '{') {
        depth++;
      } else if (c == '}') {
        depth--;
        if (depth == 0) return i;
      }
    }
    throw new ParsingException("Unbalanced braces!", line, -1);
  }

  /**
   * Splits a CSS fragment on the specified delimiter, ignoring delimiters inside strings and functions.
   */
  private static List<String> splitTopLevel(String css, char delimiter) {
    List<String> parts = new ArrayList<>();
    int start = 0;
    ScanState state = new ScanState();
    for (int i = 0; i < css.length(); i++) {
      char c = css.charAt(i);
      if (state.accept(c)) {
        continue;
      }
      if (c == delimiter) {
        parts.add(css.substring(start, i));
        start = i + 1;
      }
    }
    parts.add(css.substring(start));
    return parts;
  }

  /**
   * Tracks quote and function state while scanning CSS.
   */
  private static final class ScanState {

    private char quote = 0;
    private boolean escaped = false;
    private int parentheses = 0;

    void reset() {
      this.quote = 0;
      this.escaped = false;
      this.parentheses = 0;
    }

    boolean accept(char c) {
      if (this.quote != 0) {
        if (this.escaped) {
          this.escaped = false;
        } else if (c == '\\') {
          this.escaped = true;
        } else if (c == this.quote) {
          this.quote = 0;
        }
        return true;
      }
      if (c == '\'' || c == '"') {
        this.quote = c;
        return true;
      }
      if (c == '(') {
        this.parentheses++;
        return true;
      }
      if (c == ')' && this.parentheses > 0) {
        this.parentheses--;
        return true;
      }
      return this.parentheses > 0;
    }
  }

  /**
   * A CSS rule.
   *
   * <p>For example, "div { border: solid 1px red; color: blue; }"
   */
  private static class Rule {

    /** The selector */
    private final String selector;

    /** Properties inside the selector. */
    private final Property[] properties;

    /** Properties inside the selector. */
    private final List<Rule> subrules;

    /**
     * Creates a rule from an already separated selector and body.
     */
    private Rule(String selector, String contents) throws ParsingException {
      this.selector = minifySelector(selector);
      String body = contents.trim();
      this.subrules = containsTopLevelRule(body) ? parseRules(body) : List.of();
      this.properties = this.subrules.isEmpty() && !body.isEmpty() ? parseProperties(body) : new Property[]{};
    }

    /**
     * Returns <code>true</code> if the CSS fragment includes a nested rule.
     */
    private static boolean containsTopLevelRule(String css) {
      ScanState state = new ScanState();
      for (int i = 0; i < css.length(); i++) {
        char c = css.charAt(i);
        if (state.accept(c)) {
          continue;
        }
        if (c == '{') return true;
      }
      return false;
    }

    /**
     * Minifies selectors without touching strings in attribute selectors.
     */
    private static String minifySelector(String selector) {
      String trimmed = selector.trim();
      StringBuilder minified = new StringBuilder(trimmed.length());
      ScanState state = new ScanState();
      int i = 0;
      while (i < trimmed.length()) {
        char c = trimmed.charAt(i);
        if (state.accept(c)) {
          minified.append(c);
          i++;
          continue;
        }
        String operator = selectorOperatorAt(trimmed, i);
        if (operator != null) {
          trimTrailingWhitespace(minified);
          minified.append(operator);
          i = skipWhitespace(trimmed, i + operator.length());
        } else {
          minified.append(c);
          i++;
        }
      }
      return minified.toString();
    }

    /**
     * Returns the selector operator at the specified position.
     */
    private static @Nullable String selectorOperatorAt(String selector, int offset) {
      if (offset + 1 < selector.length()) {
        String pair = selector.substring(offset, offset + 2);
        if ("~=".equals(pair) || "^=".equals(pair) || "$=".equals(pair)
            || "*=".equals(pair) || "|=".equals(pair)) {
          return pair;
        }
      }
      char c = selector.charAt(offset);
      return c == '+' || c == '~' || c == ',' || c == '=' || c == '>' ? String.valueOf(c) : null;
    }

    /**
     * Removes whitespace from the end of the builder.
     */
    private static void trimTrailingWhitespace(StringBuilder builder) {
      while (builder.length() > 0 && Character.isWhitespace(builder.charAt(builder.length() - 1))) {
        builder.deleteCharAt(builder.length() - 1);
      }
    }

    /**
     * Skips whitespace from the specified position.
     */
    private static int skipWhitespace(String value, int offset) {
      int i = offset;
      while (i < value.length() && Character.isWhitespace(value.charAt(i))) {
        i++;
      }
      return i;
    }

    /**
     * Prints out this selector and its contents nicely, with the contents sorted alphabetically.
     *
     * @return A string representing this selector, minified.
     */
    @Override
    public String toString() {
      return append(new StringBuilder()).toString();
    }

    /**
     * Prints out this selector and its contents nicely, with the contents sorted alphabetically.
     *
     * @param min The
     * @return the string bufferer
     */
    public StringBuilder append(StringBuilder min) {
      min.append(this.selector).append('{');
      for (Rule s : this.subrules) {
        s.append(min);
      }
      for (Property p : this.properties) {
        p.append(min);
      }
      if (min.charAt(min.length() - 1) == ';') {
        min.deleteCharAt(min.length() - 1);
      }
      min.append('}');
      return min;
    }

    /**
     * Parses out the properties of a selector's body.
     *
     * @param contents The body; for example, "border: solid 1px red; color: blue;"
     * @return An array of properties parsed from this selector.
     */
    private Property[] parseProperties(String contents) {
      List<String> parts = splitTopLevel(contents, ';');
      List<Property> valid = new ArrayList<>(parts.size());
      for (String part : parts) {
        if (part.isBlank()) continue;
        try {
          valid.add(new Property(part));
        } catch (Exception e) {
          LOGGER.warn(e.getMessage());
        }
      }
      valid.sort(Property.BY_NAME);
      return valid.toArray(new Property[0]);
    }

  }

  /**
   * A CSS property.
   *
   * <p>For example:
   * <ul>
   *   <li><code>"border: solid 1px red;"</code></li>
   *   <li><code>"-moz-box-shadow: 3px 3px 3px rgba(255, 255, 0, 0.5);"</code></li>
   * </ul>
   */
  private static class Property {

    private static final Comparator<Property> BY_NAME = (a, b) -> {
      String aN = stripSortPrefix(a.name);
      String bN = stripSortPrefix(b.name);
      return aN.compareTo(bN);
    };

    private static String stripSortPrefix(String name) {
      if (name.charAt(0) == '-') {
        String s = name.substring(1);
        return s.substring(s.indexOf('-') + 1);
      } else if (name.charAt(0) < 'a') {
        return name.substring(1);
      }
      return name;
    }

    private static final Pattern RGB_PATTERN = Pattern.compile(
        "rgb\\s*+\\(\\s*+(\\d++\\s*+,\\s*+\\d++\\s*+,\\s*+\\d++)\\s*+\\)",
        Pattern.CASE_INSENSITIVE);

    /**
     * Name of the property
     */
    private final String name;

    /**
     * The various parts of the property.
     */
    private final Part[] parts;

    /**
     * Creates a new Property using the supplied strings.
     *
     * <p>Parses out the values of the property selector.
     *
     * @param property The property;
     * @throws ParsingException If the property is incomplete and cannot be parsed.
     */
    public Property(String property) throws ParsingException {
      int colon = indexOfTopLevel(property, ':');
      if (colon == -1) throw new ParsingException("Warning: Incomplete property: "+property, -1, -1);
      this.name = property.substring(0, colon).trim().toLowerCase();
      this.parts = parseValues(simplifyColours(property.substring(colon + 1).trim()));
      if (this.parts.length == 0) throw new ParsingException("Warning: Incomplete property: "+property, -1, -1);
    }

    /**
     * Prints out this property nicely.
     *
     * @return A string representing this property, minified.
     */
    @Override
    public String toString() {
      return append(new StringBuilder()).toString();
    }

    /**
     * Prints out this property nicely.
     * @param min the minified string to append to.
     * @return A string representing this property, minified.
     */
    public StringBuilder append(StringBuilder min) {
      min.append(this.name).append(":");
      for (Part p : this.parts) {
        min.append(p).append(",");
      }
      min.deleteCharAt(min.length() - 1); // Delete the trailing comma.
      min.append(";");
      return min;
    }


    /**
     * Finds a delimiter outside strings and functions.
     */
    private static int indexOfTopLevel(String css, char delimiter) {
      ScanState state = new ScanState();
      for (int i = 0; i < css.length(); i++) {
        char c = css.charAt(i);
        if (state.accept(c)) {
          continue;
        }
        if (c == delimiter) return i;
      }
      return -1;
    }

    /**
     * Parse the values out of a property.
     *
     * @param contents The property to parse
     * @return An array of Parts
     */
    private Part[] parseValues(String contents) {
      List<String> rawParts = splitTopLevel(contents, ',');
      List<Part> valid = new ArrayList<>(rawParts.size());
      for (String raw : rawParts) {
        if (raw.isBlank()) continue;
        try {
          valid.add(Part.newPart(raw, this.name));
        } catch (Exception ex) {
          LOGGER.warn(ex.getMessage());
        }
      }
      return valid.toArray(new Part[0]);
    }

    /**
     * Convert rgb(51,102,153) to #336699 (this code largely based on YUI code).
     *
     * @param contents The color to replace
     * @return the simplified color.
     */
    private static String simplifyColours(String contents) {
      StringBuilder newContents = new StringBuilder();
      Matcher matcher = RGB_PATTERN.matcher(contents);
      while (matcher.find()) {
        StringBuilder hexColour = new StringBuilder("#");
        for (String rgbColour : matcher.group(1).split(",")) {
          int colourValue = Integer.parseInt(rgbColour.trim());
          if (colourValue < 16) hexColour.append("0");
          hexColour.append(Integer.toHexString(colourValue));
        }
        matcher.appendReplacement(newContents, hexColour.toString());
      }
      matcher.appendTail(newContents);
      return newContents.toString();
    }
  }

  /**
   * A property part.
   */
  private static class Part {

    private static final Pattern ZERO_UNIT_PATTERN =
        Pattern.compile("(\\s)(0)(px|em|%|in|cm|mm|pc|pt|ex)", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEX_COLOR_PATTERN =
        Pattern.compile("#([0-9a-fA-F])([0-9a-fA-F])([0-9a-fA-F])([0-9a-fA-F])([0-9a-fA-F])([0-9a-fA-F])");
    private static final Pattern CSS_IDENTIFIER_PATTERN = Pattern.compile("-?[_a-zA-Z][_a-zA-Z0-9-]*");

    /** Color name → shorter hex value. */
    private static final Map<String, String> COLOR_NAME_TO_HEX;
    /** Hex value → shorter color name. */
    private static final Map<String, String> COLOR_HEX_TO_NAME;

    static {
      String[] names = Constants.HTML_COLOR_NAMES;
      String[] values = Constants.HTML_COLOR_VALUES;
      Map<String, String> n2h = new HashMap<>();
      Map<String, String> h2n = new HashMap<>();
      for (int i = 0; i < names.length; i++) {
        String name = names[i].trim();
        String hex = values[i];
        if (hex.length() < name.length()) n2h.put(name, hex);
        if (name.length() < hex.length()) h2n.put(hex, name);
      }
      COLOR_NAME_TO_HEX = Map.copyOf(n2h);
      COLOR_HEX_TO_NAME = Map.copyOf(h2n);
    }

    /**
     * The property value.
     */
    private final String value;

    /**
     * Create a new property part by parsing the given string.
     *
     * @param value The value for this part.
     */
    private Part(String value) {
      this.value = value;
    }

    /**
     * Create a new property part by parsing the given string.
     *
     * @param contents The string to parse.
     * @param property The name of the property is part belongs to.
     */
    public static Part newPart(String contents, String property) {
      // Many of these regular expressions are adapted from those used in the YUI CSS Compressor.
      // For simpler regexes.
      return new Part(simplify(property,  " " + contents));
    }

    /**
     * Simplifies the part.
     */
    public static String simplify(String property, String value) {
      // !important doesn't need to be spaced
      String result = value.replace(" " + IMPORTANT, IMPORTANT);

      // Replace 0in, 0cm, etc. with just 0
      result = ZERO_UNIT_PATTERN.matcher(result).replaceAll("$1$2");

      // Now we can trim
      result = result.trim();

      // Simplify multiple zeroes
      if (result.equals("0 0 0 0") || result.equals("0 0 0") || result.equals("0 0")) {
        result = "0";
      }

      // Simplify multiple-parameter properties
      result = simplifyParameters(result);

      // Simplify font weights (only applies to `font-weight`)
      if (property.equals("font-weight")) {
        result = simplifyFontWeights(result);
      }

      // Strip unnecessary quotes from url() and single-word parts, and make as much lowercase as possible.
      result = simplifyQuotesAndCaps(result);

      // Simplify colours
      result = simplifyColourNames(result);
      result = simplifyHexColours(result);

      // Done!
      return result;
    }

    /**
     * Simplifies multiple-parameter properties.
     */
    protected static String simplifyParameters(String value) {
      List<String> params = splitWhitespace(value);
      if (params.isEmpty() || isQuoted(params.get(0))) return value;

      // We can drop off the fourth item if the second and fourth items match
      // ie turn 3px 0 3px 0 into 3px 0 3px
      if (params.size() == 4 && params.get(1).equalsIgnoreCase(params.get(3))) {
        params = new ArrayList<>(params.subList(0, 3));
      }
      // We can drop off the third item if the first and third items match
      // ie turn 3px 0 3px into 3px 0
      if (params.size() == 3 && params.get(0).equalsIgnoreCase(params.get(2))) {
        params = new ArrayList<>(params.subList(0, 2));
      }
      // We can drop off the second item if the first and second items match
      // ie turn 3px 3px into 3px
      if (params.size() == 2 && params.get(0).equalsIgnoreCase(params.get(1))) {
        params = new ArrayList<>(params.subList(0, 1));
      }

      return String.join(" ", params);
    }

    /**
     * Simplifies font weights.
     */
    protected static String simplifyFontWeights(String value) {
      String result = FONT_WEIGHTS.get(value.toLowerCase());
      return result != null? result : value;
    }

    /**
     * Simplifies quotes and caps.
     */
    protected static String simplifyQuotesAndCaps(String value) {
      String result = value.trim();
      // Strip quotes from URLs
      if ((result.length() > 4) && ("url(".equalsIgnoreCase(result.substring(0, 4)))) {
        UrlValue urlValue = parseUrlFunction(result);
        if (urlValue != null) {
          result = isSafeUnquotedUrl(urlValue.value) ?
              "url(" + urlValue.value + ")" : "url(" + urlValue.quote + urlValue.value + urlValue.quote + ")";
        }
      } else {
        List<String> words = splitWhitespace(result);
        if (words.size() == 1) {
          String word = words.get(0);
          result = isQuoted(word) && isCssIdentifier(word.substring(1, word.length() - 1)) ?
              word.substring(1, word.length() - 1).toLowerCase() : lowercaseIfUnquoted(word);
        }
      }
      return result;
    }

    /**
     * Simplifies color names.
     */
    protected static String simplifyColourNames(String value) {
      String important = "";
      String core = value.trim();
      if (core.toLowerCase().endsWith(IMPORTANT)) {
        important = IMPORTANT;
        core = core.substring(0, core.length() - important.length());
      }
      String lc = core.toLowerCase();
      String hex = COLOR_NAME_TO_HEX.get(lc);
      if (hex != null) return hex + important;
      String name = COLOR_HEX_TO_NAME.get(lc);
      return name != null ? name + important : value;
    }

    /**
     * Simplifies color names.
     */
    protected static String simplifyHexColours(String value) {
      StringBuilder result = new StringBuilder();
      Matcher matcher = HEX_COLOR_PATTERN.matcher(value);
      while (matcher.find()) {
        if (matcher.group(1).equalsIgnoreCase(matcher.group(2))
            && matcher.group(3).equalsIgnoreCase(matcher.group(4))
            && matcher.group(5).equalsIgnoreCase(matcher.group(6))) {
          matcher.appendReplacement(result,
              Matcher.quoteReplacement("#" + matcher.group(1).toLowerCase() + matcher.group(3).toLowerCase() + matcher.group(5).toLowerCase()));
        } else {
          matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group().toLowerCase()));
        }
      }
      matcher.appendTail(result);
      return result.toString();
    }

    /**
     * Splits a value on whitespace outside quoted sections.
     */
    @SuppressWarnings("java:S3776") // Splitting this method would help
    private static List<String> splitWhitespace(String value) {
      List<String> words = new ArrayList<>();
      StringBuilder word = new StringBuilder();
      char quote = 0;
      boolean escaped = false;
      for (int i = 0; i < value.length(); i++) {
        char c = value.charAt(i);
        if (quote != 0) {
          word.append(c);
          if (escaped) {
            escaped = false;
          } else if (c == '\\') {
            escaped = true;
          } else if (c == quote) {
            quote = 0;
          }
        } else if (c == '\'' || c == '"') {
          quote = c;
          word.append(c);
        } else if (Character.isWhitespace(c)) {
          if (word.length() > 0) {
            words.add(word.toString());
            word.setLength(0);
          }
        } else {
          word.append(c);
        }
      }
      if (word.length() > 0) {
        words.add(word.toString());
      }
      return words;
    }

    /**
     * @return <code>true</code> if the supplied value is a quoted CSS string.
     */
    private static boolean isQuoted(String value) {
      return value.length() >= 2
          && ((value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"')
          || (value.charAt(0) == '\'' && value.charAt(value.length() - 1) == '\''));
    }

    /**
     * Parses a complete CSS url(...) value.
     */
    private static @Nullable UrlValue parseUrlFunction(String value) {
      if (value.length() < 5
          || !startsWithIgnoreCase(value, "url(")
          || value.charAt(value.length() - 1) != ')') {
        return null;
      }
      String content = value.substring(4, value.length() - 1).trim();
      if (content.isEmpty()) {
        return new UrlValue("", "\"");
      }
      char first = content.charAt(0);
      if (first == '\'' || first == '"') {
        int end = findStringEnd(content, first);
        if (end > 0 && isBlank(content, end + 1, content.length())) {
          return new UrlValue(content.substring(1, end), String.valueOf(first));
        }
        return null;
      }
      return new UrlValue(content, "\"");
    }

    /**
     * @return the index of the end quote or <code>-1</code> if the string is unterminated.
     */
    private static int findStringEnd(String value, char quote) {
      boolean escaped = false;
      for (int i = 1; i < value.length(); i++) {
        char c = value.charAt(i);
        if (escaped) {
          escaped = false;
        } else if (c == '\\') {
          escaped = true;
        } else if (c == quote) {
          return i;
        }
      }
      return -1;
    }

    /**
     * @return <code>true</code> if the range only contains whitespace.
     */
    private static boolean isBlank(String value, int from, int to) {
      for (int i = from; i < to; i++) {
        if (!Character.isWhitespace(value.charAt(i))) {
          return false;
        }
      }
      return true;
    }

    /**
     * Case-insensitive prefix check.
     */
    private static boolean startsWithIgnoreCase(String value, String prefix) {
      return value.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    /**
     * Lower-cases a single token when doing so cannot alter a string literal.
     */
    private static String lowercaseIfUnquoted(String value) {
      return isQuoted(value) ? value : value.toLowerCase();
    }

    /**
     * @return <code>true</code> if the value can be represented as an unquoted CSS identifier.
     */
    private static boolean isCssIdentifier(String value) {
      return CSS_IDENTIFIER_PATTERN.matcher(value).matches();
    }

    /**
     * @return <code>true</code> if a URL can safely lose its quotes.
     */
    private static boolean isSafeUnquotedUrl(String url) {
      for (int i = 0; i < url.length(); i++) {
        char c = url.charAt(i);
        if (Character.isWhitespace(c) || c == '"' || c == '\'' || c == '(' || c == ')' || c == '\\') {
          return false;
        }
      }
      return true;
    }

    /**
     * Returns itself.
     * @return this part's string representation.
     */
    @Override
    public String toString() {
      return this.value;
    }

    /**
     * Parsed url(...) content.
     */
    private static final class UrlValue {

      private final String value;
      private final String quote;

      private UrlValue(String value, String quote) {
        this.value = value;
        this.quote = quote;
      }
    }

  }

  /**
   * Main entry point for CSSMin from the command-line.
   *
   * <p><b>Usage:</b> CSSMin <i>[Input file]</i>, <i>[Output file]</i>
   *
   * @param args The command-line arguments
   */
  @SuppressWarnings("java:S106")
  public static void main(String[] args) {
    if (args.length < 1) {
      System.err.println("Usage: ");
      System.err.println("CSSMin [Input file] [Output file]");
      System.err.println("If no output file is specified, stdout will be used.");
      return;
    }

    try {
      File file = new File(args[0]).getCanonicalFile();
      String currentPath = new File(".").getCanonicalPath();
      if (!file.toPath().startsWith(currentPath) || !file.exists() || file.isDirectory())
        throw new IllegalArgumentException("Illegal filepath argument");
      minimize(file, System.out);
    } catch (IOException | IllegalArgumentException ex) {
      System.err.println(ex.getMessage());
      System.exit(1);
    }
  }

  /**
   * Constants for replacement.
   */
  private static final class Constants {

    private Constants() {}

    /**
     * Color name - index must match color codes below.
     */
    static final String[] HTML_COLOR_NAMES = {
        "aliceblue",
        "antiquewhite",
        "aqua",
        "aquamarine",
        "azure",
        "beige",
        "bisque",
        "black",
        "blanchedalmond",
        "blue",
        "blueviolet",
        "brown",
        "burlywood",
        "cadetblue",
        "chartreuse",
        "chocolate",
        "coral",
        "cornflowerblue",
        "cornsilk",
        "crimson",
        "cyan",
        "darkblue",
        "darkcyan",
        "darkgoldenrod",
        "darkgray",
        "darkgreen",
        "darkkhaki",
        "darkmagenta",
        "darkolivegreen",
        "darkorange",
        "darkorchid",
        "darkred",
        "darksalmon",
        "darkseagreen",
        "darkslateblue",
        "darkslategray",
        "darkturquoise",
        "darkviolet",
        "deeppink",
        "deepskyblue",
        "dimgray",
        "dodgerblue",
        "firebrick",
        "floralwhite",
        "forestgreen",
        "fuchsia",
        "gainsboro",
        "ghostwhite",
        "gold",
        "goldenrod",
        "gray",
        "green",
        "greenyellow",
        "honeydew",
        "hotpink",
        "indianred ",
        "indigo ",
        "ivory",
        "khaki",
        "lavender",
        "lavenderblush",
        "lawngreen",
        "lemonchiffon",
        "lightblue",
        "lightcoral",
        "lightcyan",
        "lightgoldenrodyellow",
        "lightgrey",
        "lightgreen",
        "lightpink",
        "lightsalmon",
        "lightseagreen",
        "lightskyblue",
        "lightslategray",
        "lightsteelblue",
        "lightyellow",
        "lime",
        "limegreen",
        "linen",
        "magenta",
        "maroon",
        "mediumaquamarine",
        "mediumblue",
        "mediumorchid",
        "mediumpurple",
        "mediumseagreen",
        "mediumslateblue",
        "mediumspringgreen",
        "mediumturquoise",
        "mediumvioletred",
        "midnightblue",
        "mintcream",
        "mistyrose",
        "moccasin",
        "navajowhite",
        "navy",
        "oldlace",
        "olive",
        "olivedrab",
        "orange",
        "orangered",
        "orchid",
        "palegoldenrod",
        "palegreen",
        "paleturquoise",
        "palevioletred",
        "papayawhip",
        "peachpuff",
        "peru",
        "pink",
        "plum",
        "powderblue",
        "purple",
        "red",
        "rosybrown",
        "royalblue",
        "saddlebrown",
        "salmon",
        "sandybrown",
        "seagreen",
        "seashell",
        "sienna",
        "silver",
        "skyblue",
        "slateblue",
        "slategray",
        "snow",
        "springgreen",
        "steelblue",
        "tan",
        "teal",
        "thistle",
        "tomato",
        "turquoise",
        "violet",
        "wheat",
        "white",
        "whitesmoke",
        "yellow",
        "yellowgreen"
    };

    /**
     * Color hex codes - index must match color names.
     */
    static final String[] HTML_COLOR_VALUES = {
        "#f0f8ff",
        "#faebd7",
        "#00ffff",
        "#7fffd4",
        "#f0ffff",
        "#f5f5dc",
        "#ffe4c4",
        "#000",
        "#ffebcd",
        "#00f",
        "#8a2be2",
        "#a52a2a",
        "#deb887",
        "#5f9ea0",
        "#7fff00",
        "#d2691e",
        "#ff7f50",
        "#6495ed",
        "#fff8dc",
        "#dc143c",
        "#0ff",
        "#00008b",
        "#008b8b",
        "#b8860b",
        "#a9a9a9",
        "#006400",
        "#bdb76b",
        "#8b008b",
        "#556b2f",
        "#ff8c00",
        "#9932cc",
        "#8b0000",
        "#e9967a",
        "#8fbc8f",
        "#483d8b",
        "#2f4f4f",
        "#00ced1",
        "#9400d3",
        "#ff1493",
        "#00bfff",
        "#696969",
        "#1e90ff",
        "#b22222",
        "#fffaf0",
        "#228b22",
        "#f0f",
        "#dcdcdc",
        "#f8f8ff",
        "#ffd700",
        "#daa520",
        "#808080",
        "#008000",
        "#adff2f",
        "#f0fff0",
        "#ff69b4",
        "#cd5c5c",
        "#4b0082",
        "#fffff0",
        "#f0e68c",
        "#e6e6fa",
        "#fff0f5",
        "#7cfc00",
        "#fffacd",
        "#add8e6",
        "#f08080",
        "#e0ffff",
        "#fafad2",
        "#d3d3d3",
        "#90ee90",
        "#ffb6c1",
        "#ffa07a",
        "#20b2aa",
        "#87cefa",
        "#789",
        "#b0c4de",
        "#ffffe0",
        "#0f0",
        "#32cd32",
        "#faf0e6",
        "#f0f",
        "#800000",
        "#66cdaa",
        "#0000cd",
        "#ba55d3",
        "#9370d8",
        "#3cb371",
        "#7b68ee",
        "#00fa9a",
        "#48d1cc",
        "#c71585",
        "#191970",
        "#f5fffa",
        "#ffe4e1",
        "#ffe4b5",
        "#ffdead",
        "#000080",
        "#fdf5e6",
        "#808000",
        "#6b8e23",
        "#ffa500",
        "#ff4500",
        "#da70d6",
        "#eee8aa",
        "#98fb98",
        "#afeeee",
        "#d87093",
        "#ffefd5",
        "#ffdab9",
        "#cd853f",
        "#ffc0cb",
        "#dda0dd",
        "#b0e0e6",
        "#800080",
        "#f00",
        "#bc8f8f",
        "#4169e1",
        "#8b4513",
        "#fa8072",
        "#f4a460",
        "#2e8b57",
        "#fff5ee",
        "#a0522d",
        "#c0c0c0",
        "#87ceeb",
        "#6a5acd",
        "#708090",
        "#fffafa",
        "#00ff7f",
        "#4682b4",
        "#d2b48c",
        "#008080",
        "#d8bfd8",
        "#ff6347",
        "#40e0d0",
        "#ee82ee",
        "#f5deb3",
        "#fff",
        "#f5f5f5",
        "#ff0",
        "#9acd32"
    };

  }

}
