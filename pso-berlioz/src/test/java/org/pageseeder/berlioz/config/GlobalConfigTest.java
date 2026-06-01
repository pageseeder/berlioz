package org.pageseeder.berlioz.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class GlobalConfigTest {

  @Test
  public void testConstructor() {
    GlobalConfig config = new GlobalConfig();
    Assertions.assertNotNull(config.properties());
    Assertions.assertTrue(config.properties().isEmpty());
  }

  @Test
  public void testLoad_Empty() throws ConfigException {
    GlobalConfig config = new GlobalConfig();
    String xml = "<global/>";
    config.load(new ByteArrayInputStream(xml.getBytes()));
    Assertions.assertNotNull(config.properties());
    Assertions.assertTrue(config.properties().isEmpty());
  }

  @Test
  public void testLoad_XXE() throws ConfigException {
    Assertions.assertThrows(ConfigException.class, () -> {
    GlobalConfig config = new GlobalConfig();
    String xml = "<!DOCTYPE global [<!ELEMENT global ANY > <!ENTITY x SYSTEM \"./x.xml\" >]><global>&x;<global/>";
    config.load(new ByteArrayInputStream(xml.getBytes()));
    });
  }

  @Test
  public void testLoad_XMLBomb() throws ConfigException {
    Assertions.assertThrows(ConfigException.class, () -> {
    GlobalConfig config = new GlobalConfig();
    String xml = "<!DOCTYPE global [\n" +
        "  <!ELEMENT global ANY >\n" +
        "  <!ENTITY lol \"lol\">\n" +
        "  <!ENTITY lol1 \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;\">\n" +
        "  <!ENTITY lol2 \"&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;\">\n" +
        "  <!ENTITY lol3 \"&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;\">\n" +
        "  <!ENTITY lol4 \"&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;\">\n" +
        "  <!ENTITY lol5 \"&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;\">\n" +
        "  <!ENTITY lol6 \"&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;\">\n" +
        "  <!ENTITY lol7 \"&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;\">\n" +
        "  <!ENTITY lol8 \"&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;\">\n" +
        "  <!ENTITY lol9 \"&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;\">\n" +
        "]><global>&lol9;</global>";
    config.load(new ByteArrayInputStream(xml.getBytes()));
    });
  }

  @Test
  public void testLoad_Global() throws ConfigException {
    GlobalConfig config = new GlobalConfig();
    String xml = "<global greeting='hello' empty=''/>";
    config.load(new ByteArrayInputStream(xml.getBytes()));
    Map<String, String> properties = config.properties();
    Assertions.assertEquals(2, properties.size());
    Assertions.assertEquals(properties.get("greeting"), "hello");
    Assertions.assertEquals(properties.get("empty"), "");
    Assertions.assertNull(properties.get("undefined"));
  }

  @Test
  public void testLoad_Tree() throws ConfigException {
    GlobalConfig config = new GlobalConfig();
    String xml = "<global><a x='1'><b y='2'><c z='3'></c><d z='4' q='5'></d></b></a></global>";
    config.load(new ByteArrayInputStream(xml.getBytes()));
    Map<String, String> properties = config.properties();
    // Element do not generate properties
    Assertions.assertNull(properties.get("a"));
    Assertions.assertNull(properties.get("a.b"));
    Assertions.assertNull(properties.get("a.b.c"));
    Assertions.assertNull(properties.get("a.b.d"));
    // Attributes do
    Assertions.assertEquals(5, properties.size());
    Assertions.assertEquals(properties.get("a.x"), "1");
    Assertions.assertEquals(properties.get("a.b.y"), "2");
    Assertions.assertEquals(properties.get("a.b.c.z"), "3");
    Assertions.assertEquals(properties.get("a.b.d.z"), "4");
    Assertions.assertEquals(properties.get("a.b.d.q"), "5");
  }

  @Test
  public void testLoad_Aliases() throws ConfigException {
    GlobalConfig config = new GlobalConfig();
    String xml = "<global a='0'><a x='1' x.y='2'><x z='3'/><x q='4'/></a><a.x w='5'/></global>";
    config.load(new ByteArrayInputStream(xml.getBytes()));
    Map<String, String> properties = config.properties();
    // Attributes do
    Assertions.assertEquals(6, properties.size());
    Assertions.assertEquals(properties.get("a"), "0");
    Assertions.assertEquals(properties.get("a.x"), "1");
    Assertions.assertEquals(properties.get("a.x.y"), "2");
    Assertions.assertEquals(properties.get("a.x.z"), "3");
    Assertions.assertEquals(properties.get("a.x.q"), "4");
    Assertions.assertEquals(properties.get("a.x.w"), "5");
  }

  @Test
  public void testLoad_Duplicates() throws ConfigException {
    GlobalConfig config = new GlobalConfig();
    String xml = "<global a.x='_' a.x.y='_'><a x='1' x.y='_'/><a.x y='2'/></global>";
    config.load(new ByteArrayInputStream(xml.getBytes()));
    Map<String, String> properties = config.properties();
    // Attributes do
    Assertions.assertEquals(2, properties.size());
    Assertions.assertEquals(properties.get("a.x"), "1");
    Assertions.assertEquals(properties.get("a.x.y"), "2");
  }

  @Test
  public void testLoad_Overrides() throws ConfigException {
    GlobalConfig config = new GlobalConfig();
    String original = "<global a='_'><a x='_' y='3'/></global>";
    String override = "<global a='1'><a x='2' z='4'/></global>";
    config.load(new ByteArrayInputStream(original.getBytes()));
    config.load(new ByteArrayInputStream(override.getBytes()));
    Map<String, String> properties = config.properties();
    // Attributes do
    Assertions.assertEquals(4, properties.size());
    Assertions.assertEquals(properties.get("a"), "1");
    Assertions.assertEquals(properties.get("a.x"), "2");
    Assertions.assertEquals(properties.get("a.y"), "3");
    Assertions.assertEquals(properties.get("a.z"), "4");
  }

  @Test
  public void testLoad_Invalid() throws ConfigException {
    Assertions.assertThrows(ConfigException.class, () -> {
    GlobalConfig config = new GlobalConfig();
    String invalid = "<global>";
    config.load(new ByteArrayInputStream(invalid.getBytes()));
    });
  }

  @Test
  public void testToXML_Empty() throws IOException, ConfigException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    new GlobalConfig().save(out);
    byte[] xml = out.toByteArray();
    GlobalConfig config = new GlobalConfig();
    config.load(new ByteArrayInputStream(xml));
    Assertions.assertNotNull(config.properties());
    Assertions.assertTrue(config.properties().isEmpty());
  }

  @Test
  public void testToXML_Global() throws ConfigException, IOException {
    Map<String, String> properties = new HashMap<>();
    properties.put("a", "1");
    properties.put("b", "2");
    // Save
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    new GlobalConfig(properties).save(out);
    byte[] xml = out.toByteArray();
    // Load
    GlobalConfig config = new GlobalConfig();
    config.load(new ByteArrayInputStream(xml));
    Assertions.assertNotNull(config.properties());
    Assertions.assertEquals(properties.size(), config.properties().size());
    Assertions.assertEquals(properties, config.properties());
  }


  @Test
  public void testToXML_ManyProperties() throws ConfigException, IOException {
    Map<String, String> properties = new HashMap<>();
    properties.put("version", "1.0");
    properties.put("berlioz.cache", "true");
    properties.put("berlioz.xslt.cache", "true");
    properties.put("app.a", "1");
    properties.put("app.b", "2");
    properties.put("app.a.x", "3");
    properties.put("app.a.y", "4");
    properties.put("app.c.m", "5");
    properties.put("app.d.m", "6");
    // Save
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    new GlobalConfig(properties).save(out);
    byte[] xml = out.toByteArray();
    // Load
    GlobalConfig config = new GlobalConfig();
    config.load(new ByteArrayInputStream(xml));
    Assertions.assertNotNull(config.properties());
    Assertions.assertEquals(properties.size(), config.properties().size());
    Assertions.assertEquals(properties, config.properties());
  }

  @Test
  public void testToXML_IllegalNames() throws ConfigException, IOException {
    Map<String, String> properties = new HashMap<>();
    properties.put("app.1", "1");
    properties.put("app.2a", "2");
    properties.put("app.a.&", "3");
    properties.put("app.a.#", "4");
    properties.put("app.c.-", "5");
    properties.put("app.1.a", "6");
    properties.put("app.&.b", "3");
    properties.put("app.#.c", "4");
    properties.put("app.-.d", "5");
    properties.put("app.!.f", "6");
    // Save
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    new GlobalConfig(properties).save(out);
    byte[] xml = out.toByteArray();
    // Load
    GlobalConfig config = new GlobalConfig();
    config.load(new ByteArrayInputStream(xml));
    Assertions.assertNotNull(config.properties());
    Assertions.assertEquals(0, config.properties().size());
  }
}
