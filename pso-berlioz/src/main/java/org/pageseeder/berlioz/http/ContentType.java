/*
 * Copyright 2026 Allette Systems (Australia)
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
package org.pageseeder.berlioz.http;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * An immutable parsed {@code Content-Type} header value.
 *
 * <p>This class parses the media type and its case-insensitive parameters. Parameter values may
 * be HTTP tokens or quoted strings; quoted-pair escapes are removed when a quoted value is
 * parsed. Malformed values and duplicate parameter names are rejected.</p>
 *
 * <p>No defaults are applied. In particular, {@link #charset()} returns {@code null} when no
 * charset is declared, leaving request validation or response fallback policy to the caller.</p>
 *
 * @author Christophe Lauret
 *
 * @version 0.14.1
 * @since 0.14.1
 */
public final class ContentType {

  /** The media type without parameters. */
  private final String mediaType;

  /** Parameters keyed by their lowercase, case-insensitive name. */
  private final Map<String, String> parameters;

  private ContentType(String mediaType, Map<String, String> parameters) {
    this.mediaType = mediaType;
    this.parameters = parameters;
  }

  /**
   * Parses a {@code Content-Type} header value.
   *
   * @param value the complete header value
   * @return the parsed content type
   * @throws NullPointerException if {@code value} is {@code null}
   * @throws IllegalArgumentException if the media type or any parameter is malformed, or a
   *                                  parameter name is repeated
   */
  public static ContentType parse(String value) {
    Objects.requireNonNull(value, "value");
    Parser parser = new Parser(value);
    return parser.parse();
  }

  /**
   * Returns the media type without parameters.
   *
   * @return the media type
   */
  public String mediaType() {
    return this.mediaType;
  }

  /**
   * Tests the media type case-insensitively.
   *
   * @param mediaType the media type to compare
   * @return {@code true} if it matches this content type
   */
  public boolean is(String mediaType) {
    return this.mediaType.equalsIgnoreCase(mediaType);
  }

  /**
   * Returns a parameter value, matching the parameter name case-insensitively.
   *
   * @param name the parameter name
   * @return the unquoted parameter value, or {@code null} if absent
   */
  public @Nullable String parameter(String name) {
    Objects.requireNonNull(name, "name");
    return this.parameters.get(name.toLowerCase(Locale.ROOT));
  }

  /**
   * Returns the declared charset.
   *
   * <p>No default is supplied when the {@code charset} parameter is absent.</p>
   *
   * @return the declared charset, or {@code null} if absent
   * @throws java.nio.charset.IllegalCharsetNameException if the declared name is illegal
   * @throws java.nio.charset.UnsupportedCharsetException if the declared charset is unsupported
   */
  public @Nullable Charset charset() {
    String name = parameter("charset");
    return name != null ? Charset.forName(name) : null;
  }

  /**
   * Parser for the media-type grammar defined by HTTP.
   */
  private static final class Parser {

    private final String value;

    private final int length;

    private int index;

    Parser(String value) {
      this.value = value;
      this.length = value.length();
    }

    ContentType parse() {
      skipWhitespace();
      String type = token("media type");
      require('/');
      String subtype = token("media subtype");
      String mediaType = type + '/' + subtype;
      Map<String, String> parameters = new LinkedHashMap<>();
      skipWhitespace();
      while (this.index < this.length) {
        require(';');
        skipWhitespace();
        if (this.index == this.length || this.value.charAt(this.index) == ';') continue;
        String name = token("parameter name");
        skipWhitespace();
        require('=');
        skipWhitespace();
        String parameterValue = this.index < this.length && this.value.charAt(this.index) == '"'
            ? quotedString()
            : token("parameter value");
        String key = name.toLowerCase(Locale.ROOT);
        if (parameters.putIfAbsent(key, parameterValue) != null) {
          throw malformed("Duplicate parameter '" + name + "'");
        }
        skipWhitespace();
        if (this.index < this.length && this.value.charAt(this.index) != ';') {
          throw malformed("Expected ';' after parameter value");
        }
      }
      return new ContentType(mediaType, Collections.unmodifiableMap(parameters));
    }

    private String token(String description) {
      int start = this.index;
      while (this.index < this.length && isTokenCharacter(this.value.charAt(this.index))) {
        this.index++;
      }
      if (start == this.index) throw malformed("Expected " + description);
      return this.value.substring(start, this.index);
    }

    private String quotedString() {
      this.index++;
      StringBuilder value = new StringBuilder();
      while (this.index < this.length) {
        char c = this.value.charAt(this.index++);
        if (c == '"') return value.toString();
        if (c == '\\') {
          if (this.index == this.length) throw malformed("Unterminated quoted parameter value");
          c = this.value.charAt(this.index++);
          if (!isQuotedPairCharacter(c)) throw malformed("Invalid quoted-pair character");
        } else if (!isQuotedTextCharacter(c)) {
          throw malformed("Invalid quoted-string character");
        }
        value.append(c);
      }
      throw malformed("Unterminated quoted parameter value");
    }

    private void require(char expected) {
      if (this.index >= this.length || this.value.charAt(this.index) != expected) {
        throw malformed("Expected '" + expected + "'");
      }
      this.index++;
    }

    private void skipWhitespace() {
      while (this.index < this.length) {
        char c = this.value.charAt(this.index);
        if (c != ' ' && c != '\t') return;
        this.index++;
      }
    }

    private IllegalArgumentException malformed(String message) {
      return new IllegalArgumentException(message + " at index " + this.index);
    }
  }

  private static boolean isTokenCharacter(char c) {
    return c >= '0' && c <= '9'
        || c >= 'A' && c <= 'Z'
        || c >= 'a' && c <= 'z'
        || "!#$%&'*+-.^_`|~".indexOf(c) >= 0;
  }

  private static boolean isQuotedTextCharacter(char c) {
    return c == '\t'
        || c == ' '
        || c == '!'
        || c >= '#' && c <= '['
        || c >= ']' && c <= '~'
        || c >= 0x80;
  }

  private static boolean isQuotedPairCharacter(char c) {
    return c == '\t' || c == ' ' || c >= '!' && c <= '~' || c >= 0x80;
  }
}
