/*
 * Copyright 2016 Allette Systems (Australia)
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

final class SHA256Test {

  @Test
  void testHash_NullString() {
    Assertions.assertThrows(NullPointerException.class, () -> SHA256.hash((String) null));
  }

  @Test
  void testHash_NullBytes() {
    Assertions.assertThrows(NullPointerException.class, () -> SHA256.hash((byte[]) null));
  }

  @Test
  void testHash_NullInputStream() throws IOException {
    Assertions.assertThrows(NullPointerException.class, () -> SHA256.hash((InputStream) null));
  }

  @Test
  void testHash_String() {
    Assertions.assertEquals(SHA256.hash(""), "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    Assertions.assertEquals(SHA256.hash("test"), "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08");
    Assertions.assertEquals(SHA256.hash("Licensed under the Apache License, Version 2.0 (the \"License\");"), "55f12528ddd4240e797f6391c80f5cf883e9c281253ad296c30b97dd4810c0a6");
  }

  @Test
  void testHash_Bytes_ConsistentWithString() {
    byte[] bytes = "test".getBytes(StandardCharsets.UTF_8);
    Assertions.assertEquals(SHA256.hash("test"), SHA256.hash(bytes));
  }

  @Test
  void testHash_InputStream_ConsistentWithString() throws IOException {
    byte[] bytes = "test".getBytes(StandardCharsets.UTF_8);
    Assertions.assertEquals(SHA256.hash("test"), SHA256.hash(new ByteArrayInputStream(bytes)));
  }

  @Test
  void testHash_Path_ConsistentWithString() throws IOException {
    byte[] content = "test".getBytes(StandardCharsets.UTF_8);
    Path temp = Files.createTempFile("sha256-test-", ".txt");
    try {
      Files.write(temp, content);
      Assertions.assertEquals(SHA256.hash("test"), SHA256.hash(temp));
    } finally {
      Files.deleteIfExists(temp);
    }
  }

}
