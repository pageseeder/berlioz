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
package org.pageseeder.berlioz.content;

import java.io.File;
import java.util.List;

import org.pageseeder.berlioz.BerliozException;

/**
 * A utility class to provide access to the content of generators.
 *
 * @author Christophe Lauret
 *
 * @deprecated Use {@link ServiceLoader} instead
 *
 * @version Berlioz 0.10.7
 * @since Berlioz 0.6
 */
@Deprecated
public final class ContentManager {

  /**
   * Prevents creation of instances.
   */
  private ContentManager() {
    // no public constructors for utility classes
  }

  /**
   * Returns the default service registry (mapped to "services.xml").
   *
   * @return the default service registry (mapped to "services.xml").
   */
  public static ServiceRegistry getDefaultRegistry() {
    return ServiceLoader.getInstance().getDefaultRegistry();
  }

  /**
   * Update the patterns based on the current generators.
   *
   * @throws BerliozException Should something unexpected happen.
   *
   * @since Berlioz 0.8.2
   */
  public static synchronized void loadIfRequired() throws BerliozException {
    ServiceLoader.getInstance().loadIfRequired();
  }

  /**
   * Loads the content access file from all services files.
   *
   * @throws BerliozException Should something unexpected happen.
   */
  public static synchronized void load() throws BerliozException {
    ServiceLoader.getInstance().load();
  }

  /**
   * Returns the list of services files to load from the config folder
   * of the repository.
   *
   * <p>This list includes the main file <code>services.xml</code> as well as
   * any file starting with <code>services!</code> and ending in <code>.xml</code>.
   *
   * <p>If it exists, the main file is always returned first. There is no
   * guaranteed ordering for the other services files.
   *
   * @return the list of services files.
   *
   * @deprecated Use {@link #listServiceFiles()} instead
   */
  @Deprecated
  public static List<File> getServiceFiles() {
    return ServiceLoader.getInstance().listServiceFiles();
  }

  /**
   * Loads the content access file.
   *
   * @param xml    The XML file to load.
   *
   * @throws BerliozException Should something unexpected happen.
   */
  public static synchronized void load(File xml) throws BerliozException {
    ServiceLoader.getInstance().load(xml);
  }

  /**
   * Update the patterns based on the current generators.
   */
  public static synchronized void clear() {
    ServiceLoader.getInstance().clear();
  }

}
