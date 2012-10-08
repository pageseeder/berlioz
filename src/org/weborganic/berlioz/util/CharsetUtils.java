package org.weborganic.berlioz.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A bunch of utility functions for dealing with character sets.
 * 
 * @author Christophe Lauret
 * @version 24 June 2011
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
    if (content == null) throw new NullPointerException("No length for null content");
    if (charset == null) throw new NullPointerException("Charset is null");
    int length = -1;
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

}
