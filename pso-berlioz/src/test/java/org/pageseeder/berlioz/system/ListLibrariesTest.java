package org.pageseeder.berlioz.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javax.servlet.ServletContext;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pageseeder.xmlwriter.XML.NamespaceAware;
import org.pageseeder.xmlwriter.XMLStringWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

public final class ListLibrariesTest {

  @BeforeEach
  public void clearCacheBefore() {
    ListLibraries.clearCache();
  }

  @AfterEach
  public void clearCacheAfter() {
    ListLibraries.clearCache();
  }

  @Test
  public void testExtractLibsListsJarsInOrder() throws Exception {
    ServletContextFixture fixture = new ServletContextFixture();
    fixture.add("/WEB-INF/lib/beta-2.0.jar",
        jar("Implementation-Title", "Beta Library", "Implementation-Version", "2.0"));
    fixture.add("/WEB-INF/lib/readme.txt", new byte[] {1, 2, 3});
    fixture.addPath("/WEB-INF/lib/nested/");
    fixture.add("/WEB-INF/lib/alpha.jar",
        jar("Implementation-Title", "Alpha Library", "Built-By", "Berlioz"));

    Element root = extract(fixture);

    Element alpha = child(root, "library", 0);
    assertEquals("alpha.jar", alpha.getAttribute("file"));
    assertEquals("alpha", alpha.getAttribute("name"));
    assertFalse(alpha.hasAttribute("version"));
    assertEquals("1.0", child(alpha, "manifest", 0).getAttribute("version"));
    assertEquals("Alpha Library", child(alpha, "implementation", 0).getAttribute("title"));
    assertEquals("Berlioz", child(alpha, "built", 0).getAttribute("by"));

    Element beta = child(root, "library", 1);
    assertEquals("beta-2.0.jar", beta.getAttribute("file"));
    assertEquals("beta", beta.getAttribute("name"));
    assertEquals("2.0", beta.getAttribute("version"));
    assertEquals("Beta Library", child(beta, "implementation", 0).getAttribute("title"));
    assertEquals("2.0", child(beta, "implementation", 0).getAttribute("version"));
  }

  @Test
  public void testExtractLibsUsesCacheUntilCleared() throws Exception {
    ServletContextFixture fixture = new ServletContextFixture();
    fixture.add("/WEB-INF/lib/library-1.0.jar", jar("Implementation-Version", "1.0"));

    Element first = child(extract(fixture), "library", 0);
    assertEquals("1.0", child(first, "implementation", 0).getAttribute("version"));
    assertEquals(1, fixture.opens("/WEB-INF/lib/library-1.0.jar"));

    fixture.add("/WEB-INF/lib/library-1.0.jar", jar("Implementation-Version", "2.0"));
    Element cached = child(extract(fixture), "library", 0);
    assertEquals("1.0", child(cached, "implementation", 0).getAttribute("version"));
    assertEquals(1, fixture.opens("/WEB-INF/lib/library-1.0.jar"));

    ListLibraries.clearCache();
    Element reloaded = child(extract(fixture), "library", 0);
    assertEquals("2.0", child(reloaded, "implementation", 0).getAttribute("version"));
    assertEquals(2, fixture.opens("/WEB-INF/lib/library-1.0.jar"));
  }

  @Test
  public void testExtractLibsIgnoresUnreadableManifest() throws Exception {
    ServletContextFixture fixture = new ServletContextFixture();
    fixture.add("/WEB-INF/lib/broken.jar", new byte[] {1, 2, 3});

    Element library = child(extract(fixture), "library", 0);

    assertEquals("broken.jar", library.getAttribute("file"));
    assertEquals("broken", library.getAttribute("name"));
    assertEquals(0, library.getChildNodes().getLength());
  }

  private static Element extract(ServletContextFixture fixture) throws Exception {
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    new ListLibraries().extractLibs(fixture.context(), xml);
    Document doc = parse(xml.toString());
    assertEquals("libraries", doc.getDocumentElement().getTagName());
    return doc.getDocumentElement();
  }

  private static byte[] jar(String... attributes) throws Exception {
    Manifest manifest = new Manifest();
    Attributes main = manifest.getMainAttributes();
    main.put(Attributes.Name.MANIFEST_VERSION, "1.0");
    for (int i = 0; i < attributes.length; i += 2) {
      main.put(new Attributes.Name(attributes[i]), attributes[i+1]);
    }
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (JarOutputStream jar = new JarOutputStream(out, manifest)) {
      // The manifest is enough for these tests.
    }
    return out.toByteArray();
  }

  private static Document parse(String xml) throws Exception {
    return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
  }

  private static Element child(Element parent, String name, int index) {
    int found = 0;
    for (int i = 0; i < parent.getChildNodes().getLength(); i++) {
      if (parent.getChildNodes().item(i) instanceof Element) {
        Element child = (Element) parent.getChildNodes().item(i);
        if (name.equals(child.getTagName())) {
          if (found == index) return child;
          found++;
        }
      }
    }
    fail("Missing child element "+name+" at index "+index);
    throw new AssertionError();
  }

  private static Object defaultValue(Class<?> type) {
    if (!type.isPrimitive()) return null;
    if (boolean.class.equals(type)) return false;
    if (byte.class.equals(type)) return (byte) 0;
    if (short.class.equals(type)) return (short) 0;
    if (int.class.equals(type)) return 0;
    if (long.class.equals(type)) return 0L;
    if (float.class.equals(type)) return 0f;
    if (double.class.equals(type)) return 0d;
    if (char.class.equals(type)) return (char) 0;
    throw new IllegalStateException("Unsupported primitive type: " + type.getName());
  }

  private static final class ServletContextFixture {

    private final Set<String> paths = new LinkedHashSet<>();
    private final Map<String, byte[]> resources = new LinkedHashMap<>();
    private final Map<String, Integer> opens = new HashMap<>();
    private final ServletContext context;

    private ServletContextFixture() {
      this.context = (ServletContext) Proxy.newProxyInstance(
          ServletContext.class.getClassLoader(),
          new Class<?>[] { ServletContext.class },
          (proxy, method, args) -> {
            switch (method.getName()) {
              case "getResourcePaths":
                if ("/WEB-INF/lib/".equals(args[0]) || "/WEB-INF/lib".equals(args[0])) {
                  return new LinkedHashSet<>(this.paths);
                }
                return null;
              case "getResourceAsStream":
                byte[] resource = this.resources.get(args[0]);
                if (resource == null) return null;
                this.opens.merge((String) args[0], 1, Integer::sum);
                return new ByteArrayInputStream(resource);
              case "toString":
                return "ServletContextFixture";
              case "hashCode":
                return System.identityHashCode(proxy);
              case "equals":
                return proxy == args[0];
              default:
                return defaultValue(method.getReturnType());
            }
          });
    }

    private ServletContext context() {
      return this.context;
    }

    private void add(String path, byte[] resource) {
      this.paths.add(path);
      this.resources.put(path, resource);
    }

    private void addPath(String path) {
      this.paths.add(path);
    }

    private int opens(String path) {
      return this.opens.getOrDefault(path, 0);
    }
  }
}
