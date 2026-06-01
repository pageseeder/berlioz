package org.pageseeder.berlioz.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;

final class RedirectConfigTest {

  File configFolder = new File("src/test/resources/org/pageseeder/berlioz/config");

  @Test
  void testConstructor() {
    RedirectConfig config = new RedirectConfig();
    Assertions.assertTrue(config.isEmpty());
  }

  @Test
  void testLoad_Empty() throws ConfigException {
    String xml = "<redirect-mapping/>";
    RedirectConfig config = RedirectConfig.newInstance(new ByteArrayInputStream(xml.getBytes()));
    Assertions.assertTrue(config.isEmpty());
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
  void testLoad_File() throws ConfigException {
    RedirectConfig config = RedirectConfig.newInstance(new File(this.configFolder, "redirect.xml"));
    Assertions.assertNull(config.redirect("/index.xml"));
    Assertions.assertNull(config.redirect("/example.html"));
    Assertions.assertNotNull(config.redirect("/"));
    Assertions.assertEquals(config.redirect("/").to(), "/html/home");
    Assertions.assertNotNull(config.redirect("/index.html"));
    Assertions.assertEquals(config.redirect("/index.html").to(), "/html/home");
    Assertions.assertNotNull( config.redirect("/html"));
    Assertions.assertEquals(config.redirect("/html").to(), "/html/home");
    Assertions.assertNotNull(config.redirect("/xml"));
    Assertions.assertEquals(config.redirect("/xml").to(), "/xml/home");
    Assertions.assertNotNull(config.redirect("/example.psml"));
    Assertions.assertEquals(config.redirect("/example.psml").to(), "/html/example");
  }

  @Test
  void testLoadFile_DTD() throws ConfigException {
    RedirectConfig config = RedirectConfig.newInstance(new File(this.configFolder, "redirect_dtd.xml"));
    Assertions.assertNotNull(config);
  }

  @Test
  void testLoadFile_Invalid() throws ConfigException {
    Assertions.assertThrows(ConfigException.class, () -> RedirectConfig.newInstance(new File(this.configFolder, "redirect_invalid.xml")));
  }

  @Test
  void testLoadFile_Malformed() throws ConfigException {
    Assertions.assertThrows(ConfigException.class, () -> RedirectConfig.newInstance(new File(this.configFolder, "redirect_malformed.xml")));
  }

  @Test
  void testLoad_XXE() throws ConfigException {
    Assertions.assertThrows(ConfigException.class, () -> {
    String xml = "<!-- XXE --><!DOCTYPE redirect-mapping [<!ELEMENT redirect-mapping ANY > <!ENTITY x SYSTEM \"/etc/password.xml\" >]><redirect-mapping>&x;</redirect-mapping>";
    RedirectConfig.newInstance(new ByteArrayInputStream(xml.getBytes()));
    });
  }

  @Test
  void testLoad_XXE2() throws ConfigException {
    Assertions.assertThrows(ConfigException.class, () -> RedirectConfig.newInstance(new File(this.configFolder, "redirect_xxe.xml")));
  }

  @Test
  void testLoad_PermanentRedirect() throws ConfigException {
    String xml = "<redirect-mapping>" +
        "<redirect from=\"/old\" to=\"/new\" permanent=\"yes\"/>" +
        "</redirect-mapping>";
    RedirectConfig config = RedirectConfig.newInstance(new ByteArrayInputStream(xml.getBytes()));
    RedirectLocation loc = config.redirect("/old");
    Assertions.assertNotNull(loc);
    Assertions.assertEquals(loc.to(), "/new");
    Assertions.assertTrue(loc.isPermanent(), "isPermanent() should be true for permanent=\"yes\"");
  }

  @Test
  void testLoad_TemporaryRedirectIsNotPermanent() throws ConfigException {
    String xml = "<redirect-mapping>" +
        "<redirect from=\"/old\" to=\"/new\"/>" +
        "</redirect-mapping>";
    RedirectConfig config = RedirectConfig.newInstance(new ByteArrayInputStream(xml.getBytes()));
    RedirectLocation loc = config.redirect("/old");
    Assertions.assertNotNull(loc);
    Assertions.assertFalse(loc.isPermanent(), "isPermanent() should be false when permanent attribute is absent");
  }

  @Test
  void testLoadFile_WeboorganicDTD() throws ConfigException {
    String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
        "<!DOCTYPE redirect-mapping PUBLIC \"-//Weborganic//DTD::Redirect Mapping 1.0//EN\" \"\">" +
        "<redirect-mapping><redirect from=\"/\" to=\"/home\"/></redirect-mapping>";
    RedirectConfig config = RedirectConfig.newInstance(new ByteArrayInputStream(xml.getBytes()));
    Assertions.assertNotNull(config.redirect("/"));
    Assertions.assertEquals(config.redirect("/").to(), "/home");
  }

  @Test
  void testLoad_XMLBomb() throws ConfigException {
    Assertions.assertThrows(ConfigException.class, () -> {
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
    });
  }

}
