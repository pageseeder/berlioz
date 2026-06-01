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

import org.pageseeder.xmlwriter.XMLWritable;

/**
 * This class provides information about the location of resource associated with the content request.
 *
 * @author Christophe Lauret
 *
 * @version 0.9.13
 * @since 0.9.13
 */
public interface Location extends XMLWritable {

  /**
   * Returns the scheme of the URI (e.g. {@code "http"} or {@code "https"}).
   *
   * @return the scheme of the URI
   */
  String scheme();

  /**
   * Returns the host of the URI (e.g. {@code "example.org"}).
   *
   * @return the host of the URI
   */
  String host();

  /**
   * Returns the port used for the URI, or {@code -1} if the default port for the scheme is used.
   *
   * @return the port used for the URI
   */
  int port();

  /**
   * Returns the complete path of the URI including context, prefix, berlioz path, and extension.
   *
   * @return the complete path of the URI
   */
  String path();

  /**
   * Returns the query string of the URI, or an empty string if there is none.
   *
   * @return the query part of the URI
   */
  String query();

  /**
   * Returns detailed path information broken into its constituent parts (context, prefix, path,
   * extension).
   *
   * @return the path information.
   */
  PathInfo info();

}
