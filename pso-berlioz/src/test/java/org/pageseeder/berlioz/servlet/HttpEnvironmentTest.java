package org.pageseeder.berlioz.servlet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Files;

class HttpEnvironmentTest {

  @TempDir
  Path tmp;

  @Test
  void testGetPublicAndPrivateFolders() throws Exception {
    File pub  = Files.createDirectory(tmp.resolve("public")).toFile();
    File priv = Files.createDirectory(tmp.resolve("private")).toFile();
    HttpEnvironment env = new HttpEnvironment(pub, priv, "max-age=3600");
    Assertions.assertEquals(pub, env.getPublicFolder());
    Assertions.assertEquals(priv, env.getPrivateFolder());
  }

  @Test
  void testGetCacheControl() throws Exception {
    File pub  = Files.createDirectory(tmp.resolve("public")).toFile();
    File priv = Files.createDirectory(tmp.resolve("private")).toFile();
    Assertions.assertEquals("max-age=3600", new HttpEnvironment(pub, priv, "max-age=3600").getCacheControl());
    Assertions.assertEquals("no-cache", new HttpEnvironment(pub, priv, "no-cache").getCacheControl());
    Assertions.assertEquals("", new HttpEnvironment(pub, priv, "").getCacheControl());
  }

  @Test
  void testGetPublicFileResolvesRelativePath() throws Exception {
    File pub  = Files.createDirectory(tmp.resolve("public")).toFile();
    File priv = Files.createDirectory(tmp.resolve("private")).toFile();
    HttpEnvironment env = new HttpEnvironment(pub, priv, "");
    Assertions.assertEquals(new File(pub, "images/logo.png"), env.getPublicFile("images/logo.png"));
  }

  @Test
  void testGetPrivateFileResolvesRelativePath() throws Exception {
    File pub  = Files.createDirectory(tmp.resolve("public")).toFile();
    File priv = Files.createDirectory(tmp.resolve("private")).toFile();
    HttpEnvironment env = new HttpEnvironment(pub, priv, "");
    Assertions.assertEquals(new File(priv, "config/services.xml"), env.getPrivateFile("config/services.xml"));
  }

  @Test
  void testGetPublicFileAndPrivateFileAreIndependent() throws Exception {
    File pub  = Files.createDirectory(tmp.resolve("public")).toFile();
    File priv = Files.createDirectory(tmp.resolve("private")).toFile();
    HttpEnvironment env = new HttpEnvironment(pub, priv, "");
    Assertions.assertNotEquals(env.getPublicFile("data.xml"), env.getPrivateFile("data.xml"));
  }
}
