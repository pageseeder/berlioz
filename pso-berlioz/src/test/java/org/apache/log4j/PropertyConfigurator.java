package org.apache.log4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Stands in for the Log4j 1 configurator, which is not on the test classpath, so that the legacy
 * loading path of <code>AppInitializer</code> can be tested.
 */
public final class PropertyConfigurator {

  /** The files configured, in order. */
  public static final List<String> CONFIGURED = new ArrayList<>();

  private PropertyConfigurator() {
  }

  public static void configure(String path) {
    CONFIGURED.add(path);
  }
}
