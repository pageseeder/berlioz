/*
 * Copyright (c) 1999-2020 Allette Systems Pty Ltd
 */
package org.pageseeder.berlioz.output;

import org.pageseeder.berlioz.Beta;

/**
 * Predefined the output for the universal printers.
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
  JSON("application/json");

  /**
   * The mediatype for the format
   */
  private final String mediaType;

  OutputType(String mediaType) {
    this.mediaType = mediaType;
  }

  /**
   * The mediatype for the format.
   */
  public String getMediaType() {
    return this.mediaType;
  }
}
