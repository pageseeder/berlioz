/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.slf4j.LoggerFactory;

/**
 * A utility class providing a simple method to generate MD5 hash values for text content.
 * 
 * <p>An MD5 hash is typically expressed as a 32-digit hexadecimal number.
 * 
 * @author Christophe Lauret
 * @version 31 May 2010
 */
public final class MD5 {

  /**
   * Character set for the hashing function.
   */
  private static final Charset UTF8 = Charset.forName("utf-8");

  /**
   * Stores the hex character for easy retrieval.
   */
  private static final char[] HEX = "0123456789abcdef".toCharArray();

  /**
   * Prevents creation of instance.
   */
  private MD5() {
  }

  /**
   * Returns a hash value for the specified text.
   *
   * @param text The text value to hash.
   * 
   * @return The {@link HashMap} value for the specified test or <code>null</code> if an error occurred.
   * 
   * @throws UnsupportedOperationException If the MD5 algorithm is not available for that platform.
   */
  public static String hash(String text) throws UnsupportedOperationException {
    MessageDigest md = getAlgorithm();
    md.update(text.getBytes(UTF8), 0, text.length());
    byte[] bytes = md.digest();
    return toHex(bytes);
  }

  /**
   * Returns a hash value for the specified file.
   * 
   * @param file The file to read
   * @return The MD5 checksum value as a string.
   * @throws IOException If the file does not exist or an error occurred while reading the file.
   * @throws UnsupportedOperationException If the MD5 algorithm is not available for that platform.
   */
  public static String hash(File file) throws IOException, UnsupportedOperationException {
    MessageDigest md = getAlgorithm();
    FileChannel in = new FileInputStream(file).getChannel();
    try {
      MappedByteBuffer buffer = in.map(FileChannel.MapMode.READ_ONLY, 0, in.size());
      md.update(buffer);
      byte[] bytes = md.digest();
      return toHex(bytes);
    } finally {
      in.close();
    }
  }

  /**
   * Returns a hash value for the specified file.
   * 
   * @param file The file to read
   * @return The MD5 checksum value as a string.
   * @throws IOException If the file does not exist or an error occurred while reading the file.
   * @throws UnsupportedOperationException If the MD5 algorithm is not available for that platform.
  public static String hash(File file) throws IOException, UnsupportedOperationException {
    long length = file.length();
    byte[] buffer = new byte[length > 8096? 8096 : (int)length];
    MessageDigest md = getAlgorithm();
    InputStream in = new FileInputStream(file);
    try {
      int len;
      do {
        len = in.read(buffer);
        if (len > 0) {
          md.update(buffer, 0, len);
        }
      } while (len != -1);
    } finally {
      in.close();
    }
    byte[] bytes = md.digest();
    return toHex(bytes);
  }
  */

  // Private helpers
  // ----------------------------------------------------------------------------------------------

  /**
   * Converts the byte data into a sequence of hexadecimal characters.
   * 
   * @param data The byte array to convert.
   * @return the corresponding sequence of hexadecimal characters.
   */
  private static String toHex(byte[] data) {
    if (data == null) return null;
    final StringBuilder hex = new StringBuilder(2 * data.length);
    for (final byte b : data) {
      hex.append(HEX[(b & 0xF0) >> 4]).append(HEX[(b & 0x0F)]);
    }
    return hex.toString();
  }

  /**
   * Returns the MD5 algorithm throwing an unchecked exception if the algorithm is not available.
   * 
   * @return the MD5 algorithm.
   * @throws UnsupportedOperationException Wrapping any occurring 'NoSuchAlgorithmException'.
   */
  private static MessageDigest getAlgorithm() throws UnsupportedOperationException {
    try {
      return MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException ex) {
      LoggerFactory.getLogger(MD5.class).warn("MD5 algorithm not available:", ex);
      throw new UnsupportedOperationException(ex);
    }
  }

}
