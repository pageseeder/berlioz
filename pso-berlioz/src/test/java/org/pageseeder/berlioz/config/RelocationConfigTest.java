package org.pageseeder.berlioz.config;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;

public final class RelocationConfigTest {

  File configFolder = new File("src/test/resources/org/pageseeder/berlioz/config");

  @Test
  public void testConstructor_empty() {
    RelocationConfig config = new RelocationConfig();
    Assert.assertTrue(config.isEmpty());
    Assert.assertEquals(0, config.size());
    Assert.assertNull(config.relocate("/"));
  }

  @Test
  public void testLoad_empty() throws ConfigException {
    String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><relocation-mapping/>";
    RelocationConfig config = RelocationConfig.newInstance(new ByteArrayInputStream(xml.getBytes()));
    Assert.assertTrue(config.isEmpty());
    Assert.assertEquals(0, config.size());
  }

  /**
   * <pre>{@code
   * <relocation-mapping>
   *   <relocation from="/"             to="/html/home"/>
   *   <relocation from="/index.html"   to="/html/home"/>
   *   <relocation from="/html"         to="/html/home"/>
   *   <relocation from="/xml"          to="/xml/home"/>
   *   <relocation from="/{+path}.psml" to="/html/{+path}"/>
   * </relocation-mapping>
   * }</pre>
   */
  @Test
  public void testLoad_file() throws ConfigException {
    RelocationConfig config = RelocationConfig.newInstance(new File(this.configFolder, "relocation.xml"));
    Assert.assertFalse(config.isEmpty());
    Assert.assertEquals(5, config.size());

    Assert.assertNull(config.relocate("/index.xml"));
    Assert.assertNull(config.relocate("/example.html"));

    Assert.assertEquals("/html/home", config.relocate("/"));
    Assert.assertEquals("/html/home", config.relocate("/index.html"));
    Assert.assertEquals("/html/home", config.relocate("/html"));
    Assert.assertEquals("/xml/home",  config.relocate("/xml"));
    Assert.assertEquals("/html/example", config.relocate("/example.psml"));
  }

  @Test
  public void testLoad_duplicatePattern() throws ConfigException {
    String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><relocation-mapping>" +
        "<relocation from=\"/a\" to=\"/b\"/>" +
        "<relocation from=\"/a\" to=\"/c\"/>" +
        "</relocation-mapping>";
    RelocationConfig config = RelocationConfig.newInstance(new ByteArrayInputStream(xml.getBytes()));
    Assert.assertEquals(2, config.size());
    // First match wins
    Assert.assertEquals("/b", config.relocate("/a"));
  }

  @Test
  public void testLoad_variableExpansion() throws ConfigException {
    String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><relocation-mapping>" +
        "<relocation from=\"/{+path}.psml\" to=\"/html/{+path}\"/>" +
        "</relocation-mapping>";
    RelocationConfig config = RelocationConfig.newInstance(new ByteArrayInputStream(xml.getBytes()));
    Assert.assertEquals("/html/docs/guide", config.relocate("/docs/guide.psml"));
    Assert.assertEquals("/html/page",       config.relocate("/page.psml"));
  }

  @Test(expected = ConfigException.class)
  public void testLoad_invalidXml() throws ConfigException {
    String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><relocation-mapping>";
    RelocationConfig.newInstance(new ByteArrayInputStream(xml.getBytes()));
  }

  @Test(expected = ConfigException.class)
  public void testLoad_xxe() throws ConfigException {
    String xml = "<!DOCTYPE relocation-mapping [<!ELEMENT relocation-mapping ANY >" +
        "<!ENTITY x SYSTEM \"/etc/passwd\" >]>" +
        "<?xml version=\"1.0\" encoding=\"utf-8\"?><relocation-mapping>&x;</relocation-mapping>";
    RelocationConfig.newInstance(new ByteArrayInputStream(xml.getBytes()));
  }

}
