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
import java.net.URL;

import org.jspecify.annotations.Nullable;

/**
 * A single service configuration document to be parsed, together with the origin it was
 * discovered from.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.2
 * @since 0.14.2
 */
public final class ServiceSource {

  private final URL url;

  private final ServiceOrigin origin;

  private ServiceSource(URL url, ServiceOrigin origin) {
    this.url = url;
    this.origin = origin;
  }

  /**
   * Creates a source for a {@code META-INF/berlioz/services.xml} resource found on the classpath.
   *
   * @param url the resource URL returned by {@link ClassLoader#getResources(String)}.
   *
   * @return the corresponding source.
   */
  public static ServiceSource classpath(URL url) {
    return new ServiceSource(url, ServiceOrigin.forClasspathResource(url));
  }

  /**
   * Creates a source for a service configuration file beneath the application's configuration
   * directory.
   *
   * @param file      the service configuration file.
   * @param configDir the application's configuration directory, may be {@code null}.
   *
   * @return the corresponding source.
   */
  public static ServiceSource filesystem(File file, @Nullable File configDir) {
    ServiceOrigin origin = ServiceOrigin.forFile(file, configDir);
    return new ServiceSource(origin.url(), origin);
  }

  /**
   * @return the location of the document to parse.
   */
  public URL url() {
    return this.url;
  }

  /**
   * @return the origin this source was discovered from.
   */
  public ServiceOrigin origin() {
    return this.origin;
  }

  /**
   * @return the kind of location this source was discovered from; shorthand for
   *         {@code origin().kind()}.
   */
  public ServiceSourceKind kind() {
    return this.origin.kind();
  }

  /**
   * @return a deterministic key used to order sources of the same kind so that discovery order is
   *         stable across repeated loads, regardless of classloader/filesystem enumeration order.
   */
  public String orderingKey() {
    return this.origin.displayName();
  }

  @Override
  public String toString() {
    return this.origin.displayName();
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) return true;
    if (!(o instanceof ServiceSource)) return false;
    ServiceSource other = (ServiceSource) o;
    return this.url.toExternalForm().equals(other.url.toExternalForm());
  }

  @Override
  public int hashCode() {
    return this.url.toExternalForm().hashCode();
  }

}
