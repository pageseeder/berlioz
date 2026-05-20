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
package org.pageseeder.berlioz.bundler;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Class of exceptions occurring while parsing content for minimization.
 *
 * <p>This class is used a base class.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.32
 * @since Berlioz 0.9.32
 */
public class ParsingException extends Exception {

  /**
   * As per requirement for the Serializable interface.
   */
  private static final long serialVersionUID = -6165339719081171607L;

  /**
   * The line number.
   */
  private final int line;

  /**
   * The column number.
   */
  private final int column;

  /**
   * Creates a new minimizer exception.
   *
   * @param message The message.
   * @param line    The line number.
   * @param column  The column number.
   */
  public ParsingException(String message, int line, int column) {
    super(message);
    this.line = line;
    this.column = column;
  }

  /**
   * @return The affected column number or -1 if unknown.
   */
  public final int getColumn() {
    return this.column;
  }

  /**
   * @return The affected line number or -1 if unknown.
   */
  public final int getLine() {
    return this.line;
  }

  @Override
  public @NonNull String getMessage() {
    return super.getMessage()+" at line "+this.line +" and column "+this.column;
  }
}
