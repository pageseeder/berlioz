/*
 * Copyright (c) 1999-2020 Allette Systems Pty Ltd
 */
package org.pageseeder.berlioz.output;

import org.pageseeder.berlioz.Beta;

/**
 * The output format produced by an {@link OutputWriter}.
 *
 * <p>Each constant carries the corresponding IANA media type string, accessible via
 * {@link #getMediaType()}.</p>
 *
 * @author Christophe Lauret
 *
 * @version 0.13.2
 * @since 0.13.0
 */
@Beta
public enum OutputType {

  /**
   * XML: <code>application/xml</code>
   */
  XML("application/xml"),

  /**
   * JSON: <code>application/json</code>
   */
  JSON("application/json"),

  /**
   * Raw bytes: <code>application/octet-stream</code>
   *
   * <p>This is the default media type for raw output. A {@code RawGenerator} may override it
   * with a more specific type via {@code Response.header("Content-Type", "image/png")}.</p>
   */
  RAW("application/octet-stream");

  /**
   * The mediatype for the format
   */
  private final String mediaType;

  OutputType(String mediaType) {
    this.mediaType = mediaType;
  }

  /**
   * The mediatype for the format.
   *
   * @return The media type for the format.
   */
  public String getMediaType() {
    return this.mediaType;
  }
}
