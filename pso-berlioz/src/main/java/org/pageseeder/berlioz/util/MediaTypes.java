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

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.GlobalSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.util.Properties;

/**
 * A bunch of utility functions for media types
 *
 * @author Christophe Lauret
 *
 * @version 0.13.0
 * @since 0.6
 */
public final class MediaTypes {

  /**
   * Displays debug information.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(MediaTypes.class);

  /**
   * The MIME properties mapping file extensions to MIME types.
   */
  private static final Properties MEDIA_TYPES = new Properties();

  /** Utility classes need no constructor. */
  private MediaTypes() {
  }

  /**
   * Returns the Media Type for the given file.
   *
   * <p>The media type is only based on the file extension.
   *
   * <p>This method uses the 'mime.properties' resource file from the classpath which maps
   * file extensions to the corresponding Media Type. This file is loaded only once.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2046">MIME Part Two: Media Types</a>
   * @see <a href="http://tools.ietf.org/html/rfc3023">XML Media Types</a>
   *
   * @param f The file.
   * @return the corresponding Media Type or <code>null</code> if not found.
   */
  public static @Nullable String getMediaType(File f) {
    // Load if empty
    if (MEDIA_TYPES.isEmpty()) {
      loadMediaTypes();
    }
    // Look up extension in properties file
    String name = f.getName();
    int dot = name.lastIndexOf(".");
    if (dot >= 0) return MEDIA_TYPES.getProperty(name.substring(dot+1));
    else
      return null;
  }

  // Private helpers ------------------------------------------------------------------------------

  /**
   * Loads the MIME properties from the Berlioz jar or the local "config/mime.properties"
   */
  private static synchronized void loadMediaTypes() {
    try (InputStream in = getMediaTypesInputStream()) {
      if (in != null) {
        MEDIA_TYPES.load(in);
      }
    } catch (IOException ex) {
      LOGGER.warn("Unable to load MIME properties", ex);
    }
  }

  private static @Nullable InputStream getMediaTypesInputStream() throws IOException {
    File file = new File(GlobalSettings.getWebInf(), "config/mediatypes.properties");
    if (file.exists()) {
      LOGGER.info("Loading MIME properties from {}", file.getAbsolutePath());
      return Files.newInputStream(file.toPath());
    } else {
      LOGGER.info("Loading MIME properties from Berlioz JAR, could not find {}", file.getAbsolutePath());
      return MediaTypes.class.getResourceAsStream("/mime.properties");
    }
  }

}
