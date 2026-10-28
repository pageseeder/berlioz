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

import javax.servlet.ServletContext;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.content.Environment;
import org.pageseeder.berlioz.util.FileUtils;

/**
 * Provides the environment common to all services.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.5
 * @since 0.9
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
   * The cache control directives for the environment (might be overridden by individual services)
   */
  private final String cacheControl;

  /**
   * The name and version of the servlet container.
   */
  private final @Nullable String serverInfo;

  /**
   * The Servlet API version supported by the container (major.minor).
   */
  private final @Nullable String servletApiVersion;

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
    this.serverInfo = null;
    this.servletApiVersion = null;
  }

  /**
   * Creates a new HTTP environment that also reports the identity of the servlet container.
   *
   * <p>The Servlet API version is only recorded when the container identifies itself.
   *
   * @param publicFolder The public directory.
   * @param privateFolder The private directory.
   * @param cacheControl The default cache control directive.
   * @param context The servlet context, to identify the container.
   *
   * @since 0.14.5
   */
  public HttpEnvironment(File publicFolder, File privateFolder, String cacheControl, ServletContext context) {
    this.publicFolder = publicFolder;
    this.privateFolder = privateFolder;
    this.cacheControl = cacheControl;
    this.serverInfo = context.getServerInfo();
    this.servletApiVersion = this.serverInfo != null ? context.getMajorVersion() + "." + context.getMinorVersion() : null;
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
    return getContainedFile(this.privateFolder, path);
  }

  @Override
  public File getPublicFile(String path) {
    return getContainedFile(this.publicFolder, path);
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

  @Override
  public @Nullable String getServerInfo() {
    return this.serverInfo;
  }

  @Override
  public @Nullable String getServletApiVersion() {
    return this.servletApiVersion;
  }

  /**
   * @return The HTTP cache control value.
   */
  public String getCacheControl() {
    return this.cacheControl;
  }

  private static File getContainedFile(File root, String path) {
    File file = new File(root, toRootRelativePath(path));
    if (FileUtils.contains(root, file)) return file;
    throw new IllegalArgumentException("Path resolves outside the configured folder: " + path);
  }

  private static String toRootRelativePath(String path) {
    int start = 0;
    while (start < path.length()) {
      char c = path.charAt(start);
      if (c != '/' && c != '\\') break;
      start++;
    }
    return path.substring(start);
  }
}
