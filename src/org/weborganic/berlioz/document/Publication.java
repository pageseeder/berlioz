package org.weborganic.berlioz.document;

import java.io.File;
import java.io.IOException;

import org.weborganic.berlioz.xml.XMLUtils;


import com.topologi.diffx.xml.XMLWritable;
import com.topologi.diffx.xml.XMLWriter;

/**
 * A publication object.
 * 
 * @author Christophe Lauret (Allette Systems)
 * 
 * @version 16 August 2007
 */
public final class Publication extends Document implements XMLWritable {

  /**
   * Creates a new publication from the specified file.
   * 
   * @param file The file.
   */
  private Publication(File file) {
    super(file, toName(file));
  }

  /**
   * {@inheritDoc}
   */
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("publication", true);
    xml.attribute("name", this.getName());
    // the actual file
    xml.openElement("file");
    xml.attribute("file", this.getFileName());
    xml.attribute("type", this.getType().toString());
    xml.attribute("size", this.getSize());
    xml.closeElement();
    // the effective date
    xml.openElement("effective-date");
    XMLUtils.dateAsXML(xml, this.getDate());
    xml.closeElement(); // 'effective-date'
    xml.closeElement(); // 'publication'
  }

  // static helpers -------------------------------------------------------------------------------

  /**
   * Create a publication instance 
   * 
   * @param file The file to use to instantiate a publication.
   * 
   * @return The corresponding publication instance or <code>null</code>.
   */
  public static Publication make(File file) {
    if (file == null) return null;
    if (Document.matchesNamePattern(file)) {
      return new Publication(file);
    }
    return null;
  }

  /**
   * Extracts the name of this publication from the specified file.
   * 
   * @param file The file corresponding to the publication.
   * 
   * @return The name of the publication.
   */
  public static String toName(File file) {
    if (file == null) return null;
    String filename = file.getName();
    if (filename.toLowerCase().indexOf("general-schedule-soc") == 11)
      return "Schedule of Pharmaceutical Benefits (Summary Of Changes)";
    if (filename.toLowerCase().indexOf("dental-book-soc") == 11)
      return "Pharmaceutical Benefits for Dental Use (Summary Of Changes)";    
    if (filename.indexOf("general-schedule") == 11)
      return "Schedule of Pharmaceutical Benefits";
    if (filename.indexOf("dental-book") == 11)
      return "Pharmaceutical Benefits for Dental Use";
    // fallback on the filename
    return filename;
  }
}
