/*
 * Copyright (c) 1999-2012 weborganic systems pty. ltd.
 */
package org.weborganic.berlioz.servlet;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.weborganic.berlioz.Beta;

/**
 * A simple war or zip file which can be unpack on top of the existing application.
 *
 * <p>A simple way to modularise aspect of the app.
 *
 * @author Christophe Lauret
 * @version Berlioz 0.9.22 - 29 November 2013
 * @since Berlioz 0.9.22
 */
@Beta
final class Overlay {

  /** Buffer when unzipping */
  private static final int BUFFER = 4096;

  /**
   * The war or zip file.
   */
  private final File _source;

  /**
   * Create a new overlay.
   *
   * @param source The war or zip file.
   */
  private Overlay(File source) {
    this._source = source;
  }

  /**
   * Look for overlays in the WEB-INF/overlays/ directory
   *
   * @param root The application root (context)
   * @return The list of overlays if any, never <code>null</code>
   */
  public static List<Overlay> list(final File root) {
    File webinfPath = new File(root, "WEB-INF");
    File overlays = new File(webinfPath, "overlays");
    if (overlays.exists() && overlays.isDirectory()) {
      File[] files = overlays.listFiles(new FileFilter() {
        @Override
        public boolean accept(File pathname) {
          String name = pathname.getName();
          return name.endsWith(".war") || name.endsWith(".zip") || name.endsWith(".jar");
        }
      });
      List<Overlay> list = new ArrayList<Overlay>();
      for (File f : files) {
        // TODO: check for multiple versions of the same overlay
        list.add(new Overlay(f));
      }
      return list;
    } else {
      return Collections.emptyList();
    }
  }

  /**
   * @return the source file
   */
  public File getSource() {
    return this._source;
  }

  /**
   * Unzip the the file at the specified location.
   *
   * @param root The root of the web application (context path)
   *
   * @return the number of file that have been unpacked
   *
   * @throws IOException Should any error occur.
   */
  public int unpack(final File root) throws IOException {
    BufferedOutputStream out = null;
    BufferedInputStream is = null;
    int unpacked = 0;
    try {
      ZipEntry entry;
      ZipFile zip = new ZipFile(this._source);
      for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements();) {
        entry = e.nextElement();
        String name = entry.getName();
        // Ignore any file in the META-INF folder
        if (name.startsWith("META-INF")) continue;
        // Ensure that the folder exists
        if (name.indexOf('/') > 0) {
          String folder = name.substring(0, name.lastIndexOf('/'));
          File dir = new File(root, folder);
          if (!dir.exists()) {
            dir.mkdirs();
          }
        }
        // Only process files
        if (!entry.isDirectory()) {
          File f = new File(root, name);
          if (!f.exists() || f.length() != entry.getSize()) {
            is = new BufferedInputStream(zip.getInputStream(entry));
            int count;
            byte[] data = new byte[BUFFER];
            FileOutputStream fos = new FileOutputStream(f);
            try {
              out = new BufferedOutputStream(fos, BUFFER);
              while ((count = is.read(data, 0, BUFFER)) != -1) {
                out.write(data, 0, count);
              }
              out.flush();
            } finally {
              out.close();
            }
            is.close();
            unpacked++;
          }
        }
      }
    } finally {
      if (is != null) is.close();
      if (out != null) out.close();
    }
    return unpacked;
  }
}
