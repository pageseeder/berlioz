package org.weborganic.berlioz.document;

/**
 * Indicates that implementations hold metadata. 
 * 
 * @author Christophe Lauret (Allette Systems)
 * 
 * @version 16 August 2007
 */
public interface MetadataHolder {

  /**
   * Returns the title of the page, document or folder.
   * 
   * @return The title of the page, document or folder.
   */
  String getTitle();

  /**
   * Returns the description of the page, document or folder.
   * 
   * @return The description of the page, document or folder.
   */
  String getDescription();

  /**
   * Sets the title for the page, document or folder.
   * 
   * @param title The title of the page, document or folder.
   */
  void setTitle(String title);

  /**
   * Sets the description for the page, document or folder.
   * 
   * @param description The description of the page, document or folder.
   */ 
  void setDescription(String description);

  /**
   * Indicates whether the metadata for the object have been loaded.
   * 
   * @return <code>true</code> if the metadata is loaded;
   *         <code>false</code> otherwise.
   */
//  boolean isLoaded();

}
