package org.weborganic.berlioz.document;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A base class for documents published on a Berlioz-base website.
 * 
 * @author Christophe Lauret (Weborganic)
 * @version 9 October 2009
 */
public class Document {

  /**
   * All publication file names must match this pattern.
   */
  protected static final String NAME_PATTERN = "\\d{4}-\\d{2}-\\d{2}-(\\w|\\-)*(\\.xml|\\.ascii)?\\.(pdf|swf|xml|zip)";

  /**
   * The date format used for date values.
   */
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

  /**
   * The file name to the application
   */
  private final File _file;

  /**
   * The name of the document.
   */
  private final String _name;

  /**
   * The date of the document.
   */
  private final Date _date;

  /**
   * The type of file.
   */
  private final DocumentType _type;

  /**
   * The file size in Kb.
   */
  private final int _size;

  /**
   * Creates a new document from the specified file.
   * 
   * @param file The file.
   */
  protected Document(File file) {
    this._file = file;
    this._name = file.getName();
    this._date = toDate(file);
    this._size = toSize(file);
    this._type = DocumentType.getDocumentType(file);
  }

  /**
   * Creates a new document from the specified file.
   * 
   * @param file The file.
   * @param name The name of the document.
   */
  protected Document(File file, String name) {
    this._file = file;
    this._name = name;
    this._date = toDate(file);
    this._size = toSize(file);
    this._type = DocumentType.getDocumentType(file);
  }

  /**
   * Returns the file.
   * 
   * @return the file.
   */
  public final File getFile() {
    return this._file;
  }

  /**
   * Returns the name of the file.
   * 
   * @return the name of the file.
   */
  public final String getFileName() {
    return this._file.getName();
  }

  /**
   * Returns the name of the document.
   * 
   * @return the name of the document.
   */
  public final String getName() {
    return this._name;
  }

  /**
   * Returns the type of the document.
   * 
   * @return the type of the document.
   */
  public final DocumentType getType() {
    return this._type;
  }

  /**
   * Returns the size of the document
   * 
   * @return the type of the document.
   */
  public final int getSize() {
    return this._size;
  }

  /**
   * Returns the date of the document
   * 
   * @return the date of the document.
   */
  public final Date getDate() {
    return (this._date != null)? new Date(this._date.getTime()) : null;
  }

  // static helpers -------------------------------------------------------------------------------

  /**
   * Indicates whether the specified file name matches the specified name pattern.
   * 
   * @param file The file to check.
   * 
   * @return <code>true</code> if the file matches the specified name pattern;
   *         <code>false</code> otherwise.
   */
  public static boolean matchesNamePattern(File file) {
    if (file == null) return false;
    return file.getName().matches(NAME_PATTERN);
  }

  /**
   * Indicates whether the specified file name matches the parameter name pattern.
   * 
   * @param file The file to check.
   * @param pattern The pattern to match with. 
   * 
   * @return <code>true</code> if the file matches the parameter name pattern;
   *         <code>false</code> otherwise.
   */
  public static boolean matchesNamePattern(File file, String pattern) {
    if (file == null) return false;
    return file.getName().matches(pattern);
  }
  
  /**
   * Extracts the date of this publication from the specified file.
   * 
   * @param file The file corresponding to the publication.
   * 
   * @return The effective date of the publication.
   */
  public static final Date toDate(File file) {
    if (file == null) return null;
    if (file.getName().length() < 10) return null;
    try {
      return DATE_FORMAT.parse(file.getName().substring(0, 10));
    } catch (Exception ex) {
      return null;
    }
  }
  
  /**
   * Extracts the date of this publication from the specified file name string.
   * 
   * @param file The file corresponding to the publication in String representation.
   * 
   * @return The effective date of the publication.
   */
  public static final Date toDate(String file) {
    if (file == null) return null;
    if (file.length() < 10) return null;
    try {
      return DATE_FORMAT.parse(file.substring(0, 10));
    } catch (Exception ex) {
      return null;
    }
  }

  /**
   * Calculates the size of the file publication.
   * 
   * @param file The file corresponding to the publication.
   * 
   * @return The effective date of the publication.
   */
  public static final int toSize(File file) {
    if (file == null) return -1;
    long length = file.length();
    length /= 1024;
    return (int)length;
  }

}
