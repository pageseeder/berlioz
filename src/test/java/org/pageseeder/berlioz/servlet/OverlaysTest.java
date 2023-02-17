package org.pageseeder.berlioz.servlet;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class OverlaysTest {

  File root = new File("./src/test/resources/org/pageseeder/berlioz/servlet");

  @Test
  public void testList() {
    List<Overlays.Overlay> overlays = Overlays.list(root);
    Assert.assertTrue(overlays.size() > 0);
  }

  @Test
  public void testUnpack() throws IOException {
    List<Overlays.Overlay> overlays = Overlays.list(root);
    File target = new File("./build/overlays/readme");
    target.mkdirs();
    for (Overlays.Overlay o : overlays) {
      if ("readme".equals(o.name())) {
        o.unpack(target);
      }
    }
  }

  @Test
  public void testUnpackIllegal() throws IOException {
    List<Overlays.Overlay> overlays = Overlays.list(root);
    File target = new File("./build/overlays/illegal");
    target.mkdirs();
    for (Overlays.Overlay o : overlays) {
      if ("illegal".equals(o.name())) {
        o.unpack(target);
      }
    }
  }

  @Test
  public void testUnpackSample() throws IOException {
    List<Overlays.Overlay> overlays = Overlays.list(root);
    File target = new File("./build/overlays/sample");
    target.mkdirs();
    for (Overlays.Overlay o : overlays) {
      if ("sample".equals(o.name())) {
        o.unpack(target);
      }
    }
  }
}
