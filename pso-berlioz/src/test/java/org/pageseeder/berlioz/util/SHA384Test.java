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

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SHA384Test {

  @Test(expected = NullPointerException.class)
  public void testHash_NullString() {
    SHA384.hash((String) null);
  }

  @Test(expected = NullPointerException.class)
  public void testHash_NullBytes() {
    SHA384.hash((byte[]) null);
  }

  @Test(expected = NullPointerException.class)
  public void testHash_NullInputStream() throws IOException {
    SHA384.hash((InputStream) null);
  }

  @Test
  public void testHash_String() {
    // NIST FIPS 180-4 test vectors
    Assert.assertEquals("38b060a751ac96384cd9327eb1b1e36a21fdb71114be07434c0cc7bf63f6e1da274edebfe76f65fbd51ad2f14898b95b", SHA384.hash(""));
    Assert.assertEquals("cb00753f45a35e8bb5a03d699ac65007272c32ab0eded1631a8b605a43ff5bed8086072ba1e7cc2358baeca134c825a7", SHA384.hash("abc"));
  }

  @Test
  public void testHash_Bytes_ConsistentWithString() {
    byte[] bytes = "test".getBytes(StandardCharsets.UTF_8);
    Assert.assertEquals(SHA384.hash("test"), SHA384.hash(bytes));
  }

  @Test
  public void testHash_InputStream_ConsistentWithString() throws IOException {
    byte[] bytes = "test".getBytes(StandardCharsets.UTF_8);
    Assert.assertEquals(SHA384.hash("test"), SHA384.hash(new ByteArrayInputStream(bytes)));
  }

  @Test
  public void testHash_Path_ConsistentWithString() throws IOException {
    byte[] content = "test".getBytes(StandardCharsets.UTF_8);
    Path temp = Files.createTempFile("sha384-test-", ".txt");
    try {
      Files.write(temp, content);
      Assert.assertEquals(SHA384.hash("test"), SHA384.hash(temp));
    } finally {
      Files.deleteIfExists(temp);
    }
  }

}
