package org.pageseeder.berlioz.generator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.InitEnvironment;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.xml.XmlStringBuilder;

import java.io.File;

class GetGlobalConfigTest {

  private static final File WEB_INF =
      new File("./src/test/resources/org/pageseeder/berlioz");

  @BeforeEach
  void setUp() {
    GlobalSettings.setup(WEB_INF);
  }

  @AfterEach
  void tearDown() {
    GlobalSettings.setup((InitEnvironment) null);
  }

  // process() tests
  // ---------------------------------------------------------------------------

  @Test
  void testProcessAlwaysWritesPropertiesElement() {
    ContentRequest req = GeneratorTestSupport.request().build();
    String out = process(req);
    Assertions.assertTrue(out.contains("<properties"), "Should contain <properties>");
  }

  @Test
  void testProcessIncludesSourceAttributeWhenConfigExists() {
    ContentRequest req = GeneratorTestSupport.request().build();
    String out = process(req);
    Assertions.assertTrue(out.contains("source="), "Should include source attribute when config file is found");
  }

  @Test
  void testETagNotNullWhenConfigFileExists() {
    GetGlobalConfig gen = new GetGlobalConfig();
    ContentRequest req = GeneratorTestSupport.request().build();
    String etag = gen.getETag((Request) req);
    Assertions.assertNotNull(etag, "ETag should not be null when a properties file is found");
    Assertions.assertFalse(etag.isEmpty());
  }

  @Test
  void testETagNullWhenNoConfigFile() {
    GlobalSettings.setup((InitEnvironment) null);
    GetGlobalConfig gen = new GetGlobalConfig();
    ContentRequest req = GeneratorTestSupport.request().build();
    Assertions.assertNull(gen.getETag((Request) req), "ETag should be null when no properties file is configured");
  }

  // helpers
  // ---------------------------------------------------------------------------

  private static String process(ContentRequest req) {
    GetGlobalConfig gen = new GetGlobalConfig();
    XmlStringBuilder xml = new XmlStringBuilder();
    gen.generate(req, xml);
    return xml.toString();
  }
}
