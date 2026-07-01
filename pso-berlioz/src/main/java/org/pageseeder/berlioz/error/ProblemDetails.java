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
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.json.JsonStringBuilder;
import org.pageseeder.berlioz.json.JsonWritable;
import org.pageseeder.berlioz.json.JsonWriter;
import org.pageseeder.berlioz.output.OutputWritable;
import org.pageseeder.berlioz.output.OutputWriter;
import org.pageseeder.berlioz.output.OutputWriter.FieldOption;
import org.pageseeder.berlioz.xml.XmlWritable;
import org.pageseeder.berlioz.xml.XmlWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
 * added via typed {@code extension(name, value)} methods. Structured extensions that own their own
 * XML and JSON representation can be added via {@link #extension(ProblemExtension)}.</p>
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

  private static final Set<String> RESERVED_NAMES =
      Set.of(FIELD_TYPE, FIELD_STATUS, FIELD_TITLE, FIELD_DETAIL, FIELD_INSTANCE);

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
   * Returns a copy with the given string extension member added.
   *
   * <p>Extension members allow frameworks and applications to add problem-specific data.
   * For example, a validation failure might add an {@code errors} member listing each invalid
   * field via {@link #extension(String, Iterable)}.</p>
   *
   * @param name  the extension member name; must not conflict with standard RFC 9457 names
   * @param value the extension member value
   * @return a new instance
   */
  public ProblemDetails extension(String name, String value) {
    return withExtension(name, Objects.requireNonNull(value, "value"));
  }

  /**
   * Returns a copy with the given boolean extension member added.
   *
   * @param name  the extension member name; must not conflict with standard RFC 9457 names
   * @param value the extension member value
   * @return a new instance
   */
  public ProblemDetails extension(String name, boolean value) {
    return withExtension(name, value);
  }

  /**
   * Returns a copy with the given integer extension member added.
   *
   * <p>{@code int} values are accepted through normal Java widening conversion.</p>
   *
   * @param name  the extension member name; must not conflict with standard RFC 9457 names
   * @param value the extension member value
   * @return a new instance
   */
  public ProblemDetails extension(String name, long value) {
    return withExtension(name, value);
  }

  /**
   * Returns a copy with the given decimal extension member added.
   *
   * @param name  the extension member name; must not conflict with standard RFC 9457 names
   * @param value the extension member value
   * @return a new instance
   */
  public ProblemDetails extension(String name, double value) {
    return withExtension(name, value);
  }

  /**
   * Returns a copy with the given string collection extension member added.
   *
   * @param name   the extension member name; must not conflict with standard RFC 9457 names
   * @param values the extension member values
   * @return a new instance
   */
  public ProblemDetails extension(String name, Iterable<String> values) {
    return withExtension(name, copyValues(values));
  }

  private ProblemDetails withExtension(String name, Object value) {
    checkExtensionName(Objects.requireNonNull(name, "name"));
    Map<String, Object> copy = new LinkedHashMap<>(this.extensions);
    copy.put(name, value);
    return new ProblemDetails(this.status, this.type, this.title, this.detail, this.instance, copy);
  }

  private static List<String> copyValues(Iterable<String> values) {
    Objects.requireNonNull(values, "values");
    List<String> copy = new ArrayList<>();
    for (String value : values) {
      copy.add(Objects.requireNonNull(value, "values item"));
    }
    return List.copyOf(copy);
  }

  /**
   * Returns a copy with the given structured extension member added.
   *
   * <p>The extension owns its member name and writes its complete XML and JSON representation.
   * This avoids ambiguity between the map key used for bookkeeping and the serialized member
   * name used on the wire.</p>
   *
   * @param extension the structured extension member
   * @return a new instance
   */
  public ProblemDetails extension(ProblemExtension extension) {
    Objects.requireNonNull(extension, "extension");
    String name = Objects.requireNonNull(extension.name(), "extension.name()");
    checkExtensionName(name);
    Map<String, Object> copy = new LinkedHashMap<>(this.extensions);
    copy.put(name, extension);
    return new ProblemDetails(this.status, this.type, this.title, this.detail, this.instance, copy);
  }

  private static void checkExtensionName(String name) {
    if (RESERVED_NAMES.contains(name))
      throw new IllegalArgumentException("'" + name + "' is a reserved RFC 9457 member name; use the dedicated method instead");
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
   * top-level fields. String, numeric, boolean, string collection, and structured extension values
   * are supported.
   *
   * @return the complete JSON object string
   */
  public String toJson() {
    try (JsonStringBuilder json = JsonStringBuilder.create()) {
      return toJson(json).toString();
    }
  }

  private static void writeExtensionJson(JsonWriter json, String name, Object value) {
    if (value instanceof ProblemExtension) {
      ((ProblemExtension) value).toJson(json);
    } else if (value instanceof String)  json.field(name, (String) value);
    else if (value instanceof Long)      json.field(name, (Long) value);
    else if (value instanceof Double)    json.field(name, (Double) value);
    else if (value instanceof Boolean)   json.field(name, (Boolean) value);
    else if (value instanceof Iterable) {
      json.startArray(name);
      for (Object item : (Iterable<?>) value) {
        json.value((String) item);
      }
      json.endArray();
    } else {
      throw new IllegalStateException("Unsupported problem extension value: " + value.getClass().getName());
    }
  }

  /**
   * Writes this problem as a {@code <problem>} XML element, aligned with the
   * {@code application/problem+xml} structure.
   *
   * <p>Standard members are written as child elements in RFC 9457 order. Extension members
   * follow as additional child elements. String collection extension values produce one element
   * per item.
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
    if (value instanceof ProblemExtension) {
      ((ProblemExtension) value).toXml(xml);
    } else if (value instanceof Iterable) {
      for (Object item : (Iterable<?>) value) {
        xml.element(name, (String) item);
      }
    } else if (value instanceof String) {
      xml.element(name, (String) value);
    } else if (value instanceof Long) {
      xml.element(name, ((Long) value).longValue());
    } else if (value instanceof Double || value instanceof Boolean) {
      xml.element(name, value.toString());
    } else {
      throw new IllegalStateException("Unsupported problem extension value: " + value.getClass().getName());
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
    if (value instanceof ProblemExtension) ((ProblemExtension) value).writeTo(out);
    else if (value instanceof String)       out.field(name, (String) value,            FieldOption.XML_ELEMENT);
    else if (value instanceof Long)         out.field(name, (Long) value,              FieldOption.XML_ELEMENT);
    else if (value instanceof Double)       out.field(name, (Double) value,            FieldOption.XML_ELEMENT);
    else if (value instanceof Boolean)      out.field(name, (Boolean) value,           FieldOption.XML_ELEMENT);
    else if (value instanceof Iterable)     out.field(name, (Iterable<String>) value,  FieldOption.XML_ELEMENT);
    else throw new IllegalStateException("Unsupported problem extension value: " + value.getClass().getName());
  }

}
