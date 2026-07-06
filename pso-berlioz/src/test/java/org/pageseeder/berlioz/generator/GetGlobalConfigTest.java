package org.pageseeder.berlioz.generator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.InitEnvironment;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.output.JsonOutputAdapter;
import org.pageseeder.berlioz.output.OutputWriter;
import org.pageseeder.berlioz.output.XmlOutputAdapter;

import java.io.File;

/**
 * Golden-value tests for {@link GetGlobalConfig}.
 *
 * <p>Assertions on {@link #process(ContentRequest)} and {@link #processJson(ContentRequest)}
 * use exact string equality rather than {@code contains}, so that any change to the XML shape
 * (backward compatibility) or the JSON shape must be a deliberate, visible edit to this file
 * rather than a side effect that slips through a looser check. Fixtures with more than one
 * property use order-independent assertions since {@link GlobalSettings#getAll()} does not
 * guarantee iteration order.
 */
class GetGlobalConfigTest {

  private static final File RESOURCES =
      new File("./src/test/resources/org/pageseeder/berlioz/generator/getglobalconfig");

  @AfterEach
  void tearDown() {
    GlobalSettings.setup((InitEnvironment) null);
  }

  // process() tests — XML
  // ---------------------------------------------------------------------------

  @Test
  void testProcessNoPropertiesWritesEmptyElement() {
    GlobalSettings.setup(new File(RESOURCES, "empty"));
    ContentRequest req = GeneratorTestSupport.request().build();
    String out = process(req);
    Assertions.assertEquals("<properties source=\"config.xml\"/>", out);
  }

  @Test
  void testProcessSingleProperty() {
    GlobalSettings.setup(new File(RESOURCES, "single"));
    ContentRequest req = GeneratorTestSupport.request().build();
    String out = process(req);
    Assertions.assertEquals(
        "<properties source=\"config.properties\">"
            + "<property name=\"app.name\" value=\"Test\"/>"
            + "</properties>", out);
  }

  @Test
  void testProcessRedactsSensitivePropertyValue() {
    GlobalSettings.setup(new File(RESOURCES, "redacted"));
    ContentRequest req = GeneratorTestSupport.request().build();
    String out = process(req);
    Assertions.assertEquals(
        "<properties source=\"config.properties\">"
            + "<property name=\"app.password\" value=\"[REDACTED]\"/>"
            + "</properties>", out);
  }

  @Test
  void testProcessMultiplePropertiesIncludesAllRegardlessOfOrder() {
    GlobalSettings.setup(new File(RESOURCES, "multi"));
    ContentRequest req = GeneratorTestSupport.request().build();
    String out = process(req);
    Assertions.assertTrue(out.startsWith("<properties source=\"config.properties\">"));
    Assertions.assertTrue(out.endsWith("</properties>"));
    Assertions.assertTrue(out.contains("<property name=\"app.name\" value=\"Test\"/>"));
    Assertions.assertTrue(out.contains("<property name=\"app.password\" value=\"[REDACTED]\"/>"));
    Assertions.assertEquals(2, out.split("<property ").length - 1, "Should contain exactly 2 properties");
  }

  // process() tests — JSON
  // ---------------------------------------------------------------------------

  @Test
  void testProcessJsonNoPropertiesWritesEmptyArray() {
    GlobalSettings.setup(new File(RESOURCES, "empty"));
    ContentRequest req = GeneratorTestSupport.request().build();
    String out = processJson(req);
    Assertions.assertEquals("{\"source\":\"config.xml\",\"properties\":[]}", out);
  }

  @Test
  void testProcessJsonSingleProperty() {
    GlobalSettings.setup(new File(RESOURCES, "single"));
    ContentRequest req = GeneratorTestSupport.request().build();
    String out = processJson(req);
    Assertions.assertEquals(
        "{\"source\":\"config.properties\","
            + "\"properties\":[{\"name\":\"app.name\",\"value\":\"Test\"}]}", out);
  }

  @Test
  void testProcessJsonRedactsSensitivePropertyValue() {
    GlobalSettings.setup(new File(RESOURCES, "redacted"));
    ContentRequest req = GeneratorTestSupport.request().build();
    String out = processJson(req);
    Assertions.assertEquals(
        "{\"source\":\"config.properties\","
            + "\"properties\":[{\"name\":\"app.password\",\"value\":\"[REDACTED]\"}]}", out);
  }

  @Test
  void testProcessJsonMultiplePropertiesIncludesAllRegardlessOfOrder() {
    GlobalSettings.setup(new File(RESOURCES, "multi"));
    ContentRequest req = GeneratorTestSupport.request().build();
    String out = processJson(req);
    Assertions.assertTrue(out.startsWith("{\"source\":\"config.properties\",\"properties\":["));
    Assertions.assertTrue(out.endsWith("]}"));
    Assertions.assertTrue(out.contains("{\"name\":\"app.name\",\"value\":\"Test\"}"));
    Assertions.assertTrue(out.contains("{\"name\":\"app.password\",\"value\":\"[REDACTED]\"}"));
    Assertions.assertEquals(2, out.split("\\{\"name\"").length - 1, "Should contain exactly 2 properties");
  }

  // getETag() tests
  // ---------------------------------------------------------------------------

  @Test
  void testETagNotNullWhenConfigFileExists() {
    GlobalSettings.setup(new File(RESOURCES, "single"));
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
    OutputWriter out = new XmlOutputAdapter();
    gen.generate(req, out);
    return out.toString();
  }

  private static String processJson(ContentRequest req) {
    GetGlobalConfig gen = new GetGlobalConfig();
    OutputWriter out = new JsonOutputAdapter();
    gen.generate(req, out);
    return out.toString();
  }
}
