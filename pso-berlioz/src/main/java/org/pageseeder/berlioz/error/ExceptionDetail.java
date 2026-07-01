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
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.util.Errors;
import org.pageseeder.berlioz.json.JsonWriter;
import org.pageseeder.berlioz.xml.XmlWriter;

import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.xml.sax.SAXParseException;

/**
 * A structured representation of a {@link Throwable} for use as an extension member in an
 * RFC 9457 problem details response.
 *
 * <p>Two verbosity levels are supported:</p>
 * <ul>
 *   <li><b>standard</b> ({@code includeStackTrace = false}) — exception class, optional type
 *       discriminator, clean message, and source location when available (line/column/system-id).
 *       Safe for staging environments.</li>
 *   <li><b>full</b> ({@code includeStackTrace = true}) — adds the safe stack trace and recursively
 *       includes the cause chain. Recommended for development only.</li>
 * </ul>
 *
 * <p>The XML serialization ({@link #toXml}) mirrors the structure produced by
 * {@link Errors#toXML(Throwable, org.pageseeder.xmlwriter.XMLWriter, boolean)} for the legacy
 * error format. The JSON serialization uses camelCase member names per
 * RFC 9457 JSON convention.</p>
 *
 * @author Christophe Lauret
 *
 * @version 0.13.5
 * @since 0.13.5
 */
@Beta
public final class ExceptionDetail implements ProblemExtension {

  private static final String NAME = "exception";

  private final String className;
  private final @Nullable String type;
  private final String message;
  private final @Nullable String stackTrace;
  private final int locationLine;
  private final int locationColumn;
  private final @Nullable String locationPublicId;
  private final @Nullable String locationSystemId;
  private final @Nullable ExceptionDetail cause;

  private ExceptionDetail(String className, @Nullable String type, String message,
      @Nullable String stackTrace,
      int locationLine, int locationColumn,
      @Nullable String locationPublicId, @Nullable String locationSystemId,
      @Nullable ExceptionDetail cause) {
    this.className = className;
    this.type = type;
    this.message = message;
    this.stackTrace = stackTrace;
    this.locationLine = locationLine;
    this.locationColumn = locationColumn;
    this.locationPublicId = locationPublicId;
    this.locationSystemId = locationSystemId;
    this.cause = cause;
  }

  /**
   * Creates an {@code ExceptionDetail} from the given throwable.
   *
   * <p>Recognises {@link SAXParseException} and {@link TransformerException} /
   * {@link TransformerConfigurationException} and extracts their source location
   * (line, column, system ID). All other throwables produce a location-free detail.</p>
   *
   * @param ex                 the throwable to describe
   * @param includeStackTrace  {@code true} for full verbosity (stack trace + cause chain);
   *                           {@code false} for standard verbosity (class, message, location only)
   * @return a new {@code ExceptionDetail}
   */
  public static ExceptionDetail of(Throwable ex, boolean includeStackTrace) {
    // BerliozException is a framework transport wrapper; surface the real cause
    if (ex instanceof org.pageseeder.berlioz.BerliozException && ex.getCause() != null) {
      ex = ex.getCause();
    }
    String className = ex.getClass().getName();
    String message = Errors.cleanMessage(ex);
    String stackTrace = includeStackTrace ? Errors.getStackTrace(ex, true) : null;
    ExceptionDetail cause = null;
    if (includeStackTrace && ex.getCause() != null) {
      cause = of(ex.getCause(), true);
    }

    if (ex instanceof SAXParseException) {
      SAXParseException spe = (SAXParseException) ex;
      String systemId = spe.getSystemId() != null ? Errors.toWebPath(spe.getSystemId()) : null;
      return new ExceptionDetail(className, "SAXParseException", message, stackTrace,
          spe.getLineNumber(), spe.getColumnNumber(), spe.getPublicId(), systemId, cause);
    }

    if (ex instanceof TransformerException) {
      String typeAttr = ex instanceof TransformerConfigurationException
          ? "TransformerConfigurationException" : "TransformerException";
      SourceLocator loc = ((TransformerException) ex).getLocator();
      int line = loc != null ? loc.getLineNumber() : -1;
      int col  = loc != null ? loc.getColumnNumber() : -1;
      String publicId  = loc != null ? loc.getPublicId() : null;
      String systemId  = loc != null && loc.getSystemId() != null
          ? Errors.toWebPath(loc.getSystemId()) : null;
      return new ExceptionDetail(className, typeAttr, message, stackTrace,
          line, col, publicId, systemId, cause);
    }

    return new ExceptionDetail(className, null, message, stackTrace, -1, -1, null, null, cause);
  }

  // --- XmlWritable -----------------------------------------------------------------------------

  @Override
  public String name() {
    return NAME;
  }

  /**
   * Writes this detail as an {@code <exception>} XML element.
   *
   * <p>Standard members: {@code class} attribute, optional {@code type} attribute,
   * {@code <message>}. Full additionally writes {@code <stack-trace>} and {@code <cause>}.
   * A {@code <location>} element is written whenever source location is available,
   * regardless of level.</p>
   */
  @Override
  public XmlWriter toXml(XmlWriter xml) {
    return toXml(xml, NAME);
  }

  private XmlWriter toXml(XmlWriter xml, String elementName) {
    xml.openElement(elementName, true);
    xml.attribute("class", this.className);
    if (this.type != null) xml.attribute("type", this.type);
    xml.element("message", this.message);
    if (this.stackTrace != null) xml.element("stack-trace", this.stackTrace);
    if (this.cause != null) this.cause.toXml(xml, "cause");
    if (hasLocation()) writeLocationXml(xml);
    return xml.closeElement();
  }

  private void writeLocationXml(XmlWriter xml) {
    xml.openElement("location");
    if (this.locationLine   != -1) xml.attribute("line",      this.locationLine);
    if (this.locationColumn != -1) xml.attribute("column",    this.locationColumn);
    if (this.locationPublicId != null) xml.attribute("public-id",  this.locationPublicId);
    if (this.locationSystemId != null) xml.attribute("system-id",  this.locationSystemId);
    xml.closeElement();
  }

  // --- JsonWritable (JSON) ---------------------------------------------------------------------

  /**
   * Writes this detail as a named {@code exception} JSON object.
   *
   * <p>Standard members: {@code class}, optional {@code type}, {@code message}, optional
   * {@code location} object. Full additionally writes {@code stackTrace} and {@code cause}.</p>
   *
   * <p>JSON member names follow camelCase convention: {@code stackTrace}, {@code systemId},
   * {@code publicId}.</p>
   */
  @Override
  public JsonWriter toJson(JsonWriter json) {
    json.startObject(NAME);
    writeFieldsJson(json);
    return json.endObject();
  }

  private void writeFieldsJson(JsonWriter json) {
    json.field("class", this.className);
    if (this.type != null) json.field("type", this.type);
    json.field("message", this.message);
    if (hasLocation()) writeLocationJson(json);
    if (this.stackTrace != null) json.field("stackTrace", this.stackTrace);
    if (this.cause != null) {
      json.startObject("cause");
      this.cause.writeFieldsJson(json);
      json.endObject();
    }
  }

  private void writeLocationJson(JsonWriter json) {
    json.startObject("location");
    if (this.locationLine   != -1) json.field("line",     this.locationLine);
    if (this.locationColumn != -1) json.field("column",   this.locationColumn);
    if (this.locationPublicId != null) json.field("publicId",  this.locationPublicId);
    if (this.locationSystemId != null) json.field("systemId",  this.locationSystemId);
    json.endObject();
  }

  // --- Private helpers -------------------------------------------------------------------------

  private boolean hasLocation() {
    return this.locationLine != -1 || this.locationColumn != -1
        || this.locationPublicId != null || this.locationSystemId != null;
  }

}
