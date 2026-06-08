/*
 * Copyright 2026 Allette Systems (Australia)
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
package org.pageseeder.berlioz.content;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

final class ServiceSchemaTest {

  @Test
  void validate_acceptsNamespacesPatchAndHandler() throws Exception {
    validate(String.join("\n",
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>",
        "<service-config version=\"1.0\">",
        "  <namespace package=\"org.pageseeder.berlioz.generator\"/>",
        "  <namespace package=\"org.pageseeder.berlioz.generator\" prefix=\"gen\"/>",
        "  <response-code use=\"highest\" rule=\"first\"/>",
        "  <services group=\"api\">",
        "    <namespace package=\"org.pageseeder.berlioz.generator\" prefix=\"local\"/>",
        "    <service id=\"patch-direct\" method=\"patch\" flags=\"secure\" cache-control=\"no-cache\">",
        "      <url pattern=\"/api/{id}\"/>",
        "      <response-code use=\"highest\" rule=\"highest\"/>",
        "      <handler class=\"local:NoContent\">",
        "        <parameter name=\"mode\" value=\"direct\"/>",
        "      </handler>",
        "    </service>",
        "  </services>",
        "</service-config>"));
  }

  @Test
  void validate_acceptsMultipleGenerators() throws Exception {
    validate(String.join("\n",
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>",
        "<service-config version=\"1.0\">",
        "  <services group=\"default\">",
        "    <service id=\"home\" method=\"get\">",
        "      <url pattern=\"/home\"/>",
        "      <generator class=\"org.pageseeder.berlioz.generator.NoContent\" name=\"main\"/>",
        "      <generator class=\"org.pageseeder.berlioz.generator.GetServices\" target=\"debug\"/>",
        "    </service>",
        "  </services>",
        "</service-config>"));
  }

  @Test
  void validate_rejectsGeneratorMixedWithHandler() {
    Assertions.assertThrows(SAXException.class, () -> validate(String.join("\n",
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>",
        "<service-config version=\"1.0\">",
        "  <services group=\"default\">",
        "    <service id=\"bad\" method=\"get\">",
        "      <url pattern=\"/bad\"/>",
        "      <generator class=\"org.pageseeder.berlioz.generator.NoContent\"/>",
        "      <handler class=\"org.pageseeder.berlioz.generator.NoContent\"/>",
        "    </service>",
        "  </services>",
        "</service-config>")));
  }

  @Test
  void validate_rejectsUnknownHttpMethod() {
    Assertions.assertThrows(SAXException.class, () -> validate(String.join("\n",
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>",
        "<service-config version=\"1.0\">",
        "  <services group=\"default\">",
        "    <service id=\"bad\" method=\"trace\">",
        "      <url pattern=\"/bad\"/>",
        "      <generator class=\"org.pageseeder.berlioz.generator.NoContent\"/>",
        "    </service>",
        "  </services>",
        "</service-config>")));
  }

  private static void validate(String xml) throws Exception {
    Schema schema = schema();
    try (InputStream in = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
      schema.newValidator().validate(new StreamSource(in));
    }
  }

  private static Schema schema() throws Exception {
    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    try (InputStream in = ServiceSchemaTest.class.getResourceAsStream("/schema/services-1.0.xsd")) {
      Assertions.assertNotNull(in, "services-1.0.xsd should be available on the test classpath");
      return factory.newSchema(new StreamSource(in));
    }
  }
}
