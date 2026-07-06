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
package org.pageseeder.berlioz.system;

import java.lang.management.ThreadInfo;

import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.content.Generator;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.output.OutputWriter;

/**
 * Returns information about a single JVM thread, including its stack trace.
 *
 * <h3>Parameters</h3>
 * <dl>
 *   <dt>{@code id}</dt>
 *   <dd>Optional. The numeric thread ID to inspect; must be zero or positive. Defaults to the
 *       current (request-handling) thread when omitted. A negative value or a value that is
 *       not a valid long throws {@link org.pageseeder.berlioz.error.InvalidParameterException},
 *       which the framework maps to a {@code 400 Bad Request} problem response.</dd>
 * </dl>
 *
 * <h3>Returned XML</h3>
 * <p>When the thread is found:
 * <pre>{@code
 * <thread id="[id]" name="[name]" priority="[n]" state="[state]" alive="true" daemon="[true|false]" group="[group]">
 *   <stacktrace>
 *     <element class="[class]" filename="[file]" method="[method]" line="[n]"/>
 *     ...
 *   </stacktrace>
 * </thread>
 * }</pre>
 * <p>When no thread with the given ID exists:
 * <pre>{@code <no-thread id="[id]"/>}</pre>
 *
 * <h3>Returned JSON</h3>
 * <p>When the thread is found:
 * <pre>{@code
 * {
 *   "id": [id], "name": "[name]", "priority": [n], "state": "[state]", "alive": true,
 *   "daemon": [true|false], "group": "[group]",
 *   "stacktrace": [
 *     {"class": "[class]", "filename": "[file]", "method": "[method]", "line": [n]},
 *     ...
 *   ]
 * }
 * }</pre>
 * <p>When no thread with the given ID exists:
 * <pre>{@code {"id": [id]} }</pre>
 *
 * <h3>Usage</h3>
 * <p>To use this generator in Berlioz (in <code>/WEB-INF/config/services.xml</code>):
 * <pre>{@code <generator class="org.pageseeder.berlioz.system.GetThreadInfo"
 *                         name="[name]" target="[target]"/>}</pre>
 *
 * @author Christophe Lauret
 *
 * @version 0.14.0
 * @since 0.9.32
 */
@Beta
public final class GetThreadInfo implements Generator {

  @Override
  public Response generate(Request req, OutputWriter out) {
    long threadId = req.parameter("id").asLong().inRange(0L, Long.MAX_VALUE)
        .optional(Thread.currentThread().getId());

    ThreadInfo thread = Threads.getThreadInfo(threadId);
    if (thread != null) {
      toOutput(thread, out);
    } else {
      out.startObject("no-thread");
      out.field("id", threadId);
      out.endObject();
    }
    return Response.ok();
  }

  /**
   * Writes a single thread with its stack trace.
   *
   * @param thread The thread information to serialize.
   * @param out    The output writer.
   */
  private static void toOutput(ThreadInfo thread, OutputWriter out) {
    out.startObject("thread");
    Threads.writeThreadAttributes(out, thread, false);
    Threads.writeStackTrace(out, thread.getStackTrace());
    out.endObject();
  }

}
