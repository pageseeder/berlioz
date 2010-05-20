/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.servlet;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.weborganic.berlioz.BerliozException;
import org.weborganic.berlioz.content.ContentGenerator;
import org.weborganic.berlioz.content.ContentManager;
import org.weborganic.berlioz.content.ContentRequest;
import org.weborganic.berlioz.content.MatchingService;
import org.weborganic.berlioz.content.Service;

import com.topologi.diffx.xml.XMLWriter;
import com.topologi.diffx.xml.XMLWriterImpl;

/**
 * Servlets that only returns XML.
 * 
 * @author Christophe Lauret (Weborganic)
 * @version 9 October 2009
 */
public final class XMLResponse {

  /**
   * Generates an XML response corresponding to the specified HTTP request.
   * 
   * @param req The HTTP servlet request.
   * @param res The HTTP servlet response.
   * 
   * @return The XML content for the appropriate content generator.
   * 
   * @throws IOException Should an I/O error occur.
   */
  public String generate(HttpServletRequest req, HttpServletResponse res) throws IOException {
    try {
      // Initialise the writer
      StringWriter writer = new StringWriter();
      XMLWriter xml = new XMLWriterImpl(writer);
      xml.xmlDecl();
      xml.openElement("root", true);

      // Get the content generator
      MatchingService match = ContentManager.getInstance(req.getPathInfo()); 

      // if the service exists
      if (match != null) {
        Service service = match.service();
        XMLResponseHeader header = new XMLResponseHeader(req, service);
        for (ContentGenerator generator : service.generators()) {
//          LOGGER.info(req.getPathInfo()+" -> "+generator.getClass().getName());
          ContentRequest wrapper = new HttpRequestWrapper(req, res);

          // write the XML for a normal response
          if (!generator.redirect()) {
            header.toXML(xml);
            xml.openElement("content", true);

            // process
            generator.process(wrapper, xml);
            xml.closeElement();
          }
        }

      // the content generator does not exist
      } else {
//        LOGGER.warn("No content generator for "+req.getPathInfo());
        XMLResponseHeader header = new XMLResponseHeader(req, "404-error");
        header.toXML(xml);
      }

      xml.closeElement(); // close 'root'
      xml.flush();
      return writer.toString();

    // if an error occurs generate the proper content
    } catch (Exception ex) {
      return generateError(req, res, ex);
    }
  }

  /**
   * Generates the XML content for when an error occurs while generating the content.
   * 
   * @param req The HTTP servlet request.
   * @param res The HTTP servlet response.
   * @param ex  The exception that was thrown.
   * 
   * @return The corresponding content.
   * 
   * @throws IOException Should an I/O error occur.
   */
  private String generateError(HttpServletRequest req, HttpServletResponse res, Exception ex) throws IOException {
    StringWriter writer = new StringWriter();
    XMLWriter xml = new XMLWriterImpl(writer);
    xml.xmlDecl();
    xml.openElement("root", true);
    new XMLResponseHeader(req, "500-error").toXML(xml);
    xml.openElement("content", true);
    if (ex instanceof BerliozException) {
      ((BerliozException)ex).toXML(xml);
    } else {
      new BerliozException("An unexcepted error occurred", ex).toXML(xml);
    }
    xml.closeElement(); // close 'content'
    xml.closeElement(); // close 'root'
    xml.flush();
    return writer.toString();
  }

}
