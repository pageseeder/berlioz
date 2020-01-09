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
package org.pageseeder.berlioz.system;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.xmlwriter.XMLWriter;

/**
 * Returns information about the underlying file system.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.32
 * @since Berlioz 0.9.32
 */
@Beta
public final class GetFileSystemInfo implements ContentGenerator {

  @Override
  public void process(ContentRequest req, XMLWriter xml) throws IOException {

    // Free and total space
    File pub = req.getEnvironment().getPublicFolder();
    File priv = req.getEnvironment().getPrivateFolder();
    xml.openElement("file-system");
    xml.attribute("free-space", Long.toString(pub.getFreeSpace()));
    xml.attribute("total-space", Long.toString(pub.getTotalSpace()));

    if ("true".equals(req.getParameter("details"))) {
      // Go through public and private folders
      analyze(pub, "public", xml);
      analyze(priv, "private", xml);
    }

    xml.closeElement();
  }

  /**
   * Analyzes the specified root directory, collect total file size and count information
   * for each direct subdirectory and print it on the XML.
   *
   * @param dir   The actual directory to scan.
   * @param name  The name of the directory object gathering information.
   * @param xml   Whether
   *
   * @throws IOException if thrown while writing the XML.
   */
  private static void analyze(File dir, String name, XMLWriter xml) throws IOException {
    DirInfo global = new DirInfo(name);
    List<DirInfo> locals = new ArrayList<>();
    File[] files = dir.listFiles();
    // TODO Use Files.walkFileTree instead
    if (files != null) {
      for (File f : files) {
        if (f.isDirectory()) {
          if (!"WEB-INF".equals(f.getName())) {
            DirInfo local = new DirInfo(f.getName());
            analyze(local, f);
            locals.add(local);
          }
        } else {
          global.add(f);
        }
      }
    }
    xml.openElement(name);
    for (DirInfo local : locals) {
      global.add(local);
    }
    xml.attribute("total-size", Long.toString(global.getSize()));
    xml.attribute("total-count", global.getCount());
    for (DirInfo local : locals) {
      xml.openElement("directory");
      xml.attribute("name", local.name());
      xml.attribute("file-size", Long.toString(local.getSize()));
      xml.attribute("file-count", local.getCount());
      xml.closeElement();
    }
    xml.closeElement();
  }

  /**
   * A recursive method analyzing the content of the specified directory
   *
   * @param local The object gathering all the information about the directory.
   * @param dir   The actual directory to scan.
   */
  private static void analyze(DirInfo local, File dir) {
    File[] files = dir.listFiles();
    if (files != null) {
      for (File f : files) {
        if (f.isDirectory()) {
          analyze(local, f);
        } else {
          local.add(f);
        }
      }
    }
  }

  /**
   * Captures essential information about a directory.
   *
   * @author Christophe Lauret
   * @version 4 February 2013
   */
  private static class DirInfo {

    /** Name of the directory */
    private final String _name;

    /** Total file size in bytes (incremented for each file found) */
    private long size = 0;

    /** Total number of files (incremented for each file found) */
    private int count = 0;

    /**
     * Creates a new directory information object.
     *
     * @param name The name of the directory
     */
    public DirInfo(String name) {
      this._name = name;
    }

    /**
     * Add a file incrementing the total file size and count.
     *
     * @param f The file to add (must not be a directory)
     */
    public void add(File f) {
      this.size = this.size + f.length();
      this.count++;
    }

    /**
     * Add a directory incrementing the total file size and count.
     *
     * @param info The directory to add
     */
    public void add(DirInfo info) {
      this.size = this.size + info.getSize();
      this.count = this.count + info.getCount();
    }

    /**
     * @return the name of the directory.
     */
    public String name() {
      return this._name;
    }

    /**
     * @return the total size (sum of all files in this directory and its descendants).
     */
    public long getSize() {
      return this.size;
    }

    /**
     * @return the number of files found in this directory and its descendants).
     */
    public int getCount() {
      return this.count;
    }
  }

}
