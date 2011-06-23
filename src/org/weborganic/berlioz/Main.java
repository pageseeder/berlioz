/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz;

import java.io.File;

import org.weborganic.berlioz.content.ContentManager;

/**
 * Convenience class to invoke this library on the command-line.
 * 
 * @author Christophe Lauret
 * @version 9 October 2009
 */
public final class Main {

  /**
   * Prevents creation of instances.
   */
  private Main() {
  }

  /**
   * Invokes this tool on the command-line.
   * 
   * @param args The command-line parameters.
   * 
   * @throws BerliozException Should an error occur while loading the services.
   */
  public static void main(String[] args) throws BerliozException {
    usage(null);
    // Try to load the content generators
    if (args.length > 1 && "-load".equals(args[0])) {
      // Set 
      GlobalSettings.setRepository(new File("."));
      ContentManager.load();
    }
  }

  /**
   * Displays the usage of this class on System.err.
   * 
   * @param message Any message (optional)
   */
  public static void usage(String message) {
    if (message != null) {
      System.err.println(message);
    }
    System.err.println("Berlioz");
    System.err.println("Usage: java "+Main.class.getName()+" ");
    System.err.println("Options");
    System.err.println("  -load [services_file]");
  }

}
