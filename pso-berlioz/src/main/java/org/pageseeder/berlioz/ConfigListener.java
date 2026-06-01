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

/**
 * An interface that can be used to provide methods for when the berlioz configuration is loaded.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.30
 * @since Berlioz 0.9.30
 */
public interface ConfigListener {

  /**
   * This method is invoked when the global settings have been loaded or reloaded.
   */
  void load();

}
