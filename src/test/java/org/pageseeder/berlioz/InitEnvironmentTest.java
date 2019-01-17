package org.pageseeder.berlioz;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public final class InitEnvironmentTest {

  @Test
  public void testCreate() {
    File f = getWebInf();
    InitEnvironment env = InitEnvironment.create(f);
    assertEquals(f, env.webInf());
    assertEquals(f, env.appData());
    assertEquals(InitEnvironment.DEFAULT_MODE, env.mode());
    assertEquals(InitEnvironment.DEFAULT_CONFIG_DIRECTORY, env.configFolder());
  }

  @Test
  public void testConfigFolder() {
    File f = getWebInf();
    InitEnvironment env = InitEnvironment.create(f);
    InitEnvironment env2 = env.configFolder("sysconfig");
    assertEquals(InitEnvironment.DEFAULT_CONFIG_DIRECTORY, env.configFolder());
    assertEquals("sysconfig", env2.configFolder());
  }

  @Test
  public void testMode() {
    File f = getWebInf();
    InitEnvironment env = InitEnvironment.create(f);
    InitEnvironment env2 = env.mode("dev");
    assertEquals(InitEnvironment.DEFAULT_MODE, env.mode());
    assertEquals("dev", env2.mode());
  }

  private final File getWebInf() {
    return new File(this.getClass().getResource("/org/pageseeder/berlioz").getFile());
  }

}
