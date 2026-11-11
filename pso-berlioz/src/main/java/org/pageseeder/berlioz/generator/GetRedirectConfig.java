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
import org.pageseeder.berlioz.servlet.RedirectFilter;

/**
 * Reports the rules that the redirect and relocation filters apply, as XML or JSON.
 *
 * <p>Enable only on administrative services protected by application or container security.
 *
 * <p>Lists every initialized {@link RedirectFilter} and
 * {@link org.pageseeder.berlioz.servlet.RelocationFilter} instance, sorted by type, then name:
 * the <code>config</code> parameter (relative to <code>WEB-INF</code>), whether the file exists,
 * and the last load. A filter loads its rules on its first request, so the status is
 * <code>not-loaded</code> until then; <code>reload-pending</code> is <code>true</code> when a
 * reload of the <code>redirects</code> scope was requested since, which the filter applies on its
 * next request. The rules are those loaded, not the current file. Rules whose <code>from</code> or
 * <code>to</code> pattern is missing or invalid are ignored by the filter, and listed as
 * <code>ignored-rule</code> with their position among the rules of the file (from 1) and the
 * reason: <code>missing-from</code>, <code>invalid-from</code>, <code>missing-to</code> or
 * <code>invalid-to</code>. At most 2000 rules and 2000 ignored rules are listed per filter:
 * <code>rule-count</code> and <code>ignored-count</code> have the totals, and <code>limit</code>
 * is written when a list is truncated.
 *
 * <p>Rule targets are redacted ({@link org.pageseeder.berlioz.util.Redaction#redactUriPattern(String)}):
 * the password of a URL (or its user information without password) and the value of every query
 * parameter are <code>[REDACTED]</code>, unless the value is a URI template expression. The
 * message of a load failure is only included when <code>berlioz.errors.detail</code> is
 * <code>standard</code> or <code>full</code>.
 *
 * <h3>Returned XML</h3>
 * <pre>{@code
 * <redirect-config>
 *   <filter name="[name]" type="redirect" config="[path]" exists="true" status="loaded"
 *           loaded="[iso8601]" rule-count="[count]" ignored-count="[count]">
 *     <rule from="[pattern]" to="[pattern]" permanent="[true|false]"/>
 *     <ignored-rule position="[position]" reason="[reason]" from="[text]" to="[text]"/>
 *   </filter>
 *   <filter name="[name]" type="relocation" config="[path]" exists="false" status="failed"
 *           loaded="[iso8601]" failure="[message]"/>
 *   <filter name="[name]" type="relocation" status="no-config"/>
 * </redirect-config>
 * }</pre>
 *
 * <h3>Returned JSON</h3>
 * <pre>{@code
 * {"filters": [
 *   {"name": "[name]", "type": "redirect", "config": "[path]", "exists": true, "status": "loaded",
 *    "loaded": "[iso8601]", "ruleCount": [count],
 *    "ignoredCount": [count],
 *    "rules": [{"from": "[pattern]", "to": "[pattern]", "permanent": [true|false]}],
 *    "ignoredRules": [{"position": [position], "reason": "[reason]", "from": "[text]", "to": "[text]"}]}
 * ]}
 * }</pre>
 *
 * <p>Statuses are <code>no-config</code> (no <code>config</code> parameter: the filter has no
 * effect), <code>not-loaded</code>, <code>loaded</code> and <code>failed</code> (the filter has
 * no rules until the next successful load).
 *
 * <p>ETags and response content share an immutable request snapshot.
 *
 * @author Christophe Lauret
 * @since 0.14.6
 */
public final class GetRedirectConfig implements Generator, Cacheable {
  private static final String ATTRIBUTE = GetRedirectConfig.class.getName() + ".snapshot";

  @Override
  public String getETag(Request request) {
    return snapshot(request).etag();
  }

  @Override
  public Response generate(Request request, OutputWriter out) {
    snapshot(request).writeTo(out);
    return Response.ok();
  }

  private ConfigurationSnapshot snapshot(Request request) {
    Object existing = request.getAttribute(ATTRIBUTE);
    if (existing instanceof ConfigurationSnapshot) return (ConfigurationSnapshot) existing;
    ConfigurationSnapshot snapshot = RedirectFilter.diagnosticSnapshot();
    request.setAttribute(ATTRIBUTE, snapshot);
    return snapshot;
  }

}
