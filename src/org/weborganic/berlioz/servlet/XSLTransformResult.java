/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.servlet;

import java.util.Properties;

import javax.xml.transform.Templates;

/**
 * Holds the results of a transformation process.
 *
 * <p>This class holds information about a process such as its content, processing time (in ms),
 * status and exception.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.14 - 22 January 2013
 * @since Berlioz 0.7
 */
public final class XSLTransformResult implements BerliozOutput {

  /**
   * The status of a process result.
   *
   * These are loosely based on the HTTP error codes.
   */
  public enum Status {

    /**
     * The process has succeeded.
     */
    OK,

    /**
     * An error has occurred during processing.
     */
    ERROR;

  };

  /**
   * The content generated by the process.
   */
  private final CharSequence _content;

  /**
   * The processing time in nano seconds.
   */
  private final long _time;

  /**
   * The status of this processing result.
   */
  private final Status _status;

  /**
   * Any caught exception.
   */
  private final Exception _ex;

  /**
   * Any error message.
   */
  private final String _error;

  /**
   * The content type produced by this transformer.
   */
  private String contentType = "text/html";

  /**
   * The encoding.
   */
  private String encoding = "utf-8";

  /**
   * Creates a successful transformation result.
   *
   * @param content   The content.
   * @param time      The processing nano seconds.
   * @param templates The templates used for the transformation.
   */
  public XSLTransformResult(CharSequence content, long time, Templates templates) {
    this._content = content;
    this._time = time;
    this._status = Status.OK;
    this._error = null;
    this._ex = null;
    if (templates != null) {
      setOutputProperties(templates);
    }
  }

  /**
   * Creates an unsuccessful process result.
   *
   * @param content   The content.
   * @param ex        An exception.
   * @param templates The templates used for the transformation.
   */
  public XSLTransformResult(CharSequence content, Exception ex, Templates templates) {
    this._content = content;
    this._time = 0;
    this._status = Status.ERROR;
    this._error = null;
    this._ex = ex;
    if (templates != null) {
      setOutputProperties(templates);
    }
  }

  // ==============================================================================================
  // Immutable fields

  /**
   * @return the transformed content.
   */
  @Override
  public CharSequence content() {
    return this._content;
  }

  /**
   * @return The processing time in nano seconds.
   */
  public long time() {
    return this._time;
  }

  /**
   * Returns the status of the process result.
   *
   * @return the status of the process result.
   */
  public Status status() {
    return this._status;
  }

  // ==============================================================================================
  // Immutable fields

  /**
   * @return An error message or <code>null</code> if none.
   */
  public String getErrorMessage() {
    return this._error;
  }

  /**
   * @return An error message or <code>null</code> if none.
   */
  public Exception getException() {
    return this._ex;
  }

  /**
   * @return The content type (MIME) as defined in the XSLT templates.
   */
  @Override
  public String getMediaType() {
    return this.contentType;
  }

  /**
   * @return The character encoding as defined in the XSLT templates.
   */
  @Override
  public String getEncoding() {
    return this.encoding;
  }

  /**
   * Sets the output properties of this transform result.
   *
   * @see <a href="http://www.w3.org/TR/xslt20/#element-output">XSLT 2.0 - 20 Serialization</a>
   *
   * @param templates the templates used to generate this.
   */
  protected void setOutputProperties(Templates templates) {
    Properties p = templates.getOutputProperties();
    this.encoding = p.getProperty("encoding", "utf-8");
    this.contentType = p.getProperty("media-type", "text/html");
  }

}
