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
 * Exception thrown when attempting to close an element when there is no
 * corresponding open element.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.12.0
 * @since Berlioz 0.12.0
 */
public final class IllegalCloseElementException extends IllegalStateException {

  /**
   * Version number for the serialised class.
   */
  private static final long serialVersionUID = 7264175736386596167L;

  /**
   * Creates a new illegal close element exception.
   */
  public IllegalCloseElementException() {
    super("Attempting to close an element with no more element to close.");
  }

}

