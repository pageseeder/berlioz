package org.pageseeder.berlioz.xml;

/**
 * <p>An Object which implements this interface can be written as XML using a
 * {@link XmlWriter} instance.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.12.0
 * @since Berlioz 0.12.0
 */
public interface XmlWritable {

  /**
   * Writes the XML representation of the implementing instance using the specified
   * {@link XmlWriter}.
   *
   * @param xml The XML writer to use.
   *
   * @return The XML writer for easy chaining
   */
  XmlWriter toXml(XmlWriter xml);

}
