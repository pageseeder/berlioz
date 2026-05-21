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
import java.nio.file.Path;

/**
 * A utility class providing simple static methods to generate SHA-384 hash values.
 *
 * <p>A SHA-384 hash is expressed as a 96-digit hexadecimal number.
 * SHA-384 is available on all common JVM distributions.
 *
 * <p>Delegates to {@link Hashes} using {@link Hashes.Algorithm#SHA_384}.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.13.0
 * @since Berlioz 0.13.0
 */
public final class SHA384 {

  /**
   * Prevents creation of instances.
   */
  private SHA384() {
  }

  /**
   * Returns a hash value for the specified text.
   *
   * @param text The text value to hash.
   *
   * @return The SHA-384 hash as a 96-character lowercase hexadecimal string.
   */
  public static String hash(String text) {
    return Hashes.hash(text, Hashes.Algorithm.SHA_384);
  }

  /**
   * Returns a hash value for the specified bytes.
   *
   * @param data The bytes to hash.
   *
   * @return The SHA-384 hash as a 96-character lowercase hexadecimal string.
   */
  public static String hash(byte[] data) {
    return Hashes.hash(data, Hashes.Algorithm.SHA_384);
  }

  /**
   * Returns a hash value for the content of the given input stream.
   *
   * <p>The stream is read until EOF but is not closed; the caller retains ownership.
   *
   * @param in The input stream to read.
   *
   * @return The SHA-384 hash as a 96-character lowercase hexadecimal string.
   *
   * @throws IOException If an error occurred while reading the stream.
   */
  public static String hash(InputStream in) throws IOException {
    return Hashes.hash(in, Hashes.Algorithm.SHA_384);
  }

  /**
   * Returns a hash value for the specified file content.
   *
   * @param file The file to read.
   *
   * @return The SHA-384 hash as a 96-character lowercase hexadecimal string.
   *
   * @throws IOException If the file does not exist or an error occurred while reading it.
   */
  public static String hash(File file) throws IOException {
    return Hashes.hash(file, Hashes.Algorithm.SHA_384);
  }

  /**
   * Returns a hash value for the specified file.
   *
   * @param file   The file to read.
   * @param strong {@code true} to hash the file content;
   *               {@code false} to hash its canonical path, length, and last-modified timestamp.
   *
   * @return The SHA-384 hash as a 96-character lowercase hexadecimal string.
   *
   * @throws IOException If the file does not exist or an error occurred while reading it.
   */
  public static String hash(File file, boolean strong) throws IOException {
    return Hashes.hash(file, strong, Hashes.Algorithm.SHA_384);
  }

  /**
   * Returns a hash value for the specified path content.
   *
   * @param path The path to read.
   *
   * @return The SHA-384 hash as a 96-character lowercase hexadecimal string.
   *
   * @throws IOException If the path does not exist or an error occurred while reading it.
   */
  public static String hash(Path path) throws IOException {
    return Hashes.hash(path, Hashes.Algorithm.SHA_384);
  }

  /**
   * Returns a hash value for the specified path.
   *
   * @param path   The path to read.
   * @param strong {@code true} to hash the file content;
   *               {@code false} to hash its real path, size, and last-modified timestamp.
   *
   * @return The SHA-384 hash as a 96-character lowercase hexadecimal string.
   *
   * @throws IOException If the path does not exist or an error occurred while reading it.
   */
  public static String hash(Path path, boolean strong) throws IOException {
    return Hashes.hash(path, strong, Hashes.Algorithm.SHA_384);
  }

}
