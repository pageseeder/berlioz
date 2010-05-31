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
   * @param content the content to compress.
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