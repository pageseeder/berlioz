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
package org.pageseeder.berlioz.generator;

import org.eclipse.jdt.annotation.NonNull;
import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.xmlwriter.XMLWriter;

/**
 * Generates no content.
 *
 * <p>This content generator is useful for when the XML header already contains
 * enough information for the purpose of application.
 *
 * <p>This generator can also be useful to be picked up by the XSLT even if no content is required.
 *
 * <h3>Configuration</h3>
 * <p>There is no configuration associated with this generator.</p>
 *
 * <h3>Parameters</h3>
 * <p>This generator does not use and require any parameter.
 *
 * <h3>Returned XML</h3>
 * <p>This generator does not have any content, so the XML content is always empty.
 * <p>Since Berlioz always wraps generators' content, the final XML is always:
 * <pre>{@code
 * <content generator="org.pageseeder.berlioz.generator.NoContent"
 *               name="[name]" target="[target]" status="ok"/>}</pre>
 *
 * <p><i>Note: since this generator does produce any data, the return status is always
 * <code>ok</code>.</i></p>
 *
 * <h3>Usage</h3>
 * <p>To use this generator in Berlioz (in <code>/WEB-INF/config/services.xml</code>):
 * <pre>{@code <generator class="org.pageseeder.berlioz.generator.NoContent"
 *                         name="[name]" target="[target]"/>}</pre>
 *
 * <h3>Etag</h3>
 * <p>This Etag for this generator is always <code>"nocontent"</code>.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.0 - 13 October 2011
 * @since Berlioz 0.7
 */
public final class NoContent implements ContentGenerator, Cacheable {

  /**
   * Always returns the <code>"nocontent"</code>.
   *
   * {@inheritDoc}
   */
  @Override
  public @NonNull String getETag(ContentRequest req) {
    return "nocontent";
  }

  /**
   * Do nothing.
   *
   * {@inheritDoc}
   */
  @Override
  public void process(ContentRequest req, XMLWriter xml) {
  }

}
