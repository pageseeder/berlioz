/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import com.topologi.diffx.xml.XMLWritable;
import com.topologi.diffx.xml.XMLWriter;

/**
 * Class of exceptions thrown by this library. 
 * 
 * <p>This class should be used to wrap exceptions thrown by the tools or utility classes that 
 * are specific to this library.
 * 
 * <p>For convenience, this class is {@link com.topologi.diffx.xml.XMLWritable} so
 * that if the exception is caught it can be converted into an XML message.
 * 
 * @see XMLWritable
 * 
 * @author Christophe Lauret (Weborganic)
 * @version 30 June 2011
 */
public class BerliozException extends Exception implements XMLWritable {

  /**
   * As per requirement for the Serializable interface.
   */
  private static final long serialVersionUID = 2528728071585695520L;

  /**
   * An Berlioz Error ID
   */
  private ErrorID _id = null;

  /**
   * Additional information, for instance an SQL statement.
   * @deprecated Will be removed in future releases.
   */
  @Deprecated private final String _extra;

  /**
   * Creates a new Berlioz exception.
   * 
   * @param message The message.
   */
  public BerliozException(String message) {
    super(message);
    this._extra = null;
  }

  /**
   * Creates a new Berlioz exception wrapping an existing exception.
   * 
   * @param message The message.
   * @param cause   The wrapped exception.
   */
  public BerliozException(String message, Exception cause) {
    super(message, cause);
    this._extra = null;
  }

  /**
   * Creates a new Berlioz exception wrapping an existing exception.
   * 
   * @param message The message.
   * @param extra   Additional information.
   * @param cause   The wrapped exception.
   * 
   * @deprecated The extra parameter will be removed in subsequent releases.
   */
  @Deprecated public BerliozException(String message, String extra, Exception cause) {
    super(message, cause);
    this._extra = extra;
  }

  /**
   * Creates a new Berlioz exception wrapping an existing exception.
   * 
   * @param message The message.
   * @param id      An error ID to help with error handling and diagnostic (may be <code>null</code>)
   */
  public BerliozException(String message, ErrorID id) {
    super(message);
    this._extra = null;
    this._id = id;
  }

  /**
   * Creates a new Berlioz exception wrapping an existing exception.
   * 
   * @param message The message.
   * @param cause   The wrapped exception.
   * @param id      An error ID to help with error handling and diagnostic (may be <code>null</code>)
   */
  public BerliozException(String message, Exception cause, ErrorID id) {
    super(message, cause);
    this._extra = null;
    this._id = id;
  }

  /**
   * Returns the ID for this Berlioz Exception.
   * 
   * @return the ID for this Berlioz Exception or <code>null</code>.
   */
  @Beta
  public final ErrorID id() {
    return this._id;
  }
  
  /**
   * Serialises this exception as XML.
   * 
   * <p>The XML generated is as follows:
   * <pre class="xml">{@code
   * <berlioz-exception>
   *   <message>message</message>
   *   <code class="comment"><!-- Only if there is additional information --></code>
   *   <extra>extra info</extra>
   *   <code class="comment"><!-- Only if there is a cause (exception trapped) --></code>
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
  @Deprecated public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("berlioz-exception", true);
    xml.element("message", super.getMessage());
    if (this._extra != null)
      xml.element("extra", this._extra);
    if (super.getCause() != null)
      xml.element("cause", super.getCause().toString());
    // print the stack trace as a string.
    StringWriter writer = new StringWriter();
    PrintWriter printer = new PrintWriter(writer);
    printStackTrace(printer);
    printer.flush();
    xml.element("stack-trace", writer.toString());
    xml.closeElement();
  }

}
