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
package org.pageseeder.berlioz.servlet;

import java.io.File;

import org.eclipse.jdt.annotation.Nullable;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.content.Environment;

/**
 * Provides the environment common to all services.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.13.0
 * @since Berlioz 0.9
 */
public final class HttpEnvironment implements Environment {

  /**
   * The location of the public folder.
   */
  private final File publicFolder;

  /**
   * The location of the private folder.
   */
  private final File privateFolder;

  /**
   * The cache control directives for the environment (may be overridden by individual services)
   */
  private final String cacheControl;

  /**
   * Creates a new HTTP environment.
   *
   * @param publicFolder The public directory.
   * @param privateFolder The private directory.
   * @param cacheControl The default cache control directive.
   */
  public HttpEnvironment(File publicFolder, File privateFolder, String cacheControl) {
    this.publicFolder = publicFolder;
    this.privateFolder = privateFolder;
    this.cacheControl = cacheControl;
  }

  @Override
  public File getPublicFolder() {
    return this.publicFolder;
  }

  @Override
  public File getPrivateFolder() {
    return this.privateFolder;
  }

  @Override
  public File getPrivateFile(String path) {
    return new File(this.privateFolder, path);
  }

  @Override
  public File getPublicFile(String path) {
    return new File(this.publicFolder, path);
  }

  @Override
  public @Nullable String getProperty(String name) {
    return GlobalSettings.get(name);
  }

  @Override
  public boolean getProperty(String name, boolean def) {
    return GlobalSettings.get(name, def);
  }

  @Override
  public int getProperty(String name, int def) {
    return GlobalSettings.get(name, def);
  }

  @Override
  public String getProperty(String name, String def) {
    return GlobalSettings.get(name, def);
  }

  /**
   * @return The HTTP cache control value.
   */
  public String getCacheControl() {
    return this.cacheControl;
  }
}
