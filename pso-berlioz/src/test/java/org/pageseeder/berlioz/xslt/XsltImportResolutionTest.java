/*
 * Copyright 2026 Allette Systems (Australia)
 * http://www.allette.com.au
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pageseeder.berlioz.xslt;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * End-to-end tests proving that {@code xsl:import}/{@code xsl:include} resolution works for
 * classpath-backed stylesheets: relative references stay within the resolving classpath resource,
 * explicit {@code classpath:} references allow deliberate cross-resource sharing, and external
 * network protocols are denied.
 *
 * <p>Exercises the resolver through the full {@link XsltTemplateCache} compile pipeline (real
 * {@code TransformerFactory}, real {@link SecureXsltUriResolver}) rather than calling the resolver
 * directly, since what matters is that a real compile succeeds/fails as expected.
 */
final class XsltImportResolutionTest {

  @TempDir
  Path tempDir;

  private URLClassLoader classLoader;
  private ClassLoader previousContextClassLoader;

  @BeforeEach
  void setUp() throws IOException {
    XsltTemplateCache.clearAllCache();
    Files.createDirectories(this.tempDir.resolve("sub"));
    this.classLoader = new URLClassLoader(new URL[] { this.tempDir.toUri().toURL() },
        XsltImportResolutionTest.class.getClassLoader());
    this.previousContextClassLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(this.classLoader);
  }

  @AfterEach
  void tearDown() throws IOException {
    Thread.currentThread().setContextClassLoader(this.previousContextClassLoader);
    this.classLoader.close();
    XsltTemplateCache.clearAllCache();
  }

  @Test
  void relativeInclude_resolvesWithinSameClasspathResource() throws IOException, TransformerException {
    write("included.xsl",
        stylesheet("<xsl:template name=\"included\"><out>included-ok</out></xsl:template>"));
    write("main-include.xsl",
        stylesheet("<xsl:include href=\"included.xsl\"/>"
            + "<xsl:template match=\"/\"><xsl:call-template name=\"included\"/></xsl:template>"));

    String result = transform("main-include.xsl");

    Assertions.assertEquals("<out>included-ok</out>", result);
  }

  @Test
  void relativeImport_resolvesWithinSameClasspathResource() throws IOException, TransformerException {
    write("imported.xsl",
        stylesheet("<xsl:template name=\"imported\"><out>imported-ok</out></xsl:template>"));
    write("main-import.xsl",
        stylesheet("<xsl:import href=\"imported.xsl\"/>"
            + "<xsl:template match=\"/\"><xsl:call-template name=\"imported\"/></xsl:template>"));

    String result = transform("main-import.xsl");

    Assertions.assertEquals("<out>imported-ok</out>", result);
  }

  @Test
  void relativeInclude_resolvesWithinSameJar() throws IOException, TransformerException {
    Path jarFile = this.tempDir.resolve("artifact.jar");
    try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(jarFile))) {
      addEntry(jar, "included.xsl",
          stylesheet("<xsl:template name=\"included\"><out>jar-included-ok</out></xsl:template>"));
      addEntry(jar, "main.xsl",
          stylesheet("<xsl:include href=\"included.xsl\"/>"
              + "<xsl:template match=\"/\"><xsl:call-template name=\"included\"/></xsl:template>"));
    }
    try (URLClassLoader jarLoader = new URLClassLoader(new URL[] { jarFile.toUri().toURL() },
        XsltImportResolutionTest.class.getClassLoader())) {
      Thread.currentThread().setContextClassLoader(jarLoader);
      URL url = jarLoader.getResource("main.xsl");
      Assertions.assertEquals("jar", url.getProtocol());
      Templates templates = new XsltTemplateCache(StylesheetLocation.forClasspath(url, "classpath:main.xsl"), null)
          .getTemplates();
      Transformer transformer = templates.newTransformer();
      StringWriter out = new StringWriter();
      transformer.transform(new StreamSource(new StringReader("<root/>")), new StreamResult(out));
      Assertions.assertEquals("<out>jar-included-ok</out>", out.toString());
    }
  }

  @Test
  void explicitClasspathReference_allowsCrossResourceSharing() throws IOException, TransformerException {
    write("sub/other.xsl",
        stylesheet("<xsl:template name=\"other\"><out>other-ok</out></xsl:template>"));
    write("main-classpath-ref.xsl",
        stylesheet("<xsl:include href=\"classpath:sub/other.xsl\"/>"
            + "<xsl:template match=\"/\"><xsl:call-template name=\"other\"/></xsl:template>"));

    String result = transform("main-classpath-ref.xsl");

    Assertions.assertEquals("<out>other-ok</out>", result);
  }

  @Test
  void explicitClasspathReference_missingResource_fails() throws IOException {
    write("main-missing-classpath-ref.xsl",
        stylesheet("<xsl:include href=\"classpath:sub/does-not-exist.xsl\"/>"
            + "<xsl:template match=\"/\"><out/></xsl:template>"));

    Assertions.assertThrows(TransformerException.class, () -> compile("main-missing-classpath-ref.xsl"));
  }

  @Test
  void networkInclude_isDenied() throws IOException {
    write("main-network-ref.xsl",
        stylesheet("<xsl:include href=\"http://example.invalid/evil.xsl\"/>"
            + "<xsl:template match=\"/\"><out/></xsl:template>"));

    Assertions.assertThrows(TransformerException.class, () -> compile("main-network-ref.xsl"));
  }

  @Test
  void networkImport_isDenied() throws IOException {
    write("main-network-import.xsl",
        stylesheet("<xsl:import href=\"https://example.invalid/evil.xsl\"/>"
            + "<xsl:template match=\"/\"><out/></xsl:template>"));

    Assertions.assertThrows(TransformerException.class, () -> compile("main-network-import.xsl"));
  }

  @Test
  void explicitClasspathReference_classLoaderReturnsDisallowedProtocol_isDenied() throws IOException {
    write("main-classpath-forged.xsl",
        stylesheet("<xsl:include href=\"classpath:evil.xsl\"/>"
            + "<xsl:template match=\"/\"><out/></xsl:template>"));

    // A classloader is trusted to resolve resource *names*, not to guarantee the protocol of the
    // URL it hands back. Simulate one (e.g. an OSGi bundle loader or vfs-backed loader) that
    // resolves "evil.xsl" to a network URL, and confirm the resolver still denies it — the
    // file/jar restriction must not rely on the classloader's own good behavior.
    ClassLoader forging = new ClassLoader(this.classLoader) {
      @Override
      public URL getResource(String name) {
        if ("evil.xsl".equals(name)) {
          try {
            return new URL("http://example.invalid/evil.xsl");
          } catch (java.net.MalformedURLException ex) {
            throw new AssertionError(ex);
          }
        }
        return super.getResource(name);
      }
    };
    Thread.currentThread().setContextClassLoader(forging);

    Assertions.assertThrows(TransformerException.class, () -> compile("main-classpath-forged.xsl"));
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private static String stylesheet(String body) {
    return "<?xml version=\"1.0\"?>"
        + "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">"
        + "<xsl:output method=\"xml\" omit-xml-declaration=\"yes\"/>"
        + body
        + "</xsl:stylesheet>";
  }

  private void write(String name, String content) throws IOException {
    Files.write(this.tempDir.resolve(name), content.getBytes(StandardCharsets.UTF_8));
  }

  private static void addEntry(JarOutputStream jar, String name, String content) throws IOException {
    jar.putNextEntry(new JarEntry(name));
    jar.write(content.getBytes(StandardCharsets.UTF_8));
    jar.closeEntry();
  }

  private Templates compile(String name) throws TransformerException {
    URL url = this.classLoader.getResource(name);
    StylesheetLocation location = StylesheetLocation.forClasspath(url, "classpath:" + name);
    return new XsltTemplateCache(location, null).getTemplates();
  }

  private String transform(String name) throws TransformerException {
    Templates templates = compile(name);
    Transformer transformer = templates.newTransformer();
    StringWriter out = new StringWriter();
    transformer.transform(new StreamSource(new StringReader("<root/>")), new StreamResult(out));
    return out.toString();
  }

}
