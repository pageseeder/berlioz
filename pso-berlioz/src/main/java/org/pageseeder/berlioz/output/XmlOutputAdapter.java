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
package org.pageseeder.berlioz.output;

import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.xml.XmlAppendable;
import org.pageseeder.berlioz.xml.XmlWriter;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * An {@link OutputWriter} that produces XML output.
 *
 * <p>Each object or array maps to an XML element; fields map to attributes, text nodes,
 * child elements, or raw XML content depending on the {@link FieldOption}.
 * Fields flagged as {@link FieldOption#JSON_ONLY} are silently skipped.
 * Contexts (objects/arrays) flagged as {@link ContextOption#JSON_ONLY} have their wrapper
 * element omitted but their children are still written at the enclosing level.</p>
 *
 * <p>By default output is buffered in an internal {@link java.io.StringWriter} and can be
 * retrieved via {@link #toString()}. Supply a {@link java.io.Writer} or an existing
 * {@link XmlWriter} to direct output elsewhere.</p>
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.13.0
 * @since Berlioz 0.13.0
 */
@Beta
public class XmlOutputAdapter implements OutputWriter {

  /**
   * What we use to write XML.
   */
  private final XmlWriter xml;

  /**
   * Stack tracking the {@link ContextOption} of each open object or array.
   * Used by {@link #endObject()} and {@link #endArray()} to decide whether to close the element.
   */
  private final Deque<ContextOption> ignore = new ArrayDeque<>();

  /**
   * Creates a new XML writer.
   *
   * <p>To capture the output as a string, supply a {@link java.io.StringWriter} via
   * {@link #XmlOutputAdapter(Writer)} and call {@link java.io.StringWriter#toString()} on it.</p>
   */
  public XmlOutputAdapter() {
    this(new StringWriter());
  }

  /**
   * Creates a new XML writer to a custom writer.
   *
   * @param out Where the XML goes.
   */
  public XmlOutputAdapter(Writer out) {
    this.xml = new XmlAppendable<>(out);
  }

  /**
   * Creates a new XML writer to a custom writer.
   *
   * @param xml Where the XML goes.
   */
  public XmlOutputAdapter(XmlWriter xml) {
    this.xml = xml;
  }

  /**
   * Always XML.
   *
   * @return Always XML (<code>application/xml</code>)
   */
  @Override
  public final OutputType getType() {
    return OutputType.XML;
  }

  @Override
  public final void field(String name, boolean value, FieldOption option) {
    switch (option) {
      case DEFAULT:
      case XML_ONLY:
        this.xml.attribute(name, value);
        return;
      case XML_TEXT:
        this.xml.xml(Boolean.toString(value));
        return;
      case XML_ELEMENT:
        this.xml.openElement(name).xml(Boolean.toString(value)).closeElement();
        return;
      case JSON_ONLY:
      default:
    }
  }

  @Override
  public final void field(String name, long value, FieldOption option) {
    switch (option) {
      case DEFAULT:
      case XML_ONLY:
        this.xml.attribute(name, value);
        return;
      case XML_TEXT:
        this.xml.xml(Long.toString(value));
        return;
      case XML_ELEMENT:
        this.xml.element(name, value);
        return;
      case JSON_ONLY:
      default:
    }
  }

  @Override
  public final void field(String name, double value, FieldOption option) {
    switch (option) {
      case DEFAULT:
      case XML_ONLY:
        this.xml.attribute(name, value);
        return;
      case XML_TEXT:
        this.xml.xml(Double.toString(value));
        return;
      case XML_ELEMENT:
        this.xml.element(name, value);
        return;
      case JSON_ONLY:
      default:
    }
  }

  @Override
  public final void field(String name, String value, FieldOption option) {
    switch (option) {
      case DEFAULT:
      case XML_ONLY:
        this.xml.attribute(name, value);
        return;
      case XML_TEXT:
        this.xml.text(value);
        return;
      case XML_ELEMENT:
        this.xml.element(name, value);
        return;
      case XML_COPY:
        this.xml.xml(value);
        return;
      case JSON_ONLY:
      default:
    }
  }

  @Override
  public void field(String name, String[] values, FieldOption option) {
    // TODO escape
    field(name, String.join(",", values), option);
  }

  @Override
  public void field(String name, Iterable<String> values, FieldOption option) {
    // TODO escape
    field(name, String.join(",", values), option);
  }

  @Override
  public final void startObject(String name, ContextOption option) {
    startElementIfXml(name, option);
  }

  @Override
  public final void endObject() {
    endElementIfXml();
  }

  @Override
  public final void startArray(String name, ContextOption option) {
    startElementIfXml(name, option);
  }

  @Override
  public final void endArray() {
    endElementIfXml();
  }

  @Override
  public final void flush() {
    this.xml.flush();
  }

  @Override
  public void close() {
    this.xml.close();
  }

  @Override
  public String toString() {
    flush();
    return this.xml.toString();
  }

  private void startElementIfXml(String name, ContextOption option) {
    this.ignore.push(option);
    if (option != ContextOption.JSON_ONLY) {
      this.xml.openElement(name);
    }
  }

  private void endElementIfXml() {
    ContextOption option = this.ignore.pop();
    if (option != ContextOption.JSON_ONLY) {
      this.xml.closeElement();
    }
  }

}
