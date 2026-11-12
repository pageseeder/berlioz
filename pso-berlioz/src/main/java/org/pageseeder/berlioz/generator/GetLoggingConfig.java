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
 * listed in the order considered, with its folder (<code>appdata</code> or <code>webinf</code>),
 * its path relative to that folder and its outcome: <code>absent</code>, <code>ignored</code>
 * (a logback file while SLF4J is not bound to logback), <code>loaded</code>,
 * <code>loaded-with-errors</code> or <code>failed</code>. The file in use is marked
 * <code>in-use</code>; <code>console-default</code> is <code>true</code> when no file could be
 * loaded, so that Berlioz configured logback to log to the console at INFO level.
 * <code>framework</code> is <code>none</code> when Berlioz configured no logging, so that the
 * logging library applies its own defaults.
 *
 * <p><code>backend</code> is the backend that SLF4J uses, identified from the logger factory:
 * <code>logback</code>, <code>log4j2</code>, <code>jul</code>, <code>simple</code>,
 * <code>nop</code> or <code>unknown</code>. <code>configuration</code> is how logging ended up,
 * as far as Berlioz knows: <code>file</code>, <code>console-default</code>, <code>native</code>
 * (<code>berlioz.logging=native</code>, so that no file was looked at) or <code>none</code>; the
 * last two say nothing about the effective configuration of the backend.
 *
 * <p><code>native-setting</code> is included when the documented system property locating the
 * native configuration of the backend is set (<code>logback.configurationFile</code>,
 * <code>log4j2.configurationFile</code> or <code>java.util.logging.config.file</code>), with its
 * <code>name</code> and <code>value</code> at every detail level. In the value, the application
 * data and <code>WEB-INF</code> folders are replaced by <code>[appdata]</code> and
 * <code>[webinf]</code>, and credentials are redacted. It is a hint: Berlioz does not check
 * whether the backend used it.
 *
 * <p>The problems reported by logback while loading a file and the reason of a failure are only
 * included when <code>berlioz.errors.detail</code> is <code>standard</code> or <code>full</code>,
 * at most 20 problems per file; <code>problem-count</code> has the total. Absolute folder paths are
 * replaced by <code>[webinf]</code> or <code>[appdata]</code>, and credentials are redacted.
 *
 * <p>This is a record of the startup: an application may reconfigure logging afterwards.
 * <code>initialised</code> is <code>false</code> when Berlioz was not started by
 * {@link AppInitializer}.
 *
 * <h3>Returned XML</h3>
 * <pre>{@code
 * <logging-config initialised="true" framework="logback" backend="logback" configuration="file"
 *                 mode="[mode]" logger-factory="[class]" time="[iso8601]">
 *   <native-setting name="logback.configurationFile" value="[value]"/>
 *   <candidate framework="logback" location="webinf" path="config/logback-[mode].xml" outcome="failed"
 *              failure="[message]"/>
 *   <candidate framework="logback" location="webinf" path="config/logback.xml" outcome="loaded-with-errors"
 *              in-use="true" problem-count="[count]">
 *     <problem level="[error|warn]" message="[message]"/>
 *   </candidate>
 * </logging-config>
 * }</pre>
 *
 * <h3>Returned JSON</h3>
 * <pre>{@code
 * {"initialised": true, "framework": "logback", "backend": "logback", "configuration": "file",
 *  "mode": "[mode]", "loggerFactory": "[class]", "time": "[iso8601]",
 *  "nativeSetting": {"name": "logback.configurationFile", "value": "[value]"},
 *  "candidates": [
 *    {"framework": "logback", "location": "webinf", "path": "config/logback.xml",
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
