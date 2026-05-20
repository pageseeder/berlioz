/*
 * Copyright 2015 Allette Systems (Australia)
 * http://www.allette.com.au
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pageseeder.berlioz.bundler;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.pageseeder.berlioz.content.Service;

/**
 * Represents an actual instance of a bundle.
 *
 * <p>A bundle instance is specific to a service.
 *
 * @author christophe Lauret
 *
 * @version Berlioz 0.13.0
 * @since Berlioz 0.9.32
 */
final class BundleInstance implements Serializable {

  /** As per requirement for Serializable. */
  private static final long serialVersionUID = -2444096252988163408L;

  /**
   * The name of this instance, "global", the name of a group or service.
   */
  private final String name;

  /**
   * The list of paths from the web root for that instance.
   */
  private final String[] paths;

  /**
   * The list of files in that instance.
   */
  private final File[] files;

  /**
   * Creates the list of paths
   *
   * @param name  The name of this bundle.
   * @param paths The paths to the files for that bundle.
   * @param root  The root of the website (paths are relative to the root)
   */
  private BundleInstance(String name, String[] paths, File root) {
    this.name = name;
    this.paths = paths;
    this.files = new File[paths.length];
    for (int i = 0; i < this.paths.length; i++) {
      this.files[i] = new File(root, this.paths[i]);
    }
  }

  /**
   * @return The name of this instance, "global", the name of a group or service id.
   */
  public String name() {
    return this.name;
  }

  /**
   * Return the bundle file for this instance.
   *
   * @param config the bundle configuration
   * @return the corresponding file or <code>null</code> if an error occurred
   */
  public @Nullable File getBundleFile(BundleConfig config) {
    List<File> existingFiles = listExistingFiles();
    WebBundleTool bundler = config.bundler();
    File bundle = null;
    try {
      if (config.type() == BundleType.JS) {
        bundle = bundler.bundleScripts(existingFiles, this.name, config.minimize());
      } else if (config.type() == BundleType.CSS) {
        bundle = bundler.bundleStyles(existingFiles, this.name, config.minimize());
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
    for (int i = 0; i < this.files.length; i++) {
      if (this.files[i].exists()) {
        paths.add(this.paths[i]);
      }
    }
  }

  /**
   * Returns all the existing paths for this bundle.
   *
   * @return a list of existing paths
   */
  public List<String> listExistingPaths() {
    return computePaths(this.paths, this.files);
  }

  /**
   * Returns all the existing paths for this bundle.
   *
   * @return a list of existing files
   */
  public List<File> listExistingFiles() {
    return computeFiles(this.files);
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
      List<String> existing = new ArrayList<>(paths.length);
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
      List<File> existing = new ArrayList<>(files.length);
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
