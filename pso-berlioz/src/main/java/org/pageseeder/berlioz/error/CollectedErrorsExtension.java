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
package org.pageseeder.berlioz.error;

import org.pageseeder.berlioz.output.OutputWriter;
import org.pageseeder.berlioz.util.CollectedError;

import java.util.List;
import java.util.Objects;

/**
 * A {@link ProblemExtension} for a list of secondary errors collected while processing a single
 * request (for example, the warnings/errors/fatals an {@link javax.xml.transform.ErrorListener}
 * accumulates while compiling or running one XSLT transform).
 *
 * <p>Each {@link CollectedError} is written as a {@code <collected level="...">} object wrapping
 * an {@link ExceptionDetail} for its underlying throwable, reusing {@code ExceptionDetail}'s
 * class/message/location extraction rather than duplicating it.</p>
 *
 * @param <T> the type of throwable collected
 *
 * @author Christophe Lauret
 *
 * @version 0.14.0
 * @since 0.14.0
 */
public final class CollectedErrorsExtension<T extends Throwable> implements ProblemExtension {

  private static final String NAME = "collected-errors";

  private final List<CollectedError<T>> items;

  /**
   * @param items the collected errors to write; must not be {@code null}
   */
  public CollectedErrorsExtension(List<CollectedError<T>> items) {
    this.items = Objects.requireNonNull(items, "items");
  }

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public OutputWriter writeTo(OutputWriter out) {
    out.startArray(NAME);
    for (CollectedError<T> item : this.items) {
      out.startObject("collected");
      out.field("level", item.level().toString());
      ExceptionDetail.of(item.error(), false).writeTo(out);
      out.endObject();
    }
    out.endArray();
    return out;
  }

}
