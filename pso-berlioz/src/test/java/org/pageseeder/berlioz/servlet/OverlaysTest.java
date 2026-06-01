package org.pageseeder.berlioz.servlet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.util.List;

class OverlaysTest {

  // Root with WEB-INF/overlays containing: illegal-1.0.zip, readme-1.0.zip, readme-2.0.war, sample-1.0.war
  private final File root = new File("./src/test/resources/org/pageseeder/berlioz/servlet");

  @TempDir
  Path tmp;

  // ---------------------------------------------------------------------------
  // list()
  // ---------------------------------------------------------------------------

  @Test
  void testListCount() {
    List<Overlays.Overlay> overlays = Overlays.list(root);
    Assertions.assertEquals(4, overlays.size());
  }

  @Test
  void testListSortedByNameThenVersion() {
    List<Overlays.Overlay> overlays = Overlays.list(root);
    Assertions.assertEquals("illegal[1.0]", overlays.get(0).toString());
    Assertions.assertEquals("readme[1.0]", overlays.get(1).toString());
    Assertions.assertEquals("readme[2.0]", overlays.get(2).toString());
    Assertions.assertEquals("sample[1.0]", overlays.get(3).toString());
  }

  @Test
  void testListNamesAndVersions() {
    List<Overlays.Overlay> overlays = Overlays.list(root);
    Assertions.assertEquals("illegal", overlays.get(0).name());
    Assertions.assertEquals("1.0", overlays.get(0).version());
    Assertions.assertEquals("readme", overlays.get(1).name());
    Assertions.assertEquals("1.0", overlays.get(1).version());
    Assertions.assertEquals("readme", overlays.get(2).name());
    Assertions.assertEquals("2.0", overlays.get(2).version());
    Assertions.assertEquals("sample", overlays.get(3).name());
    Assertions.assertEquals("1.0", overlays.get(3).version());
  }

  @Test
  void testListEmptyWhenNoOverlaysDirectory() throws IOException {
    File emptyRoot = Files.createTempDirectory(tmp, "d").toFile();
    new File(emptyRoot, "WEB-INF").mkdirs();
    Assertions.assertTrue(Overlays.list(emptyRoot).isEmpty());
  }

  @Test
  void testListEmptyWhenOverlaysFolderIsEmpty() throws IOException {
    File emptyRoot = Files.createTempDirectory(tmp, "d").toFile();
    new File(emptyRoot, "WEB-INF/overlays").mkdirs();
    Assertions.assertTrue(Overlays.list(emptyRoot).isEmpty());
  }

  // ---------------------------------------------------------------------------
  // unpack() — readme-1.0.zip (single file: README.txt)
  // ---------------------------------------------------------------------------

  @SuppressWarnings("java:S5976")
  @Test
  void testUnpackReadmeCount() throws IOException {
    File target = Files.createTempDirectory(tmp, "d").toFile();
    // README.txt only; __MACOSX entries in the 2.0.war must be skipped
    int count = find(root, "readme", "1.0").unpack(target);
    Assertions.assertEquals(1, count);
  }

  @Test
  void testUnpackReadmeExtractsFile() throws IOException {
    File target = Files.createTempDirectory(tmp, "d").toFile();
    find(root, "readme", "1.0").unpack(target);
    Assertions.assertTrue(new File(target, "README.txt").exists());
  }

  // ---------------------------------------------------------------------------
  // unpack() — sample-1.0.war (files + __MACOSX + .DS_Store to skip)
  // ---------------------------------------------------------------------------

  @Test
  void testUnpackSampleCount() throws IOException {
    File target = Files.createTempDirectory(tmp, "d").toFile();
    // Extracted: test/sample.html, WEB-INF/psml/test.psml,
    //            WEB-INF/config/services!test.xml, README.txt
    // Skipped:   __MACOSX/*, *.DS_Store, directories
    int count = find(root, "sample", "1.0").unpack(target);
    Assertions.assertEquals(4, count);
  }

  @Test
  void testUnpackSampleSkipsMacOsEntries() throws IOException {
    File target = Files.createTempDirectory(tmp, "d").toFile();
    find(root, "sample", "1.0").unpack(target);
    Assertions.assertFalse(new File(target, "__MACOSX").exists());
  }

  @Test
  void testUnpackSampleSkipsDsStoreFiles() throws IOException {
    File target = Files.createTempDirectory(tmp, "d").toFile();
    find(root, "sample", "1.0").unpack(target);
    Assertions.assertFalse(new File(target, "WEB-INF/.DS_Store").exists());
  }

  @Test
  void testUnpackSampleExtractsLegalWebInfFile() throws IOException {
    File target = Files.createTempDirectory(tmp, "d").toFile();
    find(root, "sample", "1.0").unpack(target);
    Assertions.assertTrue(new File(target, "WEB-INF/config/services!test.xml").exists());
  }

  // ---------------------------------------------------------------------------
  // unpack() — illegal-1.0.zip (contains web.xml and services.xml that must be blocked)
  // ---------------------------------------------------------------------------

  @Test
  void testUnpackIllegalSkipsWebXml() throws IOException {
    File target = Files.createTempDirectory(tmp, "d").toFile();
    find(root, "illegal", "1.0").unpack(target);
    Assertions.assertFalse(new File(target, "WEB-INF/web.xml").exists(), "WEB-INF/web.xml must not be extracted from an overlay");
  }

  @Test
  void testUnpackIllegalSkipsServicesXml() throws IOException {
    File target = Files.createTempDirectory(tmp, "d").toFile();
    find(root, "illegal", "1.0").unpack(target);
    Assertions.assertFalse(new File(target, "WEB-INF/config/services.xml").exists(), "WEB-INF/config/services.xml must not be extracted from an overlay");
  }

  @Test
  void testUnpackIllegalCount() throws IOException {
    File target = Files.createTempDirectory(tmp, "d").toFile();
    // Legal files only: test/sample.html, WEB-INF/psml/test.psml, README.txt
    // Blocked: WEB-INF/web.xml, WEB-INF/config/services.xml
    // Skipped: __MACOSX/*, *.DS_Store, directories
    int count = find(root, "illegal", "1.0").unpack(target);
    Assertions.assertEquals(3, count);
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private static Overlays.Overlay find(File root, String name, String version) {
    return Overlays.list(root).stream()
        .filter(o -> name.equals(o.name()) && version.equals(o.version()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("overlay not found: " + name + "-" + version));
  }
}
