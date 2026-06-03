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
package org.pageseeder.berlioz;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.jspecify.annotations.Nullable;
import org.pageseeder.xmlwriter.XMLWritable;
import org.pageseeder.xmlwriter.XMLWriter;

/**
 * Class of exceptions thrown by this library.
 *
 * <p>This class should be used to wrap exceptions thrown by the tools or utility classes that
 * are specific to this library.
 *
 * <p>For convenience, this class is {@link org.pageseeder.xmlwriter.XMLWritable} so
 * that if the exception is caught it can be converted into an XML message.
 *
 * @see XMLWritable
 *
 * @author Christophe Lauret
 *
 * @version 0.11.2
 * @since 0.8
 */
public class BerliozException extends Exception implements XMLWritable {

  /**
   * As per requirement for the Serializable interface.
   */
  private static final long serialVersionUID = 2528728071585695520L;

  /**
   * A Berlioz Error ID
   */
  @SuppressWarnings("java:S1165") // setId() is deprecated for removal; field will be made final once it is removed
  private @Nullable ErrorID id = null;

  /**
   * Creates a new Berlioz exception.
   *
   * @param message The message.
   */
  public BerliozException(String message) {
    super(message);
  }

  /**
   * Creates a new Berlioz exception wrapping an existing exception.
   *
   * @param message The message.
   * @param cause   The wrapped exception.
   */
  public BerliozException(String message, Exception cause) {
    super(message, cause);
  }

  /**
   * Creates a new Berlioz exception wrapping an existing exception.
   *
   * @param message The message.
   * @param id      An error ID to help with error handling and diagnostic (might be <code>null</code>)
   */
  public BerliozException(String message, ErrorID id) {
    super(message);
    this.id = id;
  }

  /**
   * Creates a new Berlioz exception wrapping an existing exception.
   *
   * @param message The message.
   * @param cause   The wrapped exception.
   * @param id      An error ID to help with error handling and diagnostic
   */
  public BerliozException(String message, Exception cause, ErrorID id) {
    super(message, cause);
    this.id = id;
  }

  /**
   * Returns the ID for this Berlioz Exception.
   *
   * @return the ID for this Berlioz Exception or <code>null</code>.
   */
  @Beta
  public final @Nullable ErrorID id() {
    return this.id;
  }

  /**
   * To set the error ID of this Berlioz exception.
   *
   * @param id The error ID of the berlioz exception.
   *
   * @deprecated Use the constructor that accepts an {@link ErrorID} instead.
   */
  @Deprecated(since = "0.13.0")
  public final void setId(ErrorID id) {
    this.id = id;
  }

  /**
   * Serializes this exception as XML.
   *
   * <p>The XML generated is as follows:
   * <pre class="xml">{@code
   * <berlioz-exception>
   *   <message>message</message>
   *   <code class="comment"><!-- Only if there is additional information --></code>
   *   <cause>exception string value</cause>
   *   <stack-trace>the stack trace</stack-trace>
   * </berlioz-exception>
   * }</pre>
   *
   * @deprecated Will be removed in new releases
   *
   * @param xml The XML writer to use.
   *
   * @throws IOException Should an error be thrown while writing
   */
  @Override
  @Deprecated(since = "0.10.7")
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("berlioz-exception", true);
    xml.element("message", super.getMessage());
    Throwable cause = super.getCause();
    if (cause != null) {
      xml.element("cause", cause.toString());
    }
    // print the stack trace as a string.
    StringWriter writer = new StringWriter();
    PrintWriter printer = new PrintWriter(writer);
    printStackTrace(printer);
    printer.flush();
    xml.element("stack-trace", writer.toString());
    xml.closeElement();
  }

}
