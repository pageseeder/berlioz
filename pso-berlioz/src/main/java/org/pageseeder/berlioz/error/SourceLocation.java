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

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.output.OutputWriter;
import org.pageseeder.berlioz.util.Errors;

import javax.xml.transform.SourceLocator;
import org.xml.sax.SAXParseException;

/**
 * A source location (line, column, public/system ID), shared by {@link ExceptionDetail} and
 * {@link XsltErrorDetail} — both extract the same fields from a {@link SourceLocator} or a
 * {@link SAXParseException}.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.0
 * @since 0.14.0
 */
final class SourceLocation {

  private final int line;
  private final int column;
  private final @Nullable String publicId;
  private final @Nullable String systemId;

  private SourceLocation(int line, int column, @Nullable String publicId, @Nullable String systemId) {
    this.line = line;
    this.column = column;
    this.publicId = publicId;
    this.systemId = systemId;
  }

  static @Nullable SourceLocation of(SAXParseException ex) {
    return of(ex.getLineNumber(), ex.getColumnNumber(), ex.getPublicId(), toWebPath(ex.getSystemId()));
  }

  static @Nullable SourceLocation of(@Nullable SourceLocator loc) {
    if (loc == null) return null;
    return of(loc.getLineNumber(), loc.getColumnNumber(), loc.getPublicId(), toWebPath(loc.getSystemId()));
  }

  private static @Nullable SourceLocation of(int line, int column,
      @Nullable String publicId, @Nullable String systemId) {
    if (!hasFields(line, column, publicId, systemId)) return null;
    return new SourceLocation(line, column, publicId, systemId);
  }

  void writeTo(OutputWriter out) {
    out.startObject("location");
    if (this.line != -1) out.field("line", this.line);
    if (this.column != -1) out.field("column", this.column);
    out.optionalField("public-id", this.publicId);
    out.optionalField("system-id", this.systemId);
    out.endObject();
  }

  private static @Nullable String toWebPath(@Nullable String systemId) {
    return systemId != null ? Errors.toWebPath(systemId) : null;
  }

  private static boolean hasFields(int line, int column,
      @Nullable String publicId, @Nullable String systemId) {
    return line != -1 || column != -1 || publicId != null || systemId != null;
  }

}
