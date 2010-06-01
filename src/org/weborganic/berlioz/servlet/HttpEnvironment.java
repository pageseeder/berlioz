/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.servlet;

import java.io.File;

import org.weborganic.berlioz.GlobalSettings;
import org.weborganic.berlioz.content.Environment;

/**
 * Provides the environment common to all services. 
 *
 * @author Christophe Lauret (Weborganic)
 * 
 * @version 26 May 2010
 */
public final class HttpEnvironment implements Environment {

  /**
   * The location of the public directory.
   */
  private final File _public; 

  /**
   * The location of the public directory.
   */
  private final File _private;

  /**
   * Creates a new HTTP environment
   * 
   * @param publicd  The public directory
   * @param privated The private directory
   */
  public HttpEnvironment(File publicd, File privated) {
    this._public = publicd;
    this._private = privated;
  }

  /**
   * {@inheritDoc}
   */
  public File getPublicFolder() {
    return this._public;
  }

  /**
   * {@inheritDoc}
   */
  public File getPrivateFolder() {
    return this._private;
  }

  /**
   * {@inheritDoc}
   */
  public File getPrivateFile(String path) {
    File f = new File(this._private, path);
    return f;
  }

  /**
   * {@inheritDoc}
   */
  public File getPublicFile(String path) {
    File f = new File(this._public, path);
    return f;
  }

  /**
   * {@inheritDoc}
   */
  public String getProperty(String name) {
    return GlobalSettings.get(name);
  }

  /**
   * {@inheritDoc}
   */
  public boolean getProperty(String name, boolean def) {
    return GlobalSettings.get(name, def);
  }

  /**
   * {@inheritDoc}
   */
  public int getProperty(String name, int def) {
    return GlobalSettings.get(name, def);
  }

  /**
   * {@inheritDoc}
   */
  public String getProperty(String name, String def) {
    return GlobalSettings.get(name, def);
  }

}
