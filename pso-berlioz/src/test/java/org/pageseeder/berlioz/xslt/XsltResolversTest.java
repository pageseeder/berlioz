package org.pageseeder.berlioz.xslt;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class XsltResolversTest {

  @TempDir Path directory;

  // secure()
  // ---------------------------------------------------------------------------

  @Test
  void testSecureResolvesClasspathReference() throws Exception {
    URL style = directory.resolve("style.xsl").toUri().toURL();
    URIResolver resolver = XsltResolvers.secure(loader(style));
    Source source = resolver.resolve("classpath:style.xsl", null);
    assertNotNull(source);
    assertEquals(style.toString(), source.getSystemId());
  }

  @Test
  void testSecureLeavesApprovedRelativeReferenceToProcessor() throws Exception {
    URIResolver resolver = XsltResolvers.secure(getClass().getClassLoader());
    String base = directory.resolve("main.xsl").toUri().toString();
    assertNull(resolver.resolve("part.xsl", base));
  }

  @Test
  void testSecureDeniesNetwork() {
    URIResolver resolver = XsltResolvers.secure();
    assertThrows(TransformerException.class, () -> resolver.resolve("http://example.invalid/style.xsl", null));
  }

  @Test
  void testSecureRequiresLoader() {
    assertThrows(NullPointerException.class, () -> XsltResolvers.secure(null));
  }

  // locate()
  // ---------------------------------------------------------------------------

  @Test
  void testLocateRelativeAgainstFileBase() throws Exception {
    String base = directory.resolve("html/main.xsl").toUri().toString();
    URL url = XsltResolvers.locate("../common/part.xsl", base, getClass().getClassLoader());
    assertEquals(directory.resolve("common/part.xsl").toUri().toURL().toString(), url.toString());
  }

  @Test
  void testLocateRelativeAgainstJarBase() throws Exception {
    String jarBase = "jar:" + directory.resolve("styles.jar").toUri() + "!/xslt/main.xsl";
    URL url = XsltResolvers.locate("part.xsl", jarBase, getClass().getClassLoader());
    assertEquals("jar:" + directory.resolve("styles.jar").toUri() + "!/xslt/part.xsl", url.toString());
  }

  @Test
  void testLocateClasspathReference() throws Exception {
    URL style = new URL("jar:" + directory.resolve("styles.jar").toUri() + "!/META-INF/berlioz/xslt/style.xsl");
    assertEquals(style, XsltResolvers.locate("classpath:META-INF/berlioz/xslt/style.xsl", null, loader(style)));
    assertEquals(style, XsltResolvers.locate("resource:META-INF/berlioz/xslt/style.xsl", null, loader(style)));
  }

  @Test
  void testLocateClasspathReferenceIgnoresLeadingSlash() throws Exception {
    AtomicInteger lookups = new AtomicInteger();
    URL style = directory.resolve("style.xsl").toUri().toURL();
    ClassLoader loader = new ClassLoader(getClass().getClassLoader()) {
      @Override public URL getResource(String name) {
        lookups.incrementAndGet();
        return "xslt/style.xsl".equals(name) ? style : null;
      }
    };
    assertEquals(style, XsltResolvers.locate("classpath:/xslt/style.xsl", null, loader));
    assertEquals(style, XsltResolvers.locate("classpath:xslt/style.xsl", null, loader));
    assertEquals(2, lookups.get());
  }

  @Test
  void testLocateMissingClasspathReference() {
    ClassLoader loader = loader(null);
    TransformerException ex = assertThrows(TransformerException.class,
        () -> XsltResolvers.locate("classpath:missing.xsl", null, loader));
    assertTrue(ex.getMessage().contains("Unable to resolve classpath"));
  }

  @Test
  void testLocateRelativeWithoutBase() {
    assertThrows(TransformerException.class,
        () -> XsltResolvers.locate("part.xsl", null, getClass().getClassLoader()));
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "http://example.invalid/style.xsl",
      "https://example.invalid/style.xsl",
      "jar:http://example.invalid/styles.jar!/style.xsl",
      "file://example.invalid/style.xsl",
      "unknown:part.xsl"
  })
  void testLocateDeniesNonLocal(String href) {
    String base = directory.resolve("main.xsl").toUri().toString();
    assertThrows(TransformerException.class,
        () -> XsltResolvers.locate(href, base, getClass().getClassLoader()));
  }

  @Test
  void testLocateDeniesRelativeToRemoteBase() {
    assertThrows(TransformerException.class,
        () -> XsltResolvers.locate("part.xsl", "http://example.invalid/main.xsl", getClass().getClassLoader()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"classpath:../secret.xsl", "classpath:xslt/../../secret.xsl", "classpath:/../secret.xsl"})
  void testLocateRejectsClasspathTraversalWithoutLookup(String href) {
    AtomicInteger lookups = new AtomicInteger();
    ClassLoader loader = new ClassLoader(getClass().getClassLoader()) {
      @Override public URL getResource(String name) {
        lookups.incrementAndGet();
        return super.getResource(name);
      }
    };
    assertThrows(TransformerException.class, () -> XsltResolvers.locate(href, null, loader));
    assertEquals(0, lookups.get());
  }

  @Test
  void testLocateDeniesRemoteClasspathResourceWithoutConnecting() throws Exception {
    AtomicInteger connections = new AtomicInteger();
    URL remote = new URL(null, "jar:http://example.invalid/styles.jar!/style.xsl", new URLStreamHandler() {
      @Override
      protected URLConnection openConnection(URL ignored) throws IOException {
        connections.incrementAndGet();
        throw new IOException("Unexpected resource access");
      }
    });
    assertThrows(TransformerException.class, () -> XsltResolvers.locate("classpath:style.xsl", null, loader(remote)));
    assertEquals(0, connections.get());
  }

  @Test
  void testLocateRequiresArguments() {
    ClassLoader loader = getClass().getClassLoader();
    assertThrows(NullPointerException.class, () -> XsltResolvers.locate(null, null, loader));
    assertThrows(NullPointerException.class, () -> XsltResolvers.locate("part.xsl", null, null));
  }

  // helpers
  // ---------------------------------------------------------------------------

  private ClassLoader loader(URL url) {
    return new ClassLoader(getClass().getClassLoader()) {
      @Override public URL getResource(String name) { return url; }
    };
  }
}
