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

import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A utility class providing a simple method to generate MD5 hash values for text content.
 *
 * <p>An MD5 hash is typically expressed as a 32-digit hexadecimal number.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.12.4
 * @since Berlioz 0.12.4
 */
public final class SHA256 {

  /**
   * Stores the hex character for easy retrieval.
   */
  private static final char[] HEX = "0123456789abcdef".toCharArray();

  /**
   * Mask for the high bits of a byte.
   */
  private static final int BYTE_MASK_HIGH = 0xF0;

  /**
   * Mask for the low bits of a byte.
   */
  private static final int BYTE_MASK_LOW = 0x0F;

  /**
   * Prevents creation of instance.
   */
  private SHA256() {
  }

  /**
   * Returns a hash value for the specified text.
   *
   * @param text The text value to hash.
   *
   * @return The Hash value for the specified test or <code>null</code> if an error occurred.
   *
   * @throws UnsupportedOperationException If the MD5 algorithm is not available for that platform.
   */
  public static String hash(String text) throws UnsupportedOperationException {
    MessageDigest md = getAlgorithm();
    md.update(text.getBytes(StandardCharsets.UTF_8), 0, text.length());
    byte[] bytes = md.digest();
    return toHex(bytes);
  }

  /**
   * Returns a hash value for the specified file content.
   *
   * <p>Implementation note: this method loads the entire file using NIO.
   *
   * @param file The file to read
   * @return The MD5 checksum value as a string.
   *
   * @throws IOException If the file does not exist or an error occurred while reading the file.
   * @throws UnsupportedOperationException If the MD5 algorithm is not available for that platform.
   */
  public static String hash(File file) throws IOException, UnsupportedOperationException {
    MessageDigest md = getAlgorithm();
    FileInputStream fis = new FileInputStream(file);
    FileChannel in = fis.getChannel();
    try {
      MappedByteBuffer buffer = in.map(FileChannel.MapMode.READ_ONLY, 0, in.size());
      md.update(buffer);
      byte[] bytes = md.digest();
      return toHex(bytes);
    } finally {
      fis.close();
    }
  }

  /**
   * Returns a hash value for the specified file.
   *
   * @param file The file to read
   * @param strong <code>true</code> to calculate a strong etag based on the file content;
   *               <code>false</code> to compute it from the canonical path, date and length.
   * @return The MD5 checksum value as a string.
   * @throws IOException If the file does not exist or an error occurred while reading the file.
   * @throws UnsupportedOperationException If the MD5 algorithm is not available for that platform.
   */
  public static String hash(File file, boolean strong) throws IOException, UnsupportedOperationException {
    if (strong) return hash(file);
    else
      return hash(file.getCanonicalPath()+'$'+file.length()+'%'+file.lastModified());
  }

  // Private helpers
  // ----------------------------------------------------------------------------------------------

  /**
   * Converts the byte data into a sequence of hexadecimal characters.
   *
   * @param data The byte array to convert.
   * @return the corresponding sequence of hexadecimal characters.
   */
  private static String toHex(byte[] data) {
    final StringBuilder hex = new StringBuilder(2 * data.length);
    final int shift = 4;
    for (final byte b : data) {
      hex.append(HEX[(b & BYTE_MASK_HIGH) >> shift]).append(HEX[(b & BYTE_MASK_LOW)]);
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
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException ex) {
      // Every implementation of the Java platform is required to support the MD5 algorithm
      throw new UnsupportedOperationException(ex);
    }
  }

}
