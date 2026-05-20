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
package org.pageseeder.berlioz.util;

import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.ErrorID;

/**
 * A Berlioz exception to includes a list of collected errors.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.13.0
 * @since Berlioz 0.8.1
 */
public final class CompoundBerliozException extends BerliozException {

  /**
   * As per requirement for the Serializable interface.
   */
  private static final long serialVersionUID = -3429111725694515123L;

  /**
   * The error collector.
   */
  private final transient ErrorCollector<? extends Throwable> collector;

  /**
   * Creates a new compound exception.
   *
   * @param message   An explanatory message.
   * @param collector The error collector.
   */
  public CompoundBerliozException(String message, ErrorCollector<? extends Throwable> collector) {
    super(message);
    this.collector = collector;
  }

  /**
   * Creates a new compound exception.
   *
   * @param message   An explanatory message.
   * @param ex        The original exception causing this exception to be raised.
   * @param collector The error collector.
   */
  public CompoundBerliozException(String message, Exception ex, ErrorCollector<? extends Throwable> collector) {
    super(message, ex);
    this.collector = collector;
  }

  /**
   * Creates a new compound exception.
   *
   * @param message   An explanatory message.
   * @param id        An error ID to help with error handling and diagnostic.
   * @param collector The error collector.
   */
  public CompoundBerliozException(String message, ErrorID id, ErrorCollector<? extends Throwable> collector) {
    super(message, id);
    this.collector = collector;
  }

  /**
   * Creates a new compound exception.
   *
   * @param message   An explanatory message.
   * @param ex        The original exception causing this exception to be raised.
   * @param id        An error ID to help with error handling and diagnostic.
   * @param collector The error collector.
   */
  public CompoundBerliozException(String message, Exception ex, ErrorID id,
      ErrorCollector<? extends Throwable> collector) {
    super(message, ex, id);
    this.collector = collector;
  }

  /**
   * The error collector included in this exception.
   *
   * @return The error collector included in this exception.
   */
  public ErrorCollector<? extends Throwable> getCollector() {
    return this.collector;
  }
}
