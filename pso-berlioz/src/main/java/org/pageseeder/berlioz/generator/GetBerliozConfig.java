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
package org.pageseeder.berlioz.generator;

import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.Generator;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.output.OutputWriter;
import org.pageseeder.berlioz.servlet.ConfigurationSnapshot;
import org.pageseeder.berlioz.servlet.BerliozConfig;

/**
 * Reports safe runtime servlet configuration as XML or JSON.
 *
 * <p>Enable only on administrative services protected by application or container security.
 * Mappings are servlet URL patterns, not container error-page registrations.
 *
 * <p>ETags and response content share an immutable request snapshot.
 *
 * @author Christophe Lauret
 * @since 0.14.3
 */
public final class GetBerliozConfig implements Generator, Cacheable {
  private static final String ATTRIBUTE = GetBerliozConfig.class.getName() + ".snapshot";

  @Override
  public String getETag(Request request) {
    return snapshot(request).etag();
  }

  @Override
  public Response generate(Request request, OutputWriter out) {
    snapshot(request).writeTo(out);
    return Response.ok();
  }

  /**
   * Return the snapshot from the request or create it if needed.
   *
   * @param request The Berlioz request
   *
   * @return The ConfigurationSnapshot
   */
  private ConfigurationSnapshot snapshot(Request request) {
    Object existing = request.getAttribute(ATTRIBUTE);
    if (existing instanceof ConfigurationSnapshot) return (ConfigurationSnapshot) existing;
    ConfigurationSnapshot snapshot = BerliozConfig.diagnosticSnapshot();
    request.setAttribute(ATTRIBUTE, snapshot);
    return snapshot;
  }

}
