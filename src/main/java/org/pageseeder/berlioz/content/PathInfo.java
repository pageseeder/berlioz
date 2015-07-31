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
package org.pageseeder.berlioz.content;

import com.topologi.diffx.xml.XMLWritable;

/**
 * Returns information about the content path.
 *
 * <p>None of the methods in this class will return <code>null</code> but empty string.
 * The full path can be constructed by concatenating the <i>context</i>, <i>prefix</i>,
 * <i>path</i> and <i>extension</i> (in that order).
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.13 - 21 January 2013
 */
public interface PathInfo extends XMLWritable {

  /**
   * Returns the context of the Web application.
   *
   * @return the context of the Web application.
   */
  String context();

  /**
   * Returns the prefix used for the request if mapped as "/xxx/*".
   *
   * @return the prefix or empty string.
   */
  String prefix();

  /**
   * Returns the berlioz path used for the request that is the part that corresponds to the wildcard.
   *
   * @return the berlioz path used for the request that is the part that corresponds to the wildcard.
   */
  String path();

  /**
   * Returns the suffix used for the request if mapped as "*.xxx" including the '.'.
   *
   * @return the suffix or empty string.
   */
  String extension();

}
