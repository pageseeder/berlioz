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

import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.content.XmlGenerator;
import org.pageseeder.berlioz.xml.XmlWriter;

/**
 * Returns JVM runtime information as XML.
 *
 * <h3>Parameters</h3>
 * <p>This generator does not use any parameter.</p>
 *
 * <h3>Returned XML</h3>
 * <pre>{@code
 * <runtime processors="[n]">
 *   <memory free="[bytes]" total="[bytes]" max="[bytes]"/>
 * </runtime>
 * }</pre>
 * <dl>
 *   <dt>{@code processors}</dt><dd>Number of processors available to the JVM.</dd>
 *   <dt>{@code free}</dt><dd>Amount of free memory in the JVM heap, in bytes.</dd>
 *   <dt>{@code total}</dt><dd>Total memory currently allocated to the JVM heap, in bytes.</dd>
 *   <dt>{@code max}</dt><dd>Maximum heap memory the JVM will attempt to use, in bytes.</dd>
 * </dl>
 *
 * <h3>Usage</h3>
 * <p>To use this generator in Berlioz (in <code>/WEB-INF/config/services.xml</code>):
 * <pre>{@code <generator class="org.pageseeder.berlioz.system.GetRuntimeInfo"
 *                         name="[name]" target="[target]"/>}</pre>
 *
 * @author Christophe Lauret
 *
 * @version 0.14.0
 * @since 0.9.32
 */
@Beta
public class GetRuntimeInfo implements XmlGenerator {

  @Override
  public Response generate(Request req, XmlWriter xml) {
    Runtime runtime = Runtime.getRuntime();

    xml.openElement("runtime");
    xml.attribute("processors", runtime.availableProcessors());

    xml.openElement("memory");
    xml.attribute("free",  runtime.freeMemory());
    xml.attribute("total", runtime.totalMemory());
    xml.attribute("max",   runtime.maxMemory());
    xml.closeElement();

    xml.closeElement();
    return Response.ok();
  }

}
