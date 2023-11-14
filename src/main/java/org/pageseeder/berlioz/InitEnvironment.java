/*
 * Copyright 2019 Allette Systems (Australia)
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
import java.io.IOException;
import java.util.Objects;

/**
 * Defines an immutable initialization environment for Berlioz.
 *
 * <p>The initialization environment includes:
 * - the `WEB-INF` folder
 * - the Web application data folder
 * - The name of the berlioz config folder
 * - The Berlioz mode
 *
 * <p>The initialization environment cannot change Berlioz is running.
 *
 * @version Berlioz 0.11.4
 * @since Berlioz 0.11.4
 */
public final class InitEnvironment {

  /**
   * Name of the default configuration to use.
   */
  public static final String DEFAULT_MODE = "default";

  /**
   * Default name of the directory containing the configuration files for Berlioz
   * including the global settings, services, logging, etc...
   *
   * <p>This can be overridden using a system property or environment variable
   */
  public static final String DEFAULT_CONFIG_DIRECTORY = "config";

  // static variables
  // --------------------------------------------------------------------------

  /**
   * The Web application directory.
   *
   * <p>This should always be the <code>WEB-INF</code> folder of the Web
   * application.
   */
  private final File webInf;

  /**
   * Web application data directory.
   *
   * <p>It can be the same as the Web application directory, but may different in
   * cases where the data needs to be persistent and separate from the
   * application itself.
   */
  private final File appData;

  /**
   * The name of the configuration folder.
   */
  private final String configFolder;

  /**
   * The name of the configuration file to use.
   */
  private final String mode;

  /**
   * @throws NullPointerException If any of the parameters is null
   */
  private InitEnvironment(File webInf, File appData, String configFolder, String mode) {
    this.webInf = Objects.requireNonNull(webInf);
    this.appData = Objects.requireNonNull(appData);
    this.configFolder = Objects.requireNonNull(configFolder);
    this.mode = Objects.requireNonNull(mode);
  }

  private InitEnvironment(File webInf) {
    this.webInf = Objects.requireNonNull(webInf);
    this.appData = webInf;
    this.configFolder = DEFAULT_CONFIG_DIRECTORY;
    this.mode = DEFAULT_MODE;
  }

  /**
   * @return An instance initialised with the specified folder.
   */
  public static InitEnvironment create(File webInf) {
    return new InitEnvironment(webInf);
  }

  /**
   * The Berlioz mode.
   */
  public String mode() {
    return this.mode;
  }

  /**
   * Web application data directory.
   *
   * <p>It can be the same as the Web application directory, but may different in
   * cases where the data needs to be persistent and separate from the
   * application itself.
   */
  public File appData() {
    return this.appData;
  }

  /**
   * The Web application directory.
   *
   * <p>This should always be the <code>WEB-INF</code> folder of the Web
   * application.
   */
  public File webInf() {
    return this.webInf;
  }

  /**
   * The name of the configuration folder.
   */
  public String configFolder() {
    return this.configFolder;
  }

  /**
   * Indicates whether the Web application data folder is different from the Web application folder.
   *
   * @return true if they are different; false otherwise.
   */
  @Beta
  public boolean hasAppData() {
    // Most common case appData set to webInf
    if (this.webInf == this.appData){
      return false;
    }
    // fallback in case appData is specified but point to same location
    try {
      if (this.webInf.getCanonicalFile().equals(this.appData.getCanonicalFile())) {
        return false;
      }
    } catch (IOException ex) {
      // NB. not as reliable as canonical but sufficient
      if (this.webInf.getAbsoluteFile().equals(this.appData.getAbsoluteFile())) {
        return false;
      }
    }
    return true;
  }

  /**
   * Return a new instance using the specified application data folder.
   *
   * @param dir the application data folder to use.
   * @return a new instance with the specified application data folder.
   */
  public InitEnvironment appData(File dir) {
    checkDirectoryExists(dir);
    return new InitEnvironment(this.webInf, dir, this.configFolder, this.mode);
  }

  /**
   * Return a new instance using the specified configuration folder name this instance if the
   * name is identical to the name in the current instance.
   *
   * @param name the new folder name.
   * @return an instance with the specified configuration folder name.
   */
  public InitEnvironment configFolder(String name) {
    Objects.requireNonNull(name, "The config folder name must be specified.");
    if (this.configFolder.equals(name)) return this;
    return new InitEnvironment(this.webInf, this.appData, name, this.mode);
  }

  /**
   * Return a new instance using the specified mode or this instance if the mode is identical to
   * the mode in the current instance.
   *
   * @param mode the new mode
   * @return an instance with the specified mode
   */
  public InitEnvironment mode(String mode) {
    Objects.requireNonNull(mode, "The configuration mode must be specified.");
    if (this.mode.equals(mode)) return this;
    return new InitEnvironment(this.webInf, this.appData, this.configFolder, mode);
  }

  /**
   * Return a new instance using the specified application folder.
   *
   * @param dir the application data folder to use.
   * @return a new instance with the specified application folder.
   */
  public InitEnvironment webInf(File dir) {
    checkDirectoryExists(dir);
    return new InitEnvironment(dir, this.appData, this.configFolder, this.mode);
  }

  /**
   * Checks that the specified file exists and is a directory.
   *
   * @param dir The file directory to check.
   *
   * @throws NullPointerException If the file is <code>null</code>
   * @throws IllegalArgumentException If the file is not a directory or does not exist
   */
  private static void checkDirectoryExists(File dir) {
    Objects.requireNonNull(dir, "The specified file "+dir+" is null");
    if (!dir.exists())
      throw new IllegalArgumentException("The specified file "+dir+" does not exist.");
    else if (!dir.isDirectory())
      throw new IllegalArgumentException("The specified file "+dir+" is not a directory.");
  }

}
