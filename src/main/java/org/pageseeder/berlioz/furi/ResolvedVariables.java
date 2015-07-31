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
package org.pageseeder.berlioz.furi;

import java.util.Set;

/**
 * Holds the values of a resolved variables.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.32
 * @since Berlioz 0.9.32
 */
public interface ResolvedVariables {

  /**
   * Returns the names of the variables which have been resolved.
   *
   * @return The names of the variables which have been resolved.
   */
  Set<String> names();

  /**
   * Returns the object corresponding to the specified variable name.
   *
   * @param name The name of the variable.
   *
   * @return The object corresponding to the specified variable; may be <code>null</code>.
   */
  Object get(String name);

}
