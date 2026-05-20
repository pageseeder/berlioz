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

import org.pageseeder.berlioz.furi.URIPattern;

public class RedirectPattern extends MovedLocationPattern {

  final boolean permanent;

  public RedirectPattern(URIPattern from, URIPattern to, boolean permanent) {
    super(from, to);
    this.permanent = permanent;
  }

  public boolean isPermanent() {
    return this.permanent;
  }

  public RedirectLocation redirect(String path) {
    if (!this.match(path)) return null;
    String target = this.getTarget(path);
    return new RedirectLocation(path, target, this.permanent);
  }

  @Override
  public String toString() {
    return "redirect: "+from()+" -> "+to()+(this.permanent ? " P" : " T");
  }
}
