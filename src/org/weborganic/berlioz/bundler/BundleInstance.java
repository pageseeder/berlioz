/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.bundler;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.weborganic.berlioz.content.Service;

/**
 * Represents an actual instance of a bundle.
 *
 * <p>A bundle instance is specific to a service.
 *
 * @author christophe Lauret
 *
 * @version Berlioz 0.9.32
 * @since Berlioz 0.9.32
 */
final class BundleInstance implements Serializable {

  /** As per requirement for Serializable. */
  private static final long serialVersionUID = -4507097239658105026L;

  /**
   * The name of this instance, "global", the name of a group or service.
   */
  private final String _name;

  /**
   * The list of paths from the web root for that instance.
   */
  private final String[] _paths;

  /**
   * The list of files in that instance.
   */
  private final File[] _files;

  /**
   * Creates the list of paths
   *
   * @param name  The name of this bundle.
   * @param paths The paths to the files for that bundle.
   * @param root  The root of the website (paths are relative to the root)
   */
  private BundleInstance(String name, String[] paths, File root) {
    this._name = name;
    this._paths = paths;
    this._files = new File[paths.length];
    for (int i = 0; i < this._paths.length; i++) {
      this._files[i] = new File(root, this._paths[i]);
    }
  }

  /**
   * @return The name of this instance, "global", the name of a group or service id.
   */
  public String name() {
    return this._name;
  }

  /**
   * Return the bundle file for this instance.
   *
   * @param config the bundle configuration
   * @return the corresponding file
   */
  public File getBundleFile(BundleConfig config) {
    List<File> files = listExistingFiles();
    WebBundleTool bundler = config.bundler();
    File bundle = null;
    try {
      if (config.type() == BundleType.JS) {
        bundle = bundler.bundleScripts(files, this._name, config.minimize());
      } else if (config.type() == BundleType.CSS) {
        bundle = bundler.bundleStyles(files, this._name, config.minimize());
      }
    } catch (IOException ex) {
      // TODO Report something!
    }
    return bundle;
  }

  /**
   * Updates the specified list of paths to include only the paths which correspond to
   * existing files.
   *
   * @param paths the list of paths to update.
   */
  public void addToExistingPaths(List<String> paths) {
    if (paths == null) return;
    for (int i = 0; i < this._files.length; i++) {
      if (this._files[i].exists()) {
        paths.add(this._paths[i]);
      }
    }
  }

  /**
   * Returns all the existing paths for this bundle.
   *
   * @return a list of existing paths
   */
  public List<String> listExistingPaths() {
    return computePaths(this._paths, this._files);
  }

  /**
   * Returns all the existing paths for this bundle.
   *
   * @return a list of existing files
   */
  public List<File> listExistingFiles() {
    return computeFiles(this._files);
  }

  /**
   * Instantiate a bundle definition for a service.
   *
   * @param config     The bundle configuration
   * @param definition The bundle definition to instantiate.
   * @param service    The service for which the definition is instantiated.
   *
   * @return The corresponding instance.
   */
  public static BundleInstance instantiate(BundleConfig config, BundleDefinition definition, Service service) {
    String name = replaceTokens(definition.filename(), service);
    int count = definition.paths().length;
    String[] paths = new String[count];
    for (int i = 0; i < count; i++) {
      paths[i] = replaceTokens(definition.paths()[i], service);
    }
    return new BundleInstance(name, paths, config.root());
  }

  /**
   * Returns the files in the bundle filtering out files which do not exist and automatically replacing tokens.
   *
   * @param paths The list of paths
   * @param files The list of files corresponding to the specified list of paths
   *
   * @return the list of paths to the files to bundles.
   */
  private static List<String> computePaths(String[] paths, File[] files) {
    if (paths == null) return Collections.emptyList();
    if (paths.length > 1) {
      // multiple paths specified
      List<String> existing = new ArrayList<String>(paths.length);
      for (int i = 0; i < files.length; i++) {
        if (files[i].exists()) {
          existing.add(paths[i]);
        }
      }
      return existing;
    } else if (paths.length == 1) {
      // only one path
      if (files[0].exists()) return Collections.singletonList(paths[0]);
    }
    return Collections.emptyList();
  }

  /**
   * Returns the files in the bundle filtering out files which do not exist and automatically replacing tokens.
   *
   * @param files The list of files to check.
   *
   * @return the list of files to bundle.
   */
  private static List<File> computeFiles(File[] files) {
    if (files == null) return Collections.emptyList();
    if (files.length > 1) {
      // multiple paths specified
      List<File> existing = new ArrayList<File>(files.length);
      for (File f : files) {
        if (f.exists()) {
          existing.add(f);
        }
      }
      return existing;
    } else if (files.length == 1) {
      // only one paths
      if (files[0].exists()) return Collections.singletonList(files[0]);
    }
    return Collections.emptyList();
  }

  /**
   * Replaces the tokens in the string.
   *
   * @param value   The value containing tokens to be replaced.
   * @param service The service.
   *
   * @return The corresponding value with all tokens replaced.
   */
  private static String replaceTokens(String value, Service service) {
    String out = value;
    while (out.contains("{GROUP}"))   {
      out = out.replace("{GROUP}", service.group());
    }
    while (out.contains("{SERVICE}")) {
      out = out.replace("{SERVICE}", service.id());
    }
    return out;
  }

  /**
   * Returns the date of the most recent file.
   * @param files the list of files.
   * @return the date of the most recent.
   */
  private static long getMostRecent(List<File> files) {
    long mostRecent = 0;
    for (File f : files) {
      if (f.lastModified() > mostRecent) {
        mostRecent = f.lastModified();
      }
    }
    return mostRecent;
  }
}
