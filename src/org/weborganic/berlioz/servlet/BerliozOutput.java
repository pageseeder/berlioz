/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.servlet;

/**
 * Defines content produced by Berlioz.
 *
 * @author Christophe Lauret
 * @version 27 July 2010
 */
public interface BerliozOutput {

  /**
   * @return The actual content.
   */
  CharSequence content();

  /**
   * Returns the media type for this output.
   *
   * @return The media type (MIME) without the character set used.
   */
  String getMediaType();

  /**
   * The character encoding for the content.
   *
   * @see <a href="http://www.iana.org/assignments/character-sets">IANA Character Sets</a>
   *
   * @return The name of the character set defined by IANA Character Sets.
   */
  String getEncoding();

}
