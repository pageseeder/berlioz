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
package org.pageseeder.berlioz.xslt;

/**
 * The kind of location an XSLT stylesheet was resolved from.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.2
 * @since 0.14.2
 */
public enum StylesheetSourceKind {

  /**
   * No stylesheet — the content is returned unmodified.
   */
  IDENTITY,

  /**
   * Loaded from a file beneath the application's <code>WEB-INF</code> directory.
   */
  FILESYSTEM,

  /**
   * Loaded from a <code>classpath:</code> (or <code>resource:</code>) resource.
   */
  CLASSPATH

}
