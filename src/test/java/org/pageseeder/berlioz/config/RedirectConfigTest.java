package org.pageseeder.berlioz.config;

import org.junit.Assert;
import org.junit.Test;
import org.pageseeder.berlioz.BerliozException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

public final class RedirectConfigTest {

  File configFolder = new File("src/test/resources/org/pageseeder/berlioz/config");

  @Test
  public void testConstructor() {
    RedirectConfig config = new RedirectConfig();
    Assert.assertTrue(config.isEmpty());
  }

  @Test
  public void testLoad_Empty() throws IOException {
    RedirectConfig config = new RedirectConfig();
    String xml = "<redirect-mapping/>";
    config.load(new ByteArrayInputStream(xml.getBytes()));
    Assert.assertTrue(config.isEmpty());
  }

  @Test
  public void testLoad_XXE() throws IOException {
    RedirectConfig config = new RedirectConfig();
    String xml = "<!DOCTYPE redirect-mapping [<!ELEMENT global ANY > <!ENTITY x SYSTEM \"/etc/password.xml\" >]><redirect-mapping>&x;</redirect-mapping>";
    config.load(new ByteArrayInputStream(xml.getBytes()));
  }

  @Test(expected = BerliozException.class)
  public void testLoad_XXE2() throws BerliozException {
    RedirectConfig config = RedirectConfig.newInstance(new File(this.configFolder, "redirect_xxe.xml"));
  }

  @Test(expected = IOException.class)
  public void testLoad_XMLBomb() throws IOException {
    RedirectConfig config = new RedirectConfig();
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
    config.load(new ByteArrayInputStream(xml.getBytes()));
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
  public void testLoad_File() {
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

}
