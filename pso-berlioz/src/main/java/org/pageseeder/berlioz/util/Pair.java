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

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * A fast immutable key made of two objects for lookup in sets and maps.
 *
 * <p>This is an immutable object.
 *
 * @param <T> The type of the first constituent of the key
 * @param <V> The type of the second constituent of the key
 *
 * @author Christophe Lauret
 *
 * @version 0.8.2
 * @since 0.8.2
 */
public final class Pair<T, V> {

  /** Used to calculate the hash code. */
  private static final int INITIAL_HASH = 17;

  /** Used to calculate the hash code. */
  private static final int HASH_MULTIPLIER = 31;

  /** The first constituent of the key. */
  private final @Nullable T first;

  /** The second constituent of the key. */
  private final @Nullable V second;

  /** Precomputed hash code. */
  private final int hash;

  /**
   * Creates a new pair.
   *
   * @param first  The first constituent of the key.
   * @param second The second constituent of the key.
   */
  public Pair(@Nullable T first, @Nullable V second) {
    this.first = first;
    this.second = second;
    this.hash = hash(first, second);
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o == this) return true;
    if (!(o instanceof Pair<?, ?>)) return false;
    Pair<?, ?> pair = (Pair<?, ?>)o;
    if (this.hash != pair.hash) return false;
    return Objects.equals(this.first, pair.first) && Objects.equals(this.second, pair.second);
  }

  @Override
  public int hashCode() {
    return this.hash;
  }

  /**
   * @return The first constituent of the key.
   */
  public @Nullable T first() {
    return this.first;
  }

  /**
   * @return The second constituent of the key.
   */
  public @Nullable V second() {
    return this.second;
  }

  /**
   * Computes the pair hash without allocating the varargs array used by {@link Objects#hash(Object...)}.
   *
   * @param first  The first constituent of the key.
   * @param second The second constituent of the key.
   * @return The hash code for the pair.
   */
  private static int hash(@Nullable Object first, @Nullable Object second) {
    int result = INITIAL_HASH;
    result = HASH_MULTIPLIER * result + Objects.hashCode(first);
    result = HASH_MULTIPLIER * result + Objects.hashCode(second);
    return result;
  }
}
