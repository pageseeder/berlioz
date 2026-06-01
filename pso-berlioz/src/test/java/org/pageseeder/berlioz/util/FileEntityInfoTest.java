package org.pageseeder.berlioz.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileEntityInfoTest {

  @TempDir
  Path tempDir;

  @Test
  void testExistingFile() throws IOException {
    Path p = tempDir.resolve("test.txt");
    Files.writeString(p, "hello");
    File file = p.toFile();

    FileEntityInfo info = new FileEntityInfo(file, "text/plain");

    assertEquals(file.lastModified(), info.getLastModified());
    assertEquals(file.length(), info.getContentLength());
    assertEquals("text/plain", info.getMimeType());
    assertNotNull(info.getETag());
    assertTrue(info.getETag().startsWith("\""), "ETag should be quoted");
    assertSame(file, info.getFile());
  }

  @Test
  void testNonExistingFile() {
    File file = new File(tempDir.toFile(), "missing.txt");
    FileEntityInfo info = new FileEntityInfo(file, "text/plain");

    assertEquals(-1L, info.getLastModified());
    assertEquals(-1L, info.getContentLength());
    assertNull(info.getETag());
  }

  @Test
  void testETagFormat() throws IOException {
    Path p = tempDir.resolve("data.xml");
    Files.writeString(p, "<root/>");
    FileEntityInfo info = new FileEntityInfo(p.toFile(), "application/xml");

    String etag = info.getETag();
    assertNotNull(etag);
    // ETag format: "<length>-<modified>"
    assertTrue(etag.matches("\"\\d+-\\d+\""), "ETag should be \"<length>-<modified>\", got: " + etag);
  }
}
