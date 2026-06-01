package org.pageseeder.berlioz.servlet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Files;

public class HttpEnvironmentTest {

  @TempDir
  Path tmp;

  @Test
  public void testGetPublicAndPrivateFolders() throws Exception {
    File pub  = Files.createDirectory(tmp.resolve("public")).toFile();
    File priv = Files.createDirectory(tmp.resolve("private")).toFile();
    HttpEnvironment env = new HttpEnvironment(pub, priv, "max-age=3600");
    Assertions.assertEquals(pub, env.getPublicFolder());
    Assertions.assertEquals(priv, env.getPrivateFolder());
  }

  @Test
  public void testGetCacheControl() throws Exception {
    File pub  = Files.createDirectory(tmp.resolve("public")).toFile();
    File priv = Files.createDirectory(tmp.resolve("private")).toFile();
    Assertions.assertEquals(new HttpEnvironment(pub, priv, "max-age=3600").getCacheControl(), "max-age=3600");
    Assertions.assertEquals(new HttpEnvironment(pub, priv, "no-cache").getCacheControl(), "no-cache");
    Assertions.assertEquals(new HttpEnvironment(pub, priv, "").getCacheControl(), "");
  }

  @Test
  public void testGetPublicFileResolvesRelativePath() throws Exception {
    File pub  = Files.createDirectory(tmp.resolve("public")).toFile();
    File priv = Files.createDirectory(tmp.resolve("private")).toFile();
    HttpEnvironment env = new HttpEnvironment(pub, priv, "");
    Assertions.assertEquals(new File(pub, "images/logo.png"), env.getPublicFile("images/logo.png"));
  }

  @Test
  public void testGetPrivateFileResolvesRelativePath() throws Exception {
    File pub  = Files.createDirectory(tmp.resolve("public")).toFile();
    File priv = Files.createDirectory(tmp.resolve("private")).toFile();
    HttpEnvironment env = new HttpEnvironment(pub, priv, "");
    Assertions.assertEquals(new File(priv, "config/services.xml"), env.getPrivateFile("config/services.xml"));
  }

  @Test
  public void testGetPublicFileAndPrivateFileAreIndependent() throws Exception {
    File pub  = Files.createDirectory(tmp.resolve("public")).toFile();
    File priv = Files.createDirectory(tmp.resolve("private")).toFile();
    HttpEnvironment env = new HttpEnvironment(pub, priv, "");
    Assertions.assertNotEquals(env.getPublicFile("data.xml"), env.getPrivateFile("data.xml"));
  }
}
