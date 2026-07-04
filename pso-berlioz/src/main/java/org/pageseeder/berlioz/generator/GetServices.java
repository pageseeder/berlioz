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

import java.io.File;
import java.util.List;

import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.content.ServiceLoader;
import org.pageseeder.berlioz.content.XmlGenerator;
import org.pageseeder.berlioz.error.DetailLevel;
import org.pageseeder.berlioz.util.SHA256;
import org.pageseeder.berlioz.xml.XmlCopier;
import org.pageseeder.berlioz.xml.XmlWriter;

/**
 * Returns the current service configuration as XML.
 *
 * <p>This content generator is mostly useful for developers to see how the services are configured.
 *
 * <h3>Configuration</h3>
 * <p>There is no configuration associated with this generator.</p>
 *
 * <h3>Parameters</h3>
 * <p>This generator does not use and require any parameter.
 *
 * <h3>Returned XML</h3>
 * <p>This generator contains the <code>/WEB-INF/config/services.xml</code> used by Berlioz to load
 * its services.</p>
 * <pre>{@code <services version="1.0"> ... </services>}</pre>
 * <p>The formatting of the XML may differ from the actual files as it is parsed before being
 * returned; the XML declaration and comments are stripped.</p>
 *
 * <h3>Error Handling</h3>
 * <p>Should there be any problem parsing or reading a services file, its content is replaced by:
 * <pre>{@code <copy-error reason="[not-found|parsing]" filename="[name]" message="[message]" line="[line]" column="[column]"/>}</pre>
 * <p>The {@code message} (and {@code line}/{@code column}) attributes are only included when the
 * {@code berlioz.errors.detail} option is set to {@code standard} or {@code full}; a malformed
 * services file never fails this generator, it only replaces that file's content with this element.
 *
 * <h3>Usage</h3>
 * <p>To use this generator in Berlioz (in <code>/WEB-INF/config/services.xml</code>):
 * <pre>{@code <generator class="org.pageseeder.berlioz.generator.GetServices"
 *                         name="[name]" target="[target]"/>}</pre>
 *
 * <h3>Etag</h3>
 * <p>This generator uses an etag based on the name, length, and last modified date of the file.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.0
 * @since 0.8
 */
public final class GetServices implements XmlGenerator, Cacheable {

  @Override
  public String getETag(Request req) {
    StringBuilder etag = new StringBuilder();
    for (File f : ServiceLoader.getInstance().listServiceFiles()) {
      etag.append('~').append(f.length()).append('!').append(f.lastModified());
    }
    return SHA256.hash(etag.toString());
  }

  @Override
  public Response generate(Request req, XmlWriter xml) {
    List<File> files = ServiceLoader.getInstance().listServiceFiles();
    boolean includeDetails = DetailLevel.parse(GlobalSettings.get(BerliozOption.ERROR_DETAIL)) != DetailLevel.MINIMAL;

    if (!files.isEmpty()) {
      File main = files.get(0);
      if (main.exists()) {
        XmlCopier.copyTo(main, xml, includeDetails);
      }
    }

    if (files.size() > 1) {
      xml.openElement("service-modules", true);
      for (int i = 1; i < files.size(); i++) {
        XmlCopier.copyTo(files.get(i), xml, includeDetails);
      }
      xml.closeElement();
    }
    return Response.ok();
  }

}
