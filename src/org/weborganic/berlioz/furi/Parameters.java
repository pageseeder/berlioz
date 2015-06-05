/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.furi;

import java.util.Set;

/**
 * An interface to hold a collection of parameters for use during the expansion process.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.32
 * @since Berlioz 0.9.32
 */
public interface Parameters {

  /**
   * Returns the value for the specified parameter.
   *
   * @param name The name of the parameter.
   *
   * @return The value for this parameter or <code>null</code> if not specified.
   */
  String getValue(String name);

  /**
   * Returns the values for the specified parameter.
   *
   * @param name The name of the parameter.
   *
   * @return The values for this parameter or <code>null</code> if not specified.
   */
  String[] getValues(String name);

  /**
   * Indicates whether the parameters for the given name has a value.
   *
   * A parameter has a value if: - it is defined in the parameter list - its array of value has at
   * least one value that is not an empty string
   *
   * @param name The name of the parameter.
   *
   * @return <code>true</code> if it has a value; <code>false</code> otherwise.
   */
  boolean exists(String name);

  /**
   * Indicates whether the parameters for the given name has a value.
   *
   * <p>A parameter has a value if:
   * <ul>
   *   <li>It is defined in the parameter list</li>
   *   <li>Its array of value has at least one value that is not an empty string</li>
   * </ul>
   *
   * @param name The name of the parameter.
   *
   * @return <code>true</code> if it has a value;
   *         <code>false</code> otherwise.
   */
  boolean hasValue(String name);

  /**
   * Returns the set of parameter names as an unmodifiable set.
   *
   * @return The set of parameter names as an unmodifiable set.
   */
  Set<String> names();

  /**
   * Set a parameter with only one value.
   *
   * @param name The name of the parameter.
   * @param value The value.
   */
  void set(String name, String value);

  /**
   * Set a parameter with only multiple values.
   *
   * @param name The name of the parameter.
   * @param values The values.
   */
  void set(String name, String[] values);

}