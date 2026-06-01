package org.pageseeder.berlioz.xml;

import org.pageseeder.berlioz.util.WriteFailureException;

import java.io.IOException;

/**
 * Wraps an IO exception occurring while writing the Xml.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.12.0
 * @since Berlioz 0.12.0
 */
public final class XmlWriteFailureException extends WriteFailureException {

  /** As per requirement for Serializable */
  private static final long serialVersionUID = 5845519205395989586L;

  public XmlWriteFailureException(IOException cause) {
    super("Unable to write to underlying XML output", cause);
  }

}
