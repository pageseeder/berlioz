/*
 * Copyright 2019 Allette Systems (Australia)
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
package org.pageseeder.berlioz.http;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Server timing information.
 *
 * @see <a href="https://www.w3.org/TR/server-timing/">W3: Server Timing</a>
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Server-Timing">MDN: Server Timing</a>
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.11.5
 * @since Berlioz 0.11.5
 */
public final class ServerTimingHeader {

  private final List<PerformanceServerTiming> _timings = new ArrayList<>();

  public void add(PerformanceServerTiming timing) {
    this._timings.add(timing);
  }

  public String toHeaderValue() {
    return this._timings.stream().map(t -> t.toHeaderString()).collect(Collectors.joining(","));
  }

}
