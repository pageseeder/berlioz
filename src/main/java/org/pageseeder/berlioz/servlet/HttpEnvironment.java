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
 * @version Berlioz 0.11.2
 * @since Berlioz 0.9
 */
public final class HttpEnvironment implements Environment {

  /**
   * The location of the public directory.
   */
  private final File _public;

  /**
   * The location of the public directory.
   */
  private final File _private;

  /**
   * The cache control directives for the environment (may be overridden by individual services)
   */
  private final String _cacheControl;

  /**
   * Creates a new HTTP environment.
   *
   * @param publicDir    The public directory.
   * @param privateDir   The private directory.
   * @param cacheControl The default cache control directive.
   */
  public HttpEnvironment(File publicDir, File privateDir, String cacheControl) {
    this._public = publicDir;
    this._private = privateDir;
    this._cacheControl = cacheControl;
  }

  @Override
  public File getPublicFolder() {
    return this._public;
  }

  @Override
  public File getPrivateFolder() {
    return this._private;
  }

  @Override
  public File getPrivateFile(String path) {
    return new File(this._private, path);
  }

  @Override
  public File getPublicFile(String path) {
    return new File(this._public, path);
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
    return this._cacheControl;
  }
}
