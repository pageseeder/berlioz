package org.pageseeder.berlioz.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;

final class RelocationConfigTest {

  File configFolder = new File("src/test/resources/org/pageseeder/berlioz/config");

  @Test
  void testConstructor_empty() {
    RelocationConfig config = new RelocationConfig();
    Assertions.assertTrue(config.isEmpty());
    Assertions.assertEquals(0, config.size());
    Assertions.assertNull(config.relocate("/"));
  }

  @Test
  void testLoad_empty() throws ConfigException {
    String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><relocation-mapping/>";
    RelocationConfig config = RelocationConfig.newInstance(new ByteArrayInputStream(xml.getBytes()));
    Assertions.assertTrue(config.isEmpty());
    Assertions.assertEquals(0, config.size());
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
  void testLoad_file() throws ConfigException {
    RelocationConfig config = RelocationConfig.newInstance(new File(this.configFolder, "relocation.xml"));
    Assertions.assertFalse(config.isEmpty());
    Assertions.assertEquals(5, config.size());

    Assertions.assertNull(config.relocate("/index.xml"));
    Assertions.assertNull(config.relocate("/example.html"));

    Assertions.assertEquals("/html/home", config.relocate("/"));
    Assertions.assertEquals("/html/home", config.relocate("/index.html"));
    Assertions.assertEquals("/html/home", config.relocate("/html"));
    Assertions.assertEquals("/xml/home", config.relocate("/xml"));
    Assertions.assertEquals("/html/example", config.relocate("/example.psml"));
  }

  @Test
  void testLoad_duplicatePattern() throws ConfigException {
    String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><relocation-mapping>" +
        "<relocation from=\"/a\" to=\"/b\"/>" +
        "<relocation from=\"/a\" to=\"/c\"/>" +
        "</relocation-mapping>";
    RelocationConfig config = RelocationConfig.newInstance(new ByteArrayInputStream(xml.getBytes()));
    Assertions.assertEquals(2, config.size());
    // First match wins
    Assertions.assertEquals("/b", config.relocate("/a"));
  }

  @Test
  void testLoad_variableExpansion() throws ConfigException {
    String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><relocation-mapping>" +
        "<relocation from=\"/{+path}.psml\" to=\"/html/{+path}\"/>" +
        "</relocation-mapping>";
    RelocationConfig config = RelocationConfig.newInstance(new ByteArrayInputStream(xml.getBytes()));
    Assertions.assertEquals("/html/docs/guide", config.relocate("/docs/guide.psml"));
    Assertions.assertEquals("/html/page", config.relocate("/page.psml"));
  }

  @Test
  void testLoad_invalidXml() {
    String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><relocation-mapping>";
    Assertions.assertThrows(ConfigException.class, () -> RelocationConfig.newInstance(new ByteArrayInputStream(xml.getBytes())));
  }

  @Test
  void testLoad_xxe() {
    String xml = "<!DOCTYPE relocation-mapping [<!ELEMENT relocation-mapping ANY >" +
        "<!ENTITY x SYSTEM \"/etc/passwd\" >]>" +
        "<?xml version=\"1.0\" encoding=\"utf-8\"?><relocation-mapping>&x;</relocation-mapping>";
    Assertions.assertThrows(ConfigException.class, () -> RelocationConfig.newInstance(new ByteArrayInputStream(xml.getBytes())));
  }

}
