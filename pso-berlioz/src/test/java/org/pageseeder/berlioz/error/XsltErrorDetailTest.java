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
package org.pageseeder.berlioz.error;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.util.CollectedError;
import org.pageseeder.berlioz.util.CollectedError.Level;
import org.pageseeder.berlioz.xml.XmlStringBuilder;
import org.pageseeder.berlioz.xslt.XsltErrorCollector;
import org.pageseeder.berlioz.xslt.XsltExceptionWrapper;
import org.slf4j.LoggerFactory;

import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import java.util.List;

class XsltErrorDetailTest {

  @Test
  void name_isXsltError() {
    XsltErrorDetail detail = XsltErrorDetail.of(new TransformerException("boom"));
    Assertions.assertEquals("xslt-error", detail.name());
  }

  @Test
  void of_configurationException_setsKindStatic() {
    XsltErrorDetail detail = XsltErrorDetail.of(new TransformerConfigurationException("bad stylesheet"));
    XmlStringBuilder out = new XmlStringBuilder();
    detail.toXml(out);
    Assertions.assertTrue(out.toString().contains("kind=\"static\""), out.toString());
  }

  @Test
  void of_plainTransformerException_setsKindDynamic() {
    XsltErrorDetail detail = XsltErrorDetail.of(new TransformerException("type error at runtime"));
    XmlStringBuilder out = new XmlStringBuilder();
    detail.toXml(out);
    Assertions.assertTrue(out.toString().contains("kind=\"dynamic\""), out.toString());
  }

  @Test
  void of_neverIncludesStackTrace() {
    TransformerException ex = new TransformerException("boom", new RuntimeException("underlying cause"));
    XsltErrorDetail detail = XsltErrorDetail.of(ex);
    XmlStringBuilder out = new XmlStringBuilder();
    detail.toXml(out);
    String xml = out.toString();
    Assertions.assertTrue(xml.contains("<message>boom</message>"), xml);
    Assertions.assertFalse(xml.contains("stack-trace"), xml);
    Assertions.assertFalse(xml.contains("underlying cause"), xml);
  }

  @Test
  void of_extractsLocationFromLocator() {
    SourceLocator locator = new SourceLocator() {
      @Override public String getPublicId() { return null; }
      @Override public String getSystemId() { return "/WEB-INF/xslt/html/default.xsl"; }
      @Override public int getLineNumber() { return 42; }
      @Override public int getColumnNumber() { return 7; }
    };
    TransformerException ex = new TransformerException("bad select expression", locator);

    XsltErrorDetail detail = XsltErrorDetail.of(ex);
    XmlStringBuilder out = new XmlStringBuilder();
    detail.toXml(out);
    String xml = out.toString();

    Assertions.assertTrue(xml.contains("<location"), xml);
    Assertions.assertTrue(xml.contains("line=\"42\""), xml);
    Assertions.assertTrue(xml.contains("column=\"7\""), xml);
    Assertions.assertTrue(xml.contains("system-id=\"/xslt/html/default.xsl\""), xml);
  }

  @Test
  void of_withoutLocator_omitsLocation() {
    XsltErrorDetail detail = XsltErrorDetail.of(new TransformerException("boom"));
    XmlStringBuilder out = new XmlStringBuilder();
    detail.toXml(out);
    Assertions.assertFalse(out.toString().contains("<location"), out.toString());
  }

  @Test
  void of_withCollectedErrors_writesOneEntryPerItemWithLevelAndMessage() {
    List<CollectedError<TransformerException>> collected = List.of(
        new CollectedError<>(Level.ERROR, new TransformerException("first failure")),
        new CollectedError<>(Level.WARNING, new TransformerException("second failure")));

    XsltErrorDetail detail = XsltErrorDetail.of(new TransformerConfigurationException("bad stylesheet"), collected);
    XmlStringBuilder out = new XmlStringBuilder();
    detail.toXml(out);
    String xml = out.toString();

    Assertions.assertTrue(xml.contains("<collected>"), xml);
    Assertions.assertTrue(xml.contains("<error level=\"error\">"), xml);
    Assertions.assertTrue(xml.contains("<error level=\"warning\">"), xml);
    Assertions.assertTrue(xml.contains("<message>first failure</message>"), xml);
    Assertions.assertTrue(xml.contains("<message>second failure</message>"), xml);
  }

  @Test
  void of_withNoCollectedErrors_omitsCollectedElement() {
    XsltErrorDetail detail = XsltErrorDetail.of(new TransformerException("boom"), List.of());
    XmlStringBuilder out = new XmlStringBuilder();
    detail.toXml(out);
    Assertions.assertFalse(out.toString().contains("<collected"), out.toString());
  }

  @Test
  void of_wrapper_unwrapsAndUsesCollectorErrors() {
    XsltErrorCollector collector = new XsltErrorCollector(LoggerFactory.getLogger(XsltErrorDetailTest.class));
    collector.collectQuietly(Level.ERROR, new TransformerException("first failure"));
    XsltExceptionWrapper wrapper = new XsltExceptionWrapper(
        new TransformerConfigurationException("bad stylesheet"), collector);

    XsltErrorDetail detail = XsltErrorDetail.of(wrapper);
    XmlStringBuilder out = new XmlStringBuilder();
    detail.toXml(out);
    String xml = out.toString();

    Assertions.assertTrue(xml.contains("kind=\"static\""), xml);
    Assertions.assertTrue(xml.contains("<message>bad stylesheet</message>"), xml);
    Assertions.assertTrue(xml.contains("<error level=\"error\">"), xml);
    Assertions.assertTrue(xml.contains("first failure"), xml);
  }

  @Test
  void of_transformerExceptionThatIsAWrapper_alsoUnwraps() {
    XsltErrorCollector collector = new XsltErrorCollector(LoggerFactory.getLogger(XsltErrorDetailTest.class));
    collector.collectQuietly(Level.WARNING, new TransformerException("noted"));
    TransformerException wrapper = new XsltExceptionWrapper(
        new TransformerConfigurationException("bad stylesheet"), collector);

    // of(TransformerException) — not the XsltExceptionWrapper-typed overload — must still unwrap
    XsltErrorDetail detail = XsltErrorDetail.of(wrapper);
    XmlStringBuilder out = new XmlStringBuilder();
    detail.toXml(out);
    Assertions.assertTrue(out.toString().contains("<error level=\"warning\">"), out.toString());
  }

  @Test
  void toJson_usesCamelCaseMemberNames() {
    XsltErrorDetail detail = XsltErrorDetail.of(new TransformerConfigurationException("bad stylesheet"));
    String json = ProblemDetails.of(ContentStatus.SERVICE_UNAVAILABLE).extension(detail).toJson();
    Assertions.assertTrue(json.contains("\"xsltError\":{"), json);
    Assertions.assertTrue(json.contains("\"kind\":\"static\""), json);
  }

  @Test
  void of_extractsSaxonErrorCode_whenSaxonExceptionOnClasspath() throws Exception {
    // Saxon-HE is a testRuntimeOnly dependency (pso-berlioz has no compile-time dependency on it),
    // so this exercises the same reflective lookup XsltErrorDetail uses in production, rather than
    // importing net.sf.saxon.trans.XPathException directly.
    Class<?> xpathExceptionClass = Class.forName("net.sf.saxon.trans.XPathException");
    Object saxonEx = xpathExceptionClass.getConstructor(String.class).newInstance("dynamic type error");
    xpathExceptionClass.getMethod("setErrorCode", String.class).invoke(saxonEx, "XPTY0004");

    XsltErrorDetail detail = XsltErrorDetail.of((TransformerException) saxonEx);
    XmlStringBuilder out = new XmlStringBuilder();
    detail.toXml(out);

    Assertions.assertTrue(out.toString().contains("code=\"XPTY0004\""), out.toString());
  }

  @Test
  void of_rejectsNullException() {
    Assertions.assertThrows(NullPointerException.class, () -> XsltErrorDetail.of((TransformerException) null));
  }

}
