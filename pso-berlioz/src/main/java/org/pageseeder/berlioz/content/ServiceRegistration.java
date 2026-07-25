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

import java.util.Objects;

import org.pageseeder.berlioz.furi.URIPattern;
import org.pageseeder.berlioz.http.HttpMethod;

/**
 * A single service-to-pattern registration together with the origin it was declared in.
 *
 * <p>This carries enough information for origin-aware conflict reporting: when two registrations
 * are keyed by the same {@link HttpMethod} and URI pattern, the replacing registration's warning
 * can name both the replaced and replacing service and origin.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.2
 * @since 0.14.2
 */
final class ServiceRegistration {

  private final Service service;

  private final HttpMethod method;

  private final URIPattern pattern;

  private final ServiceOrigin origin;

  ServiceRegistration(Service service, HttpMethod method, URIPattern pattern, ServiceOrigin origin) {
    this.service = Objects.requireNonNull(service, "service is required");
    this.method = Objects.requireNonNull(method, "method is required");
    this.pattern = Objects.requireNonNull(pattern, "pattern is required");
    this.origin = Objects.requireNonNull(origin, "origin is required");
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

  @Override
  public String toString() {
    return this.service + " [" + this.method + " " + this.pattern + "] from " + this.origin;
  }

}
