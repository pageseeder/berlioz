package org.pageseeder.berlioz.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public final class XMLConfigTest {

  @Test
  public void testConstructor() {
    XMLConfig config = new XMLConfig();
    Assert.assertNotNull(config.properties());
    Assert.assertTrue(config.properties().isEmpty());
  }

  @Test
  public void testLoad_Empty() throws IOException {
    XMLConfig config = new XMLConfig();
    String xml = "<global/>";
    config.load(new ByteArrayInputStream(xml.getBytes()));
    Assert.assertNotNull(config.properties());
    Assert.assertTrue(config.properties().isEmpty());
  }

  @Test
  public void testLoad_Global() throws IOException {
    XMLConfig config = new XMLConfig();
    String xml = "<global greeting='hello' empty=''/>";
    config.load(new ByteArrayInputStream(xml.getBytes()));
    Map<String, String> properties = config.properties();
    Assert.assertEquals(2, properties.size());
    Assert.assertEquals("hello", properties.get("greeting"));
    Assert.assertEquals("", properties.get("empty"));
    Assert.assertNull(properties.get("undefined"));
  }

  @Test
  public void testLoad_Tree() throws IOException {
    XMLConfig config = new XMLConfig();
    String xml = "<global><a x='1'><b y='2'><c z='3'></c><d z='4' q='5'></d></b></a></global>";
    config.load(new ByteArrayInputStream(xml.getBytes()));
    Map<String, String> properties = config.properties();
    // Element do not generate properties
    Assert.assertNull(properties.get("a"));
    Assert.assertNull(properties.get("a.b"));
    Assert.assertNull(properties.get("a.b.c"));
    Assert.assertNull(properties.get("a.b.d"));
    // Attributes do
    Assert.assertEquals(5, properties.size());
    Assert.assertEquals("1", properties.get("a.x"));
    Assert.assertEquals("2", properties.get("a.b.y"));
    Assert.assertEquals("3", properties.get("a.b.c.z"));
    Assert.assertEquals("4", properties.get("a.b.d.z"));
    Assert.assertEquals("5", properties.get("a.b.d.q"));
  }

  @Test
  public void testLoad_Aliases() throws IOException {
    XMLConfig config = new XMLConfig();
    String xml = "<global a='0'><a x='1' x.y='2'><x z='3'/><x q='4'/></a><a.x w='5'/></global>";
    config.load(new ByteArrayInputStream(xml.getBytes()));
    Map<String, String> properties = config.properties();
    // Attributes do
    Assert.assertEquals(6, properties.size());
    Assert.assertEquals("0", properties.get("a"));
    Assert.assertEquals("1", properties.get("a.x"));
    Assert.assertEquals("2", properties.get("a.x.y"));
    Assert.assertEquals("3", properties.get("a.x.z"));
    Assert.assertEquals("4", properties.get("a.x.q"));
    Assert.assertEquals("5", properties.get("a.x.w"));
  }

  @Test
  public void testLoad_Duplicates() throws IOException {
    XMLConfig config = new XMLConfig();
    String xml = "<global a.x='_' a.x.y='_'><a x='1' x.y='_'/><a.x y='2'/></global>";
    config.load(new ByteArrayInputStream(xml.getBytes()));
    Map<String, String> properties = config.properties();
    // Attributes do
    Assert.assertEquals(2, properties.size());
    Assert.assertEquals("1", properties.get("a.x"));
    Assert.assertEquals("2", properties.get("a.x.y"));
  }

  @Test
  public void testLoad_Overrides() throws IOException {
    XMLConfig config = new XMLConfig();
    String original = "<global a='_'><a x='_' y='3'/></global>";
    String override = "<global a='1'><a x='2' z='4'/></global>";
    config.load(new ByteArrayInputStream(original.getBytes()));
    config.load(new ByteArrayInputStream(override.getBytes()));
    Map<String, String> properties = config.properties();
    // Attributes do
    Assert.assertEquals(4, properties.size());
    Assert.assertEquals("1", properties.get("a"));
    Assert.assertEquals("2", properties.get("a.x"));
    Assert.assertEquals("3", properties.get("a.y"));
    Assert.assertEquals("4", properties.get("a.z"));
  }

  @Test(expected = IOException.class)
  public void testLoad_Invalid() throws IOException {
    XMLConfig config = new XMLConfig();
    String invalid = "<global>";
    config.load(new ByteArrayInputStream(invalid.getBytes()));
  }

}
