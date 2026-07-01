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
package org.pageseeder.berlioz.error;

import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.json.JsonWritable;
import org.pageseeder.berlioz.output.OutputWritable;
import org.pageseeder.berlioz.xml.XmlWritable;

/**
 * A structured extension member for an RFC 9457 problem details response.
 *
 * <p>Unlike simple values added with typed {@code ProblemDetails.extension(name, value)}
 * methods, a
 * {@code ProblemExtension} owns its extension member name and its complete XML and JSON
 * representation. Implementations should write a self-contained member: an XML element in
 * {@link #toXml(org.pageseeder.berlioz.xml.XmlWriter)} and a named JSON object or value in
 * {@link #toJson(org.pageseeder.berlioz.json.JsonWriter)}.</p>
 *
 * @author Christophe Lauret
 *
 * @version 0.13.5
 * @since 0.13.5
 */
@Beta
public interface ProblemExtension extends XmlWritable, JsonWritable, OutputWritable {

  /**
   * The RFC 9457 extension member name.
   *
   * @return the name used for collision checks and by the extension's serialized representation
   */
  String name();

}
