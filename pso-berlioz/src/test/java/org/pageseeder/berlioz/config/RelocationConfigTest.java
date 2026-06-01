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

    Assertions.assertEquals(config.relocate("/"), "/html/home");
    Assertions.assertEquals(config.relocate("/index.html"), "/html/home");
    Assertions.assertEquals(config.relocate("/html"), "/html/home");
    Assertions.assertEquals(config.relocate("/xml"), "/xml/home");
    Assertions.assertEquals(config.relocate("/example.psml"), "/html/example");
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
    Assertions.assertEquals(config.relocate("/a"), "/b");
  }

  @Test
  void testLoad_variableExpansion() throws ConfigException {
    String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><relocation-mapping>" +
        "<relocation from=\"/{+path}.psml\" to=\"/html/{+path}\"/>" +
        "</relocation-mapping>";
    RelocationConfig config = RelocationConfig.newInstance(new ByteArrayInputStream(xml.getBytes()));
    Assertions.assertEquals(config.relocate("/docs/guide.psml"), "/html/docs/guide");
    Assertions.assertEquals(config.relocate("/page.psml"), "/html/page");
  }

  @Test
  void testLoad_invalidXml() throws ConfigException {
    Assertions.assertThrows(ConfigException.class, () -> {
    String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><relocation-mapping>";
    RelocationConfig.newInstance(new ByteArrayInputStream(xml.getBytes()));
    });
  }

  @Test
  void testLoad_xxe() throws ConfigException {
    Assertions.assertThrows(ConfigException.class, () -> {
    String xml = "<!DOCTYPE relocation-mapping [<!ELEMENT relocation-mapping ANY >" +
        "<!ENTITY x SYSTEM \"/etc/passwd\" >]>" +
        "<?xml version=\"1.0\" encoding=\"utf-8\"?><relocation-mapping>&x;</relocation-mapping>";
    RelocationConfig.newInstance(new ByteArrayInputStream(xml.getBytes()));
    });
  }

}
