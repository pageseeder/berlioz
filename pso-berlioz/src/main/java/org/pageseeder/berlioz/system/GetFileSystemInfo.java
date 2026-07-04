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
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.content.XmlGenerator;
import org.pageseeder.berlioz.xml.XmlWriter;

/**
 * Returns information about the underlying file system as XML.
 *
 * <p>Always reports overall free and total disk space. When the {@code details} parameter is
 * {@code "true"}, also scans the public and private folders and reports per-subdirectory file
 * counts and sizes. The {@code WEB-INF} directory is excluded from the public folder scan.
 *
 * <h3>Parameters</h3>
 * <dl>
 *   <dt>{@code details}</dt>
 *   <dd>Optional. When {@code "true"}, includes per-directory breakdown. Defaults to
 *       {@code "false"}.</dd>
 * </dl>
 *
 * <h3>Returned XML</h3>
 * <p>Without details:
 * <pre>{@code
 * <file-system free-space="[bytes]" total-space="[bytes]"/>
 * }</pre>
 * <p>With {@code details=true}:
 * <pre>{@code
 * <file-system free-space="[bytes]" total-space="[bytes]">
 *   <public total-size="[bytes]" total-count="[n]">
 *     <directory name="[name]" file-size="[bytes]" file-count="[n]"/>
 *     ...
 *   </public>
 *   <private total-size="[bytes]" total-count="[n]">
 *     <directory name="[name]" file-size="[bytes]" file-count="[n]"/>
 *     ...
 *   </private>
 * </file-system>
 * }</pre>
 *
 * <h3>Usage</h3>
 * <p>To use this generator in Berlioz (in <code>/WEB-INF/config/services.xml</code>):
 * <pre>{@code <generator class="org.pageseeder.berlioz.system.GetFileSystemInfo"
 *                         name="[name]" target="[target]"/>}</pre>
 *
 * @author Christophe Lauret
 *
 * @version 0.14.0
 * @since 0.9.32
 */
@Beta
public final class GetFileSystemInfo implements XmlGenerator {

  private static final String DETAILS_PARAMETER = "details";

  private static final String WEB_INF_DIRECTORY = "WEB-INF";

  @Override
  public Response generate(Request req, XmlWriter xml) {
    File pub = req.getEnvironment().getPublicFolder();
    File priv = req.getEnvironment().getPrivateFolder();
    xml.openElement("file-system");
    xml.attribute("free-space", pub.getFreeSpace());
    xml.attribute("total-space", pub.getTotalSpace());

    if ("true".equals(req.getParameter(DETAILS_PARAMETER))) {
      analyze(pub, "public", xml);
      analyze(priv, "private", xml);
    }

    xml.closeElement();
    return Response.ok();
  }

  /**
   * Analyzes the specified root directory, collect total file size and count information
   * for each direct subdirectory, and print it on the XML.
   *
   * @param dir   The actual directory to scan.
   * @param name  The name of the directory object gathering information.
   * @param xml   The XML writer.
   */

  private static void analyze(File dir, String name, XmlWriter xml) {
    DirInfo global = new DirInfo(name);
    List<DirInfo> locals = analyzeDirectChildren(dir.toPath(), global);
    xml.openElement(name);
    for (DirInfo local : locals) {
      global.add(local);
    }
    xml.attribute("total-size", global.getSize());
    xml.attribute("total-count", global.getCount());
    for (DirInfo local : locals) {
      xml.openElement("directory");
      xml.attribute("name", local.name());
      xml.attribute("file-size", local.getSize());
      xml.attribute("file-count", local.getCount());
      xml.closeElement();
    }
    xml.closeElement();
  }

  /**
   * Analyzes the immediate children of the specified directory.
   *
   * @param root    The directory to scan.
   * @param global  The object gathering files directly below the root.
   *
   * @return directory information for each direct subdirectory.
   */
  private static List<DirInfo> analyzeDirectChildren(Path root, DirInfo global) {
    List<DirInfo> locals = new ArrayList<>();
    if (!Files.isDirectory(root)) {
      return locals;
    }

    try (DirectoryStream<Path> children = Files.newDirectoryStream(root)) {
      for (Path child : children) {
        String childName = getFileName(child);
        if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
          if (!WEB_INF_DIRECTORY.equals(childName)) {
            DirInfo local = new DirInfo(childName);
            analyzeDirectory(local, child);
            locals.add(local);
          }
        } else {
          addFile(global, child);
        }
      }
    } catch (DirectoryIteratorException | IOException ex) {
      return locals;
    }

    locals.sort(Comparator.comparing(DirInfo::name));
    return locals;
  }

  /**
   * Analyzes the content of the specified directory.
   *
   * @param local The object gathering all the information about the directory.
   * @param dir   The actual directory to scan.
   */
  private static void analyzeDirectory(DirInfo local, Path dir) {
    try {
      Files.walkFileTree(dir, new SimpleFileVisitor<>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
          if (attrs.isRegularFile()) {
            local.add(attrs.size());
          }
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException ex) {
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path directory, IOException ex) {
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException ex) {
      // Ignore unreadable directories to keep the generator best-effort.
    }
  }

  private static void addFile(DirInfo info, Path file) {
    try {
      if (Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
        info.add(Files.size(file));
      }
    } catch (IOException ex) {
      // Ignore files that disappear or cannot be read while scanning.
    }
  }

  private static String getFileName(Path path) {
    Path filename = path.getFileName();
    return filename != null ? filename.toString() : path.toString();
  }

  /**
   * Captures essential information about a directory.
   */
  private static class DirInfo {

    /** Name of the directory */
    private final String name;

    /** Total file size in bytes (incremented for each file found) */
    private long size = 0;

    /** Total number of files (incremented for each file found) */
    private long count = 0;

    /**
     * Creates a new directory information object.
     *
     * @param name The name of the directory
     */
    public DirInfo(String name) {
      this.name = name;
    }

    /**
     * Add a file incrementing the total file size and count.
     *
     * @param fileSize The file size in bytes
     */
    public void add(long fileSize) {
      this.size = this.size + fileSize;
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
      return this.name;
    }

    /**
     * @return the total size (sum of all files in this directory and its descendants).
     */
    public long getSize() {
      return this.size;
    }

    /**
     * @return the number of files found in this directory and its descendants.
     */
    public long getCount() {
      return this.count;
    }
  }

}
