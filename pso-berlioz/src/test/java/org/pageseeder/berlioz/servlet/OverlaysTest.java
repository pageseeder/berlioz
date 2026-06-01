package org.pageseeder.berlioz.servlet;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class OverlaysTest {

  // Root with WEB-INF/overlays containing: illegal-1.0.zip, readme-1.0.zip, readme-2.0.war, sample-1.0.war
  private final File root = new File("./src/test/resources/org/pageseeder/berlioz/servlet");

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  // ---------------------------------------------------------------------------
  // list()
  // ---------------------------------------------------------------------------

  @Test
  public void testListCount() {
    List<Overlays.Overlay> overlays = Overlays.list(root);
    Assert.assertEquals(4, overlays.size());
  }

  @Test
  public void testListSortedByNameThenVersion() {
    List<Overlays.Overlay> overlays = Overlays.list(root);
    Assert.assertEquals("illegal[1.0]", overlays.get(0).toString());
    Assert.assertEquals("readme[1.0]",  overlays.get(1).toString());
    Assert.assertEquals("readme[2.0]",  overlays.get(2).toString());
    Assert.assertEquals("sample[1.0]",  overlays.get(3).toString());
  }

  @Test
  public void testListNamesAndVersions() {
    List<Overlays.Overlay> overlays = Overlays.list(root);
    Assert.assertEquals("illegal", overlays.get(0).name());
    Assert.assertEquals("1.0",     overlays.get(0).version());
    Assert.assertEquals("readme",  overlays.get(1).name());
    Assert.assertEquals("1.0",     overlays.get(1).version());
    Assert.assertEquals("readme",  overlays.get(2).name());
    Assert.assertEquals("2.0",     overlays.get(2).version());
    Assert.assertEquals("sample",  overlays.get(3).name());
    Assert.assertEquals("1.0",     overlays.get(3).version());
  }

  @Test
  public void testListEmptyWhenNoOverlaysDirectory() throws IOException {
    File emptyRoot = tmp.newFolder();
    new File(emptyRoot, "WEB-INF").mkdirs();
    Assert.assertTrue(Overlays.list(emptyRoot).isEmpty());
  }

  @Test
  public void testListEmptyWhenOverlaysFolderIsEmpty() throws IOException {
    File emptyRoot = tmp.newFolder();
    new File(emptyRoot, "WEB-INF/overlays").mkdirs();
    Assert.assertTrue(Overlays.list(emptyRoot).isEmpty());
  }

  // ---------------------------------------------------------------------------
  // unpack() — readme-1.0.zip (single file: README.txt)
  // ---------------------------------------------------------------------------

  @SuppressWarnings("java:S5976")
  @Test
  public void testUnpackReadmeCount() throws IOException {
    File target = tmp.newFolder();
    // README.txt only; __MACOSX entries in the 2.0.war must be skipped
    int count = find(root, "readme", "1.0").unpack(target);
    Assert.assertEquals(1, count);
  }

  @Test
  public void testUnpackReadmeExtractsFile() throws IOException {
    File target = tmp.newFolder();
    find(root, "readme", "1.0").unpack(target);
    Assert.assertTrue(new File(target, "README.txt").exists());
  }

  // ---------------------------------------------------------------------------
  // unpack() — sample-1.0.war (files + __MACOSX + .DS_Store to skip)
  // ---------------------------------------------------------------------------

  @Test
  public void testUnpackSampleCount() throws IOException {
    File target = tmp.newFolder();
    // Extracted: test/sample.html, WEB-INF/psml/test.psml,
    //            WEB-INF/config/services!test.xml, README.txt
    // Skipped:   __MACOSX/*, *.DS_Store, directories
    int count = find(root, "sample", "1.0").unpack(target);
    Assert.assertEquals(4, count);
  }

  @Test
  public void testUnpackSampleSkipsMacOsEntries() throws IOException {
    File target = tmp.newFolder();
    find(root, "sample", "1.0").unpack(target);
    Assert.assertFalse(new File(target, "__MACOSX").exists());
  }

  @Test
  public void testUnpackSampleSkipsDsStoreFiles() throws IOException {
    File target = tmp.newFolder();
    find(root, "sample", "1.0").unpack(target);
    Assert.assertFalse(new File(target, "WEB-INF/.DS_Store").exists());
  }

  @Test
  public void testUnpackSampleExtractsLegalWebInfFile() throws IOException {
    File target = tmp.newFolder();
    find(root, "sample", "1.0").unpack(target);
    Assert.assertTrue(new File(target, "WEB-INF/config/services!test.xml").exists());
  }

  // ---------------------------------------------------------------------------
  // unpack() — illegal-1.0.zip (contains web.xml and services.xml that must be blocked)
  // ---------------------------------------------------------------------------

  @Test
  public void testUnpackIllegalSkipsWebXml() throws IOException {
    File target = tmp.newFolder();
    find(root, "illegal", "1.0").unpack(target);
    Assert.assertFalse("WEB-INF/web.xml must not be extracted from an overlay",
        new File(target, "WEB-INF/web.xml").exists());
  }

  @Test
  public void testUnpackIllegalSkipsServicesXml() throws IOException {
    File target = tmp.newFolder();
    find(root, "illegal", "1.0").unpack(target);
    Assert.assertFalse("WEB-INF/config/services.xml must not be extracted from an overlay",
        new File(target, "WEB-INF/config/services.xml").exists());
  }

  @Test
  public void testUnpackIllegalCount() throws IOException {
    File target = tmp.newFolder();
    // Legal files only: test/sample.html, WEB-INF/psml/test.psml, README.txt
    // Blocked: WEB-INF/web.xml, WEB-INF/config/services.xml
    // Skipped: __MACOSX/*, *.DS_Store, directories
    int count = find(root, "illegal", "1.0").unpack(target);
    Assert.assertEquals(3, count);
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
