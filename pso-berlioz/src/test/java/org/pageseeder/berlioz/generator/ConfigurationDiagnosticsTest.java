package org.pageseeder.berlioz.generator;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import javax.servlet.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.InitEnvironment;
import org.pageseeder.berlioz.content.*;
import org.pageseeder.berlioz.output.*;
import org.pageseeder.berlioz.servlet.*;
import static org.junit.jupiter.api.Assertions.*;

class ConfigurationDiagnosticsTest {
  @TempDir Path root;
  private final Map<Class<?>, Map<Object, Object>> saved = new HashMap<>();

  @BeforeEach void setup() throws Exception {
    Files.createDirectories(root.resolve("WEB-INF/config"));
    GlobalSettings.setup(root.resolve("WEB-INF").toFile());
    for (Class<?> type : List.of(BerliozConfig.class, ErrorHandlerServlet.class)) {
      Map<Object, Object> registry = registry(type);
      saved.put(type, new HashMap<>(registry));
      registry.clear();
    }
  }

  @AfterEach void cleanup() throws Exception {
    for (Class<?> type : saved.keySet()) {
      registry(type).clear();
      registry(type).putAll(saved.get(type));
    }
    GlobalSettings.setup((InitEnvironment) null);
  }

  @SuppressWarnings("unchecked")
  private Map<Object, Object> registry(Class<?> type) throws Exception {
    Field field = type.getDeclaredField("CONFIGS");
    field.setAccessible(true);
    return (Map<Object, Object>) field.get(null);
  }

  @Test void emptySchemas() {
    assertEquals("{\"servlets\":[]}", json(new GetBerliozConfig(), request()));
    assertEquals("{\"servlets\":[]}", json(new GetErrorHandlerConfig(), request()));
    assertEquals("<berlioz-config/>", xml(new GetBerliozConfig(), request()));
    assertEquals("<error-handler-config/>", xml(new GetErrorHandlerConfig(), request()));
  }

  @Test void berliozExactSchemasAndLifecycle() throws Exception {
    BerliozServlet servlet = new BerliozServlet();
    servlet.init(config("html", Map.of("stylesheet", "resource:/xslt/{GROUP}.xsl",
        "fallback-stylesheet", "classpath:xslt/default.xsl", "cache-control", "no-cache",
        "http-compression", "true"), List.of("/pages/*", "*.html"), false));
    GetBerliozConfig generator = new GetBerliozConfig();
    assertEquals("{\"servlets\":[{\"name\":\"html\",\"mappings\":[\"*.html\",\"/pages/*\"],"
        + "\"mediaType\":\"text/html\",\"charset\":\"UTF-8\",\"cacheControl\":\"no-cache\",\"compression\":true,"
        + "\"xsltCache\":\"manual\",\"stylesheet\":{\"allocation\":\"group\",\"source\":\"classpath\",\"path\":\"classpath:xslt/{GROUP}.xsl\"},"
        + "\"fallback\":{\"source\":\"classpath\",\"path\":\"classpath:xslt/default.xsl\"}}]}", json(generator, request()));
    assertEquals("<berlioz-config><servlet name=\"html\" mappings=\"*.html /pages/*\" media-type=\"text/html\" charset=\"UTF-8\" "
        + "cache-control=\"no-cache\" compression=\"true\" xslt-cache=\"manual\"><stylesheet allocation=\"group\" source=\"classpath\" "
        + "path=\"classpath:xslt/{GROUP}.xsl\"/><fallback source=\"classpath\" path=\"classpath:xslt/default.xsl\"/></servlet></berlioz-config>", xml(generator, request()));
    servlet.destroy();
    assertEquals("{\"servlets\":[]}", json(generator, request()));
  }

  @Test void errorExactSchemasAndLifecycle() throws Exception {
    options("berlioz.errors.stylesheet=xslt/missing.xsl\nberlioz.errors.detail=full\n");
    ErrorHandlerServlet servlet = new ErrorHandlerServlet();
    servlet.init(config("errors", Map.of("forward-extensions", "xml,html,json", "ignore-extensions", "png,css",
        "forward-default", "html"), List.of("/errors/*"), false));
    GetErrorHandlerConfig generator = new GetErrorHandlerConfig();
    assertEquals("{\"servlets\":[{\"name\":\"errors\",\"mappings\":[\"/errors/*\"],\"problemXml\":true,\"detail\":\"full\","
        + "\"forward\":{\"source\":\"explicit\",\"extensions\":[\"html\",\"json\",\"xml\"],\"defaultExtension\":\"html\"},"
        + "\"ignore\":{\"extensions\":[\"css\",\"png\"]},\"stylesheet\":{\"configured\":\"custom\",\"path\":\"xslt/missing.xsl\","
        + "\"selected\":\"built-in\",\"fallbackReason\":\"missing-or-unreadable\"}}]}", json(generator, request()));
    assertEquals("<error-handler-config><servlet name=\"errors\" mappings=\"/errors/*\" problem-xml=\"true\" detail=\"full\">"
        + "<forward source=\"explicit\" extensions=\"html json xml\" default-extension=\"html\"/><ignore extensions=\"css png\"/>"
        + "<stylesheet configured=\"custom\" path=\"xslt/missing.xsl\" selected=\"built-in\" fallback-reason=\"missing-or-unreadable\"/>"
        + "</servlet></error-handler-config>", xml(generator, request()));
    servlet.destroy();
    assertEquals("{\"servlets\":[]}", json(generator, request()));
  }

  @Test void snapshotsRemainConsistentAcrossReloadAndGenerationOrder() throws Exception {
    ErrorHandlerServlet servlet = new ErrorHandlerServlet();
    servlet.init(config("errors", Map.of(), List.of(), false));
    GetErrorHandlerConfig generator = new GetErrorHandlerConfig();
    Request first = request();
    String etag = generator.getETag(first);
    String before = json(generator, first);
    options("berlioz.errors.detail=full\n");
    assertEquals(etag, generator.getETag(first));
    assertEquals(before, json(generator, first));
    Request second = request();
    String after = json(generator, second);
    String changed = generator.getETag(second);
    assertNotEquals(etag, changed);
    assertTrue(after.contains("\"detail\":\"full\""));
    options("berlioz.errors.detail=minimal\n");
    assertEquals(changed, generator.getETag(second));
    assertEquals(after, json(generator, second));
    assertEquals(etag, generator.getETag(request()));
    assertTrue(etag.matches("[0-9a-f]{64}"));
  }

  @Test void safePathsSecretsAndIdentity() throws Exception {
    BerliozConfig.newConfig(config("z", Map.of("stylesheet", "IDENTITY", "control-key", "SECRET"), List.of(), false));
    BerliozConfig.newConfig(config("a", Map.of("stylesheet", "/private/SECRET.xsl",
        "fallback-stylesheet", "jar:file:/SECRET.jar!/x.xsl"), List.of(), false));
    GetBerliozConfig generator = new GetBerliozConfig();
    String json = json(generator, request());
    assertTrue(json.indexOf("\"name\":\"a\"") < json.indexOf("\"name\":\"z\""));
    assertTrue(json.contains("\"stylesheet\":{\"allocation\":\"identity\"}"));
    assertTrue(json.contains("\"fallback\":{\"source\":\"filesystem\"}"));
    assertTrue(xml(generator, request()).contains("<fallback source=\"filesystem\"/>"));
    assertFalse(json.contains("SECRET"));
    assertFalse(xml(generator, request()).contains("SECRET"));
    String etag = generator.getETag(request());
    options("secret=CHANGED\n");
    assertEquals(etag, generator.getETag(request()));
    options("berlioz.xslt.cache=auto\n");
    assertNotEquals(etag, generator.getETag(request()));
  }

  @Test void forwardingSourcesAndSelectionChanges() throws Exception {
    ErrorHandlerServlet legacy = new ErrorHandlerServlet();
    legacy.init(config("legacy", Map.of(), List.of(), false));
    ErrorHandlerServlet discovered = new ErrorHandlerServlet();
    discovered.init(config("discovered", Map.of(), List.of("*.xml"), true));
    GetErrorHandlerConfig generator = new GetErrorHandlerConfig();
    String json = json(generator, request());
    assertTrue(json.contains("\"source\":\"legacy-default\""));
    assertTrue(json.contains("\"source\":\"discovered\""));
    assertTrue(json.contains("\"configured\":\"built-in\",\"selected\":\"built-in\""));
    options("berlioz.errors.stylesheet=custom.xsl\n");
    String missing = generator.getETag(request());
    Files.writeString(root.resolve("WEB-INF/custom.xsl"), "not compiled by diagnostics");
    assertTrue(json(generator, request()).contains("\"selected\":\"custom\""));
    assertNotEquals(missing, generator.getETag(request()));
    options("berlioz.errors.stylesheet=../SECRET.xsl\n");
    assertFalse(json(generator, request()).contains("SECRET"));
  }

  @Test void replacementRegistrationSurvivesOldInstanceDestruction() throws Exception {
    BerliozServlet first = new BerliozServlet();
    BerliozServlet second = new BerliozServlet();
    first.init(config("same", Map.of(), List.of(), false));
    second.init(config("same", Map.of(), List.of(), false));
    first.destroy();
    assertTrue(json(new GetBerliozConfig(), request()).contains("same"));
    second.destroy();
    assertEquals("{\"servlets\":[]}", json(new GetBerliozConfig(), request()));
  }

  @Test void berliozSnapshotSurvivesDestructionAndSettingsReload() throws Exception {
    BerliozServlet servlet = new BerliozServlet();
    servlet.init(config("html", Map.of("http-compression", "false"), List.of("*.html"), false));
    GetBerliozConfig generator = new GetBerliozConfig();
    Request request = request();
    String body = json(generator, request);
    String etag = generator.getETag(request);
    options("berlioz.http.compression=true\nberlioz.http.cache-control=max-age=3600\n");
    assertEquals(body, json(generator, request()));
    servlet.destroy();
    assertEquals(body, json(generator, request));
    assertEquals(etag, generator.getETag(request));
    assertNotEquals(etag, generator.getETag(request()));
  }

  @Test void failedInitializationDoesNotPublishConfiguration() {
    ServletConfig ordinary = config("failed", Map.of(), List.of(), false);
    ServletContext failingContext = (ServletContext) Proxy.newProxyInstance(getClass().getClassLoader(),
        new Class<?>[]{ServletContext.class}, (proxy, method, args) -> {
          if (method.getName().equals("getNamedDispatcher")) throw new IllegalStateException("failure after configuration creation");
          return method.invoke(ordinary.getServletContext(), args);
        });
    ServletConfig failingConfig = (ServletConfig) Proxy.newProxyInstance(getClass().getClassLoader(),
        new Class<?>[]{ServletConfig.class}, (proxy, method, args) ->
            method.getName().equals("getServletContext") ? failingContext : method.invoke(ordinary, args));
    BerliozServlet servlet = new BerliozServlet();
    assertThrows(IllegalStateException.class, () -> servlet.init(failingConfig));
    assertEquals("{\"servlets\":[]}", json(new GetBerliozConfig(), request()));
  }

  @Test void identicalErrorRegistrationSurvivesOldInstanceDestruction() throws Exception {
    ErrorHandlerServlet first = new ErrorHandlerServlet();
    ErrorHandlerServlet second = new ErrorHandlerServlet();
    ServletConfig config = config("errors", Map.of(), List.of(), false);
    first.init(config);
    second.init(config);
    first.destroy();
    assertTrue(json(new GetErrorHandlerConfig(), request()).contains("errors"));
    second.destroy();
    assertEquals("{\"servlets\":[]}", json(new GetErrorHandlerConfig(), request()));
  }

  private void options(String properties) throws Exception {
    Files.writeString(root.resolve("WEB-INF/config/config.properties"), properties);
    GlobalSettings.setup(root.resolve("WEB-INF").toFile());
  }

  private Request request() { return GeneratorTestSupport.request().build(); }
  private String json(Generator generator, Request request) {
    OutputWriter out = new JsonOutputAdapter(); generator.generate(request, out); return out.toString();
  }
  private String xml(Generator generator, Request request) {
    OutputWriter out = new XmlOutputAdapter(); generator.generate(request, out); return out.toString();
  }

  private ServletConfig config(String name, Map<String, String> params, List<String> mappings, boolean discover) {
    ServletRegistration registration = (ServletRegistration) Proxy.newProxyInstance(getClass().getClassLoader(),
        new Class<?>[]{ServletRegistration.class}, (proxy, method, args) -> {
          switch (method.getName()) {
            case "getMappings": return mappings;
            case "getClassName": return BerliozServlet.class.getName();
            default: return null;
          }
        });
    ServletContext context = (ServletContext) Proxy.newProxyInstance(getClass().getClassLoader(),
        new Class<?>[]{ServletContext.class}, (proxy, method, args) -> {
          switch (method.getName()) {
            case "getRealPath": return root.toString();
            case "getServletRegistration": return registration;
            case "getServletRegistrations": return discover ? Map.of("html", registration) : Map.of();
            default: return null;
          }
        });
    return (ServletConfig) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ServletConfig.class},
        (proxy, method, args) -> {
          switch (method.getName()) {
            case "getServletName": return name;
            case "getServletContext": return context;
            case "getInitParameter": return params.get(args[0]);
            default: return null;
          }
        });
  }
}
