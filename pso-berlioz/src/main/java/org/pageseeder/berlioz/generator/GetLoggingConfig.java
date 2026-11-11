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
import org.pageseeder.berlioz.servlet.AppInitializer;
import org.pageseeder.berlioz.servlet.ConfigurationSnapshot;

/**
 * Reports how Berlioz configured logging when the application started, as XML or JSON.
 *
 * <p>Enable only on administrative services protected by application or container security.
 *
 * <p>Berlioz tries <code>logback-[mode].xml</code> in the application data folder, then in
 * <code>WEB-INF</code>, then <code>logback.xml</code>, and the same for log4j. Each candidate is
 * listed in the order considered, with its folder (<code>app-data</code> or <code>web-inf</code>),
 * its path relative to that folder and its outcome: <code>absent</code>, <code>ignored</code>
 * (a logback file while SLF4J is not bound to logback), <code>loaded</code>,
 * <code>loaded-with-errors</code> or <code>failed</code>. The file in use is marked
 * <code>in-use</code>; <code>console-default</code> is <code>true</code> when no file could be
 * loaded, so that Berlioz configured logback to log to the console at INFO level.
 * <code>framework</code> is <code>none</code> when Berlioz configured no logging, so that the
 * logging library applies its own defaults.
 *
 * <p>The problems reported by logback while loading a file and the reason of a failure are only
 * included when <code>berlioz.errors.detail</code> is <code>standard</code> or <code>full</code>,
 * at most 20 problems per file; <code>problem-count</code> has the total. Absolute folder paths are
 * replaced by <code>[WEB-INF]</code> or <code>[app-data]</code>, and credentials are redacted.
 *
 * <p>This is a record of the startup: an application may reconfigure logging afterwards.
 * <code>initialised</code> is <code>false</code> when Berlioz was not started by
 * {@link AppInitializer}.
 *
 * <h3>Returned XML</h3>
 * <pre>{@code
 * <logging-config initialised="true" framework="logback" mode="[mode]"
 *                 logger-factory="[class]" time="[iso8601]">
 *   <candidate framework="logback" location="web-inf" path="config/logback-[mode].xml" outcome="failed"
 *              failure="[message]"/>
 *   <candidate framework="logback" location="web-inf" path="config/logback.xml" outcome="loaded-with-errors"
 *              in-use="true" problem-count="[count]">
 *     <problem level="[error|warn]" message="[message]"/>
 *   </candidate>
 * </logging-config>
 * }</pre>
 *
 * <h3>Returned JSON</h3>
 * <pre>{@code
 * {"initialised": true, "framework": "logback", "mode": "[mode]", "loggerFactory": "[class]",
 *  "time": "[iso8601]",
 *  "candidates": [
 *    {"framework": "logback", "location": "web-inf", "path": "config/logback.xml",
 *     "outcome": "loaded-with-errors", "inUse": true, "problemCount": [count],
 *     "problems": [{"level": "[error|warn]", "message": "[message]"}]}
 *  ]}
 * }</pre>
 *
 * <p>ETags and response content share an immutable request snapshot.
 *
 * @author Christophe Lauret
 * @since 0.14.6
 */
public final class GetLoggingConfig implements Generator, Cacheable {
  private static final String ATTRIBUTE = GetLoggingConfig.class.getName() + ".snapshot";

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
    ConfigurationSnapshot snapshot = AppInitializer.loggingSnapshot();
    request.setAttribute(ATTRIBUTE, snapshot);
    return snapshot;
  }

}
