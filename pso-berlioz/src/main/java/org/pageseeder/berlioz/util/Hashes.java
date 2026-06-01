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
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility providing hash functions for common message digest algorithms.
 *
 * <p>MD5 and SHA-256 are required by every Java SE implementation.
 * SHA-384 and SHA-512 are available in all common JVM distributions but are not
 * mandated by the Java SE specification.
 *
 * <p>The individual algorithm classes ({@link MD5}, {@link SHA256}, {@link SHA384},
 * {@link SHA512}) delegate to this class and exist for convenience and backward compatibility.
 *
 * @author Christophe Lauret
 *
 * @version 0.13.0
 * @since 0.13.0
 */
public final class Hashes {

  /**
   * Stores the hex characters for easy retrieval.
   */
  private static final char[] HEX = "0123456789abcdef".toCharArray();

  /**
   * Message digest algorithms supported by this utility.
   *
   * <p>MD5 and SHA-256 are required by every Java SE implementation.
   * SHA-384 and SHA-512 are available on all common JVM distributions.
   */
  public enum Algorithm {

    /** MD5 produces a 128-bit (16-byte) hash. Required by Java SE. Not for security use. */
    MD5("MD5"),

    /** SHA-256 produces a 256-bit (32-byte) hash. Required by Java SE. */
    SHA_256("SHA-256"),

    /** SHA-384 produces a 384-bit (48-byte) hash. Available on all common JVM distributions. */
    SHA_384("SHA-384"),

    /** SHA-512 produces a 512-bit (64-byte) hash. Available on all common JVM distributions. */
    SHA_512("SHA-512");

    private final String algorithmName;

    Algorithm(String algorithmName) {
      this.algorithmName = algorithmName;
    }

    /**
     * @return the JCA algorithm name used by {@link MessageDigest#getInstance(String)}.
     */
    public String algorithmName() {
      return this.algorithmName;
    }
  }

  /**
   * Prevents creation of instances.
   */
  private Hashes() {
  }

  /**
   * Returns a hash value for the specified text using the given algorithm.
   *
   * @param text      The text value to hash.
   * @param algorithm The algorithm to use.
   *
   * @return The hash value as a lowercase hexadecimal string.
   */
  public static String hash(String text, Algorithm algorithm) {
    byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
    return toHex(newDigest(algorithm).digest(bytes));
  }

  /**
   * Returns a hash value for the specified bytes using the given algorithm.
   *
   * @param data      The bytes to hash.
   * @param algorithm The algorithm to use.
   *
   * @return The hash value as a lowercase hexadecimal string.
   */
  public static String hash(byte[] data, Algorithm algorithm) {
    return toHex(newDigest(algorithm).digest(data));
  }

  /**
   * Returns a hash value for the content of the given input stream using the given algorithm.
   *
   * <p>The stream is read until EOF but is not closed; the caller retains ownership.
   *
   * @param in        The input stream to read.
   * @param algorithm The algorithm to use.
   *
   * @return The hash value as a lowercase hexadecimal string.
   *
   * @throws IOException If an error occurred while reading the stream.
   */
  public static String hash(InputStream in, Algorithm algorithm) throws IOException {
    MessageDigest md = newDigest(algorithm);
    // Use an 8KiB buffer as a conventional I/O default that balances throughput and memory use
    byte[] buffer = new byte[8192];
    int read;
    while ((read = in.read(buffer)) != -1) {
      md.update(buffer, 0, read);
    }
    return toHex(md.digest());
  }

  /**
   * Returns a hash value for the specified file content using the given algorithm.
   *
   * @param file      The file to read.
   * @param algorithm The algorithm to use.
   *
   * @return The hash value as a lowercase hexadecimal string.
   *
   * @throws IOException If the file does not exist or an error occurred while reading it.
   */
  public static String hash(File file, Algorithm algorithm) throws IOException {
    try (InputStream in = Files.newInputStream(file.toPath())) {
      return hash(in, algorithm);
    }
  }

  /**
   * Returns a hash value for the specified file using the given algorithm.
   *
   * @param file      The file to read.
   * @param strong    {@code true} to hash the file content; {@code false} to hash its
   *                  canonical path, length, and last-modified timestamp.
   * @param algorithm The algorithm to use.
   *
   * @return The hash value as a lowercase hexadecimal string.
   *
   * @throws IOException If the file does not exist or an error occurred while reading it.
   */
  public static String hash(File file, boolean strong, Algorithm algorithm) throws IOException {
    if (strong) return hash(file, algorithm);
    return hash(file.getCanonicalPath() + '$' + file.length() + '%' + file.lastModified(), algorithm);
  }

  /**
   * Returns a hash value for the specified path content using the given algorithm.
   *
   * @param path      The path to read.
   * @param algorithm The algorithm to use.
   *
   * @return The hash value as a lowercase hexadecimal string.
   *
   * @throws IOException If the path does not exist or an error occurred while reading it.
   */
  public static String hash(Path path, Algorithm algorithm) throws IOException {
    try (InputStream in = Files.newInputStream(path)) {
      return hash(in, algorithm);
    }
  }

  /**
   * Returns a hash value for the specified path using the given algorithm.
   *
   * @param path      The path to read.
   * @param strong    {@code true} to hash the file content; {@code false} to hash its
   *                  real path, size, and last-modified timestamp.
   * @param algorithm The algorithm to use.
   *
   * @return The hash value as a lowercase hexadecimal string.
   *
   * @throws IOException If the path does not exist or an error occurred while reading it.
   */
  public static String hash(Path path, boolean strong, Algorithm algorithm) throws IOException {
    if (strong) return hash(path, algorithm);
    Path real = path.toRealPath();
    return hash(real.toString() + '$' + Files.size(real) + '%' + Files.getLastModifiedTime(real).toMillis(), algorithm);
  }

  // Private helpers
  // ----------------------------------------------------------------------------------------------

  /**
   * Converts the byte data into a sequence of lowercase hexadecimal characters.
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
   * Returns a new {@link MessageDigest} for the given algorithm.
   *
   * @param algorithm The algorithm to instantiate.
   * @return a new {@link MessageDigest} instance.
   * @throws AssertionError If the algorithm is not available on this platform.
   */
  private static MessageDigest newDigest(Algorithm algorithm) {
    try {
      return MessageDigest.getInstance(algorithm.algorithmName());
    } catch (NoSuchAlgorithmException ex) {
      throw new AssertionError(algorithm.algorithmName() + " should always be available", ex);
    }
  }

}
