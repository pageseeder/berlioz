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
package org.pageseeder.berlioz.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A bunch of utility functions for dealing with character sets.
 *
 * @author Christophe Lauret
 *
 * @version 0.11.2
 * @since 0.8.1
 */
public final class CharsetUtils {

  /**
   * Displays debug information.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(CharsetUtils.class);

  /** Utility class */
  private CharsetUtils() {
  }

  /**
   * Calculates the byte length of the specified content using the given charset.
   *
   * @param content The content to measure
   * @param charset The character set
   *
   * @return the byte length of the content based on a specified charset; or -1 if unable to calculate it
   *
   * @throws NullPointerException if either argument is <code>null</code>.
   */
  public static int length(CharSequence content, Charset charset) {
    Objects.requireNonNull(content, "No length for null content");
    Objects.requireNonNull(charset, "Charset is null");
    // UTF-8 is by far the most common charset for Berlioz output; count bytes directly
    // instead of running a full encode pass through a freshly allocated encoder and buffer.
    if (StandardCharsets.UTF_8.equals(charset)) {
      return lengthUtf8(content);
    }
    int length;
    try {
      CharsetEncoder encoder = charset.newEncoder();
      ByteBuffer bytes;
      bytes = encoder.encode(CharBuffer.wrap(content));
      length = bytes.limit();
    } catch (CharacterCodingException ex) {
      LOGGER.error("Unable to determine the length of specified content", ex);
      length = -1;
    }
    return length;
  }

  /**
   * Computes the UTF-8 byte length of the specified content without allocating an encoder or
   * an output buffer, matching the semantics of {@link CharsetEncoder#encode(CharBuffer)} which
   * reports (rather than replaces) malformed surrogate pairs.
   *
   * @param content The content to measure.
   *
   * @return the UTF-8 byte length; or -1 if the content contains an unpaired surrogate.
   */
  private static int lengthUtf8(CharSequence content) {
    int length = 0;
    int count = content.length();
    int i = 0;
    while (i < count) {
      char c = content.charAt(i);
      if (c < 0x80) {
        length += 1;
        i += 1;
      } else if (c < 0x800) {
        length += 2;
        i += 1;
      } else if (Character.isHighSurrogate(c)) {
        if (i + 1 < count && Character.isLowSurrogate(content.charAt(i + 1))) {
          length += 4;
          i += 2;
        } else {
          LOGGER.error("Unable to determine the length of specified content: unpaired surrogate at index {}", i);
          return -1;
        }
      } else if (Character.isLowSurrogate(c)) {
        LOGGER.error("Unable to determine the length of specified content: unpaired surrogate at index {}", i);
        return -1;
      } else {
        length += 3;
        i += 1;
      }
    }
    return length;
  }

}
