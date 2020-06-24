package org.pageseeder.berlioz.xml;

import java.io.Writer;

/**
 *
 * @version Berlioz 0.12.0
 * @since Berlioz 0.12.0
 */
public class Xml {

  private Xml() {
  }

  /**
   * Always return a XML Writer.
   *
   * @param writer The writer receiving the XML output.
   *
   * @return The corresponding XML writer to use.
   */
  public static XmlWriter newWriter(Writer writer) {
    return new XmlAppendable<Writer>(writer);
  }

}
