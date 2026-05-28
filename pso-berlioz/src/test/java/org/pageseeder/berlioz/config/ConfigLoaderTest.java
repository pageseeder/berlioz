package org.pageseeder.berlioz.config;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;

public final class ConfigLoaderTest {

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  // File size limit
  // ---------------------------------------------------------------------------

  @Test(expected = ConfigException.class)
  public void testFileLargerThan1MBThrowsConfigException() throws Exception {
    File large = tmp.newFile("big.xml");
    try (FileOutputStream out = new FileOutputStream(large)) {
      byte[] chunk = new byte[10_000];
      Arrays.fill(chunk, (byte) ' ');
      for (int i = 0; i < 101; i++) {
        out.write(chunk);
      }
    }
    RedirectConfig.newInstance(large);
  }

  @Test
  public void testFileExactly1MBDoesNotThrow() throws Exception {
    File exactly = tmp.newFile("exact.xml");
    byte[] xml = "<redirect-mapping/>".getBytes();
    // pad with spaces to reach exactly 1,000,000 bytes (the limit is strictly > 1MB)
    byte[] padding = new byte[1_000_000 - xml.length];
    Arrays.fill(padding, (byte) ' ');
    try (FileOutputStream out = new FileOutputStream(exactly)) {
      out.write(xml);
      out.write(padding);
    }
    Assert.assertEquals(1_000_000, exactly.length());
    RedirectConfig config = RedirectConfig.newInstance(exactly);
    Assert.assertTrue(config.isEmpty());
  }

  // Non-existent file
  // ---------------------------------------------------------------------------

  @Test(expected = ConfigException.class)
  public void testNonExistentFileThrowsConfigException() throws Exception {
    File missing = new File(tmp.getRoot(), "does-not-exist.xml");
    RedirectConfig.newInstance(missing);
  }

  // Directory instead of file
  // ---------------------------------------------------------------------------

  @Test(expected = ConfigException.class)
  public void testDirectoryThrowsConfigException() throws Exception {
    File dir = tmp.newFolder("notafile");
    RedirectConfig.newInstance(dir);
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
    Assert.assertEquals(1, config.size());
    Assert.assertNotNull(config.redirect("/old"));
    Assert.assertEquals("/new", config.redirect("/old").to());
  }

}
