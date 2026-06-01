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

public final class MD5Test {

  @Test
  public void testHash_NullString() {
    Assertions.assertThrows(NullPointerException.class, () -> MD5.hash((String) null));
  }

  @Test
  public void testHash_NullBytes() {
    Assertions.assertThrows(NullPointerException.class, () -> MD5.hash((byte[]) null));
  }

  @Test
  public void testHash_NullInputStream() throws IOException {
    Assertions.assertThrows(NullPointerException.class, () -> MD5.hash((InputStream) null));
  }

  @Test
  public void testHash_String() {
    Assertions.assertEquals(MD5.hash(""), "d41d8cd98f00b204e9800998ecf8427e");
    Assertions.assertEquals(MD5.hash("test"), "098f6bcd4621d373cade4e832627b4f6");
    Assertions.assertEquals(MD5.hash("Licensed under the Apache License, Version 2.0 (the \"License\");"), "942a46d563d50475e73c41765b35cbbf");
  }

  @Test
  public void testHash_Bytes_ConsistentWithString() {
    byte[] bytes = "test".getBytes(StandardCharsets.UTF_8);
    Assertions.assertEquals(MD5.hash("test"), MD5.hash(bytes));
  }

  @Test
  public void testHash_InputStream_ConsistentWithString() throws IOException {
    byte[] bytes = "test".getBytes(StandardCharsets.UTF_8);
    Assertions.assertEquals(MD5.hash("test"), MD5.hash(new ByteArrayInputStream(bytes)));
  }

  @Test
  public void testHash_Path_ConsistentWithString() throws IOException {
    byte[] content = "test".getBytes(StandardCharsets.UTF_8);
    Path temp = Files.createTempFile("md5-test-", ".txt");
    try {
      Files.write(temp, content);
      Assertions.assertEquals(MD5.hash("test"), MD5.hash(temp));
    } finally {
      Files.deleteIfExists(temp);
    }
  }

}
