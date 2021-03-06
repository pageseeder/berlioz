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
package org.pageseeder.berlioz.servlet;

/**
 * Holds the results of a transformation process.
 *
 * <p>This class holds information about a process such as its content, processing time (in ms),
 * status and exception.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.14 - 22 January 2013
 * @since Berlioz 0.6
 */
public final class XMLContent implements BerliozOutput {

  /**
   * The content generated by the process.
   */
  private final CharSequence _content;

  /**
   * Creates some new XML content.
   *
   * @param content The content.
   */
  public XMLContent(CharSequence content) {
    this._content = content;
  }

  /**
   * @return The actual XML content.
   */
  @Override
  public CharSequence content() {
    return this._content;
  }

  /**
   * @return Always <code>application/xml</code>.
   */
  @Override
  public String getMediaType() {
    return "application/xml";
  }

  /**
   * @return Always <code>utf-8</code>.
   */
  @Override
  public String getEncoding() {
    return "utf-8";
  }

}
