package org.pageseeder.berlioz.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileUtilsTest {

  @TempDir
  Path tempDir;

  // contains()

  @Test
  void testContains_nullRoot() {
    assertFalse(FileUtils.contains(null, new File("anything")));
  }

  @Test
  void testContains_nullFile() {
    assertFalse(FileUtils.contains(new File("."), null));
  }

  @Test
  void testContains_bothNull() {
    assertFalse(FileUtils.contains(null, null));
  }

  @Test
  void testContains_fileInsideRoot() throws IOException {
    Path child = tempDir.resolve("child.txt");
    Files.createFile(child);
    assertTrue(FileUtils.contains(tempDir.toFile(), child.toFile()));
  }

  @Test
  void testContains_fileIsRoot() {
    assertTrue(FileUtils.contains(tempDir.toFile(), tempDir.toFile()));
  }

  @Test
  void testContains_fileOutsideRoot() throws IOException {
    Path other = Files.createTempDirectory("other");
    try {
      assertFalse(FileUtils.contains(tempDir.toFile(), other.toFile()));
    } finally {
      Files.deleteIfExists(other);
    }
  }

  // path()

  @Test
  void testPath_childFile() throws IOException {
    Path child = tempDir.resolve("subdir").resolve("file.txt");
    Files.createDirectories(child.getParent());
    Files.createFile(child);
    String p = FileUtils.path(tempDir.toFile(), child.toFile());
    assertNotNull(p);
    assertEquals("subdir/file.txt", p);
  }

  @Test
  void testPath_directChild() throws IOException {
    Path child = tempDir.resolve("file.txt");
    Files.createFile(child);
    String p = FileUtils.path(tempDir.toFile(), child.toFile());
    assertEquals("file.txt", p);
  }

  @Test
  void testPath_outsideRoot_throws() throws IOException {
    Path outside = Files.createTempDirectory("outside");
    try {
      File tmp = tempDir.toFile();
      File out = outside.toFile();
      assertThrows(IllegalArgumentException.class, () -> FileUtils.path(tmp, out));
    } finally {
      Files.deleteIfExists(outside);
    }
  }
}
