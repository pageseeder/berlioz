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

import java.util.Objects;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Defines a variable type.
 *
 * The variable type may be used for any purpose. It is just a mechanism to qualify variables further
 * than merely by name.
 *
 * Systems may choose to use a variable type to indicate a particular behaviour for the variable or
 * to enforce a particular type of value.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.11.2
 * @since Berlioz 0.9.32
 */
public final class VariableType {

  /**
   * The pattern for a valid variable name.
   */
  private static final Pattern VALID_NAME = Pattern.compile("[a-zA-Z0-9][\\w.-]*");

  /**
   * The name of this variable type.
   */
  private final String _name;

  /**
   * Create a new variable type.
   *
   * @param name The name of the variable
   *
   * @throws NullPointerException If the specified name is <code>null</code>.
   * @throws IllegalArgumentException If the specified name is an empty string.
   */
  public VariableType(String name) {
    this._name = Objects.requireNonNull(name, "A variable type must have a name, but was null");
    if (!isValidName(name))
      throw new IllegalArgumentException("The variable name is not valid: " + name);
  }

  /**
   * Return the name of this variable type.
   *
   * @return The name of this variable type.
   */
  public String getName() {
    return this._name;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o == this)
      return true;
    if ((o == null) || (o.getClass() != this.getClass()))
      return false;
    VariableType v = (VariableType) o;
    // name and default cannot be null
    return this._name.equals(v._name);
  }

  @Override
  public int hashCode() {
    return this._name.hashCode();
  }

  @Override
  public String toString() {
    return this._name;
  }

  /**
   * Indicates whether the name of this variable type is valid.
   *
   * @param name The name of the variable.
   *
   * @return <code>true</code> if the name is valid; <code>false</code> otherwise.
   */
  protected static boolean isValidName(@Nullable String name) {
    if (name == null)
      return false;
    return VALID_NAME.matcher(name).matches();
  }

}
