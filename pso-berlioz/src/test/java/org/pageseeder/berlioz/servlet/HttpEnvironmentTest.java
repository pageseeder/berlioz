package org.pageseeder.berlioz.servlet;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

public class HttpEnvironmentTest {

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  @Test
  public void testGetPublicAndPrivateFolders() throws Exception {
    File pub  = tmp.newFolder("public");
    File priv = tmp.newFolder("private");
    HttpEnvironment env = new HttpEnvironment(pub, priv, "max-age=3600");
    Assert.assertEquals(pub, env.getPublicFolder());
    Assert.assertEquals(priv, env.getPrivateFolder());
  }

  @Test
  public void testGetCacheControl() throws Exception {
    File pub  = tmp.newFolder("public");
    File priv = tmp.newFolder("private");
    Assert.assertEquals("max-age=3600", new HttpEnvironment(pub, priv, "max-age=3600").getCacheControl());
    Assert.assertEquals("no-cache",     new HttpEnvironment(pub, priv, "no-cache").getCacheControl());
    Assert.assertEquals("",             new HttpEnvironment(pub, priv, "").getCacheControl());
  }

  @Test
  public void testGetPublicFileResolvesRelativePath() throws Exception {
    File pub  = tmp.newFolder("public");
    File priv = tmp.newFolder("private");
    HttpEnvironment env = new HttpEnvironment(pub, priv, "");
    Assert.assertEquals(new File(pub, "images/logo.png"), env.getPublicFile("images/logo.png"));
  }

  @Test
  public void testGetPrivateFileResolvesRelativePath() throws Exception {
    File pub  = tmp.newFolder("public");
    File priv = tmp.newFolder("private");
    HttpEnvironment env = new HttpEnvironment(pub, priv, "");
    Assert.assertEquals(new File(priv, "config/services.xml"), env.getPrivateFile("config/services.xml"));
  }

  @Test
  public void testGetPublicFileAndPrivateFileAreIndependent() throws Exception {
    File pub  = tmp.newFolder("public");
    File priv = tmp.newFolder("private");
    HttpEnvironment env = new HttpEnvironment(pub, priv, "");
    Assert.assertNotEquals(env.getPublicFile("data.xml"), env.getPrivateFile("data.xml"));
  }
}
