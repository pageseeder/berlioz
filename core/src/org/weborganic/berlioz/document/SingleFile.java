package org.weborganic.berlioz.document;

import java.io.File;
import java.io.IOException;

import com.topologi.diffx.xml.XMLWritable;
import com.topologi.diffx.xml.XMLWriter;

/**
 * A Single File object.
 * 
 * @author William Liem (Allette Systems)
 * 
 * @version 28 January 2009
 */
public final class SingleFile extends Document implements XMLWritable {

  private String _path;
  
  /**
   * Creates a new document from the specified file.
   * 
   * @param file The file.
   */
  private SingleFile(File file) {
    super(file, file.getName());
  }

  /**
   * {@inheritDoc}
   */
  public void toXML(XMLWriter xml) throws IOException {
    // the actual file
    xml.openElement("file");
    xml.attribute("file", this.getFileName());
    xml.attribute("type", this.getType().toString());
    xml.attribute("size", this.getSize());
    xml.attribute("path", this.getPath());
    xml.closeElement();
  }

  // static helpers -------------------------------------------------------------------------------

  public String getPath() {
	return this._path;  
  }
  
  public void setPath(String path) {
		this._path = path;  
  }
	  
  /**
   * Create a publication instance 
   * 
   * @param file The file to use to instantiate a publication.
   * @param pattern The pattern match to check before creating the file.
   * 
   * @return The corresponding publication instance or <code>null</code>.
   */
  public static SingleFile make(File file, String pattern) {
    if (file == null) return null;
    if (Document.matchesNamePattern(file, pattern)) {
      return new SingleFile(file);
    }
    return null;
  }

  /**
   * Create a publication instance 
   * 
   * @param file The file to use to instantiate a publication.
   * 
   * @return The corresponding publication instance or <code>null</code>.
   */
  public static SingleFile make(File file) {
    if (file == null) return null;
    //@TODO: since no checking is done, need to do more vigorous testing.
    return new SingleFile(file);
  }
}
