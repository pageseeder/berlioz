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
package org.pageseeder.berlioz;

import java.io.Serializable;

/**
 * An ID for errors to help with error handling and diagnostic.
 *
 * <p>Error IDs starting with "bz" are reserved by Berlioz.
 *
 * <p>Note: these are different and complementary to HTTP response codes.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.8.3 - 30 June 2011
 * @since Berlioz 0.8
 */
@Beta public interface ErrorID extends Serializable {

  /**
   * Returns the error identifier as a string.
   *
   * <p>Implementations should also ensure that the <code>toString()</code> returns the same value.
   *
   * @return the error ID as a string.
   */
  String id();

}
