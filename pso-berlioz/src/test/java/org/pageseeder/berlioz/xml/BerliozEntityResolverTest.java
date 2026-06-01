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
package org.pageseeder.berlioz.xml;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * A test case for the Berlioz entity resolver.
 */
class BerliozEntityResolverTest {

  /**
   * Tests the {BerliozEntityResolver#toFileName} method.
   */
  @Test
  void testToFileName() {
    // No public ID
    Assertions.assertNull(BerliozEntityResolver.toFileName(null));
    // Public ID does not match prefix
    Assertions.assertNull(BerliozEntityResolver.toFileName("X"));
    // Public ID matched prefix (empty)
    Assertions.assertNull(BerliozEntityResolver.toFileName("-//Weborganic//DTD::Berlioz "));
    // Public ID matched prefix (correct rules)
    Assertions.assertEquals("abc.7.dtd", BerliozEntityResolver.toFileName("-//Weborganic//DTD::Berlioz ABC.7//EN"));
    Assertions.assertEquals("a-bc-.-7.dtd", BerliozEntityResolver.toFileName("-//Weborganic//DTD::Berlioz A BC . 7//EN"));
    // Public ID matched prefix (known DTDs)
    Assertions.assertEquals("services-1.0.dtd", BerliozEntityResolver.toFileName("-//Weborganic//DTD::Berlioz Services 1.0//EN"));
    Assertions.assertEquals("web-access-1.0.dtd", BerliozEntityResolver.toFileName("-//Weborganic//DTD::Berlioz Web Access 1.0//EN"));
    Assertions.assertEquals("properties-1.0.dtd", BerliozEntityResolver.toFileName("-//Weborganic//DTD::Berlioz Properties 1.0//EN"));
    // Alias public IDs
    Assertions.assertEquals("web-access-1.0.dtd", BerliozEntityResolver.toFileName("-//Berlioz//DTD::Web Access 1.0//EN"));
    Assertions.assertEquals("properties-1.0.dtd", BerliozEntityResolver.toFileName("-//Berlioz//DTD::Properties 1.0//EN"));
  }
}
