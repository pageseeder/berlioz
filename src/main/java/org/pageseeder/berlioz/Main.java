/*
 * Copyright 2015 Allette Systems (Australia)
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
package org.pageseeder.berlioz;

import java.io.File;

import org.eclipse.jdt.annotation.Nullable;
import org.pageseeder.berlioz.content.ServiceLoader;

/**
 * Convenience class to invoke this library on the command-line.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.11.2
 * @since Berlioz 0.6
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
      GlobalSettings.setup(new File("."));
      ServiceLoader.getInstance().load();
    }
  }

  /**
   * Displays the usage of this class on System.err.
   *
   * @param message Any message (optional)
   */
  public static void usage(@Nullable String message) {
    if (message != null) {
      System.err.println(message);
    }
    System.err.println("Berlioz");
    System.err.println("Usage: java "+Main.class.getName()+" ");
    System.err.println("Options");
    System.err.println("  -load [services_file]");
  }

}
