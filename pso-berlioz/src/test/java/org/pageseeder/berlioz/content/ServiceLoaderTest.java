package org.pageseeder.berlioz.content;

import org.junit.Test;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.GlobalSettings;

import java.io.File;

public class ServiceLoaderTest {

  File webinf = new File("./src/test/resources/org/pageseeder/berlioz");

  @Test
  public void testLoad() throws BerliozException {
    ServiceLoader loader = ServiceLoader.getInstance();
    GlobalSettings.setup(webinf);
    loader.load(new File(webinf, "config/services.xml"));
    System.err.println(loader.getDefaultRegistry().getServices());
  }

}
