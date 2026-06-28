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
package org.pageseeder.berlioz.content;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.json.JsonStringBuilder;
import org.pageseeder.berlioz.json.JsonWritable;
import org.pageseeder.berlioz.json.JsonWriter;
import org.pageseeder.berlioz.output.OutputWritable;
import org.pageseeder.berlioz.output.OutputWriter;
import org.pageseeder.berlioz.output.OutputWriter.FieldOption;
import org.pageseeder.berlioz.xml.XmlWritable;
import org.pageseeder.berlioz.xml.XmlWriter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * An immutable RFC 9457 Problem Details object for reporting errors from generators.
 *
 * <p>Standard members:</p>
 * <ul>
 *   <li>{@code type} — a URI identifying the problem type (should resolve to documentation)</li>
 *   <li>{@code title} — a short, human-readable summary of the problem type</li>
 *   <li>{@code status} — the HTTP status code</li>
 *   <li>{@code detail} — a human-readable explanation specific to this occurrence</li>
 *   <li>{@code instance} — a URI identifying the specific occurrence</li>
 * </ul>
 *
 * <p>Extension members (e.g. {@code errors} for parameter validation failures) can be
 * added via {@link #extension(String, Object)}.</p>
 *
 * <p>Berlioz renders problem details according to the negotiated output path:</p>
 * <ul>
 *   <li>JSON: {@code application/problem+json}</li>
 *   <li>XML: {@code application/problem+xml}</li>
 *   <li>HTML: problem XML fed through XSLT</li>
 * </ul>
 *
 * <p>All {@code with*} methods return a new instance; this class is immutable.</p>
 *
 * @see <a href="https://www.rfc-editor.org/rfc/rfc9457">RFC 9457 – Problem Details for HTTP APIs</a>
 *
 * @author Christophe Lauret
 *
 * @version 0.13.5
 * @since 0.13.5
 */
public final class ProblemDetails implements OutputWritable, XmlWritable, JsonWritable {

  private static final String FIELD_TYPE     = "type";
  private static final String FIELD_STATUS   = "status";
  private static final String FIELD_TITLE    = "title";
  private static final String FIELD_DETAIL   = "detail";
  private static final String FIELD_INSTANCE = "instance";

  private final int status;
  private final @Nullable String type;
  private final @Nullable String title;
  private final @Nullable String detail;
  private final @Nullable String instance;
  private final Map<String, Object> extensions;

  private ProblemDetails(int status, @Nullable String type, @Nullable String title,
      @Nullable String detail, @Nullable String instance, Map<String, Object> extensions) {
    this.status = status;
    this.type = type;
    this.title = title;
    this.detail = detail;
    this.instance = instance;
    this.extensions = extensions.isEmpty() ? Map.of()
        : Collections.unmodifiableMap(new LinkedHashMap<>(extensions));
  }

  /**
   * Creates a minimal problem with the given HTTP status code.
   *
   * @param code an HTTP status code in the range 100–599
   * @return a new {@code ProblemDetails} instance
   * @throws IllegalArgumentException if {@code code} is outside 100–599
   */
  public static ProblemDetails of(int code) {
    if (code < 100 || code > 599) throw new IllegalArgumentException("Invalid HTTP status code: " + code);
    return new ProblemDetails(code, null, null, null, null, Map.of());
  }

  /**
   * Creates a minimal problem with the given content status.
   *
   * @param status the HTTP status; must not be {@code null}
   * @return a new {@code ProblemDetails} instance
   */
  public static ProblemDetails of(ContentStatus status) {
    Objects.requireNonNull(status, FIELD_STATUS);
    return of(status.code());
  }

  /**
   * Returns a copy with the given {@code type} URI.
   *
   * @param type a URI identifying the problem type
   * @return a new instance
   */
  public ProblemDetails type(String type) {
    Objects.requireNonNull(type, FIELD_TYPE);
    return new ProblemDetails(this.status, type, this.title, this.detail, this.instance, this.extensions);
  }

  /**
   * Returns a copy with the given human-readable {@code title}.
   *
   * @param title a short summary of the problem type
   * @return a new instance
   */
  public ProblemDetails title(String title) {
    Objects.requireNonNull(title, FIELD_TITLE);
    return new ProblemDetails(this.status, this.type, title, this.detail, this.instance, this.extensions);
  }

  /**
   * Returns a copy with the given {@code detail} explanation.
   *
   * @param detail a human-readable explanation of this specific occurrence
   * @return a new instance
   */
  public ProblemDetails detail(String detail) {
    Objects.requireNonNull(detail, FIELD_DETAIL);
    return new ProblemDetails(this.status, this.type, this.title, detail, this.instance, this.extensions);
  }

  /**
   * Returns a copy with the given {@code instance} URI.
   *
   * @param instance a URI identifying this specific occurrence of the problem
   * @return a new instance
   */
  public ProblemDetails instance(String instance) {
    Objects.requireNonNull(instance, FIELD_INSTANCE);
    return new ProblemDetails(this.status, this.type, this.title, this.detail, instance, this.extensions);
  }

  /**
   * Returns a copy with the given extension member added.
   *
   * <p>Extension members allow frameworks and applications to add problem-specific data.
   * For example, a validation failure might add an {@code errors} member listing each
   * invalid field.</p>
   *
   * @param name  the extension member name; must not conflict with standard RFC 9457 names
   * @param value the extension member value
   * @return a new instance
   */
  public ProblemDetails extension(String name, Object value) {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(value, "value");
    Map<String, Object> copy = new LinkedHashMap<>(this.extensions);
    copy.put(name, value);
    return new ProblemDetails(this.status, this.type, this.title, this.detail, this.instance, copy);
  }

  /** @return the HTTP status code */
  public int status() { return this.status; }

  /** @return the problem type URI, or {@code null} if not set */
  public @Nullable String type() { return this.type; }

  /** @return the short human-readable summary, or {@code null} if not set */
  public @Nullable String title() { return this.title; }

  /** @return the human-readable explanation for this occurrence, or {@code null} if not set */
  public @Nullable String detail() { return this.detail; }

  /** @return the URI identifying this specific occurrence, or {@code null} if not set */
  public @Nullable String instance() { return this.instance; }

  /** @return an unmodifiable map of extension members; empty if none were set */
  public Map<String, Object> extensions() { return this.extensions; }

  // --- Serialization ---------------------------------------------------------------------------

  public JsonWriter toJson(JsonWriter json) {
    json.startObject();
    if (this.type   != null) json.field(FIELD_TYPE,     this.type);
    json.field(FIELD_STATUS, (long) this.status);
    if (this.title  != null) json.field(FIELD_TITLE,    this.title);
    if (this.detail != null) json.field(FIELD_DETAIL,   this.detail);
    if (this.instance != null) json.field(FIELD_INSTANCE, this.instance);
    for (Map.Entry<String, Object> e : this.extensions.entrySet()) {
      writeExtensionJson(json, e.getKey(), e.getValue());
    }
    json.endObject();
    json.flush();
    return json;
  }

  /**
   * Serializes this problem as a complete {@code application/problem+json} document body.
   *
   * <p>Standard members are written in RFC 9457 order. Extension members follow as additional
   * top-level fields. String, numeric, boolean, and {@code List<?>} extension values are supported;
   * other values are serialized via {@code toString()}.
   *
   * @return the complete JSON object string
   */
  public String toJson() {
    try (JsonStringBuilder json = JsonStringBuilder.create()) {
      return toJson(json).toString();
    }
  }

  private static void writeExtensionJson(JsonWriter json, String name, Object value) {
    if (value instanceof String)       json.field(name, (String) value);
    else if (value instanceof Long)    json.field(name, (Long) value);
    else if (value instanceof Integer) json.field(name, (Integer) value);
    else if (value instanceof Double)  json.field(name, (Double) value);
    else if (value instanceof Boolean) json.field(name, (Boolean) value);
    else if (value instanceof List) {
      json.startArray(name);
      for (Object item : (List<?>) value) {
        json.value(item instanceof String ? (String) item : item.toString());
      }
      json.endArray();
    } else {
      json.field(name, value.toString());
    }
  }

  /**
   * Writes this problem as a {@code <problem>} XML element, aligned with the
   * {@code application/problem+xml} structure.
   *
   * <p>Standard members are written as child elements in RFC 9457 order. Extension members
   * follow as additional child elements. {@code List<?>} extension values produce one element
   * per list item; all other values use {@code toString()}.
   *
   * @param xml the XML writer to write to; the caller controls the surrounding document context
   * @return the same writer for chaining
   */
  @Override
  public XmlWriter toXml(XmlWriter xml) {
    xml.openElement("problem", true);
    writeTextElement(xml, FIELD_TYPE,     this.type);
    xml.element(FIELD_STATUS, this.status);
    writeTextElement(xml, FIELD_TITLE,    this.title);
    writeTextElement(xml, FIELD_DETAIL,   this.detail);
    writeTextElement(xml, FIELD_INSTANCE, this.instance);
    for (Map.Entry<String, Object> e : this.extensions.entrySet()) {
      writeExtensionXml(xml, e.getKey(), e.getValue());
    }
    return xml.closeElement();
  }

  private static void writeTextElement(XmlWriter xml, String name, @Nullable String value) {
    if (value == null) return;
    xml.element(name, value);
  }

  private static void writeExtensionXml(XmlWriter xml, String name, Object value) {
    if (value instanceof List) {
      for (Object item : (List<?>) value) {
        xml.element(name, item.toString());
      }
    } else {
      xml.element(name, value.toString());
    }
  }

  // --- OutputWritable --------------------------------------------------------------------------

  /**
   * Writes this problem to the given output writer in a format-agnostic way.
   *
   * <p>All standard members are written as {@link FieldOption#XML_ELEMENT} fields so that they
   * appear as JSON properties in JSON mode and as child elements in XML mode, matching the
   * structure required by both {@code application/problem+json} and {@code application/problem+xml}.
   *
   * <p>This method is intended for <em>inline embedding</em> — for example, when a generator
   * writes a problem into its own output. For a standalone top-level problem response, use
   * {@link #toJson()} or {@link #toXml(XmlWriter)} directly.
   *
   * @param out the output writer; must not be {@code null}
   * @return the same writer for chaining
   */
  @Override
  public OutputWriter writeTo(OutputWriter out) {
    out.startObject("problem");
    out.optionalField(FIELD_TYPE,     this.type,             FieldOption.XML_ELEMENT);
    out.field        (FIELD_STATUS,   this.status,           FieldOption.XML_ELEMENT);
    out.optionalField(FIELD_TITLE,    this.title,            FieldOption.XML_ELEMENT);
    out.optionalField(FIELD_DETAIL,   this.detail,           FieldOption.XML_ELEMENT);
    out.optionalField(FIELD_INSTANCE, this.instance,         FieldOption.XML_ELEMENT);
    for (Map.Entry<String, Object> e : this.extensions.entrySet()) {
      writeExtensionOutput(out, e.getKey(), e.getValue());
    }
    return out.endObject();
  }

  @SuppressWarnings("unchecked")
  private static void writeExtensionOutput(OutputWriter out, String name, Object value) {
    if (value instanceof String)       out.field(name, (String) value,            FieldOption.XML_ELEMENT);
    else if (value instanceof Long)    out.field(name, (Long) value,              FieldOption.XML_ELEMENT);
    else if (value instanceof Integer) out.field(name, (Integer) value,           FieldOption.XML_ELEMENT);
    else if (value instanceof Double)  out.field(name, (Double) value,            FieldOption.XML_ELEMENT);
    else if (value instanceof Boolean) out.field(name, (Boolean) value,           FieldOption.XML_ELEMENT);
    else if (value instanceof Iterable) out.field(name, (Iterable<String>) value, FieldOption.XML_ELEMENT);
    else                               out.field(name, value.toString(),          FieldOption.XML_ELEMENT);
  }

  // --- Framework factories ---------------------------------------------------------------------

  /**
   * Creates a {@code 400 Bad Request} problem from a request parameter validation failure.
   *
   * @param ex the exception carrying the parameter name, value, and reason
   * @return a new {@code ProblemDetails} with {@code type}, {@code title}, {@code detail},
   *         {@code parameter}, and {@code reason} members set
   */
  public static ProblemDetails forInvalidParameter(InvalidParameterException ex) {
    return ProblemDetails.of(ContentStatus.BAD_REQUEST)
        .type("urn:berlioz:problem:invalid-parameter")
        .title("Invalid Request Parameter")
        .detail(ex.getMessage())
        .extension("parameter", ex.getParameterName())
        .extension("reason", reasonString(ex.getReason()));
  }

  /**
   * Creates a {@code 502 Bad Gateway} problem from an upstream service failure.
   *
   * <p>If the exception names the failing dependency via {@link UpstreamException#getUpstreamService()},
   * it is included as an {@code upstream-service} extension member.
   *
   * @param ex the upstream exception
   * @return a new {@code ProblemDetails} with {@code type}, {@code title}, and {@code detail} set
   */
  public static ProblemDetails forUpstreamException(UpstreamException ex) {
    ProblemDetails problem = ProblemDetails.of(ContentStatus.BAD_GATEWAY)
        .type("urn:berlioz:problem:upstream-error")
        .title("Upstream Service Error")
        .detail(ex.getMessage());
    String service = ex.getUpstreamService();
    return service != null ? problem.extension("upstream-service", service) : problem;
  }

  /**
   * Creates a {@code 500 Internal Server Error} problem for an unhandled generator failure.
   *
   * <p>The error detail is intentionally omitted to avoid leaking internal information to clients.
   *
   * @return a new {@code ProblemDetails} with {@code type} and {@code title} set
   */
  public static ProblemDetails forGeneratorError() {
    return ProblemDetails.of(ContentStatus.INTERNAL_SERVER_ERROR)
        .type("urn:berlioz:problem:generator-error")
        .title("Internal Server Error");
  }

  private static String reasonString(InvalidParameterException.Reason reason) {
    return reason.name().toLowerCase().replace('_', '-');
  }

}
