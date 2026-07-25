/*
 * Copyright 2026 Allette Systems (Australia)
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
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * Identifies where a service configuration document was loaded from.
 *
 * <p>The {@link #displayName()} is safe to expose in diagnostics: for a classpath resource it is
 * the contributing JAR's file name plus the resource path (or just the resource path for an
 * exploded/development classpath entry with no enclosing JAR); for a filesystem file it is the
 * path relative to the application's configuration directory. The full {@link #url()} is kept for
 * internal use (deduplication, server-log detail) and should not be surfaced by diagnostic
 * generators.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.2
 * @since 0.14.2
 */
public final class ServiceOrigin {

  /**
   * The logical resource path used to discover {@code META-INF/berlioz/services.xml} resources.
   */
  static final String CLASSPATH_RESOURCE_PATH = "META-INF/berlioz/services.xml";

  private final ServiceSourceKind kind;

  private final String displayName;

  private final String resourcePath;

  private final URL url;

  private ServiceOrigin(ServiceSourceKind kind, String displayName, String resourcePath, URL url) {
    this.kind = kind;
    this.displayName = displayName;
    this.resourcePath = resourcePath;
    this.url = url;
  }

  /**
   * Creates the origin for a {@code META-INF/berlioz/services.xml} resource found on the classpath.
   *
   * @param url the resource URL returned by {@link ClassLoader#getResources(String)}.
   *
   * @return the corresponding origin.
   */
  public static ServiceOrigin forClasspathResource(URL url) {
    Objects.requireNonNull(url, "url is required");
    String displayName = classpathDisplayName(url, CLASSPATH_RESOURCE_PATH);
    return new ServiceOrigin(ServiceSourceKind.CLASSPATH, displayName, CLASSPATH_RESOURCE_PATH, url);
  }

  /**
   * Creates the origin for a service configuration file beneath the application's configuration
   * directory.
   *
   * @param file      the service configuration file.
   * @param configDir the application's configuration directory, may be {@code null}.
   *
   * @return the corresponding origin.
   *
   * @throws IllegalArgumentException if the file cannot be converted to a URL.
   */
  public static ServiceOrigin forFile(File file, @Nullable File configDir) {
    Objects.requireNonNull(file, "file is required");
    String displayName = filesystemDisplayName(file, configDir);
    URL url;
    try {
      url = file.toURI().toURL();
    } catch (MalformedURLException ex) {
      throw new IllegalArgumentException("Unable to convert file to URL: " + file, ex);
    }
    return new ServiceOrigin(ServiceSourceKind.FILESYSTEM, displayName, displayName, url);
  }

  /**
   * @return the kind of location this configuration was loaded from.
   */
  public ServiceSourceKind kind() {
    return this.kind;
  }

  /**
   * @return a safe, human-readable identification of this origin (never an absolute filesystem
   *         path or a raw JAR URL).
   */
  public String displayName() {
    return this.displayName;
  }

  /**
   * @return the logical resource path (e.g. {@code META-INF/berlioz/services.xml}, or the
   *         configuration file name).
   */
  public String resourcePath() {
    return this.resourcePath;
  }

  /**
   * @return the internal source URL; for internal/server-log use only, not for diagnostics output.
   */
  public URL url() {
    return this.url;
  }

  @Override
  public String toString() {
    return this.displayName;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) return true;
    if (!(o instanceof ServiceOrigin)) return false;
    ServiceOrigin other = (ServiceOrigin) o;
    return this.url.toExternalForm().equals(other.url.toExternalForm());
  }

  @Override
  public int hashCode() {
    return this.url.toExternalForm().hashCode();
  }

  /**
   * Derives a safe display name for a classpath resource URL.
   *
   * <p>For a {@code jar:} URL this is the enclosing JAR's file name plus the resource path
   * (e.g. {@code my-overlay.jar!META-INF/berlioz/services.xml}). For any other protocol (a plain
   * {@code file:} URL from an exploded classpath directory, or a container-specific protocol such
   * as {@code vfs:} or {@code bundle:}), it is just the resource path, since there is no reliable,
   * portable way to derive a short "artifact name" from it.
   *
   * @param url          the resource URL.
   * @param resourcePath the logical resource path that was looked up.
   *
   * @return a safe display name.
   */
  private static String classpathDisplayName(URL url, String resourcePath) {
    if (!"jar".equals(url.getProtocol())) return resourcePath;
    String path = url.getPath();
    int separator = path.indexOf("!/");
    String jarPart = separator >= 0 ? path.substring(0, separator) : path;
    int lastSlash = jarPart.lastIndexOf('/');
    String jarName = lastSlash >= 0 ? jarPart.substring(lastSlash + 1) : jarPart;
    return jarName.isEmpty() ? resourcePath : jarName + "!" + resourcePath;
  }

  /**
   * Derives a safe display name for a filesystem service configuration file: its path relative to
   * the configuration directory, or just its file name if it cannot be relativized (or if the
   * configuration directory is unknown).
   *
   * @param file      the service configuration file.
   * @param configDir the application's configuration directory, may be {@code null}.
   *
   * @return a safe display name.
   */
  private static String filesystemDisplayName(File file, @Nullable File configDir) {
    if (configDir == null) return file.getName();
    try {
      Path relative = configDir.toPath().relativize(file.toPath());
      return relative.toString();
    } catch (IllegalArgumentException ex) {
      return file.getName();
    }
  }

}
