package org.pageseeder.berlioz.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.furi.URIPattern;
import org.pageseeder.berlioz.xml.BerliozEntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

final class RedirectConfigTest {

  File configFolder = new File("src/test/resources/org/pageseeder/berlioz/config");

  @Test
  void testConstructor() {
    RedirectConfig config = new RedirectConfig();
    Assertions.assertTrue(config.isEmpty());
  }

  @Test
  void testLoad_IgnoredRules() throws ConfigException {
    String xml = "<redirect-mapping>"
        + "<redirect from=\"/a\" to=\"/b\"/>"
        + "<redirect from=\"/{!x}\" to=\"/c\"/>"
        + "<redirect from=\"/d\" to=\"/{!y}?token=t\"/>"
        + "<redirect from=\"/e\" to=\"/f\"/>"
        + "</redirect-mapping>";
    RedirectConfig config = RedirectConfig.newInstance(new ByteArrayInputStream(xml.getBytes()));
    Assertions.assertEquals(2, config.size());
    Assertions.assertEquals(2, config.ignored().size());
    IgnoredRule first = config.ignored().get(0);
    Assertions.assertEquals(2, first.position());
    Assertions.assertEquals(IgnoredRule.Reason.INVALID_FROM, first.reason());
    Assertions.assertEquals("/{!x}", first.from());
    Assertions.assertEquals("/c", first.to());
    IgnoredRule second = config.ignored().get(1);
    Assertions.assertEquals(3, second.position());
    Assertions.assertEquals(IgnoredRule.Reason.INVALID_TO, second.reason());
    Assertions.assertTrue(new RedirectConfig().ignored().isEmpty());
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
    Assertions.assertEquals("/html/home", config.redirect("/").to());
    Assertions.assertNotNull(config.redirect("/index.html"));
    Assertions.assertEquals("/html/home", config.redirect("/index.html").to());
    Assertions.assertNotNull( config.redirect("/html"));
    Assertions.assertEquals("/html/home", config.redirect("/html").to());
    Assertions.assertNotNull(config.redirect("/xml"));
    Assertions.assertEquals("/xml/home", config.redirect("/xml").to());
    Assertions.assertNotNull(config.redirect("/example.psml"));
    Assertions.assertEquals("/html/example", config.redirect("/example.psml").to());
  }

  @Test
  void testLoadFile_DTD() throws ConfigException {
    RedirectConfig config = RedirectConfig.newInstance(new File(this.configFolder, "redirect_dtd.xml"));
    Assertions.assertNotNull(config);
  }

  /**
   * The DTD must accept the values that the schema and the parser accept, so that a file written
   * to the DTD loads.
   */
  @Test
  void testDTD_permanentMatchesSchema() throws Exception {
    for (String value : new String[]{"yes", "no"}) {
      String xml = withDoctype("<redirect from=\"/old\" to=\"/new\" permanent=\"" + value + "\"/>");
      Assertions.assertNull(dtdError(xml), "The DTD must accept permanent=\"" + value + "\"");
      RedirectConfig config = RedirectConfig.newInstance(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
      Assertions.assertEquals("yes".equals(value), config.redirect("/old").isPermanent());
    }
    for (String value : new String[]{"true", "false"}) {
      String xml = withDoctype("<redirect from=\"/old\" to=\"/new\" permanent=\"" + value + "\"/>");
      Assertions.assertNotNull(dtdError(xml), "The DTD must reject permanent=\"" + value + "\"");
      Assertions.assertThrows(ConfigException.class,
          () -> RedirectConfig.newInstance(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));
    }
  }

  private static String withDoctype(String redirects) {
    return "<?xml version=\"1.0\"?>"
        + "<!DOCTYPE redirect-mapping PUBLIC \"-//Berlioz//DTD::Redirect Mapping 1.0//EN\""
        + " \"https://pageseeder.org/schema/berlioz/redirect-mapping-1.0.dtd\">"
        + "<redirect-mapping>" + redirects + "</redirect-mapping>";
  }

  /** Validates against the DTD shipped in the library, returning the first error or null. */
  private static String dtdError(String xml) throws Exception {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setValidating(true);
    XMLReader reader = factory.newSAXParser().getXMLReader();
    reader.setEntityResolver(BerliozEntityResolver.INSTANCE);
    String[] error = {null};
    reader.setErrorHandler(new DefaultHandler() {
      @Override
      public void error(SAXParseException ex) {
        if (error[0] == null) error[0] = ex.getMessage();
      }
    });
    reader.parse(new InputSource(new StringReader(xml)));
    return error[0];
  }

  @Test
  void testLoadFile_Invalid() {
    Assertions.assertThrows(ConfigException.class, () -> RedirectConfig.newInstance(new File(this.configFolder, "redirect_invalid.xml")));
  }

  @Test
  void testLoadFile_Malformed() {
    Assertions.assertThrows(ConfigException.class, () -> RedirectConfig.newInstance(new File(this.configFolder, "redirect_malformed.xml")));
  }

  @Test
  void testLoad_XXE() {
    String xml = "<!-- XXE --><!DOCTYPE redirect-mapping [<!ELEMENT redirect-mapping ANY > <!ENTITY x SYSTEM \"/etc/password.xml\" >]><redirect-mapping>&x;</redirect-mapping>";
    Assertions.assertThrows(ConfigException.class, () -> RedirectConfig.newInstance(new ByteArrayInputStream(xml.getBytes())));
  }

  @Test
  void testLoad_XXE2() {
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
    Assertions.assertEquals("/new", loc.to());
    Assertions.assertTrue(loc.isPermanent(), "isPermanent() should be true for permanent=\"yes\"");
  }

  /**
   * <code>status</code> sets the status, and takes precedence over <code>permanent</code>;
   * without it, <code>permanent</code> chooses between 301 and 302 as before.
   */
  @ParameterizedTest
  @CsvSource(delimiter = '|', value = {
      "status=\"301\"                   | 301 | true",
      "status=\"302\"                   | 302 | false",
      "status=\"307\"                   | 307 | false",
      "status=\"308\"                   | 308 | true",
      "permanent=\"yes\"                | 301 | true",
      "permanent=\"no\"                 | 302 | false",
      "''                               | 302 | false",
      "permanent=\"yes\" status=\"307\" | 307 | false",
      "permanent=\"no\" status=\"308\"  | 308 | true",
      "permanent=\"yes\" status=\"302\" | 302 | false",
  })
  void testLoad_Status(String attributes, int code, boolean permanent) throws ConfigException {
    String xml = "<redirect-mapping><redirect from=\"/old\" to=\"/new\" " + attributes + "/></redirect-mapping>";
    RedirectConfig config = RedirectConfig.newInstance(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    RedirectLocation loc = config.redirect("/old");
    Assertions.assertNotNull(loc);
    Assertions.assertEquals(code, loc.status().code());
    Assertions.assertEquals(permanent, loc.isPermanent());
    Assertions.assertEquals(code, config.patterns().get(0).status().code());
    Assertions.assertEquals(permanent, config.patterns().get(0).isPermanent());
  }

  @ParameterizedTest
  @ValueSource(strings = {"200", "300", "303", "304", "305", "404", "permanent", " 301", ""})
  void testLoad_UnsupportedStatusFailsValidation(String status) {
    String xml = "<redirect-mapping><redirect from=\"/old\" to=\"/new\" status=\"" + status + "\"/></redirect-mapping>";
    Assertions.assertThrows(ConfigException.class,
        () -> RedirectConfig.newInstance(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));
  }

  @Test
  void testDTD_statusMatchesSchema() throws Exception {
    for (String value : new String[]{"301", "302", "307", "308"}) {
      String xml = withDoctype("<redirect from=\"/old\" to=\"/new\" status=\"" + value + "\"/>");
      Assertions.assertNull(dtdError(xml), "The DTD must accept status=\"" + value + "\"");
      RedirectConfig config = RedirectConfig.newInstance(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
      Assertions.assertEquals(Integer.parseInt(value), config.redirect("/old").status().code());
    }
    String xml = withDoctype("<redirect from=\"/old\" to=\"/new\" status=\"303\"/>");
    Assertions.assertNotNull(dtdError(xml), "The DTD must reject status=\"303\"");
  }

  @Test
  @SuppressWarnings("removal")
  void testPattern_Status() {
    URIPattern from = new URIPattern("/old");
    URIPattern to = new URIPattern("/new");
    Assertions.assertEquals(ContentStatus.MOVED_PERMANENTLY, new RedirectPattern(from, to, true).status());
    Assertions.assertEquals(ContentStatus.FOUND, new RedirectPattern(from, to, false).status());
    RedirectPattern pattern = new RedirectPattern(from, to, ContentStatus.PERMANENT_REDIRECT);
    Assertions.assertTrue(pattern.isPermanent());
    Assertions.assertEquals("redirect: /old -> /new 308", pattern.toString());
    Assertions.assertEquals(ContentStatus.PERMANENT_REDIRECT, pattern.redirect("/old").status());
    Assertions.assertThrows(IllegalArgumentException.class, () -> new RedirectPattern(from, to, ContentStatus.SEE_OTHER));
    Assertions.assertThrows(IllegalArgumentException.class, () -> new RedirectPattern(from, to, ContentStatus.OK));
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
    Assertions.assertEquals("/home", config.redirect("/").to());
  }

  @Test
  void testLoad_XMLBomb() {
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
    Assertions.assertThrows(ConfigException.class, () -> RedirectConfig.newInstance(new ByteArrayInputStream(xml.getBytes())));
  }

}
