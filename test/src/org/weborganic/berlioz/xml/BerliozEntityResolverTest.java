package org.weborganic.berlioz.xml;

import junit.framework.TestCase;

/**
 * A test case for the Berlioz entity resolver.
 * 
 * @author Christophe Lauret (Weborganic)
 * @version 29 October 2009
 */
public class BerliozEntityResolverTest extends TestCase {

  /**
   * Tests the {BerliozEntityResolver#toFileName} method.
   */
  public void testToFileName() {
    // No public ID
    assertNull(BerliozEntityResolver.toFileName(null));
    // Public ID does not match prefix
    assertNull(BerliozEntityResolver.toFileName("X"));
    // Public ID matched prefix (empty)
    assertNull(BerliozEntityResolver.toFileName("-//Weborganic//DTD::Berlioz "));
    // Public ID matched prefix (correct rules)
    assertEquals("abc.7.dtd", BerliozEntityResolver.toFileName("-//Weborganic//DTD::Berlioz ABC.7//EN"));
    assertEquals("a-bc-.-7.dtd", BerliozEntityResolver.toFileName("-//Weborganic//DTD::Berlioz A BC . 7//EN"));
    // Public ID matched prefix (known DTDs)
    assertEquals("services-1.0.dtd",   BerliozEntityResolver.toFileName("-//Weborganic//DTD::Berlioz Services 1.0//EN"));
    assertEquals("web-access-1.0.dtd", BerliozEntityResolver.toFileName("-//Weborganic//DTD::Berlioz Web Access 1.0//EN"));
    assertEquals("properties-1.0.dtd", BerliozEntityResolver.toFileName("-//Weborganic//DTD::Berlioz Properties 1.0//EN"));
    // Alias public IDs
    assertEquals("web-access-1.0.dtd", BerliozEntityResolver.toFileName("-//Berlioz//DTD::Web Access 1.0//EN"));
    assertEquals("properties-1.0.dtd", BerliozEntityResolver.toFileName("-//Berlioz//DTD::Properties 1.0//EN"));
  }
}
