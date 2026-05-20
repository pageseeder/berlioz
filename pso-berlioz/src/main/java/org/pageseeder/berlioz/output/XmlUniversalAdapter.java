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

@Beta
public class XmlUniversalAdapter implements UniversalWriter {

  /**
   * What we use to write XML.
   */
  private final XmlWriter xml;

  /**
   * When true
   */
  private final Deque<ContextOption> ignore = new ArrayDeque<>();

  /**
   * Creates a new XML writer to a <code>StringWriter</code>.
   *
   * <p>The generated XML can be retrieved from the {@link #toString()} method.</p>
   */
  public XmlUniversalAdapter() {
    this(new StringWriter());
  }

  /**
   * Creates a new XML writer to a custom writer.
   *
   * @param out Where the XML goes.
   */
  public XmlUniversalAdapter(Writer out) {
    this.xml = new XmlAppendable<>(out);
  }

  /**
   * Creates a new XML writer to a custom writer.
   *
   * @param xml Where the XML goes.
   */
  public XmlUniversalAdapter(XmlWriter xml) {
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
  public final void startObject(String name, ContextOption option) {
    this.ignore.push(option);
    this.xml.openElement(name);
  }

  @Override
  public final void endObject() {
    this.ignore.pop();
    this.xml.closeElement();
  }

  @Override
  public final void startArray(String name, ContextOption option) {
    this.ignore.push(option);
    if (option != ContextOption.JSON_ONLY) {
      this.xml.openElement(name);
    }
  }

  @Override
  public final void endArray() {
    ContextOption option = this.ignore.pop();
    if (option != ContextOption.JSON_ONLY) {
      this.xml.closeElement();
    }
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
    this.flush();
    // TODO
//    if (this._out instanceof StringWriter) return this._out.toString();
//    if (this._xml instanceof XMLStringWriter) return this._xml.toString();
    return super.toString();
  }

}
