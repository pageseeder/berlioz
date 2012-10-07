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
 * @author Christophe Lauret (Weborganic)
 * @version Berlioz 0.9.3 - 9 December 2011
 * @since Berlioz 0.9
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
   * The cache control directives for the environment (may be overridden by individual services)
   */
  private final String _cacheControl;

  /**
   * Creates a new HTTP environment.
   *
   * @param publicDir    The public directory.
   * @param privateDir   The private directory.
   * @param cacheControl The default cache control directive.
   */
  public HttpEnvironment(File publicDir, File privateDir, String cacheControl) {
    this._public = publicDir;
    this._private = privateDir;
    this._cacheControl = cacheControl;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public File getPublicFolder() {
    return this._public;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public File getPrivateFolder() {
    return this._private;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public File getPrivateFile(String path) {
    File f = new File(this._private, path);
    return f;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public File getPublicFile(String path) {
    File f = new File(this._public, path);
    return f;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getProperty(String name) {
    return GlobalSettings.get(name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean getProperty(String name, boolean def) {
    return GlobalSettings.get(name, def);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getProperty(String name, int def) {
    return GlobalSettings.get(name, def);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getProperty(String name, String def) {
    return GlobalSettings.get(name, def);
  }

  /**
   *
   */
  public String getCacheControl() {
    return this._cacheControl;
  }
}
