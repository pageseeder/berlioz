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
package org.pageseeder.berlioz.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class to compress the contents of a resource.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.8 - 8 October 2012
 * @since Berlioz 0.8.2
 */
public final class ResourceCompressor {

  /**
   * Displays debug information.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(ResourceCompressor.class);

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
   * @return The compressed content or an empty array if an error occurred.
   */
  public static byte[] compress(CharSequence content, Charset charset) {
    ByteArrayOutputStream os = new ByteArrayOutputStream(content.length());
    byte[] compressed = null;
    GZIPOutputStream compressor = null;
    try {
      compressor = new GZIPOutputStream(os);
      Writer w = new OutputStreamWriter(compressor, charset);
      w.write(content.toString());
      w.close();
      compressor.finish();
      compressed = os.toByteArray();
    } catch (IOException ex) {
      // If an error occurs, we return a empty array
      compressed = new byte[]{};
    } finally {
      // clean up and make sure the resources are released as soon as possible
      try {
        if (compressor != null) {
          compressor.close();
        }
      } catch (IOException ex) {
        LOGGER.error("Unable to close GZIPOutputStream stream", ex);
      }
      os = null;
      compressor = null;
    }
    return compressed;
  }

}
