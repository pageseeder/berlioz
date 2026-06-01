package org.pageseeder.berlioz.servlet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.util.List;

public class OverlaysTest {

  // Root with WEB-INF/overlays containing: illegal-1.0.zip, readme-1.0.zip, readme-2.0.war, sample-1.0.war
  private final File root = new File("./src/test/resources/org/pageseeder/berlioz/servlet");

  @TempDir
  Path tmp;

  // ---------------------------------------------------------------------------
  // list()
  // ---------------------------------------------------------------------------

  @Test
  public void testListCount() {
    List<Overlays.Overlay> overlays = Overlays.list(root);
    Assertions.assertEquals(4, overlays.size());
  }

  @Test
  public void testListSortedByNameThenVersion() {
    List<Overlays.Overlay> overlays = Overlays.list(root);
    Assertions.assertEquals(overlays.get(0).toString(), "illegal[1.0]");
    Assertions.assertEquals(overlays.get(1).toString(), "readme[1.0]");
    Assertions.assertEquals(overlays.get(2).toString(), "readme[2.0]");
    Assertions.assertEquals(overlays.get(3).toString(), "sample[1.0]");
  }

  @Test
  public void testListNamesAndVersions() {
    List<Overlays.Overlay> overlays = Overlays.list(root);
    Assertions.assertEquals(overlays.get(0).name(), "illegal");
    Assertions.assertEquals(overlays.get(0).version(), "1.0");
    Assertions.assertEquals(overlays.get(1).name(), "readme");
    Assertions.assertEquals(overlays.get(1).version(), "1.0");
    Assertions.assertEquals(overlays.get(2).name(), "readme");
    Assertions.assertEquals(overlays.get(2).version(), "2.0");
    Assertions.assertEquals(overlays.get(3).name(), "sample");
    Assertions.assertEquals(overlays.get(3).version(), "1.0");
  }

  @Test
  public void testListEmptyWhenNoOverlaysDirectory() throws IOException {
    File emptyRoot = Files.createTempDirectory(tmp, "d").toFile();
    new File(emptyRoot, "WEB-INF").mkdirs();
    Assertions.assertTrue(Overlays.list(emptyRoot).isEmpty());
  }

  @Test
  public void testListEmptyWhenOverlaysFolderIsEmpty() throws IOException {
    File emptyRoot = Files.createTempDirectory(tmp, "d").toFile();
    new File(emptyRoot, "WEB-INF/overlays").mkdirs();
    Assertions.assertTrue(Overlays.list(emptyRoot).isEmpty());
  }

  // ---------------------------------------------------------------------------
  // unpack() — readme-1.0.zip (single file: README.txt)
  // ---------------------------------------------------------------------------

  @SuppressWarnings("java:S5976")
  @Test
  public void testUnpackReadmeCount() throws IOException {
    File target = Files.createTempDirectory(tmp, "d").toFile();
    // README.txt only; __MACOSX entries in the 2.0.war must be skipped
    int count = find(root, "readme", "1.0").unpack(target);
    Assertions.assertEquals(1, count);
  }

  @Test
  public void testUnpackReadmeExtractsFile() throws IOException {
    File target = Files.createTempDirectory(tmp, "d").toFile();
    find(root, "readme", "1.0").unpack(target);
    Assertions.assertTrue(new File(target, "README.txt").exists());
  }

  // ---------------------------------------------------------------------------
  // unpack() — sample-1.0.war (files + __MACOSX + .DS_Store to skip)
  // ---------------------------------------------------------------------------

  @Test
  public void testUnpackSampleCount() throws IOException {
    File target = Files.createTempDirectory(tmp, "d").toFile();
    // Extracted: test/sample.html, WEB-INF/psml/test.psml,
    //            WEB-INF/config/services!test.xml, README.txt
    // Skipped:   __MACOSX/*, *.DS_Store, directories
    int count = find(root, "sample", "1.0").unpack(target);
    Assertions.assertEquals(4, count);
  }

  @Test
  public void testUnpackSampleSkipsMacOsEntries() throws IOException {
    File target = Files.createTempDirectory(tmp, "d").toFile();
    find(root, "sample", "1.0").unpack(target);
    Assertions.assertFalse(new File(target, "__MACOSX").exists());
  }

  @Test
  public void testUnpackSampleSkipsDsStoreFiles() throws IOException {
    File target = Files.createTempDirectory(tmp, "d").toFile();
    find(root, "sample", "1.0").unpack(target);
    Assertions.assertFalse(new File(target, "WEB-INF/.DS_Store").exists());
  }

  @Test
  public void testUnpackSampleExtractsLegalWebInfFile() throws IOException {
    File target = Files.createTempDirectory(tmp, "d").toFile();
    find(root, "sample", "1.0").unpack(target);
    Assertions.assertTrue(new File(target, "WEB-INF/config/services!test.xml").exists());
  }

  // ---------------------------------------------------------------------------
  // unpack() — illegal-1.0.zip (contains web.xml and services.xml that must be blocked)
  // ---------------------------------------------------------------------------

  @Test
  public void testUnpackIllegalSkipsWebXml() throws IOException {
    File target = Files.createTempDirectory(tmp, "d").toFile();
    find(root, "illegal", "1.0").unpack(target);
    Assertions.assertFalse(new File(target, "WEB-INF/web.xml").exists(), "WEB-INF/web.xml must not be extracted from an overlay");
  }

  @Test
  public void testUnpackIllegalSkipsServicesXml() throws IOException {
    File target = Files.createTempDirectory(tmp, "d").toFile();
    find(root, "illegal", "1.0").unpack(target);
    Assertions.assertFalse(new File(target, "WEB-INF/config/services.xml").exists(), "WEB-INF/config/services.xml must not be extracted from an overlay");
  }

  @Test
  public void testUnpackIllegalCount() throws IOException {
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
