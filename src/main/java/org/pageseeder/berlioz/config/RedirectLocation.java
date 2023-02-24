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
package org.pageseeder.berlioz.config;

public final class RedirectLocation {

  private final String _from;

  private final String _to;

  private final boolean _permanent;

  RedirectLocation(String from, String to, boolean permanent) {
    this._from = from;
    this._to = to;
    this._permanent = permanent;
  }

  public String from() {
    return this._from;
  }

  public String to() {
    return this._to;
  }

  public boolean isPermanent() {
    return this._permanent;
  }

}
