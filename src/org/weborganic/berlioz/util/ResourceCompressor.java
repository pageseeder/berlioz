/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.zip.GZIPOutputStream;

/**
 * A utility class to compress the contents of a resource.
 * 
 * @author Christophe Lauret
 * @version 31 May 2010
 */
public final class ResourceCompressor {

  /**
   * Utility class.
   */
  private ResourceCompressor() {
  }

  /**
   * Compresses the specified content.
   * 
   * @param content The content to compress.
   * @param charset The Character set to use to encode the char sequence.
   * 
   * @return The compressed content or <code>null</code> if an error occurred.
   */
  public static byte[] compress(CharSequence content, Charset charset) {
    ByteArrayOutputStream os = new ByteArrayOutputStream(content.length());
    byte[] compressed = null;
    GZIPOutputStream compressor = null;
    try {
      compressor = new GZIPOutputStream(os);
      Writer w = new OutputStreamWriter(compressor, charset); 
      w.write(content.toString());
      w.flush();
      compressor.close();
      compressed = os.toByteArray();
    } catch (IOException ex) {
      compressed = new byte[]{};
    }
    return compressed;
  }

}
