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
package org.pageseeder.berlioz.xml;

import java.io.File;
import java.io.Reader;

import javax.xml.parsers.SAXParser;

import org.pageseeder.berlioz.BerliozException;
import org.xml.sax.ContentHandler;

/**
 * A utility class to help with some simple XML operations.
 *
 * @author Christophe Lauret
 *
 * @version 0.13.1
 * @since 0.6
 *
 * @deprecated since 0.13.1. Use {@link Xml} instead.
 */
@Deprecated(since = "0.13.1", forRemoval = true)
public final class XMLUtils {

  private XMLUtils() {
  }

  /**
   * @param handler the content handler to use
   * @param xml     the XML file to parse
   *
   * @throws BerliozException if the parsing fails
   *
   * @deprecated since 0.13.1. Use {@link Xml#parse(ContentHandler, File)} instead.
   */
  @Deprecated(since = "0.13.1", forRemoval = true)
  public static void parse(ContentHandler handler, File xml) throws BerliozException {
    Xml.parse(handler, xml);
  }

  /**
   * @param handler  the content handler to use
   * @param reader   the reader to parse from
   * @param validate whether to validate the XML
   *
   * @throws BerliozException if the parsing fails
   *
   * @deprecated since 0.13.1. Use {@link Xml#parse(ContentHandler, Reader, boolean)} instead.
   */
  @Deprecated(since = "0.13.1", forRemoval = true)
  public static void parse(ContentHandler handler, Reader reader, boolean validate) throws BerliozException {
    Xml.parse(handler, reader, validate);
  }

  /**
   * @param handler  the content handler to use
   * @param xml      the XML file to parse
   * @param validate whether to validate the XML
   *
   * @throws BerliozException if the parsing fails
   *
   * @deprecated since 0.13.1. Use {@link Xml#parse(ContentHandler, File, boolean)} instead.
   */
  @Deprecated(since = "0.13.1", forRemoval = true)
  public static void parse(ContentHandler handler, File xml, boolean validate) throws BerliozException {
    Xml.parse(handler, xml, validate);
  }

  /**
   * @param validating whether the parser should validate the XML
   *
   * @return a new SAX parser
   *
   * @throws BerliozException if the parser cannot be created
   *
   * @deprecated since 0.13.1. Use {@link Xml#newSafeParser(boolean)} instead.
   */
  @Deprecated(since = "0.13.1", forRemoval = true)
  public static SAXParser getParser(boolean validating) throws BerliozException {
    try {
      return Xml.newSafeParser(validating);
    } catch (Exception ex) {
      throw new BerliozException("Could not configure SAX parser.", ex);
    }
  }

}
