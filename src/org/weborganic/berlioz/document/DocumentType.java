package org.weborganic.berlioz.document;

import java.io.File;
import java.util.regex.Pattern;

/**
 * Defines the document types in use in a Berlioz-based web application.
 * 
 * @deprecated
 * 
 * @author Christophe Lauret (Allette Systems)
 * @version 16 August 2007
 */
public final class DocumentType {

  /**
   * FlashPaper file type, file name should end in ".swf".
   */
  public static final DocumentType FLASHPAPER = new DocumentType("FlashPaper", "[\\w\\W]*.swf\\z");
  // TODO: define regex

  /**
   * Acrobat PDF file type, file name should end in ".pdf".
   */
  public static final DocumentType ACROBAT_PDF = new DocumentType("AcrobatPDF", "[\\w\\W]*.pdf\\z");
  // TODO: define regex

  /**
   * XML file type, file name should end in ".xml".
   */
  public static final DocumentType XML = new DocumentType("XML", "[\\w\\W]*.xml\\z");
  // TODO: define regex

  /**
   * Zipped XML file type, file name should end in ".xml.zip".
   */
  public static final DocumentType ZIPPED_XML = new DocumentType("ZippedXML", "[\\w\\W]*.xml.zip\\z");  
  // TODO: define regex

  /**
   * Zipped text (ASCII) file type, file name should end in ".ascii.zip".
   */
  public static final DocumentType ZIPPED_ASCII = new DocumentType("ZippedText", "[\\w\\W]*.ascii.zip\\z");
  // TODO: define regex

  /**
   * Unknown file type - used as a fallback
   */
  public static final DocumentType UNKNOWN = new DocumentType("Unknown", "");  
  // TODO: define regex

  /**
   * The string name of the document type.
   */
  private final String type;

  /**
   * The pattern that the file name must match in order to be identified as this document type.
   */
  private final Pattern pattern;

  /**
   * Protect constructor, prevent creation of other instances.
   *
   * @param type  The type for document type.
   * @param regex The regular expression that the file must match to correspond to the document type.
   */
  private DocumentType(String type, String regex) {
    this.type = type;
    this.pattern = Pattern.compile(regex);
  }

  /**
   * Parse a type of field and return the correponding constant.
   * <p>
   * This method returns:
   * <ul>
   *   <li><code>UNINDEXED</code> if name is equal to "unindexed" in any case.</li>
   *   <li><code>TEXT</code> if name is equal to "text" in any case.</li>
   *   <li><code>UNSTORED</code> if name is equal to "unstored" in any case.</li>
   *   <li><code>KEYWORD</code> for any other value.</li>
   * </ul>
   *
   * @param name The name of the field type.
   * @return the corresponding constant.
   */
  public static DocumentType parse(String name) {
    if (name == null)  return UNKNOWN;
    else if ("".equals(name))  return UNKNOWN;
    else if (FLASHPAPER.type.equalsIgnoreCase(name)) return FLASHPAPER;
    else if (ACROBAT_PDF.type.equalsIgnoreCase(name)) return ACROBAT_PDF;
    else if (XML.type.equalsIgnoreCase(name)) return XML;
    else if (ZIPPED_ASCII.type.equalsIgnoreCase(name)) return ZIPPED_ASCII;
    else if (ZIPPED_XML.type.equalsIgnoreCase(name)) return ZIPPED_XML;
    else return UNKNOWN;
  }

  /**
   * Returns the document type corresponding to the specified file
   *
   * This method will check that the given file matches one of the patterns defined for this
   * document type, otherwise it will return UNKNOWN.
   *
   * @param file The file to analyse.
   * 
   * @return The corresponding constant.
   */
  public static DocumentType getDocumentType(File file) {
    if (file == null)  return UNKNOWN;
    String name = file.getName();
    if (FLASHPAPER.pattern.matcher(name).matches()) return FLASHPAPER;
    else if (ACROBAT_PDF.pattern.matcher(name).matches()) return ACROBAT_PDF;
    else if (XML.pattern.matcher(name).matches()) return XML;
    else if (ZIPPED_ASCII.pattern.matcher(name).matches()) return ZIPPED_ASCII;
    else if (ZIPPED_XML.pattern.matcher(name).matches()) return ZIPPED_XML;
    else return UNKNOWN;
  }

  /**
   * Returns the type of the file publication.
   * 
   * @deprecated use {@link #getDocumentType(File)}
   * 
   * @param file The file corresponding to the publication.
   * 
   * @return The effective date of the publication.
   */
  public static DocumentType toType(File file) {
    if (file == null) return null;
    String name = file.getName();
    if (name.endsWith(".swf"))       return FLASHPAPER;
    if (name.endsWith(".pdf"))       return ACROBAT_PDF;
    if (name.endsWith(".xml"))       return XML;
    if (name.endsWith(".xml.zip"))   return ZIPPED_XML;
    if (name.endsWith(".ascii.zip")) return ZIPPED_ASCII;
    return UNKNOWN;
  }

  /**
   * Return the String representation of this object.
   * 
   * {@inheritDoc}
   */
  public String toString() {
    return this.type;
  }
}
