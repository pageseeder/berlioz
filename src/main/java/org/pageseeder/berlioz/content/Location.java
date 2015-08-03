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
 * @version Berlioz 0.9.13 - 21 January 2013
 */
public interface Location extends XMLWritable {

  /**
   * @return the scheme of the URI
   */
  String scheme();

  /**
   * @return the host of the URI
   */
  String host();

  /**
   * @return the port used for the URI
   */
  int port();

  /**
   * @return the complete path path of the URI
   */
  String path();

  /**
   * @return the query part of the URI
   */
  String query();

  /**
   * @return the path information.
   */
  PathInfo info();

}
