package org.pageseeder.berlioz.config;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;

public final class RedirectConfigTest {

  File configFolder = new File("src/test/resources/org/pageseeder/berlioz/config");

  @Test
  public void testConstructor() {
    RedirectConfig config = new RedirectConfig();
    Assert.assertTrue(config.isEmpty());
  }

  @Test
  public void testLoad_Empty() throws ConfigException {
    String xml = "<redirect-mapping/>";
    RedirectConfig config = RedirectConfig.newInstance(new ByteArrayInputStream(xml.getBytes()));
    Assert.assertTrue(config.isEmpty());
  }

  /**
   * <pre>{@code
   * <redirect-mapping>
   *   <redirect from="/"             to="/html/home"/>
   *   <redirect from="/index.html"   to="/html/home"/>
   *   <redirect from="/html"         to="/html/home"/>
   *   <redirect from="/xml"          to="/xml/home"/>
   *   <redirect from="/{+path}.psml" to="/html/{+path}"/>
   * </redirect-mapping>
   * }</pre>
   */
  @Test
  public void testLoad_File() throws ConfigException {
    RedirectConfig config = RedirectConfig.newInstance(new File(this.configFolder, "redirect.xml"));
    Assert.assertNull(config.redirect("/index.xml"));
    Assert.assertNull(config.redirect("/example.html"));
    Assert.assertNotNull(config.redirect("/"));
    Assert.assertEquals("/html/home", config.redirect("/").to());
    Assert.assertNotNull(config.redirect("/index.html"));
    Assert.assertEquals("/html/home", config.redirect("/index.html").to());
    Assert.assertNotNull( config.redirect("/html"));
    Assert.assertEquals("/html/home", config.redirect("/html").to());
    Assert.assertNotNull(config.redirect("/xml"));
    Assert.assertEquals("/xml/home", config.redirect("/xml").to());
    Assert.assertNotNull(config.redirect("/example.psml"));
    Assert.assertEquals("/html/example", config.redirect("/example.psml").to());
  }

  @Test
  public void testLoadFile_DTD() throws ConfigException {
    RedirectConfig.newInstance(new File(this.configFolder, "redirect_dtd.xml"));
  }

  @Test(expected = ConfigException.class)
  public void testLoadFile_Invalid() throws ConfigException {
    RedirectConfig.newInstance(new File(this.configFolder, "redirect_invalid.xml"));
  }

  @Test(expected = ConfigException.class)
  public void testLoadFile_Malformed() throws ConfigException {
    RedirectConfig.newInstance(new File(this.configFolder, "redirect_malformed.xml"));
  }

  @Test(expected = ConfigException.class)
  public void testLoad_XXE() throws ConfigException {
    String xml = "<!-- XXE --><!DOCTYPE redirect-mapping [<!ELEMENT redirect-mapping ANY > <!ENTITY x SYSTEM \"/etc/password.xml\" >]><redirect-mapping>&x;</redirect-mapping>";
    RedirectConfig.newInstance(new ByteArrayInputStream(xml.getBytes()));
  }

  @Test(expected = ConfigException.class)
  public void testLoad_XXE2() throws ConfigException {
    RedirectConfig.newInstance(new File(this.configFolder, "redirect_xxe.xml"));
  }

  @Test(expected = ConfigException.class)
  public void testLoad_XMLBomb() throws ConfigException {
    String xml = "<!DOCTYPE redirect-mapping [\n" +
        "  <!ELEMENT redirect-mapping ANY >\n" +
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
        "]><redirect-mapping><redirect from=\"&lol9;\" to=\"&lol9\"/></redirect-mapping>";
    RedirectConfig.newInstance(new ByteArrayInputStream(xml.getBytes()));
  }

}
