package org.weborganic.berlioz.content;

import java.io.File;

/**
 * Returns the environment for the service.
 * 
 * @author Christophe Lauret
 * @version 25 MAy 2010
 */
public interface Environment {

  /**
   * Returns the directory pointing to the public area of the Web site.
   * 
   * @return the directory pointing to the public area of the Web site.
   */
  File getPublicFolder();

  /**
   * Returns the directory pointing to the private area of the Web site (generally WEB-INF).
   * 
   * @return the directory pointing to the private area of the Web site (generally WEB-INF).
   */
  File getPrivateFolder();

  /**
   * Returns the file pointing to the public area of the Web site.
   * 
   * @param path The path of the requested file.
   * @return the requested file.
   */
  File getPublicFile(String path);

  /**
   * Returns the file pointing to the private area of the Web site (that is within WEB-INF).
   * 
   * @param path The path of the requested file.
   * @return the requested file.
   */
  File getPrivateFile(String path);

  /**
   * Return the requested property.
   *
   * <p>Returns <code>null</code> if the property is not found or defined.
   *
   * @param name  the name of the property
   * @return  the property value or <code>null</code>.
   */
  String getProperty(String name);

  /**
   * Returns the requested property or it default value.
   *
   * <p>The given default value is returned only if the property is not found.
   *
   * @param name  The name of the property.
   * @param def   A default value for the property.
   *
   * @return  the property value or the default value.
   */
  String getProperty(String name, String def);

  /**
   * Returns the requested int property or it default value.
   *
   * @param name  The name of the property.
   * @param def   A default value for the property.
   *
   * @return  the property value or the default value.
   */
  int getProperty(String name, int def);

  /**
   * Returns the requested property or it default value.
   *
   * <p>The given default value is returned only if the property is not found.
   *
   * @param name  The name of the property.
   * @param def   A default value for the property.
   *
   * @return  the property value or the default value.
   */
  boolean getProperty(String name, boolean def);

}
