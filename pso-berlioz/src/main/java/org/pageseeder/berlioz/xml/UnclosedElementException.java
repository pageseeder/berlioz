/*
 * Copyright 2010-2015 Allette Systems (Australia)
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
package org.pageseeder.berlioz.xml;

/**
 * Exception thrown when an XML writer is closed but there is still and open
 * element tag without it closing element.
 *
 * <p>This exception simply notifies that the XML will not be well-formed, if
 * writer is closed before the remaining open elements are closed.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.12.0
 * @since Berlioz 0.12.0
 */
public final class UnclosedElementException extends IllegalStateException {

  /**
   * Version number for the serialised class.
   */
  private static final long serialVersionUID = -186657976801720211L;

  /**
   * Create a new unclosed element exception.
   *
   * @param name The name of the unclosed element.
   */
  public UnclosedElementException(String name) {
    super("Attempting to close the XML Writer while element "+name+" has not been closed.");
  }

}
