package org.pageseeder.berlioz.bundler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class WebBundleTest {

  @TempDir
  Path temp;

  private File createFile(String name, String content) throws IOException {
    Path p = temp.resolve(name);
    Files.writeString(p, content, StandardCharsets.UTF_8);
    return p.toFile();
  }

  // --- Getters ---

  @Test
  void testName() throws IOException {
    File f = createFile("main.js", "alert(1)");
    WebBundle bundle = new WebBundle("main", List.of(f), false);
    assertEquals("main", bundle.name());
  }

  @Test
  void testFiles_isImmutableCopy() throws IOException {
    File f = createFile("app.js", "var x=1;");
    WebBundle bundle = new WebBundle("app", List.of(f), false);
    List<File> files = bundle.files();
    assertEquals(1, files.size());
    assertSame(f, files.get(0));
    assertThrows(UnsupportedOperationException.class, () -> files.add(f));
  }

  @Test
  void testId_isDeterministic() throws IOException {
    File f = createFile("style.css", "body{}");
    WebBundle a = new WebBundle("s", List.of(f), false);
    WebBundle b = new WebBundle("s", List.of(f), false);
    assertEquals(a.id(), b.id());
  }

  @Test
  void testId_differsForDifferentFiles() throws IOException {
    File f1 = createFile("one.js", "1");
    File f2 = createFile("two.js", "2");
    String id1 = WebBundle.id(List.of(f1));
    String id2 = WebBundle.id(List.of(f2));
    assertNotEquals(id1, id2);
  }

  // --- isMinimized ---

  @Test
  void testIsMinimized_false() throws IOException {
    File f = createFile("app.js", "var x=1;");
    WebBundle bundle = new WebBundle("app", List.of(f), false);
    assertFalse(bundle.isMinimized());
  }

  @Test
  void testIsMinimized_true() throws IOException {
    File f = createFile("app.js", "var x=1;");
    WebBundle bundle = new WebBundle("app", List.of(f), true);
    assertTrue(bundle.isMinimized());
  }

  // --- isCSSMinimizable ---

  @Test
  void testIsCSSMinimizable_trueForRegularCSS() throws IOException {
    File f = createFile("style.css", "body{}");
    WebBundle bundle = new WebBundle("s", List.of(f), false);
    assertTrue(bundle.isCSSMinimizable());
  }

  @Test
  void testIsCSSMinimizable_falseForMinCSS() throws IOException {
    File f = createFile("style.min.css", "body{}");
    WebBundle bundle = new WebBundle("s", List.of(f), false);
    assertFalse(bundle.isCSSMinimizable());
  }

  @Test
  void testIsCSSMinimizable_falseWhenImportedFileIsMinCSS() throws IOException {
    File f = createFile("main.css", "body{}");
    File imported = createFile("vendor.min.css", ".x{}");
    WebBundle bundle = new WebBundle("s", List.of(f), false);
    bundle.addImport(imported);
    assertFalse(bundle.isCSSMinimizable());
  }

  @Test
  void testIsCSSMinimizable_trueForJS() throws IOException {
    File f = createFile("app.js", "var x=1;");
    WebBundle bundle = new WebBundle("b", List.of(f), false);
    assertTrue(bundle.isCSSMinimizable());
  }

  // --- getETag ---

  @Test
  void testGetETag_notNull() throws IOException {
    File f = createFile("app.js", "content");
    WebBundle bundle = new WebBundle("b", List.of(f), false);
    assertNotNull(bundle.getETag(false));
  }

  @Test
  void testGetETag_consistentWithoutRefresh() throws IOException {
    File f = createFile("app.js", "content");
    WebBundle bundle = new WebBundle("b", List.of(f), false);
    String first = bundle.getETag(false);
    String second = bundle.getETag(false);
    assertEquals(first, second);
  }

  @Test
  void testGetETag_refreshReturnsNewETag() throws IOException {
    File f = createFile("app.js", "content");
    WebBundle bundle = new WebBundle("b", List.of(f), false);
    String before = bundle.getETag(false);
    String refreshed = bundle.getETag(true);
    assertEquals(before, refreshed); // same content → same etag after refresh
  }

  // --- isFresh ---

  @Test
  void testIsFresh_trueAfterGetETag() throws IOException {
    File f = createFile("app.js", "content");
    WebBundle bundle = new WebBundle("b", List.of(f), false);
    bundle.getETag(false); // cache the etag
    assertTrue(bundle.isFresh());
  }

  // --- import management ---

  @Test
  void testAddImport_and_clearImport() throws IOException {
    File main = createFile("main.css", "body{}");
    File imported = createFile("extra.css", ".x{}");
    WebBundle bundle = new WebBundle("s", List.of(main), false);
    String etagBefore = bundle.getETag(true);
    bundle.addImport(imported);
    String etagAfter = bundle.getETag(true);
    assertNotEquals(etagBefore, etagAfter);
    bundle.clearImport();
    String etagCleared = bundle.getETag(true);
    assertEquals(etagBefore, etagCleared);
  }

  // --- getFileName ---

  @Test
  void testGetFileName_containsNameAndExt() throws IOException {
    File f = createFile("bundle.js", "var x=1;");
    WebBundle bundle = new WebBundle("bundle", List.of(f), false);
    bundle.getETag(false);
    String name = bundle.getFileName();
    assertTrue(name.startsWith("bundle-"));
    assertTrue(name.endsWith(".js"));
    assertFalse(name.contains(".min."));
  }

  @Test
  void testGetFileName_containsMinForMinimizedBundle() throws IOException {
    File f = createFile("bundle.css", "body{}");
    WebBundle bundle = new WebBundle("bundle", List.of(f), true);
    bundle.getETag(false);
    String name = bundle.getFileName();
    assertTrue(name.contains(".min.css"));
  }

  // --- static id ---

  @Test
  void testStaticId_nonNull() throws IOException {
    File f = createFile("x.js", "x");
    assertNotNull(WebBundle.id(List.of(f)));
  }

  @Test
  void testStaticId_samePathSameId() throws IOException {
    File f = createFile("y.js", "y");
    assertEquals(WebBundle.id(List.of(f)), WebBundle.id(List.of(f)));
  }
}
