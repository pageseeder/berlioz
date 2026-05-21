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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A utility class providing a simple method to generate MD5 hash values for text content.
 *
 * <p>An MD5 hash is typically expressed as a 32-digit hexadecimal number.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.13.0
 * @since Berlioz 0.12.4
 */
public final class SHA256 {

  /**
   * Stores the hex character for easy retrieval.
   */
  private static final char[] HEX = "0123456789abcdef".toCharArray();

  /**
   * The SHA-256 algorithm name.
   */
  private static final String ALGORITHM = "SHA-256";

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
    byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
    return toHex(getAlgorithm().digest(bytes));
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
    // Use an 8 KiB buffer as a conventional I/O default that balances throughput and memory use
    byte[] buffer = new byte[8192];
    try (InputStream in = Files.newInputStream(file.toPath())) {
      int read;
      while ((read = in.read(buffer)) != -1) {
        md.update(buffer, 0, read);
      }
    }
    return toHex(md.digest());
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
    StringBuilder hex = new StringBuilder(2 * data.length);
    for (byte b : data) {
      hex.append(HEX[(b >>> 4) & 0x0f]);
      hex.append(HEX[b & 0x0f]);
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
      return MessageDigest.getInstance(ALGORITHM);
    } catch (NoSuchAlgorithmException ex) {
      // SHA-256 is required by every Java platform implementation.
      throw new AssertionError(ALGORITHM + " should always be available", ex);
    }
  }

}
