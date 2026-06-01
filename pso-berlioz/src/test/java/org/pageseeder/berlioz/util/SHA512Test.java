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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class SHA512Test {

  @Test
  void testHash_NullString() {
    Assertions.assertThrows(NullPointerException.class, () -> SHA512.hash((String) null));
  }

  @Test
  void testHash_NullBytes() {
    Assertions.assertThrows(NullPointerException.class, () -> SHA512.hash((byte[]) null));
  }

  @Test
  void testHash_NullInputStream() throws IOException {
    Assertions.assertThrows(NullPointerException.class, () -> SHA512.hash((InputStream) null));
  }

  @Test
  void testHash_String() {
    // NIST FIPS 180-4 test vectors
    Assertions.assertEquals(SHA512.hash(""), "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e");
    Assertions.assertEquals(SHA512.hash("abc"), "ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f");
  }

  @Test
  void testHash_Bytes_ConsistentWithString() {
    byte[] bytes = "test".getBytes(StandardCharsets.UTF_8);
    Assertions.assertEquals(SHA512.hash("test"), SHA512.hash(bytes));
  }

  @Test
  void testHash_InputStream_ConsistentWithString() throws IOException {
    byte[] bytes = "test".getBytes(StandardCharsets.UTF_8);
    Assertions.assertEquals(SHA512.hash("test"), SHA512.hash(new ByteArrayInputStream(bytes)));
  }

  @Test
  void testHash_Path_ConsistentWithString() throws IOException {
    byte[] content = "test".getBytes(StandardCharsets.UTF_8);
    Path temp = Files.createTempFile("sha512-test-", ".txt");
    try {
      Files.write(temp, content);
      Assertions.assertEquals(SHA512.hash("test"), SHA512.hash(temp));
    } finally {
      Files.deleteIfExists(temp);
    }
  }

}
