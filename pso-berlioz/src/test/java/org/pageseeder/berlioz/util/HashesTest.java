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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class HashesTest {

  // -- Null-input guards ---------------------------------------------------

  @Test(expected = NullPointerException.class)
  public void testHash_NullString() {
    Hashes.hash((String) null, Hashes.Algorithm.SHA_256);
  }

  @Test(expected = NullPointerException.class)
  public void testHash_NullBytes() {
    Hashes.hash((byte[]) null, Hashes.Algorithm.SHA_256);
  }

  @Test(expected = NullPointerException.class)
  public void testHash_NullInputStream() throws IOException {
    Hashes.hash((InputStream) null, Hashes.Algorithm.SHA_256);
  }

  // -- Output length -------------------------------------------------------

  @Test
  public void testHash_String_OutputLength() {
    Assert.assertEquals(32,  Hashes.hash("test", Hashes.Algorithm.MD5).length());
    Assert.assertEquals(64,  Hashes.hash("test", Hashes.Algorithm.SHA_256).length());
    Assert.assertEquals(96,  Hashes.hash("test", Hashes.Algorithm.SHA_384).length());
    Assert.assertEquals(128, Hashes.hash("test", Hashes.Algorithm.SHA_512).length());
  }

  // -- Known values (NIST FIPS 180-4) -------------------------------------

  @Test
  public void testHash_String_KnownValues() {
    // MD5 — from existing test coverage
    Assert.assertEquals("d41d8cd98f00b204e9800998ecf8427e", Hashes.hash("", Hashes.Algorithm.MD5));
    Assert.assertEquals("098f6bcd4621d373cade4e832627b4f6", Hashes.hash("test", Hashes.Algorithm.MD5));

    // SHA-256 — from existing test coverage
    Assert.assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
        Hashes.hash("", Hashes.Algorithm.SHA_256));
    Assert.assertEquals("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",
        Hashes.hash("test", Hashes.Algorithm.SHA_256));

    // SHA-384 — NIST FIPS 180-4 test vectors
    Assert.assertEquals("38b060a751ac96384cd9327eb1b1e36a21fdb71114be07434c0cc7bf63f6e1da274edebfe76f65fbd51ad2f14898b95b",
        Hashes.hash("", Hashes.Algorithm.SHA_384));
    Assert.assertEquals("cb00753f45a35e8bb5a03d699ac65007272c32ab0eded1631a8b605a43ff5bed8086072ba1e7cc2358baeca134c825a7",
        Hashes.hash("abc", Hashes.Algorithm.SHA_384));

    // SHA-512 — NIST FIPS 180-4 test vectors
    Assert.assertEquals("cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e",
        Hashes.hash("", Hashes.Algorithm.SHA_512));
    Assert.assertEquals("ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f",
        Hashes.hash("abc", Hashes.Algorithm.SHA_512));
  }

  // -- Cross-type consistency ---------------------------------------------

  @Test
  public void testHash_Bytes_ConsistentWithString() {
    for (Hashes.Algorithm algorithm : Hashes.Algorithm.values()) {
      String text = "test";
      byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
      Assert.assertEquals(
          algorithm + ": byte[] hash should match String hash",
          Hashes.hash(text, algorithm),
          Hashes.hash(bytes, algorithm));
    }
  }

  @Test
  public void testHash_InputStream_ConsistentWithBytes() throws IOException {
    for (Hashes.Algorithm algorithm : Hashes.Algorithm.values()) {
      byte[] bytes = "test".getBytes(StandardCharsets.UTF_8);
      Assert.assertEquals(
          algorithm + ": InputStream hash should match byte[] hash",
          Hashes.hash(bytes, algorithm),
          Hashes.hash(new ByteArrayInputStream(bytes), algorithm));
    }
  }

  @Test
  public void testHash_File_ConsistentWithString() throws IOException {
    byte[] content = "test".getBytes(StandardCharsets.UTF_8);
    Path temp = Files.createTempFile("hashes-test-", ".txt");
    try {
      Files.write(temp, content);
      File file = temp.toFile();
      for (Hashes.Algorithm algorithm : Hashes.Algorithm.values()) {
        Assert.assertEquals(
            algorithm + ": File hash should match String hash",
            Hashes.hash("test", algorithm),
            Hashes.hash(file, algorithm));
      }
    } finally {
      Files.deleteIfExists(temp);
    }
  }

  @Test
  public void testHash_Path_ConsistentWithFile() throws IOException {
    byte[] content = "test".getBytes(StandardCharsets.UTF_8);
    Path temp = Files.createTempFile("hashes-test-", ".txt");
    try {
      Files.write(temp, content);
      for (Hashes.Algorithm algorithm : Hashes.Algorithm.values()) {
        Assert.assertEquals(
            algorithm + ": Path hash should match File hash",
            Hashes.hash(temp.toFile(), algorithm),
            Hashes.hash(temp, algorithm));
      }
    } finally {
      Files.deleteIfExists(temp);
    }
  }

  // -- Weak (metadata-based) hash -----------------------------------------

  @Test
  public void testHash_File_WeakHash() throws IOException {
    Path temp = Files.createTempFile("hashes-test-", ".txt");
    try {
      Files.write(temp, "test".getBytes(StandardCharsets.UTF_8));
      File file = temp.toFile();
      String weak = Hashes.hash(file, false, Hashes.Algorithm.SHA_256);
      String strong = Hashes.hash(file, true, Hashes.Algorithm.SHA_256);
      Assert.assertEquals(64, weak.length());
      Assert.assertNotEquals(strong, weak);
    } finally {
      Files.deleteIfExists(temp);
    }
  }

  @Test
  public void testHash_Path_WeakHash() throws IOException {
    Path temp = Files.createTempFile("hashes-test-", ".txt");
    try {
      Files.write(temp, "test".getBytes(StandardCharsets.UTF_8));
      String weak = Hashes.hash(temp, false, Hashes.Algorithm.SHA_256);
      String strong = Hashes.hash(temp, true, Hashes.Algorithm.SHA_256);
      Assert.assertEquals(64, weak.length());
      Assert.assertNotEquals(strong, weak);
    } finally {
      Files.deleteIfExists(temp);
    }
  }

  @Test
  public void testHash_File_WeakAndPath_WeakConsistent() throws IOException {
    Path temp = Files.createTempFile("hashes-test-", ".txt");
    try {
      Files.write(temp, "test".getBytes(StandardCharsets.UTF_8));
      Assert.assertEquals(
          Hashes.hash(temp.toFile(), false, Hashes.Algorithm.SHA_256),
          Hashes.hash(temp, false, Hashes.Algorithm.SHA_256));
    } finally {
      Files.deleteIfExists(temp);
    }
  }

}
