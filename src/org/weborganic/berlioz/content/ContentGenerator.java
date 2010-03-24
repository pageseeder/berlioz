/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.content;

import java.io.IOException;

import org.weborganic.berlioz.BerliozException;

import com.topologi.diffx.xml.XMLWriter;

/**
 * An interface to generate XML content.
 * 
 * <p>Each content generator performs a particular function and should write the results onto
 * the given XML writer.
 * 
 * @author Christophe Lauret (Weborganic)
 * @version 8 October 2009
 */
public interface ContentGenerator {

  /**
   * Sets the area the content generator instance should be considered part of.
   * 
   * <p>The area is simply used to group content generators that are always used in the same
   * part of the system.
   * 
   * @param area The area this content generator is part of.
   */
  void setArea(String area);

  /**
   * Return the area this content generator is part of.
   * 
   * @see #setArea(String)
   * 
   * @return The area this content generator is part of.
   */
  String getArea();

  /**
   * Sets the service the content generator instance is associated with.
   * 
   * <p>The service appears in the header and can be used to distinguish too generators
   * performing the same function but in a different context.
   * 
   * @param service The associated service.
   */
  void setService(String service);

  /**
   * Returns the service the content generator instance is associated with.
   * 
   * @see #setService(String)
   * 
   * @return The associated service.
   */
  String getService();

  /**
   * Indicates whether this content generator intends to redirect to a different
   * content generator after it is finished producing its content.
   * 
   * @return <code>true</code> if the content generator will include a redirect;
   *         <code>false</code> otherwise.
   */
  boolean redirect();

  /**
   * Constructs the redirect URL using the given content request.
   * 
   * @param req The content request to produce a redirect URL for this generator.
   * 
   * @return The corresponding redirect URL.
   */
  String getRedirectURL(ContentRequest req);

  /**
   * Produces the actual content.
   * 
   * <p>This is the main method of this interface, it should:
   * <ul>
   *   <li>perform one specific function</li>
   *   <li>write the result on the XML writer</li>
   * </ul>
   * 
   * <p>Implementation should specify which attribute or parameters are used or required.
   * 
   * @param req The content request.
   * @param xml The XML output.
   * 
   * @throws BerliozException If an exception is thrown, it should be wrapped into a Berlioz 
   *                         exception in order to provide additional details.
   *
   * @throws IOException    If an I/O error occurs whilst writing to the XML writer.
   */
  void process(ContentRequest req, XMLWriter xml)
    throws BerliozException, IOException;

  /**
   * Allows this content generator to be managed.
   * 
   * <p>This may involve clearing cached information, reloading files, etc... 
   * 
   * @param req The content request.
   * @param xml The XML output.
   * 
   * @throws BerliozException If an exception is thrown, it should be wrapped into a Berlioz 
   *                          exception in order to provide additional details.
   *
   * @throws IOException    If an I/O error occurs while writing to the XML writer.
   */
  void manage(ContentRequest req, XMLWriter xml) throws BerliozException, IOException;

}
