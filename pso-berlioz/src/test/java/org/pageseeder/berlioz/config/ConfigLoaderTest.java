package org.pageseeder.berlioz.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.FileOutputStream;
import java.util.Arrays;

public final class ConfigLoaderTest {

  @TempDir
  Path tmp;

  // File size limit
  // ---------------------------------------------------------------------------

  @Test
  public void testFileLargerThan1MBThrowsConfigException() throws Exception {
    Assertions.assertThrows(ConfigException.class, () -> {
    File large = Files.createFile(tmp.resolve("big.xml")).toFile();
    try (FileOutputStream out = new FileOutputStream(large)) {
      byte[] chunk = new byte[10_000];
      Arrays.fill(chunk, (byte) ' ');
      for (int i = 0; i < 101; i++) {
        out.write(chunk);
      }
    }
    RedirectConfig.newInstance(large);
    });
  }

  @Test
  public void testFileExactly1MBDoesNotThrow() throws Exception {
    File exactly = Files.createFile(tmp.resolve("exact.xml")).toFile();
    byte[] xml = "<redirect-mapping/>".getBytes();
    // pad with spaces to reach exactly 1,000,000 bytes (the limit is strictly > 1MB)
    byte[] padding = new byte[1_000_000 - xml.length];
    Arrays.fill(padding, (byte) ' ');
    try (FileOutputStream out = new FileOutputStream(exactly)) {
      out.write(xml);
      out.write(padding);
    }
    Assertions.assertEquals(1_000_000, exactly.length());
    RedirectConfig config = RedirectConfig.newInstance(exactly);
    Assertions.assertTrue(config.isEmpty());
  }

  // Non-existent file
  // ---------------------------------------------------------------------------

  @Test
  public void testNonExistentFileThrowsConfigException() throws Exception {
    Assertions.assertThrows(ConfigException.class, () -> {
    File missing = new File(tmp.toFile(), "does-not-exist.xml");
    RedirectConfig.newInstance(missing);
    });
  }

  // Directory instead of file
  // ---------------------------------------------------------------------------

  @Test
  public void testDirectoryThrowsConfigException() throws Exception {
    Assertions.assertThrows(ConfigException.class, () -> {
    File dir = Files.createDirectory(tmp.resolve("notafile")).toFile();
    RedirectConfig.newInstance(dir);
    });
  }

  // Weborganic DOCTYPE stripping
  // ---------------------------------------------------------------------------

  @Test
  public void testWeboorganicDoctypeIsStrippedAndParsesSuccessfully() throws ConfigException {
    String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
        "<!DOCTYPE redirect-mapping PUBLIC \"-//Weborganic//DTD::Redirect Mapping 1.0//EN\" \"\">" +
        "<redirect-mapping><redirect from=\"/old\" to=\"/new\"/></redirect-mapping>";
    RedirectConfig config = RedirectConfig.newInstance(
        new java.io.ByteArrayInputStream(xml.getBytes()));
    Assertions.assertEquals(1, config.size());
    Assertions.assertNotNull(config.redirect("/old"));
    Assertions.assertEquals(config.redirect("/old").to(), "/new");
  }

}
