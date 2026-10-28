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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.pageseeder.berlioz.furi.URIPattern;
import org.pageseeder.berlioz.http.HttpMethod;

/**
 * A single service-to-pattern registration together with the origin it was declared in.
 *
 * <p>This carries enough information for origin-aware conflict reporting: when two registrations
 * are keyed by the same {@link HttpMethod} and URI pattern, the replacing registration's warning
 * can name both the replaced and replacing service and origin. The registration that wins also
 * keeps the declarations it replaced, in load order, so that diagnostics can list them.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.3
 * @since 0.14.3
 */
final class ServiceRegistration {

  private final Service service;

  private final HttpMethod method;

  private final URIPattern pattern;

  private final ServiceOrigin origin;

  private final List<ServiceRegistration> overridden;

  ServiceRegistration(Service service, HttpMethod method, URIPattern pattern, ServiceOrigin origin) {
    this(service, method, pattern, origin, List.of());
  }

  private ServiceRegistration(Service service, HttpMethod method, URIPattern pattern, ServiceOrigin origin,
      List<ServiceRegistration> overridden) {
    this.service = Objects.requireNonNull(service, "service is required");
    this.method = Objects.requireNonNull(method, "method is required");
    this.pattern = Objects.requireNonNull(pattern, "pattern is required");
    this.origin = Objects.requireNonNull(origin, "origin is required");
    this.overridden = overridden;
  }

  /**
   * Returns a copy of this registration that records the specified registration, and the
   * declarations it had itself replaced, as overridden by this one.
   *
   * @param previous the registration replaced by this one for the same method and pattern.
   *
   * @return a new registration.
   */
  ServiceRegistration replacing(ServiceRegistration previous) {
    List<ServiceRegistration> replaced = new ArrayList<>(previous.overridden);
    replaced.add(new ServiceRegistration(previous.service, previous.method, previous.pattern, previous.origin));
    return new ServiceRegistration(this.service, this.method, this.pattern, this.origin, List.copyOf(replaced));
  }

  Service service() {
    return this.service;
  }

  HttpMethod method() {
    return this.method;
  }

  URIPattern pattern() {
    return this.pattern;
  }

  ServiceOrigin origin() {
    return this.origin;
  }

  /**
   * @return the declarations this registration replaced, in load order.
   */
  List<ServiceRegistration> overridden() {
    return this.overridden;
  }

  @Override
  public String toString() {
    return this.service + " [" + this.method + " " + this.pattern + "] from " + this.origin;
  }

}
