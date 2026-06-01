/*
 * Copyright 2020 Allette Systems (Australia)
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
package org.pageseeder.berlioz.json;

import org.pageseeder.berlioz.util.WriteFailureException;

import java.io.IOException;

/**
 * Wraps an {@link IOException} that occurs while writing JSON output.
 *
 * @author Christophe Lauret
 *
 * @version 0.13.0
 * @since 0.12.0
 */
public final class JsonWriteFailureException extends WriteFailureException {

  /** As per requirement for Serializable */
  private static final long serialVersionUID = 5845519205395989586L;

  /**
   * Creates a new exception wrapping the given cause.
   *
   * @param cause the underlying IO error.
   */
  public JsonWriteFailureException(IOException cause) {
    super("Unable to write to underlying JSON output", cause);
  }

}
