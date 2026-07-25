/*
 * Copyright 2026 Allette Systems (Australia)
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
package org.pageseeder.berlioz.content;

/**
 * The kind of location a service configuration was loaded from.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.2
 * @since 0.14.2
 */
public enum ServiceSourceKind {

  /**
   * Discovered as a <code>META-INF/berlioz/services.xml</code> resource on the classpath.
   */
  CLASSPATH,

  /**
   * Loaded from a file beneath the application's <code>WEB-INF/config</code> directory.
   */
  FILESYSTEM

}
