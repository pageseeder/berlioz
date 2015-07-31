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
package org.pageseeder.berlioz.util;

/**
 * An implementation of an object that can be used as a fast key made of two objects for lookup in
 * sets and map.
 *
 * <p>This is an immutable object.
 *
 * @param <T> The type of the first constituent of the key
 * @param <V> The type of the second constituent of the key
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.8.2 - 29 June 2011
 * @since Berlioz 0.8.2
 */
public class Pair<T, V> {

  /** Used to calculate the hashcode */
  private static final int PRIME1 = 17;

  /** Used to calculate the hashcode */
  private static final int PRIME2 = 31;

  /** The first constituent of the key */
  private final T _a;

  /** The second constituent of the key */
  private final V _b;

  /** Precompiled hash code */
  private final int hash;

  /**
   * Creates a new scoped path.
   *
   * @param a The first constituent of the key
   * @param b The second constituent of the key
   */
  public Pair(T a, V b) {
   this._a = a;
   this._b = b;
   this.hash = PRIME1
             + (a == null? 0 : PRIME2 * a.hashCode())
             + (b == null? 0 : PRIME2 * b.hashCode());
  }

  @Override
  public final boolean equals(Object o) {
    if (o == this) return true;
    if (o == null || !(o instanceof Pair<?, ?>)) return false;
    Pair<?, ?> k = (Pair<?, ?>)o;
    if (k.hash != this.hash) return false;
    return equals(this._a, k._a) && equals(this._b, k._b);
  }

  @Override
  public final int hashCode() {
    return this.hash;
  }

  /**
   * @return The first constituent of the key.
   */
  public final T first() {
    return this._a;
  }

  /**
   * @return The second constituent of the key.
   */
  public final V second() {
    return this._b;
  }

  /**
   * Compare two objects for equality.
   *
   * @param o1 The first object to compare.
   * @param o2 The second object to compare.
   * @return <code>true</code> if both are equals (including <code>null</code>);
   *         <code>false</code> otherwise.
   */
  private static boolean equals(Object o1, Object o2) {
    // if they are both null, they are both equal
    if (o1 == o2) return true;
    // if one of them is null return false
    if (o1 == null || o2 == null) return false;
    return o1.equals(o2);
  }
}
