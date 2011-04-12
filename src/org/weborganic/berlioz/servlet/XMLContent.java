/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.servlet;

/**
 * Holds the results of a transformation process.
 * 
 * <p>This class holds information about a process such as its content, processing time (in ms),
 * status and exception.
 * 
 * @author Christophe Lauret
 * @version 27 July 2010
 */
public final class XMLContent implements BerliozOutput {

  /**
   * The content generated by the process.
   */
  private final CharSequence _content;

  /**
   * Creates some new XML content.
   * 
   * @param content The content.
   */
  public XMLContent(CharSequence content) {
    this._content = content;
  }

  /**
   * {@inheritDoc}
   */
  public CharSequence content() {
    return this._content;
  }

  /**
   * Always <code>application/xml</code>.
   * 
   * {@inheritDoc}
   */
  public String getMediaType() {
    return "application/xml";
  }

  /**
   * Always <code>utf-8</code>.
   * 
   * {@inheritDoc}
   */
  public String getEncoding() {
    return "utf-8";
  }

}
