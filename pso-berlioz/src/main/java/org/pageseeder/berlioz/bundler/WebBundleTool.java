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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bundles web resources together to reduce the number of resources requested by a browser.
 *
 * <p>A tool instance writes bundles to a single output directory. CSS bundle freshness is cached across tool
 * instances because CSS imports affect the generated filename; refreshes for the same bundle key are synchronized
 * so that concurrent requests do not mutate the cached bundle state at the same time.
 *
 * @author Christophe Lauret
 *
 * @version 0.11.2
 * @since 0.9.32
 */
public final class WebBundleTool {

  /**
   * Logger for this class.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(WebBundleTool.class);

  /**
   * Matches URL references in CSS.
   */
  private static final Pattern CSS_URL = Pattern.compile("url\\(([^)]*)\\)");

  /**
   * Stores bundles instances to check for freshness.
   */
  private static final ConcurrentMap<String, WebBundle> instances = new ConcurrentHashMap<>();

  /**
   * The maximum size for turning the content of an image into a data URI.
   */
  private static final long DATA_URI_MAX_SIZE = 4096L;

  // class attributes
  // ----------------------------------------------------------------------------------------------

  /**
   * Where the bundles should be saved
   */
  private final File bundles;

  /**
   * The virtual location of the bundles (to calculate the relative path).
   */
  private File virtual;

  /**
   * The maximum size for converting the content of an image in CSS into a data URI.
   */
  private volatile long dataURIThreshold = DATA_URI_MAX_SIZE;

  /**
   * Creates a new Resource Bundler saving the bundles in the specified location.
   *
   * @param bundles Where the bundles should be saved (must exist and be a directory).
   *
   * @throws NullPointerException If the specified file is <code>null</code>.
   * @throws IllegalArgumentException If the specified file does not exist or is not a directory.
   */
  public WebBundleTool(File bundles) {
    this.virtual = Objects.requireNonNull(bundles, "The location where bundles are saved must not be null");
    this.bundles = checkBundlesFile(bundles);
  }

  private static File checkBundlesFile(File bundles) {
    if (!bundles.exists())
      throw new IllegalArgumentException("The location where bundles are saved must exist: "+bundles);
    if (!bundles.isDirectory())
      throw new IllegalArgumentException("The location where bundles are saved must be a directory: "+bundles);
    return bundles;
  }

  /**
   * Sets the virtual location of the generated bundles.
   *
   * <p>The virtual location is used to rewrite relative CSS URLs from their original stylesheet location to their
   * location relative to the generated bundle.
   *
   * @param virtual the virtual location of the bundles.
   */
  public void setVirtual(File virtual) {
    this.virtual = Objects.requireNonNull(virtual, "The virtual location of bundles must not be null");
  }

  /**
   * Sets the threshold for data URIs.
   *
   * <p>Images smaller than this threshold may be embedded as data URIs when CSS URLs are expanded. A threshold of
   * zero or less disables this optimization.
   *
   * @param threshold the threshold to set
   */
  public void setDataURIThreshold(long threshold) {
    this.dataURIThreshold = threshold;
  }

  /**
   * Returns the directory where generated bundles are stored.
   *
   * @return where the bundles are being stored.
   */
  public File getBundlesDir() {
    return this.bundles;
  }

  /**
   * Returns the bundle file that would be used for the specified list of files.
   *
   * <p>This method does not generate the bundle; it only calculates the filename from the supplied files.
   *
   * @param files    The list of files to bundle together.
   * @param prefix   The prefix for the bundle.
   * @param minimize Whether to minimize the files.
   *
   * @return The file corresponding to the generated bundle.
   */
  public @Nullable File getBundle(List<File> files, String prefix, boolean minimize) {
    if (files.isEmpty()) return null;
    String filename = new WebBundle(prefix, files, minimize).getFileName();
    return new File(this.bundles, filename);
  }

  /**
   * Bundles the specified files together by automatically detecting the bundle type from the first file extension.
   *
   * @param files The list of files to bundle together.
   * @param name  The name of the bundle.
   * @param minimize Whether to minimize the files.
   *
   * @return The file corresponding to the generated bundle.
   *
   * @throws IOException should an error occur while reading the files or writing the bundle.
   */
  public @Nullable File bundle(List<File> files, String name, boolean minimize) throws IOException {
    if (files.isEmpty()) return null;
    String ext = getExtension(files.get(0));
    for (BundleType t : BundleType.values()) {
      if (ext != null && t.matches(ext)) return bundle(files, name, t, minimize);
    }
    // no matching type
    return null;
  }

  /**
   * Bundles the specified files together using the requested bundle type.
   *
   * @param files The list of files to bundle together.
   * @param name  The name of the bundle.
   * @param type  The type of bundling.
   * @param minimize Whether to minimize the files.
   *
   * @return The file corresponding to the generated bundle.
   *
   * @throws IOException should an error occur while reading the files or writing the bundle.
   */
  public @Nullable File bundle(List<File> files, String name, BundleType type, boolean minimize) throws IOException {
    switch (type) {
      case JS : return bundleScripts(files, name, minimize);
      case CSS : return bundleStyles(files, name, minimize);
      default : return null;
    }
  }

  /**
   * Bundles the specified JavaScript files together.
   *
   * @param files    The list of files to bundle together.
   * @param name     The name of the bundle.
   * @param minimize Whether to minimize the scripts.
   *
   * @return The file corresponding to the generated bundle.
   *
   * @throws IOException should an error occur while reading the files or writing the bundle.
   */
  public @Nullable File bundleScripts(List<File> files, String name, boolean minimize) throws IOException {
    if (files.isEmpty()) return null;
    // Generate the hash value based on the filename, length and last modified date
    File bundle = getBundle(files, name, minimize);
    // concatenate the content if the file does not already exist
    if (bundle != null && !bundle.exists()) {
      LOGGER.debug("Generating bundle:{} with {} files", bundle.getName(), files.size());
      concatenate(files, bundle, minimize);
      bundle.deleteOnExit();
    }
    return bundle;
  }

  /**
   * Bundles the specified CSS files together.
   *
   * <p>Relative CSS {@code @import} rules are expanded into the generated bundle and relative {@code url(...)}
   * references are rewritten from the original stylesheet location to the generated bundle location.
   *
   * @param files    The list of files to bundle together.
   * @param name     The name of the bundle
   * @param minimize <code>true</code> to minimize the CSS;
   *                 <code>false</code> to simply concatenate.
   *
   * @return The file corresponding to the generated bundle.
   *
   * @throws IOException should an error occur while reading the files or writing the bundle.
   */
  public @Nullable File bundleStyles(List<File> files, String name, boolean minimize) throws IOException {
    if (files.isEmpty()) return null;
    String key = cacheKey(name, files, minimize);
    WebBundle bundle = instances.computeIfAbsent(key, unused -> new WebBundle(name, files, minimize));
    synchronized (bundle) {
      String filename = bundle.getFileName();
      File file = new File(this.bundles, filename);

      // Concatenate the content if the file does not already exist or if the cached bundle is stale.
      if (!bundle.isFresh() || !file.exists()) {
        LOGGER.debug("Generating bundle:{} with {} files", filename, files.size());

        bundle.clearImport();
        StringWriter writer = new StringWriter();
        expandStyles(bundle, writer, new File(this.virtual, file.getName()), minimize, this.dataURIThreshold);
        bundle.getETag(true);
        filename = bundle.getFileName();

        StringReader reader = new StringReader(writer.toString());
        file = new File(this.bundles, filename);
        if (minimize && bundle.isCSSMinimizable()) {
          CSSMin.minimize(reader, new FileOutputStream(file));
        } else {
          copyTo(reader, new FileOutputStream(file));
        }
        file.deleteOnExit();
      }
      return file;
    }
  }

  // Private helpers
  // ----------------------------------------------------------------------------------------------

  /**
   * Concatenate the contents of each file in the bundle.
   *
   * @param files    The list of files to concatenate.
   * @param bundle   The bundle to write to.
   * @param minimize <code>true</code> to minimize the script;
   *                 <code>false</code> otherwise.
   *
   * @throws IOException if an input/output error occurs.
   */
  private static void concatenate(List<File> files, File bundle, boolean minimize) throws IOException {
    // Copy the input stream to the output stream
    try (FileOutputStream out = new FileOutputStream(bundle)) {
      for (File f : files) {
        if (minimize && !f.getName().endsWith(".min.js")) {
          minimizeAndCopyTo(f, out);
        } else {
          copyTo(f, out);
        }
      }
    }
  }

  /**
   * Concatenate the contents of each file in the bundle.
   *
   * @param bundle    The list of files to concatenate.
   * @param writer    The bundle to write to.
   * @param virtual   The virtual location of the bundle.
   * @param minimize  Whether to minimize the expanded styles.
   * @param threshold The threshold for data URIs
   *
   * @throws IOException if an input/output error occurs.
   */
  protected static void expandStyles(WebBundle bundle, Writer writer, File virtual, boolean minimize, long threshold) throws IOException {

    // Copy the input stream to the output stream
    IOException exception = null;
    List<File> processed = new ArrayList<>();
    for (File f : bundle.files()) {
      exception = expandStylesTo(bundle, f, virtual, writer, processed, minimize, threshold);
      writer.write('\n'); // insert new line
    }

    // Re-throw any exception that has occurred
    if (exception != null) throw exception;
  }

  /**
   * Returns a cache key for a CSS bundle.
   *
   * @param name     the bundle name.
   * @param files    the files included in the bundle.
   * @param minimize whether the generated bundle is minimized.
   *
   * @return the cache key.
   */
  private static String cacheKey(String name, List<File> files, boolean minimize) {
    return name + '|' + minimize + '|' + WebBundle.id(files);
  }

  /**
   * Copy the contents of the specified input stream to the specified output stream and ensure that both streams are
   * closed before returning (even in the face of an exception).
   *
   * @param file The file to read.
   * @param out  Where the output goes to.
   *
   * @throws IOException if an input/output error occurs
   */
  private static void minimizeAndCopyTo(File file, OutputStream out) throws IOException {
    try (FileInputStream input = new FileInputStream(file)) {
      try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
        JSMin minimizer = new JSMin(input, buffer);
        minimizer.jsmin();
        byte[] min = buffer.toByteArray();
        out.write(min);
        out.write('\n');
        out.flush();
      } catch (ParsingException ex) {
        LOGGER.warn("Unable to minimize {}: {}", file.getName(), ex.getMessage());
        copyTo(file, out);
      }
    }
  }

  /**
   * Copy the contents of the specified file to the specified output stream and ensure that both streams are
   * closed before returning (even in the face of an exception).
   *
   * @param file The file to read.
   * @param out  Where the output goes to.
   *
   * @throws IOException if an input/output error occurs
   */
  private static void copyTo(File file, OutputStream out) throws IOException {
    try (BufferedReader reader = newBufferedReader(file)){
      OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
      String line = reader.readLine();
      while (line != null) {
        writer.append(line);
        writer.append('\n');
        line = reader.readLine();
      }
      writer.flush();
    }
  }

  /**
   * Copy the contents of the specified file to the specified output stream and ensure that both streams are
   * closed before returning (even in the face of an exception).
   *
   * @param reader The file to read.
   * @param out  Where the output goes to.
   *
   * @throws IOException if an input/output error occurs
   */
  private static void copyTo(StringReader reader, OutputStream out) throws IOException {
    OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
    char[] buffer = new char[1024];
    while (true) {
      int length = reader.read(buffer);
      if (length < 0) {
        break;
      }
      writer.write(buffer, 0, length);
    }
    writer.flush();
  }

  /**
   * Returns the extension of the specified file.
   *
   * @param file the file which extension is needed.
   * @return the extension or <code>null</code> if none available.
   */
  private static @Nullable String getExtension(File file) {
    int dot = file.getName().lastIndexOf('.');
    return dot >= 0? file.getName().substring(dot) : null;
  }

  /**
   * Copy the contents of the specified input stream to the specified output stream and ensure that both streams are
   * closed before returning (even in the face of an exception).
   *
   * @param bundle    The bundle being processed.
   * @param file      The file to read.
   * @param virtual   The location of the bundle (virtual).
   * @param out       Writes the content for the bundle.
   * @param processed The list of files already processed to prevent circular references.
   * @param threshold the file size threshold for images not to be included using data URIs
   *
   * @return IOException if an input/output error occurs
   *
   * @throws IOException if unable to read the file.
   */
  private static @Nullable IOException expandStylesTo(WebBundle bundle, File file, File virtual, Writer out, List<File> processed, boolean minimize, long threshold)
      throws IOException {
    // prevent circular references
    if (processed.contains(file)) return null;
    processed.add(file);
    // start processing
    IOException exception = null;
    try (BufferedReader reader = newBufferedReader(file)) {
      String line = reader.readLine();
      while (line != null) {
        IOException lineException = expandStyleLine(bundle, file, virtual, out, line, minimize, threshold, processed);
        if (exception == null) {
          exception = lineException;
        }
        // Always end with a new line
        out.write('\n');
        line = reader.readLine();
      }
    } catch (IOException ex) {
      exception = ex;
    }
    return exception;
  }

  /**
   * Expands a single CSS line.
   *
   * @param bundle    the bundle being generated.
   * @param file      the CSS file being processed.
   * @param virtual   the virtual location of the generated bundle.
   * @param out       the writer receiving CSS content.
   * @param line      the CSS line to expand.
   * @param minimize  whether the generated bundle is minimized.
   * @param threshold the data URI threshold.
   * @param processed the list of already processed files.
   *
   * @return an exception raised while expanding an imported file, or <code>null</code>.
   *
   * @throws IOException if writing to the bundle fails.
   */
  @SuppressWarnings("java:S107")
  private static @Nullable IOException expandStyleLine(WebBundle bundle, File file, File virtual, Writer out, String line, boolean minimize, long threshold,
      List<File> processed) throws IOException {
    Matcher matcher = CSS_URL.matcher(line);
    if (!matcher.find()) {
      out.write(line);
      return null;
    }
    if (line.trim().toLowerCase().startsWith("@import")) {
      return expandImport(bundle, file, virtual, out, line, matcher, minimize, threshold, processed);
    }
    out.write(rewriteStyleUrls(file, virtual, matcher, threshold));
    return null;
  }

  /**
   * Expands an import rule or writes it unchanged when the imported URL is not relative.
   */
  @SuppressWarnings("java:S107")
  private static @Nullable IOException expandImport(WebBundle bundle, File file, File virtual, Writer out, String line, Matcher matcher, boolean minimize,
      long threshold, List<File> processed) throws IOException {
    String path = unquote(matcher.group(1));
    if (!isRelative(path)) {
      writeLine(out, line);
      return null;
    }
    File imported = new File(file.getParentFile(), path);
    if (!imported.exists()) {
      writeMissingImport(out, line, path);
      return null;
    }
    return writeImportedStyle(bundle, imported, virtual, out, path, minimize, threshold, processed);
  }

  /**
   * Writes an expanded CSS import.
   */
  @SuppressWarnings("java:S107")
  private static @Nullable IOException writeImportedStyle(WebBundle bundle, File imported, File virtual, Writer out, String path, boolean minimize, long threshold,
      List<File> processed) throws IOException {
    if (minimize && path.endsWith("min.css")) {
      out.write("/*!nomin*/\n");
    }
    out.write("/* START import "+path+" */\n");
    bundle.addImport(imported);
    IOException exception = expandStylesTo(bundle, imported, virtual, out, processed, minimize, threshold);
    out.write("/* END import "+path+ " */\n");
    if (minimize && path.endsWith("min.css")) {
      out.write("/*!min*/\n");
    }
    return exception;
  }

  /**
   * Writes a CSS import that could not be resolved.
   */
  private static void writeMissingImport(Writer out, String line, String path) throws IOException {
    out.write("/* ERROR Unable to import */\n");
    LOGGER.warn("Unable to find referenced CSS file: {}", path);
    writeLine(out, line);
  }

  /**
   * Rewrites relative CSS URLs in a line.
   */
  private static String rewriteStyleUrls(File file, File virtual, Matcher matcher, long threshold) {
    matcher.reset();
    StringBuilder sb = new StringBuilder();
    while (matcher.find()) {
      String url = unquote(matcher.group(1));
      String query = "";
      int q = url.indexOf('?');
      if (q > 0) {
        query = url.substring(q);
        url = url.substring(0, q);
      }
      String location = "url("+getLocation(file, virtual, url, threshold)+query+")";
      matcher.appendReplacement(sb, Matcher.quoteReplacement(location));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  /**
   * Writes a line followed by a newline.
   */
  private static void writeLine(Writer out, String line) throws IOException {
    out.write(line);
    out.write('\n');
  }

  /**
   * Recalculates the specified path from the original file (source) to the new file (target).
   *
   * @param source The source file.
   * @param target The target file.
   * @param path   The location based on the source file.
   * @param threshold the file size threshold for images not to be included using data URIs
   *
   * @return The location based on the target file.
   */
  static String getLocation(File source, File target, String path, long threshold) {
    // Ignore data URIs, full URLs and absolute paths
    if (!isRelative(path)) return path;
    StringBuilder location = new StringBuilder();
    try {
      // Locate the referenced URL
      File ftarget = new File(source.getParentFile(), path);
      boolean isImage = path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".gif");
      if (isImage && ftarget.exists() && ftarget.length() < threshold) {
        // Replace short images by data uri
        location.append("data:image/").append(path.substring(path.lastIndexOf('.')+1)).append(";base64,");
        location.append(encodeBase64(ftarget));
      } else {
        String csource = ftarget.getCanonicalPath();
        // Check difference with bundle file
        String ctarget = target.getCanonicalPath();
        int x = common(csource, ctarget);
        // Start path to return to common base
        String rbundle = ctarget.substring(x);
        for (int i = 0; i < rbundle.length(); i++) {
          if (rbundle.charAt(i) == File.separatorChar) {
            location.append("../");
          }
        }
        // Continue with remaining path
        location.append(csource.substring(x).replace('\\', '/'));
      }
    } catch (IOException ex) {
      LOGGER.warn("Error while calculating location", ex);
    }
    return location.toString();
  }

  /**
   * Returns the number of common characters between the two strings.
   *
   * @param a The first string to compare.
   * @param b The second string.
   * @return The number of characters in common; 0 if none.
   */
  private static int common(String a, String b) {
    int i = 0;
    while (i < a.length() && i < b.length() && a.charAt(i) == b.charAt(i)) {
      i++;
    }
    return i;
  }

  /**
   * Returns the number of common characters between the two strings.
   *
   * @param url Removes the quotes.
   *
   * @return The number of characters in common; 0 if none.
   */
  private static String unquote(String url) {
    if (url.length() < 2) return url;
    char first = url.charAt(0);
    char last  = url.charAt(url.length()-1);
    if ((first == '\'' && last == '\'') || first == '"' && last == '"') // quoted
      return url.substring(1, url.length()-1);
    else // unquoted
      return url;
  }

  /**
   * Indicates whether the specified URL is relative.
   *
   * <p>It is NOT considered relative if starting with "http://", "https://", "data:", "/" or "<".
   *
   * @param url The URL to check
   * @return <code>true</code> if considered relative; <code>false</code> otherwise.
   */
  private static boolean isRelative(String url) {
    return !(url.startsWith("https://")
          || url.startsWith("http://")
          || url.startsWith("data:")
          || url.startsWith("/")
          || url.startsWith("<"));
  }

  /**
   * Returns a new buffered reader on a file using a UTF-8 decoder.
   *
   * @param f The file to read.
   *
   * @return the new buffered reader
   *
   * @throws IOException if the file could not be opened.
   */
  private static BufferedReader newBufferedReader(File f) throws IOException {
    return Files.newBufferedReader(f.toPath(), StandardCharsets.UTF_8);
  }

  /**
   * Encodes the file using the Java 8 Base64 method.
   *
   * @param file The file to encode
   * @return file content encoded with Base64
   *
   * @throws IOException If thrown while reading file content
   */
  private static String encodeBase64(File file) throws IOException {
    Base64.Encoder encoder = Base64.getEncoder();
    byte[] bytes = Files.readAllBytes(file.toPath());
    return encoder.encodeToString(bytes);
  }

}
